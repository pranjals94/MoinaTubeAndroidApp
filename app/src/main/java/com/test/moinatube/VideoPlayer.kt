package com.test.moinatube

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@UnstableApi
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoPlayer(
    context: Context,
//        video: VideoItem,
    videoList: List<playableFile>,
    selectedIndex: Int,
    onBack: () -> Unit,
    onIndexChange: (Int) -> Unit
) {
    val exoPlayer = remember(selectedIndex) {
        ExoPlayer.Builder(context).build().apply {

//            videoList.forEachIndexed { index, item ->
//                Log.d("VideoPlayer", "Item $index: ${item.name}")
//            }
            setMediaItems(videoList.map { MediaItem.fromUri(it.videoUrl) }, selectedIndex, 0L)
            prepare()
            playWhenReady = true
        }
    }

    val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }
    val isControllerVisible = remember { mutableStateOf(false) }
    val isLooping = remember { mutableStateOf(false) }

    DisposableEffect(selectedIndex) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && !isLooping.value) {
                    if (selectedIndex + 1 < videoList.size) {
                        onIndexChange(selectedIndex + 1)
                    }else{
//                        onIndexChange(0)// start again from video index 0
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    BackHandler {
        if (isControllerVisible.value) {
            playerViewRef.value?.hideController()
        } else {
            exoPlayer.release()
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = true
                keepScreenOn = true // ✅ Prevent screen from sleeping
                controllerAutoShow = true
                setShowBuffering(SHOW_BUFFERING_ALWAYS)

                setControllerVisibilityListener(
                    PlayerView.ControllerVisibilityListener { visibility ->
                        isControllerVisible.value = (visibility == View.VISIBLE)
                        if (visibility == View.GONE) {
                            // if Controller is hidden
                            this@apply.requestFocus()
//                            Log.d("PlayerView", "Controller hidden")
                        }
                    }
                )
                isFocusable = true
                isFocusableInTouchMode = true
                requestFocus()

                playerViewRef.value = this
                // ✅ Safe to ensure again, though already set above
                playerViewRef.value?.keepScreenOn = true
            }
        }, modifier = Modifier.fillMaxSize())

        if (isControllerVisible.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = if (isLooping.value) "🔁 ON" else "🔁 OFF",
                    fontSize = 24.sp,
                    color = if (isLooping.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clickable {
                            val newMode = if (isLooping.value) {
                                isLooping.value = false
                                Player.REPEAT_MODE_OFF
                            } else {
                                isLooping.value = true
                                Player.REPEAT_MODE_ONE
                            }
                            exoPlayer.repeatMode = newMode
                        }
                        .padding(12.dp)
                )
            }
        }
    }
}