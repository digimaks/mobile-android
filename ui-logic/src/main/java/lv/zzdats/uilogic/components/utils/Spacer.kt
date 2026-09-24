// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components.utils

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object VSpacer {

    @Composable
    fun Custom(space: Int) = Spacer(modifier = Modifier.height(space.dp))

    @Composable
    fun ExtraSmall() = Spacer(modifier = Modifier.height(SPACING_EXTRA_SMALL.dp))

    @Composable
    fun Small() = Spacer(modifier = Modifier.height(SPACING_SMALL.dp))

    @Composable
    fun Medium() = Spacer(modifier = Modifier.height(SPACING_MEDIUM.dp))

    @Composable
    fun Large() = Spacer(modifier = Modifier.height(SPACING_LARGE.dp))

    @Composable
    fun ExtraLarge() = Spacer(modifier = Modifier.height(SPACING_EXTRA_LARGE.dp))

    @Composable
    fun ExtraExtraLarge() = Spacer(modifier = Modifier.height(SPACING_EXTRA_EXTRA_LARGE.dp))
}

object HSpacer {

    @Composable
    fun Custom(space: Int) = Spacer(modifier = Modifier.width(space.dp))

    @Composable
    fun ExtraSmall() = Spacer(modifier = Modifier.width(SPACING_EXTRA_SMALL.dp))

    @Composable
    fun Small() = Spacer(modifier = Modifier.width(SPACING_SMALL.dp))

    @Composable
    fun Medium() = Spacer(modifier = Modifier.width(SPACING_MEDIUM.dp))

    @Composable
    fun Large() = Spacer(modifier = Modifier.width(SPACING_LARGE.dp))

    @Composable
    fun ExtraLarge() = Spacer(modifier = Modifier.width(SPACING_EXTRA_LARGE.dp))

}