package com.dazou.iptvplayer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.dazou.iptvplayer.databinding.ActivityMainBinding
import com.dazou.iptvplayer.fragments.*
import com.dazou.iptvplayer.model.XtreamServer
import com.dazou.iptvplayer.player.PlayerCallback
import com.dazou.iptvplayer.player.PlayerManager
import com.dazou.iptvplayer.viewmodel.LiveViewModel
import androidx.lifecycle.ViewModelProvider


class MainActivity : AppCompatActivity(), PlayerCallback {


    private lateinit var binding: ActivityMainBinding

    lateinit var playerManager: PlayerManager

    // الحساب الحالي
    var currentServer: XtreamServer? = null


    private lateinit var liveViewModel: LiveViewModel



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        // دعم Android TV والريموت
        window.decorView.isFocusableInTouchMode = true
        window.decorView.requestFocus()



        playerManager = PlayerManager(this)


        binding.playerView.player =
            playerManager.player



        playerManager.setOnPlaybackEndedListener {

            onNextChannel()

        }



        liveViewModel =
            ViewModelProvider(this)
                .get(LiveViewModel::class.java)



        setupBottomNav()



        loadFragment(
            HomeFragment()
        )



        // أول تركيز للريموت
        Handler(Looper.getMainLooper())
            .postDelayed({

                binding.btnHome.apply {

                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()

                }

            },500)

    }





    private fun setupBottomNav(){


        binding.btnHome.setOnClickListener {

            loadFragment(HomeFragment())

        }



        binding.btnLive.setOnClickListener {


            loadFragment(LiveFragment())

        }



        binding.btnMovies.setOnClickListener {


            loadFragment(MoviesFragment())

        }



        binding.btnSeries.setOnClickListener {


            loadFragment(SeriesFragment())

        }



        binding.btnFavorites.setOnClickListener {


            loadFragment(FavoritesFragment())

        }



        binding.btnAccounts.setOnClickListener {


            loadFragment(AccountsFragment())

        }


    }







    fun loadFragment(fragment: Fragment){


        supportFragmentManager
            .beginTransaction()
            .replace(
                binding.fragmentContainer.id,
                fragment
            )
            .commit()


    }







    override fun playStream(
        url:String,
        name:String,
        type:String
    ){


        binding.playerView.requestFocus()


        playerManager.play(
            url,
            name,
            type
        )

    }







    override fun onNextChannel(){

        // سيتم ربط تغيير القناة لاحقاً

    }







    override fun onPreviousChannel(){

        // سيتم ربط تغيير القناة لاحقاً

    }







    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {


        if(event.action ==
            KeyEvent.ACTION_DOWN){


            when(event.keyCode){


                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {


                    val focused =
                        currentFocus


                    focused?.performClick()


                    return true

                }



                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {


                    playerManager.pause()


                    return true

                }



                KeyEvent.KEYCODE_BACK -> {


                    if(binding.playerView.hasFocus()){


                        binding.playerView.clearFocus()


                        return true

                    }


                }

            }


        }


        return super.dispatchKeyEvent(event)

    }







    override fun onKeyDown(
        keyCode:Int,
        event:KeyEvent?
    ):Boolean {


        return when(keyCode){



            KeyEvent.KEYCODE_MEDIA_NEXT -> {


                onNextChannel()

                true

            }



            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {


                onPreviousChannel()

                true

            }



            else -> super.onKeyDown(
                keyCode,
                event
            )

        }

    }







    override fun onDestroy(){


        playerManager.release()


        super.onDestroy()


    }


}