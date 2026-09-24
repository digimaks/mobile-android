// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components.wrap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lv.zzdats.uilogic.components.utils.SPACING_EXTRA_SMALL
import lv.zzdats.uilogic.components.utils.SPACING_MEDIUM


@Composable
fun WrapExpandableCard(
    modifier: Modifier = Modifier,
    cardTitleContent: @Composable () -> Unit,
    cardTitlePadding: PaddingValues? = null,
    cardContent: @Composable () -> Unit,
    cardContentPadding: PaddingValues? = null,
    onCardClick: (() -> Unit)? = null,
    throttleClicks: Boolean = true,
    cardColors: CardColors? = null,
    enabled: Boolean = true,
    expandCard: Boolean,
) {
    WrapCard(
        modifier = modifier,
        onClick = onCardClick,
        throttleClicks = throttleClicks,
        colors = cardColors ?: CardDefaults.cardColors(
            contentColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.background
        ),
        enabled = enabled
    ) {
        Column(
            modifier = Modifier
                .padding(
                    paddingValues = cardTitlePadding ?: PaddingValues(
                        SPACING_MEDIUM.dp
                    )
                )
        ) {
            cardTitleContent()

            AnimatedVisibility(visible = expandCard) {
                Column(
                    modifier = Modifier.padding(
                        paddingValues = cardContentPadding ?: PaddingValues(
                            SPACING_EXTRA_SMALL.dp
                        )
                    ),
                    verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
                ) {
                    cardContent()
                }
            }
        }
    }
}