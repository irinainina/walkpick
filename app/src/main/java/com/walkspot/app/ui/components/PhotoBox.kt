package com.walkspot.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.walkspot.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A photo from its file, cropped to fill the box. A pale ground stands in while it is read and when
 * there is no file. [sample] reads every n-th pixel: a thumbnail needs no more.
 */
@Composable
fun PhotoBox(
    file: File?,
    modifier: Modifier = Modifier,
    sample: Int = 1,
    scale: ContentScale = ContentScale.Crop,
    ground: Color = AppTheme.colors.surfaceTint,
) {
    val picture by produceState<ImageBitmap?>(null, file) {
        value = withContext(Dispatchers.IO) {
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            file?.let { BitmapFactory.decodeFile(it.path, options) }?.asImageBitmap()
        }
    }
    val box = modifier.background(ground)
    picture?.let { Image(it, null, box, contentScale = scale) } ?: Box(box)
}
