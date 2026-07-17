package com.dazou.iptvplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.dazou.iptvplayer.model.XtreamChannel

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // بيانات حية للمشغل
    val activeChannel = MutableLiveData<XtreamChannel?>()
    val isFullscreen = MutableLiveData(false)

    // بيانات للشاشات الجانبية
    val categories = MutableLiveData<List<XtreamCategory>>()
    val channels = MutableLiveData<List<XtreamChannel>>()

    fun playChannel(channel: XtreamChannel) {
        activeChannel.value = channel
        // حفظ القناة الأخيرة في SharedPrefs
        getApplication<Application>().getSharedPreferences("app_data", 0)
            .edit().putInt("last_channel_id", channel.streamId).apply()
    }
    
    fun toggleFullscreen() {
        isFullscreen.value = !(isFullscreen.value ?: false)
    }
}