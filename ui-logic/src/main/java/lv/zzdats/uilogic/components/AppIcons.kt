// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import lv.zzdats.resourceslogic.R

@Stable
data class IconData(
    @DrawableRes val resourceId: Int?,
    @StringRes val contentDescriptionId: Int,
    val imageVector: ImageVector? = null,
) {
    init {
        require(
            resourceId == null && imageVector != null
                    || resourceId != null && imageVector == null
                    || resourceId != null && imageVector != null
        ) {
            "An Icon should at least have a valid resourceId or a valid imageVector."
        }
    }
}

object AppIcons {
    val Logo: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Filled.Home,
    )

    val ArrowForward: IconData = IconData(
        resourceId = R.drawable.arrow_forward,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = null,
    )

    val Close: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Filled.Close,
    )

    val ArrowBack: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
    )

    val Backspace: IconData = IconData(
        resourceId = R.drawable.backspace,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = null,
    )

    val VerticalMore: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Filled.MoreVert,
    )

    val TouchId: IconData = IconData(
        resourceId = R.drawable.ic_touch_id,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = null
    )
    val Verified: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Filled.Favorite,
    )

    val Error: IconData = IconData(
        resourceId = R.drawable.ic_warning,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = null,
    )

    val ClockTimer: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Default.Face
    )

    val Id: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Default.Face
    )

    val QR: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Default.Face
    )

    val Plus: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Default.Add
    )

    val Message: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Default.Email
    )

    val Settings: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Rounded.Settings
    )

    val AddCircle: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Outlined.AddCircle
    )

    val VisibilityOff: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Default.Face
    )

    val Visibility: IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Default.Face
    )

    val Warning : IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Default.Warning
    )

    val KeyboardArrowUp : IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Default.KeyboardArrowUp
    )

    val KeyboardArrowDown : IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Default.KeyboardArrowDown
    )

    val KeyboardArrowRight : IconData = IconData(
        resourceId = null,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = Icons.Default.Face
    )

    val AppLogo: IconData = IconData(
        resourceId = R.drawable.app_icon,
        contentDescriptionId = R.string.content_description_user_image,
        imageVector = null
    )
}