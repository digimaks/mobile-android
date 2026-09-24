// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lv.zzdats.businesslogic.util.safeLet
import lv.zzdats.uilogic.components.AppIcons
import lv.zzdats.uilogic.components.ClickableText
import lv.zzdats.uilogic.components.utils.SIZE_SMALL
import lv.zzdats.uilogic.components.utils.SPACING_MEDIUM
import lv.zzdats.uilogic.components.utils.VSpacer
import lv.zzdats.uilogic.components.wrap.WrapIcon
import lv.zzdats.uilogic.extension.clickableNoRipple
import lv.zzdats.uilogic.extension.throttledClickable

@Composable
fun ContentTitle(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleWithBadge: TitleWithBadge? = null,
    onTitleWithBadgeClick: (() -> Unit)? = null,
    titleStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    subtitle: String? = null,
    clickableSubtitle: String? = null,
    onSubtitleClick: (() -> Unit)? = null,
    subTitleMaxLines: Int = Int.MAX_VALUE,
    subTitleStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        fontSize = 14.sp
    ),
    verticalPadding: PaddingValues = PaddingValues(bottom = SPACING_MEDIUM.dp),
    subtitleTrailingContent: (@Composable RowScope.() -> Unit)? = null,
    trailingLabel: String? = null,
    trailingAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .wrapContentHeight()
            .padding(verticalPadding)
            .then(
                if (trailingLabel != null) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.wrapContentWidth()
                }
            ),
        horizontalArrangement = if (trailingLabel != null) {
            Arrangement.SpaceBetween
        } else {
            Arrangement.Start
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(SIZE_SMALL.dp)
        ) {
            if (titleWithBadge != null) {
                val inlineContentMap = mapOf(
                    "badgeIconId" to InlineTextContent(
                        Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.TextCenter)
                    ) {
                        WrapIcon(
                            iconData = AppIcons.Verified,
                            customTint = Color.Green
                        )
                    }
                )

                Text(
                    modifier = onTitleWithBadgeClick?.let {
                        Modifier.clickableNoRipple(
                            onClick = it
                        )
                    } ?: Modifier,
                    text = titleWithBadge.annotatedString,
                    style = titleStyle,
                    inlineContent = inlineContentMap
                )
            } else if (!title.isNullOrEmpty()) {
                Text(
                    text = title,
                    style = titleStyle
                )
            }

            if (!subtitle.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (clickableSubtitle != null && onSubtitleClick != null) {
                        val annotatedSubtitle = buildAnnotatedString {
                            append(subtitle)
                            withStyle(
                                style = SpanStyle(
                                    fontStyle = subTitleStyle.fontStyle,
                                    color = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                pushStringAnnotation(
                                    tag = clickableSubtitle,
                                    annotation = clickableSubtitle
                                )
                                append(clickableSubtitle)
                            }
                        }

                        ClickableText(
                            text = annotatedSubtitle,
                            onClick = { offset ->
                                annotatedSubtitle.getStringAnnotations(offset, offset)
                                    .firstOrNull()?.let {
                                        onSubtitleClick()
                                    }
                            },
                            style = subTitleStyle,
                            maxLines = subTitleMaxLines,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = subtitle,
                            style = subTitleStyle,
                            maxLines = subTitleMaxLines,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    subtitleTrailingContent?.invoke(this)
                }
            }
        }

        if (trailingLabel != null) {
            Text(
                modifier = Modifier.throttledClickable(
                    enabled = trailingAction != null,
                    onClick = { trailingAction?.invoke() }
                ),
                text = trailingLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

data class TitleWithBadge(
    private val textBeforeBadge: String? = null,
    private val textAfterBadge: String? = null,
    val isTrusted: Boolean
) {
    val annotatedString = buildAnnotatedString {
        if (!textBeforeBadge.isNullOrEmpty()) {
            append(textBeforeBadge)
        }
        if (isTrusted) {
            append(" ")
            appendInlineContent(id = "badgeIconId")
        }
        if (!textAfterBadge.isNullOrEmpty()) {
            append(textAfterBadge)
        }
    }

    val plainText: String
        get() = buildString {
            if (!textBeforeBadge.isNullOrEmpty()) {
                append(textBeforeBadge)
            }
            if (!textAfterBadge.isNullOrEmpty()) {
                append(textAfterBadge)
            }
        }
}