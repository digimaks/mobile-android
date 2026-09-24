// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components.wrap

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import lv.zzdats.uilogic.components.IconData

@Composable
fun WrapImage(
    iconData: IconData,
    modifier: Modifier = Modifier
) {
    val iconContentDescription = stringResource(id = iconData.contentDescriptionId)

    iconData.resourceId?.let { resId ->
        Image(
            modifier = modifier,
            painter = painterResource(id = resId),
            contentDescription = iconContentDescription
        )
    } ?: run {
        iconData.imageVector?.let { imageVector ->
            Image(
                modifier = modifier,
                imageVector = imageVector,
                contentDescription = iconContentDescription
            )
        }
    }
}

@Composable
fun WrapImage(
    modifier: Modifier = Modifier,
    painter: BitmapPainter,
    contentDescription: String,
    contentScale: ContentScale? = null,
) {
    Image(
        modifier = modifier,
        painter = painter,
        contentDescription = contentDescription,
        contentScale = contentScale ?: ContentScale.FillBounds,
    )
}

@Composable
fun WrapImage(
    modifier: Modifier = Modifier,
    bitmap: ImageBitmap?,
    contentDescription: String,
    contentScale: ContentScale? = null,
) {
    bitmap?.let {
        Image(
            modifier = modifier,
            bitmap = it,
            contentDescription = contentDescription,
            contentScale = contentScale ?: ContentScale.FillBounds,
        )
    }
}