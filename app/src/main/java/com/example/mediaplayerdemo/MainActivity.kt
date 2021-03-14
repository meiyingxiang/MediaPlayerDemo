package com.example.mediaplayerdemo

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mediaplayerdemo.databinding.ActivityMainBinding
import com.example.mediaplayerdemo.model.PlayViewModel
import com.example.mediaplayerdemo.model.PlayerStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var playViewModel: PlayViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        playViewModel = ViewModelProvider(this).get(PlayViewModel::class.java).apply {
            progressBarVisibility.observe(this@MainActivity, Observer {
                viewBinding.progressBar.visibility = it
            })
            videoResolution.observe(this@MainActivity, Observer {
                viewBinding.controllerFrame.seekBar.max = mediaPlayer.duration
                viewBinding.playerFrame.post {
                    resizePlayer(it.first, it.second)
                }
            })
            controllerFrameVisibility.observe(this@MainActivity, Observer {
                viewBinding.controllerFrame.frameLayoutTop.visibility = it
            })
            bufferPercent.observe(this@MainActivity, Observer {
                viewBinding.controllerFrame.seekBar.secondaryProgress =
                        viewBinding.controllerFrame.seekBar.max * it / 100
            })
            playerStatus.observe(this@MainActivity, Observer {
                viewBinding.controllerFrame.imageView.isClickable = true
                when (it) {
                    PlayerStatus.Paused -> viewBinding.controllerFrame.imageView.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                    PlayerStatus.Completed -> viewBinding.controllerFrame.imageView.setImageResource(R.drawable.ic_baseline_replay_24)
                    PlayerStatus.NotReady -> viewBinding.controllerFrame.imageView.isClickable = false
                    else -> viewBinding.controllerFrame.imageView.setImageResource(R.drawable.ic_baseline_pause_24)
                }
            })
        }
        viewBinding.playerFrame.setOnClickListener {
            playViewModel.toggleControllerVisibility()
        }
        viewBinding.controllerFrame.imageView.setOnClickListener {
            playViewModel.togglePlayerStatus()
        }
        lifecycle.addObserver(playViewModel.mediaPlayer)
        viewBinding.surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
            }

            override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
            ) {
                playViewModel.mediaPlayer.setDisplay(holder)
                //播放时屏幕一直亮
                playViewModel.mediaPlayer.setScreenOnWhilePlaying(true)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }

        })
        updatePlayerProgress()
        viewBinding.controllerFrame.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    //来自于用户行为
                    playViewModel.playerSeekToProgress(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        //        if (hasFocus) hideSystemUI()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            hideSystemUI()
            playViewModel.emmitVideoResolution()
        }
    }

    private fun resizePlayer(width: Int, height: Int) {
        if (width == 0 || height == 0) {
            return
        }
        viewBinding.surfaceView.layoutParams = FrameLayout.LayoutParams(
                viewBinding.playerFrame.height * width / height,
                FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER
        )
    }

    private fun updatePlayerProgress() {
        //协程
        lifecycleScope.launch {
            while (true) {
                delay(500)
                viewBinding.controllerFrame.seekBar.progress = playViewModel.mediaPlayer.currentPosition
            }
        }
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

}