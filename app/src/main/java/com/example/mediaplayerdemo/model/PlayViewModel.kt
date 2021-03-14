package com.example.mediaplayerdemo.model

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediaplayerdemo.model.PlayerStatus.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PlayerStatus {
    Playing, Paused, Completed, NotReady
}

class PlayViewModel : ViewModel() {
    val mediaPlayer = BaseMediaPlayer()
    private val _progressBarVisibility = MutableLiveData(View.VISIBLE)
    val progressBarVisibility: LiveData<Int> = _progressBarVisibility
    private val _videoResolution = MutableLiveData(Pair(0, 0))
    val videoResolution: LiveData<Pair<Int, Int>> = _videoResolution

    //控制进度条的ui显示
    private val _controllerFrameVisibility = MutableLiveData(View.INVISIBLE)
    val controllerFrameVisibility: LiveData<Int> = _controllerFrameVisibility
    private var controllerShowTime = 0L

    //视频缓存进度
    private val _bufferPercent = MutableLiveData(0)
    val bufferPercent: LiveData<Int> = _bufferPercent

    //播放状态
    private val _playerStatus = MutableLiveData(NotReady)
    val playerStatus: LiveData<PlayerStatus> = _playerStatus

    init {
        loadVideo()
    }

    private fun loadVideo() {
        mediaPlayer.apply {
            _playerStatus.value = NotReady
            _progressBarVisibility.value = View.VISIBLE
            setDataSource("https://stream7.iqilu.com/10339/upload_transcode/202002/17/20200217101826WjyFCbUXQ2.mp4")
            setOnPreparedListener {
                _progressBarVisibility.value = View.INVISIBLE
                //循环播放
//                isLooping = true
                it.start()
                _playerStatus.value = Playing
            }
            setOnVideoSizeChangedListener { _, width, height ->
                _videoResolution.value = Pair(width, height)
            }
            setOnBufferingUpdateListener { _, percent ->
                //缓冲监听
                _bufferPercent.value = percent
            }
            setOnCompletionListener {
                _playerStatus.value = Completed
            }
            setOnSeekCompleteListener {
                mediaPlayer.start()
                _progressBarVisibility.value = View.INVISIBLE
            }
            prepareAsync()
        }
    }

    fun toggleControllerVisibility() {
        if (_controllerFrameVisibility.value == View.INVISIBLE) {
            _controllerFrameVisibility.value = View.VISIBLE
            controllerShowTime = System.currentTimeMillis()
            //显示之后间隔几秒消失
            viewModelScope.launch {
                delay(3000)
                if (System.currentTimeMillis() - controllerShowTime >= 3000) {
                    _controllerFrameVisibility.value = View.INVISIBLE
                }
            }
        } else {
            _controllerFrameVisibility.value = View.INVISIBLE
        }
    }

    fun togglePlayerStatus() {
        when (_playerStatus.value) {
            Playing -> {
                mediaPlayer.pause()
                _playerStatus.value = Paused
            }
            Paused, Completed -> {
                mediaPlayer.start()
                _playerStatus.value = Playing
            }
            else -> return
        }
    }

    fun playerSeekToProgress(progress: Int) {
        _progressBarVisibility.value = View.VISIBLE
        mediaPlayer.seekTo(progress)
    }

    fun emmitVideoResolution() {
        _videoResolution.value = _videoResolution.value
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer.release()
    }
}