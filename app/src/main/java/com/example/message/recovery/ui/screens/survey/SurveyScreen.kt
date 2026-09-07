package com.example.message.recovery.ui.screens.survey

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.message.recovery.R
import com.example.message.recovery.ui.theme.Accent
import com.example.message.recovery.ui.theme.AppTheme
import com.example.message.recovery.ui.theme.BgColor
import com.example.message.recovery.ui.theme.DevicePreviews
import com.example.message.recovery.ui.theme.StrokeColor
import com.example.message.recovery.ui.theme.SurveyBtnDisabled
import com.example.message.recovery.ui.theme.SurveyBtnDisabledText
import com.example.message.recovery.ui.theme.TextPrimary
import com.example.message.recovery.ui.theme.TextSecondary
import com.example.message.recovery.ui.theme.isShortScreen
import com.example.message.recovery.ui.theme.prefersSideBySide
import com.example.message.recovery.ui.theme.windowSize

private val CardShape = RoundedCornerShape(16.dp)
private val SearchShape = RoundedCornerShape(50)
private val CtaShape = RoundedCornerShape(50)

@Composable
fun SurveyScreen(
    state: SurveyContract.UiState,
    onEvent: (SurveyContract.Event) -> Unit,
    onAnalyticsToggle: () -> Unit = {},
    onAnalyticsNext: () -> Unit = {},
    adSlot: (@Composable (Modifier) -> Unit)? = null,
) {
    val window = windowSize()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp, vertical = if (window.isShortScreen) 8.dp else 16.dp),
    ) {
        Text(
            text = stringResource(R.string.survey_title),
            fontSize = if (window.isShortScreen) 22.sp else 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.survey_subtitle),
            color = TextSecondary,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(if (window.isShortScreen) 12.dp else 16.dp))
        SearchField(
            query = state.query,
            onQueryChange = { onEvent(SurveyContract.Event.QueryChanged(it)) },
        )
        Spacer(Modifier.height(12.dp))

        val list: @Composable (Modifier) -> Unit = { m ->
            LazyColumn(
                modifier = m,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.visibleApps, key = { it.packageName }) { app ->
                    WatchedAppRow(
                        app = app,
                        selected = app.isInstalled && app.packageName in state.selectedPackages,
                        onClick = {
                            if (!app.isInstalled) return@WatchedAppRow
                            onAnalyticsToggle()
                            onEvent(SurveyContract.Event.Toggle(app.packageName))
                        },
                    )
                }
            }
        }

        val adAndCta: @Composable (Modifier) -> Unit = { m ->
            Column(modifier = m) {
                Text(
                    text = pluralStringResource(
                        R.plurals.apps_selected,
                        state.selectedCount,
                        state.selectedCount,
                    ),
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        onAnalyticsNext()
                        onEvent(SurveyContract.Event.Next)
                    },
                    enabled = state.canContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CtaShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        disabledContainerColor = SurveyBtnDisabled,
                        disabledContentColor = SurveyBtnDisabledText,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.continuee).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
                if (adSlot != null) {
                    Spacer(Modifier.height(8.dp))
                    adSlot(Modifier.fillMaxWidth())
                }
            }
        }

        if (window.prefersSideBySide && adSlot != null) {
            Row(modifier = Modifier.weight(1f)) {
                list(Modifier.weight(1f).fillMaxHeight())
                adAndCta(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 12.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
        } else {
            list(Modifier.weight(1f))
            adAndCta(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SearchShape)
            .border(1.dp, StrokeColor, SearchShape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            singleLine = true,
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(stringResource(R.string.search_apps), color = TextSecondary, fontSize = 15.sp)
                }
                inner()
            },
        )
    }
}

@Composable
private fun WatchedAppRow(
    app: WatchedAppUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val dimmed = !app.isInstalled
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, StrokeColor, CardShape)
            .clickable(
                enabled = app.isInstalled,
                role = Role.Checkbox,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .alpha(if (dimmed) 0.72f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(app.avatarColor)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = app.letter,
                color = Accent,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = app.name,
                color = if (dimmed) TextSecondary else TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!app.isInstalled) {
                Text(
                    text = stringResource(R.string.not_installed_on_this_phone),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun SurveyScreenPreview() {
    AppTheme {
        SurveyScreen(
            state = SurveyContract.UiState(
                apps = listOf(
                    WatchedAppUi(1, "wa", "WhatsApp", "W", 0xFFC8E6C9, true),
                    WatchedAppUi(2, "ms", "Messenger", "M", 0xFFBBDEFB, true),
                    WatchedAppUi(3, "ig", "Instagram", "I", 0xFFDCEDC8, true),
                    WatchedAppUi(4, "sc", "Snapchat", "S", 0xFFB3E5FC, false),
                ),
                selectedPackages = setOf("wa", "ms", "ig"),
            ),
            onEvent = {},
        )
    }
}
