package com.dazou.iptvplayer

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.dazou.iptvplayer.databinding.ActivityMainBinding
import com.dazou.iptvplayer.fragments.*
import com.dazou.iptvplayer.model.XtreamServer
import com.dazou.iptvplayer.player.PlayerCallback
import com.dazou.iptvplayer.player.PlayerManager


class MainActivity : AppCompatActivity(), PlayerCallback {


    private lateinit var binding: ActivityMainBinding

    lateinit var playerManager: PlayerManager


    var currentServer: XtreamServer? = null



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)



        // منع المشغل من خطف اللمس والتركيز
        binding.playerView.apply {

            useController = false
            isFocusable = false
            isClickable = false
        }



        playerManager = PlayerManager(this)


        binding.playerView.player =
            playerManager.player



        setupBottomNav()


        loadFragment(HomeFragment())


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

        playerManager.play(
            url,
            name,
            type
        )

    }




    override fun onNextChannel(){

    }



    override fun onPreviousChannel(){

    }





    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {


        if(event.action == KeyEvent.ACTION_DOWN){


            when(event.keyCode){


                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {

                    playerManager.pause()
                    return true

                }

            }

        }


        return super.dispatchKeyEvent(event)

    }




    override fun onDestroy(){

        playerManager.release()

        super.onDestroy()

    }

}