package com.dazou.iptvplayer

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
    
    // متغيرات لحفظ بيانات السيرفر
    private var currentHost = ""
    private var currentUser = ""
    private var currentPass = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("IPTV_Prefs", MODE_PRIVATE)
        
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
                if (url.isNotEmpty()) {
                    setupPlayer(url)
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            player?.release()
            player = null
            binding.playerContainer.visibility = View.GONE
            binding.categoriesLayout.visibility = View.GONE
            binding.loginLayout.visibility = View.VISIBLE
        }
    }

    private fun connectToXtream(host: String, user: String, pass: String) {
        val baseUrl = if (host.endsWith("/")) host else "$host/"

        try {
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(XtreamApiService::class.java)

            apiService.authenticate(user, pass).enqueue(object : Callback<XtreamAuthResponse> {
                override fun onResponse(call: Call<XtreamAuthResponse>, response: Response<XtreamAuthResponse>) {
                    binding.btnConnect.text = "اتصال"
                    binding.btnConnect.isEnabled = true

                    if (response.isSuccessful && response.body()?.userInfo != null) {
                        val status = response.body()?.userInfo?.status
                        if (status == "Active" || status == "active") {
                            // الدخول نجح، نحفظ البيانات ونطلب الباقات
                            currentHost = baseUrl
                            currentUser = user
                            currentPass = pass
                            fetchCategories()
                        } else {
                            Toast.makeText(this@MainActivity, "عذراً، هذا الحساب منتهي أو متوقف", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "بيانات الدخول غير صحيحة", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<XtreamAuthResponse>, t: Throwable) {
                    binding.btnConnect.text = "اتصال"
                    binding.btnConnect.isEnabled = true
                    Toast.makeText(this@MainActivity, "فشل الاتصال", Toast.LENGTH_LONG).show()
                }
            })
        } catch (e: Exception) {
            binding.btnConnect.text = "اتصال"
            binding.btnConnect.isEnabled = true
        }
    }

    private fun fetchCategories() {
        binding.loginLayout.visibility = View.GONE
        binding.categoriesLayout.visibility = View.VISIBLE
        Toast.makeText(this, "جاري جلب الباقات...", Toast.LENGTH_SHORT).show()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(currentHost)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(XtreamApiService::class.java)
        
        apiService.getLiveCategories(currentUser, currentPass).enqueue(object : Callback<List<XtreamCategory>> {
            override fun onResponse(call: Call<List<XtreamCategory>>, response: Response<List<XtreamCategory>>) {
                if (response.isSuccessful && response.body() != null) {
                    val categories = response.body()!!
                    val categoryNames = categories.map { it.categoryName }
                    
                    val adapter = ArrayAdapter(this@MainActivity, R.layout.item_category, R.id.tvCategoryName, categoryNames)
                    binding.categoriesList.adapter = adapter
                    
                    binding.categoriesList.setOnItemClickListener { _, _, position, _ ->
                        val selectedCategory = categories[position]
                        Toast.makeText(this@MainActivity, "اخترت: ${selectedCategory.categoryName}", Toast.LENGTH_SHORT).show()
                        // هنا سنبرمج جلب قنوات الباقة لاحقاً
                    }
                }
            }

            override fun onFailure(call: Call<List<XtreamCategory>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "خطأ في جلب الباقات", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupPlayer(url: String) {
        binding.loginLayout.visibility = View.GONE
        binding.categoriesLayout.visibility = View.GONE
        binding.playerContainer.visibility = View.VISIBLE
        
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
        player?.setMediaItem(MediaItem.fromUri(url))
        player?.prepare()
        player?.playWhenReady = true
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }
}
