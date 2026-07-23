package com.dazou.iptvplayer.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class PlayerManager(context: Context) {

    private val userAgent = "Mozilla/5.0 (Linux; Android 10; DAZOU-IPTV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    // ✅ عميل OkHttp مشترك — بيعيد استخدام نفس اتصالات TCP/TLS المفتوحة عبر
    // كل طلبات التشغيل، بدل ما تفتح اتصال جديد بالكامل كل تبديل قناة (زي
    // ما كانت الطبقة الأساسية المدمجة بأندرويد تسوي). هالشي بيسرّع تبديل
    // القنوات ملموسًا، خصوصًا مع سيرفرات HTTPS يلي فيها كلفة "مصافحة" عالية.
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        .setUserAgent(userAgent)

    // ✅ تخزين مؤقت محلي (بحد أقصى 300 ميجابايت) — يسرّع إعادة تشغيل نفس الفيلم/الحلقة
    // أو الرجوع لقناة اتفتحت قبل قليل، بدل ما يعيد التحميل من الصفر كل مرة.
    // يُخزَّن بمجلد الكاش الخاص بالتطبيق، فالنظام قادر يحرره تلقائيًا لو المساحة قلّت.
    private val cache: SimpleCache by lazy {
        val cacheDir = File(context.cacheDir, "media_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(300L * 1024 * 1024)
        val databaseProvider = StandaloneDatabaseProvider(context)
        SimpleCache(cacheDir, evictor, databaseProvider)
    }

    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(httpDataSourceFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    private val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(cacheDataSourceFactory)

    // ✅ إعدادات Buffer مسرّعة (كانت 15000/50000/2500/5000) — قللناها عشان
    // تبديل قنوات البث المباشر يحس أسرع وأخف، مع الحفاظ على قدر كافٍ من
    // التخزين المؤقت يمنع التقطيع المتكرر. القيم الجديدة لسا آمنة للأفلام
    // والمسلسلات (يلي أقل حساسية لسرعة البدء من البث المباشر).
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            8_000,
            30_000,
            1_500,
            3_000
        )
        .build()

    // ✅ يقدّر سرعة الاتصال الأولية بذكاء حسب نوع الشبكة الفعلي (واي فاي/بيانات خلوية/إيثرنت)
    // بدل ما يبلّش ExoPlayer بتخمين عام موحّد لكل الأجهزة — بيساعد ياخد قرار جودة أقرب للصح
    // من أول ثانية تشغيل، بدل ما يبلّش بجودة واطية أو عالية غلط ويصحح بعدين.
    private fun buildBandwidthMeter(context: Context): DefaultBandwidthMeter {
        val builder = DefaultBandwidthMeter.Builder(context)
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val caps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            } else null

            val initialEstimateBps = when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> 20_000_000L
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> 8_000_000L
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> 2_500_000L
                else -> 4_000_000L // قيمة وسطية آمنة لو ما قدرنا نحدد نوع الشبكة
            }
            builder.setInitialBitrateEstimate(initialEstimateBps)
        } catch (_: Exception) {
            // لو صار أي خطأ بقراءة حالة الشبكة، منسيب ExoPlayer يستخدم تقديره الافتراضي
        }
        return builder.build()
    }

    private val bandwidthMeter = buildBandwidthMeter(context)

    // ✅ سلوك تبديل الجودة: bandwidthFraction أوطى (0.75 بدل 1.0 الافتراضي) يعني الپلاير
    // بياخد قرار الجودة بناءً على 75% بس من السرعة المقاسة (احتياط)، فبيقلل احتمال التقطيع
    // لو الشبكة تذبذبت فجأة. minDurationForQualityIncreaseMs أعلى شوي يمنع "قفز" الجودة
    // لفوق بسرعة زايدة قبل ما نتأكد إنه الاتصال مستقر فعلاً.
    private val trackSelectionFactory = AdaptiveTrackSelection.Factory(
        /* minDurationForQualityIncreaseMs= */ 12_000,
        /* maxDurationForQualityDecreaseMs= */ 20_000,
        /* minDurationToRetainAfterDiscardMs= */ 30_000,
        /* bandwidthFraction= */ 0.75f
    )

    private val trackSelector = DefaultTrackSelector(context, trackSelectionFactory).apply {
        setParameters(
            buildUponParameters()
                .setSelectUndeterminedTextLanguage(true)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowVideoNonSeamlessAdaptiveness(true)
        )
    }

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .setLoadControl(loadControl)
        .setTrackSelector(trackSelector)
        .setBandwidthMeter(bandwidthMeter)
        .build()

    var currentStreamUrl: String? = null
        private set

    private var currentName: String = ""
    private var currentType: String = ""

    val isPlaying: Boolean
        get() = player.isPlaying

    private var retryCount = 0
    private val maxRetries = 3
    private val retryHandler = Handler(Looper.getMainLooper())

    // ✅ true لما كل محاولات إعادة الاتصال التلقائية تفشل — الواجهة بتستخدمه
    // لتظهير زر "إعادة المحاولة الآن" اليدوي بدل ما يضل المستخدم عالق.
    var retriesExhausted = false
        private set

    /** ✅ يمثّل خيار مسار صوت واحد ممكن نعرضه للمستخدم يختار منه */
    data class AudioTrackOption(
        val mediaTrackGroup: TrackGroup,
        val trackIndex: Int,
        val label: String
    )

    fun play(
        url: String,
        name: String = "",
        type: String = "",
        startPositionMs: Long = 0L
    ) {
        currentStreamUrl = url
        currentName = name
        currentType = type
        retryCount = 0
        retriesExhausted = false

        val itemBuilder = MediaItem.Builder().setUri(Uri.parse(url))

        // ✅ إعدادات خاصة بالبث المباشر فقط: تسمح للپلاير يسرّع/يبطّئ التشغيل بشكل
        // غير محسوس (بين 0.97x و 1.03x) عشان يحافظ على مسافة ثابتة عن "الحافة المباشرة"
        // (targetOffsetMs) — هاد بيقلل احتمال التقطيع لما الشبكة تتردد شوي، بدون ما
        // يأثر هالشي على الأفلام والمسلسلات (اللي مش بحاجة هالسلوك أصلاً).
        if (type == "live") {
            itemBuilder.setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(12_000)
                    .setMinPlaybackSpeed(0.97f)
                    .setMaxPlaybackSpeed(1.03f)
                    .build()
            )
        }

        val mediaItem = itemBuilder.build()
        player.setMediaItem(mediaItem)
        player.prepare()
        if (startPositionMs > 0L) {
            player.seekTo(startPositionMs)
        }
        player.play()
    }

    /** ✅ تقديم 10 ثواني (أو أي مدة محددة) — بيتوقف عند نهاية المحتوى بدل ما يطلع بره الحدود */
    fun seekForward(ms: Long = 10_000L) {
        val duration = player.duration
        val target = player.currentPosition + ms
        player.seekTo(if (duration > 0) target.coerceAtMost(duration) else target)
    }

    /** ✅ إرجاع 10 ثواني (أو أي مدة محددة) — بيتوقف عند الصفر بدل ما ياخد رقم سالب */
    fun seekBackward(ms: Long = 10_000L) {
        val target = (player.currentPosition - ms).coerceAtLeast(0L)
        player.seekTo(target)
    }

    /** ✅ يرجّع كل مسارات الصوت المتاحة بالمحتوى الحالي (لغات متعددة مثلاً) */
    fun getAvailableAudioTracks(): List<AudioTrackOption> {
        val options = mutableListOf<AudioTrackOption>()
        val groups = player.currentTracks.groups
        for (group in groups) {
            if (group.type != C.TRACK_TYPE_AUDIO) continue
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val label = when {
                    !format.language.isNullOrBlank() -> format.language!!.uppercase()
                    !format.label.isNullOrBlank() -> format.label!!
                    else -> "Track ${options.size + 1}"
                }
                options.add(AudioTrackOption(group.mediaTrackGroup, i, label))
            }
        }
        return options
    }

    /** ✅ يفعّل مسار صوت محدد يدويًا (بدل الاختيار التلقائي) */
    fun selectAudioTrack(option: AudioTrackOption) {
        val override = TrackSelectionOverride(option.mediaTrackGroup, option.trackIndex)
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(override)
            .build()
    }

    /** ✅ يرجّع اختيار مسار الصوت للوضع التلقائي (الافتراضي) */
    fun resetAudioTrackToAuto() {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .build()
    }

    /** ✅ يرجّع تسمية الجودة الحالية (1080p/720p/...) بناءً على الفيديو الفعلي الشغال، أو null لو مش معروف بعد */
    fun getCurrentQualityLabel(): String? {
        val height = player.videoFormat?.height ?: return null
        if (height <= 0) return null
        return "${height}p"
    }

    fun retryCurrent(onExhausted: () -> Unit) {
        val url = currentStreamUrl ?: return
        if (retryCount >= maxRetries) {
            retriesExhausted = true
            onExhausted()
            return
        }
        retryCount++
        retryHandler.postDelayed({
            play(url, currentName, currentType)
        }, 2000L * retryCount)
    }

    /** ✅ إعادة محاولة فورية بضغطة المستخدم (بدل الانتظار/الاستسلام بعد المحاولات التلقائية) */
    fun manualRetry() {
        val url = currentStreamUrl ?: return
        retryCount = 0
        retriesExhausted = false
        play(url, currentName, currentType)
    }

    fun resetRetry() {
        retryCount = 0
        retriesExhausted = false
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        player.play()
    }

    fun release() {
        retryHandler.removeCallbacksAndMessages(null)
        player.release()
        cache.release()
    }
}