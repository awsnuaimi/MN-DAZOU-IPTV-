package com.dazou.iptvplayer

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dazou.iptvplayer.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null
    
    private var currentHost = ""
    private var currentUser = ""
    private var currentPass = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // التحقق من وجود بيانات محفوظة للدخول التلقائي
        val prefs = getSharedPreferences("IPTV_Prefs", Context.MODE_PRIVATE)
        val savedHost = prefs.getString("xtream_host", "")
        val savedUser = prefs.getString("xtream_user", "")
        val savedPass = prefs.getString("xtream_pass", "")

        if (!savedHost.isNullOrEmpty() && !savedUser.isNullOrEmpty() && !savedPass.isNullOrEmpty()) {
            binding.loginLayout.visibility = View.GONE
            Toast.makeText(this, "جاري الدخول التلقائي...", Toast.LENGTH_SHORT).show()
            connectToXtream(savedHost, savedUser, savedPass)
        }

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val isXtream = (checkedId == R.id.rbXtream)
            binding.etUser.visibility = if (isXtream) View.VISIBLE else View.GONE
            binding.etPass.visibility = if (isXtream) View.VISIBLE else View.GONE
            binding.etUrl.hint = if (isXtream) "رابط الخادم (مثال: http://host:port)" else "أدخل رابط M3U"
        }

        binding.btnConnect.setOnClickListener {
            if (binding.rbXtream.isChecked) {
                val host = binding.etUrl.text.toString()
                val user = binding.etUser.text.toString()
                val pass = binding.etPass.text.toString()
                
                if (host.isNotEmpty() && user.isNotEmpty() && pass.isNotEmpty()) {
                    binding.btnConnect.text = "جاري الاتصال..."
                    binding.btnConnect.isEnabled = false
                    connectToXtream(host, user, pass)
                } else {
                    Toast.makeText(this, "يرجى إدخال جميع البيانات", Toast.LENGTH_SHORT).show()
                }
            } else {
                val url = binding.etUrl.text.toString()
                if (url.isNotEmpty()) setupPlayer(url)
            }
        }

        // أزرار الشاشة الرئيسية
        binding.btnLiveTv.setOnClickListener { fetchCategories("live") }
        binding.btnMovies.setOnClickListener { Toast.makeText(this, "سيتم برمجة الأفلام في التحديث القادم!", Toast.LENGTH_SHORT).show() }
        binding.btnSeries.setOnClickListener { Toast.makeText(this, "سيتم برمجة المسلسلات في التحديث القادم!", Toast.LENGTH_SHORT).show() }
        
        binding.btnLogoutHome.setOnClickListener { logout() }
        binding.btnBackToHome.setOnClickListener { 
            binding.categoriesLayout.visibility = View.GONE
            binding.homeLayout.visibility = View.VISIBLE
        }
        binding.btnBackToCategories.setOnClickListener {
            binding.channelsLayout.visibility = View.GONE
            binding.categoriesLayout.visibility = View.VISIBLE
        }

        binding.btnLogoutPlayer.setOnClickListener {
            player?.release()
            player = null
            binding.playerContainer.visibility = View.GONE
            binding.channelsLayout.visibility = View.VISIBLE
        }
    }

    private fun connectToXtream(host: String, user: String, pass: String) {
        val baseUrl = if (host.endsWith("/")) host else "$host/"

        try {
            val retrofit = Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create()).build()
            val apiService = retrofit.create(XtreamApiService::class.java)

            apiService.authenticate(user, pass).enqueue(object : Callback<XtreamAuthResponse> {
                override fun onResponse(call: Call<XtreamAuthResponse>, response: Response<XtreamAuthResponse>) {
                    binding.btnConnect.text = "اتصال"
                    binding.btnConnect.isEnabled = true

                    if (response.isSuccessful && response.body()?.userInfo != null) {
                        val status = response.body()?.userInfo?.status
                        if (status == "Active" || status == "active") {
                            currentHost = baseUrl
                            currentUser = user
                            currentPass = pass
                            
                            // حفظ البيانات للدخول التلقائي القادم
                            val prefs = getSharedPreferences("IPTV_Prefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putString("xtream_host", host)
                                .putString("xtream_user", user)
                                .putString("xtream_pass", pass)
                                .apply()

                            // إظهار الشاشة الرئيسية
                            binding.loginLayout.visibility = View.GONE
                            binding.homeLayout.visibility = View.VISIBLE
                        } else {
                            Toast.makeText(this@MainActivity, "الحساب متوقف", Toast.LENGTH_LONG).show()
                            logout()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "بيانات غير صحيحة", Toast.LENGTH_LONG).show()
                        logout()
                    }
                }

                override fun onFailure(call: Call<XtreamAuthResponse>, t: Throwable) {
                    binding.btnConnect.text = "اتصال"
                    binding.btnConnect.isEnabled = true
                    Toast.makeText(this@MainActivity, "فشل الاتصال", Toast.LENGTH_LONG).show()
                    logout()
                }
            })
        } catch (e: Exception) {
            binding.btnConnect.text = "اتصال"
            binding.btnConnect.isEnabled = true
            logout()
        }
    }

    private fun fetchCategories(type: String) {
        binding.homeLayout.visibility = View.GONE
        binding.categoriesLayout.visibility = View.VISIBLE
        binding.tvCategoryTitle.text = "باقات البث المباشر"
        
        val retrofit = Retrofit.Builder().baseUrl(currentHost).addConverterFactory(GsonConverterFactory.create()).build()
        val apiService = retrofit.create(XtreamApiService::class.java)
        
        apiService.getLiveCategories(currentUser, currentPass).enqueue(object : Callback<List<XtreamCategory>> {
            override fun onResponse(call: Call<List<XtreamCategory>>, response: Response<List<XtreamCategory>>) {
                if (response.isSuccessful && response.body() != null) {
                    val categories = response.body()!!
                    val categoryNames = categories.map { it.categoryName }
                    val adapter = ArrayAdapter(this@MainActivity, R.layout.item_category, R.id.tvCategoryName, categoryNames)
                    binding.categoriesList.adapter = adapter
                    
                    binding.categoriesList.setOnItemClickListener { _, _, position, _ ->
                        fetchStreams(categories[position].categoryId)
                    }
                }
            }
            override fun onFailure(call: Call<List<XtreamCategory>>, t: Throwable) {}
        })
    }

    private fun fetchStreams(categoryId: String) {
        binding.categoriesLayout.visibility = View.GONE
        binding.channelsLayout.visibility = View.VISIBLE
        Toast.makeText(this, "جاري تحميل القنوات...", Toast.LENGTH_SHORT).show()
        
        val retrofit = Retrofit.Builder().baseUrl(currentHost).addConverterFactory(GsonConverterFactory.create()).build()
        val apiService = retrofit.create(XtreamApiService::class.java)
        
        apiService.getLiveStreams(currentUser, currentPass, categoryId = categoryId).enqueue(object : Callback<List<XtreamStream>> {
            override fun onResponse(call: Call<List<XtreamStream>>, response: Response<List<XtreamStream>>) {
                if (response.isSuccessful && response.body() != null) {
                    val streams = response.body()!!
                    val streamNames = streams.map { it.name }
                    val adapter = ArrayAdapter(this@MainActivity, R.layout.item_category, R.id.tvCategoryName, streamNames)
                    binding.channelsList.adapter = adapter
                    
                    binding.channelsList.setOnItemClickListener { _, _, position, _ ->
                        val selectedStream = streams[position]
                        val streamUrl = "${currentHost}${currentUser}/${currentPass}/${selectedStream.streamId}"
                        setupPlayer(streamUrl)
                    }
                }
            }
            override fun onFailure(call: Call<List<XtreamStream>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "خطأ في تحميل القنوات", Toast.LENGTH_SHORT).show()
                binding.channelsLayout.visibility = View.GONE
                binding.categoriesLayout.visibility = View.VISIBLE
            }
        })
    }

    private fun setupPlayer(url: String) {
        binding.channelsLayout.visibility = View.GONE
        binding.playerContainer.visibility = View.VISIBLE
        
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
        player?.setMediaItem(MediaItem.fromUri(url))
        player?.prepare()
        player?.playWhenReady = true
    }

    private fun logout() {
        val prefs = getSharedPreferences("IPTV_Prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply() // مسح البيانات المحفوظة
        
        binding.homeLayout.visibility = View.GONE
        binding.categoriesLayout.visibility = View.GONE
        binding.channelsLayout.visibility = View.GONE
        binding.playerContainer.visibility = View.GONE
        binding.loginLayout.visibility = View.VISIBLE
        
        player?.release()
        player = null
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }
}
