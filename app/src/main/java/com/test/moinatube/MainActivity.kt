package com.test.moinatube

// release version could not connect to the network, but debug mode worked

import android.app.Activity
import android.content.Context
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextField

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URLEncoder


// when the wifi hotspot is closed the ip also changes so apply the changes accordingly

// for tv app ethernet
//const val hostname: String ="http://192.168.18.62:8000/"  // lenovo laptop (developer)
//const val hostname: String ="http://192.168.1.10:8000/"  // ubuntu server ethernet
//const val hostname: String ="http://192.168.117.217:8000/" // ubuntu server wifi


// to separate the tv and the mobile app, for mobile the server runs in a different port

// for mobile app wifi
//const val hostname: String ="http://192.168.117.62:8001/"  // lenovo laptop (developer)
//const val hostname: String ="http://192.168.66.62:8001/" // ubuntu server wifi
//var hostname: String ="http://192.168.2.62:8001/" // ubuntu server wifi
// also update in networksecurity.xml and the  fast api backend

object AppConfig {
    var hostname by mutableStateOf("http://0.0.0.0:80/")
}


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

//object ApiClient {
//    private val BASE_URL = AppConfig.hostname
//    val retrofit: VideoApiService = Retrofit.Builder()
//        .baseUrl(BASE_URL)
//        .addConverterFactory(GsonConverterFactory.create())
//        .build()
//        .create(VideoApiService::class.java)
//}

object ApiClient {
    fun getService(): VideoApiService {
        return Retrofit.Builder()
            .baseUrl(AppConfig.hostname)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VideoApiService::class.java)
    }
}


interface filenamesApiService {
    @GET("apis/filenames/{path}")
    suspend fun getItems(@Path("path", encoded = true) path: String): List<playableFile>
}

object filenamesApiClient {
    private val BASE_URL = AppConfig.hostname
    val retrofit: filenamesApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(filenamesApiService::class.java)
}

// DataStore setup
val Context.dataStore by preferencesDataStore(name = "settings")
val SAVED_IP = stringPreferencesKey("saved_ip")
val SAVED_PORT = stringPreferencesKey("saved_port")

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
            var connected by remember { mutableStateOf(false) }

            val background = colorResource(id = R.color.background)
            val alert_background = colorResource(id = R.color.alert_red)
//            val textColor = colorResource(id = R.color.textColor)

            LaunchedEffect(Unit) {
                isLoading = true
                isError = false
                try {
                    listItems =ApiClient.getService().getItems(Uri.encode(listItemsPath))
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
                    listItems =ApiClient.getService().getItems(Uri.encode(listItemsPath))
                } catch (e: Exception) {
                    isError = true
                } finally {
                    isLoading = false
                }
            }

            // Try to auto-connect if saved IP and port exist
            LaunchedEffect(Unit) {
                val ip = loadIP(context)
                val port = loadPort(context)
                if (ip.isNotBlank() && testConnection(ip, port)) {
                    AppConfig.hostname= "http://$ip:$port/"
                    connected = true
                    
                    isLoading = true
                    isError = false
                    try {
                        listItems =ApiClient.getService().getItems(Uri.encode(listItemsPath))
                    } catch (e: Exception) {
                        isError = true
                    } finally {
                        isLoading = false
                    }
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

                if (connected) {
                    //MainScreen()
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
                                        listItems = ApiClient.getService().getItems(Uri.encode(listItemsPath))
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
                } else {
                    ConnectScreen(onConnectSuccess = { connected = true })
                }
            }

        }
    }

} //Activity Main scope end

// Save and load helpers

suspend fun saveIP(context: Context, ip: String) {
    context.dataStore.edit { prefs -> prefs[SAVED_IP] = ip }
}

suspend fun savePort(context: Context, port: String) {
    context.dataStore.edit { prefs -> prefs[SAVED_PORT] = port }
}

suspend fun loadIP(context: Context): String {
    return context.dataStore.data.first()[SAVED_IP] ?: ""
}

suspend fun loadPort(context: Context): String {
    return context.dataStore.data.first()[SAVED_PORT] ?: ""
}

// TCP connection test
suspend fun testConnection(ip: String, port: String, timeout: Int = 1000): Boolean {
    val port = port.toIntOrNull()
    if (port == null) {
        Log.e("SocketTest", "Invalid port number")
        return false
    }
    return withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeout)
                true
            }
        } catch (e: Exception) {
            Log.e("SocketTest", "Connection failed: ${e.message}", e)
            false
        }
    }
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ConnectScreen(onConnectSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var ip by rememberSaveable { mutableStateOf("192.168.") }
    var port by rememberSaveable { mutableStateOf("8001") }
    var status by remember { mutableStateOf("") }

    Column(Modifier.padding(24.dp)) {
        TextField(
            value = ip,
            onValueChange = { if (it.matches(Regex("""[\d.]*"""))) {
                ip = it
            }},
            label = { Text("IP Address") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Port Number") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            scope.launch {
                status = if (testConnection(ip, port)) {
                    saveIP(context, ip)
                    savePort(context, port)
                    AppConfig.hostname= "http://$ip:$port/"
                    "Connected ..! Restart App."
                } else {
                    "Connection failed"
                }
            }
        }) {
            Text("Connect")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Status: $status")
    }
}
