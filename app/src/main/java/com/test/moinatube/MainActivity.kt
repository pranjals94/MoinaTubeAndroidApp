package com.test.moinatube

// release version could not connect to the network, but debug mode worked

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler

import androidx.activity.compose.setContent
import androidx.compose.foundation.background

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

import androidx.media3.common.util.UnstableApi

import androidx.tv.material3.lightColorScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import androidx.compose.ui.graphics.Color
import java.net.URLEncoder


// when the wifi hotspot is closed the ip also changes so apply the changes accordingly

// for tv app ethernet
//const val hostname: String ="http://192.168.18.62:8000/"  // lenovo laptop (developer)
const val hostname: String ="http://192.168.1.10:8000/"  // ubuntu server ethernet
//const val hostname: String ="http://192.168.117.217:8000/" // ubuntu server wifi


// to separate the tv and the mobile app, for mobile the server runs in a different port

// for mobile app wifi
//const val hostname: String ="http://192.168.117.62:8001/"  // lenovo laptop (developer)
//const val hostname: String ="http://192.168.18.217:8001/" // ubuntu server wifi

// ========== DATA ==========

data class Item(
    val name: String,
    val thumbnailUrl: String,
    val is_dir: Boolean
)

data class playableFile(
    val name: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val is_dir: Boolean
)

interface VideoApiService {
    @GET("apis/{path}")
    suspend fun getItems(@Path("path", encoded = true) path: String): List<Item>
}

object ApiClient {

    private const val BASE_URL = hostname
    val retrofit: VideoApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(VideoApiService::class.java)
}


interface filenamesApiService {
    @GET("apis/filenames/{path}")
    suspend fun getItems(@Path("path", encoded = true) path: String): List<playableFile>
}

object filenamesApiClient {
    private const val BASE_URL = hostname
    val retrofit: filenamesApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(filenamesApiService::class.java)
}


// ========== MAIN ACTIVITY ==========

class MainActivity : ComponentActivity() {

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            var listItems by remember { mutableStateOf<List<Item>>(emptyList()) }
            var listItemsPath by remember { mutableStateOf("listItems/") }  // Your base path
            var playableFiles by remember { mutableStateOf<List<playableFile>>(emptyList()) }
            var selectedIndex by remember { mutableStateOf(-1) }
            var isLoading by remember { mutableStateOf(false) }
            var isError by remember { mutableStateOf(false) }
            var backPressedTime by remember { mutableStateOf(0L) }

            val background = colorResource(id = R.color.background)
            val alert_background = colorResource(id = R.color.alert_red)
//            val textColor = colorResource(id = R.color.textColor)

            LaunchedEffect(Unit) {
                isLoading = true
                isError = false
                try {
                    listItems = ApiClient.retrofit.getItems(Uri.encode(listItemsPath))
                    isLoading = false
                } catch (e: Exception) {
//                    Log.e("VideoListScreen", "Failed to load items", e)
                    isError = true
                    isLoading = false
                }
            }

            // Automatically run when listItemsPath changes
            LaunchedEffect(listItemsPath) {
                isLoading = true
                isError = false
                try {
                    listItems = ApiClient.retrofit.getItems(Uri.encode(listItemsPath))
                } catch (e: Exception) {
                    isError = true
                } finally {
                    isLoading = false
                }
            }

            // Observe lifecycle
            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        finishAffinity() // 💣 Clean app exit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            MaterialTheme(colorScheme = lightColorScheme()) {

                // 🟡 If a video is selected, show VideoPlayer instead of the grid
                if (selectedIndex != -1) {

//                    Log.d("TriggerCheck", "Calling VideoPlayer")

                    VideoPlayer(
                        context = context,
                        videoList = playableFiles,
                        selectedIndex = selectedIndex,
                        onBack = { selectedIndex = -1 },
                        onIndexChange = { newIndex -> selectedIndex = newIndex }
                    )
                } else {

                    // 👇 Add this block to handle folder back navigation
                    BackHandler(enabled = listItemsPath != "") {
                        val parts = listItemsPath.trimEnd('/').split("/")
                        if (parts.size > 1) {
                            listItemsPath = parts.dropLast(1).joinToString("/") + "/"
//                            Log.d("BackPressed", "Path: $listItemsPath")

                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    listItems = ApiClient.retrofit.getItems(listItemsPath)
                                } catch (_: Exception) {
//                                    Log.e("BackNavigation", "Error fetching folder: ${e.message}")
                                }
                            }
                        }else {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - backPressedTime < 2000) {
                                (context as? Activity)?.finish()
                            } else {
                                backPressedTime = currentTime
                                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {

                                Text(
                                    "MoinaTube: /" + listItemsPath.removePrefix("listItems/"),
                                    fontSize = 28.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                if (listItems.isEmpty()) {
                                    Spacer(Modifier.height(100.dp))
                                    Text(
                                        "No Files Found.",
                                        fontSize = 32.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                } else {
                                    Spacer(Modifier.height(8.dp))
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 200.dp), // Automatically adjusts columns
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxSize().padding(10.dp)
                                    ) {


                                        itemsIndexed(listItems) { index, item ->
                                            Card(Item = item) { // this lamda function is called when the user clicks an on the card item
                                                if (item.is_dir) {
                                                    listItemsPath += (item.name + "/")

                                                } else {
//                                            Log.d("FileClick", "Clicked on file: ${item.name}")

                                                    val encodedName = Uri.encode(item.name)
//                                                    val encodedName = URLEncoder.encode(item.name, "UTF-8")
                                                    val fullPath =
                                                        listItemsPath.removePrefix("listItems/") + encodedName
                                        Log.d("filenames api:", fullPath)
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        try {
                                                            isLoading = true
                                                            isError = false
                                                            val videoList =
                                                                filenamesApiClient.retrofit.getItems(
                                                                    fullPath
                                                                )
                                                            isLoading = false
//                                                    Log.d("filnames fetch",videoList[0].videoUrl)
                                                            val index =
                                                                videoList.indexOfFirst { it.name == item.name }
//                                                    Log.d("selected index: ", index.toString())

                                                            withContext(Dispatchers.Main) {
                                                                playableFiles = videoList
                                                                selectedIndex = index
                                                            }
                                                        } catch (_: Exception) {
                                                            isLoading = false
                                                            isError = true
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            when {
                isLoading -> {
                    // Loading indicator
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)) // ⬅️ semi-transparent background
                            .padding(10.dp).pointerInput(Unit) {
                            // Absorb all touch events to block interaction below
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(64.dp), // bigger size for TV
                            color = Color.White, // adjust color to suit background
                            strokeWidth = 6.dp)
                    }
                }

                isError -> {
                    // Error message
                    Box(
                        Modifier.fillMaxSize().padding(10.dp).background(Color.Black.copy(alpha = 0.4f)).pointerInput(Unit) {
                            // Absorb all touch events to block interaction below
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.fillMaxSize().padding(10.dp),
                            contentAlignment = Alignment.Center
                        ){Text("Failed to load items. Please close the app and try again.",modifier = Modifier
                            .background(alert_background)
                            .padding(8.dp) // Optional: padding inside background
                         )}

                    }
                }
            }

        }
            }

}
