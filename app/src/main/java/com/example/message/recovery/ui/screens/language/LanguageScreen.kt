package com.example.message.recovery.ui.screens.language

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.message.recovery.R
import com.example.message.recovery.model.LanguageModel
import com.example.message.recovery.ui.compose.ExitConfirmDialog
import com.example.message.recovery.ui.compose.NativeAdContainer
import com.example.message.recovery.ui.compose.SelectionIndicator
import com.example.message.recovery.ui.compose.TopPillAction
import com.example.message.recovery.ui.theme.Accent
import com.example.message.recovery.ui.theme.BgColor
import com.example.message.recovery.ui.theme.LanguageItemStroke
import com.example.message.recovery.ui.theme.PlusJakarta
import com.example.message.recovery.ui.theme.TextPrimary
import com.example.message.recovery.ui.theme.TextSecondary
import com.example.message.recovery.ui.theme.AppTheme
import com.example.message.recovery.ui.theme.DevicePreviews
import com.example.message.recovery.ui.theme.isShortScreen
import com.example.message.recovery.ui.theme.prefersSideBySide
import com.example.message.recovery.ui.theme.listItemMinWidth
import com.example.message.recovery.ui.theme.windowSize

private val LanguageItemShape = RoundedCornerShape(12.dp)

@Composable
fun LanguageScreen(
    languages: List<LanguageModel>,
    selected: LanguageModel?,
    doneLabel: String,
    showExitDialog: Boolean,
    onLanguageClick: (LanguageModel) -> Unit,
    onDone: () -> Unit,
    onExitConfirm: () -> Unit,
    onExitDismiss: () -> Unit,
    /**
     * The native ad, supplied by the route so this screen stays free of ad plumbing and remains
     * previewable. Null when no ad should be shown.
     */
    adSlot: (@Composable (Modifier) -> Unit)? = null,
) {
    val window = windowSize()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    // A landscape phone cannot spare 24dp of header padding on top of the list and
                    // the inline ad, so the header tightens up rather than squeezing the list.
                    top = if (window.isShortScreen) 8.dp else 16.dp,
                    bottom = if (window.isShortScreen) 4.dp else 8.dp,
                ),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.choose_language),
                    fontSize = if (window.isShortScreen) 20.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                if (!window.isShortScreen) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.language_subtitle),
                        color = TextSecondary,
                        fontSize = 14.sp,
                    )
                }
            }
            TopPillAction(
                text = doneLabel,
                onClick = onDone,
                showCheckmark = true,
            )
        }

        // One column on a phone, more wherever the width allows — a single stretched row of flag +
        // name + radio reads badly across a tablet.
        val list: @Composable (Modifier) -> Unit = { m ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = listItemMinWidth),
                modifier = m,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(languages, key = { it.id }) { model ->
                    LanguageItem(
                        model = model,
                        selected = selected?.id == model.id,
                        onClick = { onLanguageClick(model) },
                    )
                }
            }
        }
        // The large native template is as tall as it needs to be, which on a landscape phone is the
        // whole viewport — stacking it under the list would leave the list no height at all. Moving
        // it alongside keeps the ad fully visible (never clipped) and the list usable.
        if (window.prefersSideBySide && adSlot != null) {
            Row(modifier = Modifier.weight(1f)) {
                list(Modifier.weight(1f).fillMaxHeight())
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                    adSlot(Modifier.fillMaxWidth())
                }
            }
        } else {
            list(Modifier.weight(1f).fillMaxWidth())
            adSlot?.invoke(Modifier.fillMaxWidth())
        }
    }
    if (showExitDialog) {
        ExitConfirmDialog(onDismiss = onExitDismiss, onConfirm = onExitConfirm)
    }
}

@Composable
private fun LanguageItem(
    model: LanguageModel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LanguageItemShape)
            .background(BgColor)
            .border(
                width = 2.dp,
                color = if (selected) Accent else LanguageItemStroke,
                shape = LanguageItemShape,
            )
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(model.flag),
            contentDescription = null,
            modifier = Modifier.size(width = 32.dp, height = 24.dp),
        )
        Text(
            text = languageDisplayName(model.name),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
            style = TextStyle(
                fontFamily = PlusJakarta,
                fontSize = 16.sp,
                textDirection = TextDirection.Ltr,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SelectionIndicator(selected = selected)
    }
}

private fun languageDisplayName(name: String): AnnotatedString {
    val start = name.indexOf('(')
    val end = name.lastIndexOf(')')
    if (start < 0 || end <= start) {
        return buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                append(name)
            }
        }
    }
    return buildAnnotatedString {
        val english = name.substring(0, start).trim()
        val native = name.substring(start, end + 1)
        val trailing = name.substring(end + 1).trim()
        if (english.isNotEmpty()) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                append(english)
            }
        }
        if (english.isNotEmpty()) append(" ")
        withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = TextSecondary)) {
            append(native)
        }
        if (trailing.isNotEmpty()) {
            append(" ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                append(trailing)
            }
        }
    }
}

@DevicePreviews
@Composable
private fun LanguageScreenPreview() {
    val sample = listOf(
        LanguageModel(id = 1, flag = R.mipmap.ic_launcher, name = "English (English)", code = "en"),
        LanguageModel(id = 2, flag = R.mipmap.ic_launcher, name = "Arabic (العربية)", code = "ar"),
        LanguageModel(id = 3, flag = R.mipmap.ic_launcher, name = "Spanish (Español)", code = "es"),
        LanguageModel(id = 4, flag = R.mipmap.ic_launcher, name = "Hindi (हिन्दी)", code = "hi"),
    )
    AppTheme {
        LanguageScreen(
            languages = sample,
            selected = sample.first(),
            doneLabel = "Done",
            showExitDialog = false,
            onLanguageClick = {},
            onDone = {},
            onExitConfirm = {},
            onExitDismiss = {},
            adSlot = null,
        )
    }
}
