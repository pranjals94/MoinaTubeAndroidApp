package com.test.moinatube

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Card(Item: Item, onClick: () -> Unit) {
    val thumbnailPainter = if (Item.is_dir) {
        painterResource(R.drawable.folder)
    } else {
        rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Item.thumbnailUrl)
                .crossfade(true)
                .error(R.drawable.file) // add a fallback image
                .build()
        )
    }

    Surface(
        modifier = Modifier
            .width(300.dp)
            .height(180.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Spacer(Modifier.height(5.dp))
            Image(
                painter = thumbnailPainter,
                contentDescription = Item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Text(
                text = Item.name,
                fontSize = 16.sp,
                modifier = Modifier.padding(3.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}