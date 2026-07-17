package com.dazou.iptvplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.dazou.iptvplayer.databinding.ActivityMainBinding
import com.dazou.iptvplayer.player.PlayerCallback
import com.dazou.iptvplayer.player.PlayerManager
import com.dazou.iptvplayer.fragments.*


class MainActivity : AppCompatActivity(), PlayerCallback {


    private lateinit var binding: ActivityMainBinding

    lateinit var playerManager: PlayerManager


    private var fullscreen = false
    private var playing = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding =
            ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)



        playerManager =
            PlayerManager(this)


        binding.videoPlayer.player =
            playerManager.player



        setupControls()


        setupMenu()


        loadFragment(HomeFragment())

    }



    private fun setupControls(){


        binding.btnPlayPause.setOnClickListener {

            if(playerManager.isPlaying){

                playerManager.pause()
                playing=false

            }else{

                playerManager.resume()
                playing=true
            }

        }



        binding.btnFullscreen.setOnClickListener {

            fullscreen = !fullscreen

        }


    }




    private fun setupMenu(){


        binding.menuHome.setOnClickListener {

            loadFragment(HomeFragment())

        }


        binding.menuLive.setOnClickListener {

            loadFragment(LiveFragment())

        }


        binding.menuMovies.setOnClickListener {

            loadFragment(MoviesFragment())

        }


        binding.menuSeries.setOnClickListener {

            loadFragment(SeriesFragment())

        }


        binding.menuEpg.setOnClickListener {

            loadFragment(EpgFragment())

        }


        binding.settings.setOnClickListener {

            loadFragment(SettingsFragment())

        }


        binding.account.setOnClickListener {

            loadFragment(AccountsFragment())

        }

    }





    private fun loadFragment(fragment: Fragment){

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

        binding.channelInfo.text =
            "📺 $name"


        playing=true

    }





    override fun onNextChannel(){

    }





    override fun onPreviousChannel(){

    }





    override fun onDestroy(){

        playerManager.release()

        super.onDestroy()

    }

}