// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components.wrap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import lv.zzdats.uilogic.components.ModalOptionUi
import lv.zzdats.uilogic.components.utils.SIZE_SMALL
import lv.zzdats.uilogic.components.utils.SPACING_LARGE
import lv.zzdats.uilogic.components.utils.SPACING_MEDIUM
import lv.zzdats.uilogic.components.utils.SPACING_SMALL
import lv.zzdats.uilogic.components.utils.VSpacer
import lv.zzdats.uilogic.extension.throttledClickable
import lv.zzdats.uilogic.mvi.ViewEvent

private val defaultBottomSheetPadding: PaddingValues = PaddingValues(
    all = SPACING_LARGE.dp
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrapModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    shape: Shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    dragHandle: @Composable (() -> Unit)? = null,
    sheetContent: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        dragHandle = dragHandle,
        content = sheetContent,
    )
}

@Composable
fun GenericBaseSheetContent(
    title: String,
    bodyContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .background(color = MaterialTheme.colorScheme.background)
            .fillMaxWidth()
            .padding(defaultBottomSheetPadding)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.primaryContainer
            )
        )
        VSpacer.Small()
        bodyContent()
    }
}

@Composable
fun GenericBaseSheetContent(
    titleContent: @Composable () -> Unit,
    bodyContent: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .background(color = MaterialTheme.colorScheme.background)
            .fillMaxWidth()
            .padding(defaultBottomSheetPadding)
    ) {
        titleContent()
        VSpacer.Large()
        bodyContent()
    }
}

@Composable
fun DialogBottomSheet(
    title: String,
    message: String,
    positiveButtonText: String? = null,
    negativeButtonText: String? = null,
    onPositiveClick: () -> Unit? = {},
    onNegativeClick: () -> Unit? = {}
) {
    GenericBaseSheetContent(
        title = title,
        bodyContent = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            )
            VSpacer.Large()
            positiveButtonText?.let {
                WrapPrimaryButton(
                    onClick = { onPositiveClick.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true
                ) {
                    Text(
                        text = positiveButtonText
                    )
                }
            }
            VSpacer.Medium()
            negativeButtonText?.let {
                WrapSecondaryButton(
                    onClick = { onNegativeClick.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true
                ) {
                    Text(
                        text = negativeButtonText
                    )
                }
            }
        }
    )
}

@Composable
fun <T : ViewEvent> BottomSheetWithOptionsList(
    title: String,
    message: String,
    options: List<ModalOptionUi<T>>,
    onEventSent: (T) -> Unit
) {
    if (options.isNotEmpty()) {
        GenericBaseSheetContent(
            title = title,
            bodyContent = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                VSpacer.Large()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    OptionsList(
                        optionItems = options,
                        itemSelected = onEventSent
                    )
                }
            }
        )
    }
}

@Composable
private fun <T : ViewEvent> OptionsList(
    optionItems: List<ModalOptionUi<T>>,
    itemSelected: (T) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
    ) {
        items(optionItems) { item ->
            OptionListItem(
                item = item,
                itemSelected = itemSelected
            )
        }
    }
}

@Composable
private fun <T : ViewEvent> OptionListItem(
    item: ModalOptionUi<T>,
    itemSelected: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SIZE_SMALL.dp))
            .background(MaterialTheme.colorScheme.background)
            .throttledClickable {
                itemSelected(item.event)
            }
            .padding(
                horizontal = SPACING_SMALL.dp,
                vertical = SPACING_MEDIUM.dp
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = item.title,
            style = MaterialTheme.typography.bodyMedium
        )
        WrapIcon(
            modifier = Modifier.wrapContentWidth(),
            iconData = item.icon,
            customTint = MaterialTheme.colorScheme.primary
        )
    }
}