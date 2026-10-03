package com.valebell.glyphsquish.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.valebell.glyphsquish.R
import com.valebell.glyphsquish.ui.audio.GlyphSoundEffect
import com.valebell.glyphsquish.ui.components.SectionLabel
import com.valebell.glyphsquish.ui.components.TopBarFade
import com.valebell.glyphsquish.ui.components.screenEdgeInsets
import com.valebell.glyphsquish.ui.theme.SquishSpacing

@Composable
internal fun SettingsScreen(
    versionName: String,
    feedback: FeedbackState,
    appearance: AppearanceState,
    onOpenGlyphToys: () -> Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    playFeedback: (GlyphSoundEffect) -> Unit = {},
) {
    var glyphLaunchFailed by rememberSaveable { mutableStateOf(false) }
    var showControls by rememberSaveable { mutableStateOf(false) }
    if (showControls) {
        ControlsHelp(onDismiss = {
            showControls = false
            playFeedback(GlyphSoundEffect.SLIDE_BACKWARD)
        })
    }
    val edgeInsets = screenEdgeInsets()
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.settings_back),
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                )
                TopBarFade()
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
    ) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = edgeInsets.start,
                        top = contentPadding.calculateTopPadding(),
                        end = edgeInsets.end,
                        bottom = edgeInsets.bottom,
                    ),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = SquishSpacing.PhoneContentMaxWidth)
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = SquishSpacing.Screen),
            ) {
                AppearanceSettingsSection(appearance)
                SettingsSectionLabel(R.string.settings_feedback_section)
                SettingsCard {
                    ToggleSetting(
                        titleResource = R.string.settings_sound,
                        descriptionResource = R.string.settings_sound_description,
                        isEnabled = feedback.settings?.isSoundEnabled == true,
                        onEnabledChange = feedback.actions.onSoundEnabledChange,
                        interactive = feedback.settings != null,
                    )
                    SettingsDivider()
                    ToggleSetting(
                        titleResource = R.string.settings_vibration,
                        descriptionResource = R.string.settings_vibration_description,
                        isEnabled = feedback.settings?.isVibrationEnabled == true,
                        onEnabledChange = feedback.actions.onVibrationEnabledChange,
                        interactive = feedback.settings != null,
                    )
                    if (feedback.hasError) {
                        SettingsError(R.string.feedback_settings_error, feedback.actions.onRetry)
                    }
                }

                SettingsSectionLabel(R.string.settings_glyph_section)
                SettingsCard {
                    ListItem(
                        onClick = { glyphLaunchFailed = !onOpenGlyphToys() },
                        trailingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_open_in_new),
                                contentDescription = null,
                            )
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        supportingContent = {
                            Column {
                                Text(stringResource(R.string.settings_glyph_description))
                                if (glyphLaunchFailed) {
                                    Text(
                                        stringResource(R.string.glyph_toys_unavailable),
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                    )
                                }
                            }
                        },
                        colors = settingsListItemColors(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.open_glyph_toys))
                    }
                }

                SettingsSectionLabel(R.string.settings_help_section)
                SettingsCard {
                    ListItem(
                        onClick = {
                            showControls = true
                            playFeedback(GlyphSoundEffect.SLIDE_FORWARD)
                        },
                        colors = settingsListItemColors(),
                    ) {
                        Text(stringResource(R.string.settings_how_to_play))
                    }
                }

                SettingsSectionLabel(R.string.settings_about_section)
                SettingsCard {
                    ListItem(
                        trailingContent = {
                            Text(
                                text = versionName,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        colors = settingsListItemColors(),
                    ) {
                        Text(stringResource(R.string.settings_version))
                    }
                }
                Spacer(Modifier.height(SquishSpacing.Section))
            }
        }
    }
}

@Composable
private fun AppearanceSettingsSection(appearance: AppearanceState) {
    SettingsSectionLabel(R.string.settings_appearance_section)
    SettingsCard {
        ToggleSetting(
            titleResource = R.string.settings_device_colors,
            descriptionResource = R.string.settings_device_colors_description,
            isEnabled = appearance.settings?.useDeviceColors == true,
            onEnabledChange = appearance.actions.onDeviceColorsChange,
            interactive = appearance.settings != null,
        )
        if (appearance.hasError) {
            SettingsError(R.string.appearance_settings_error, appearance.actions.onRetry)
        }
    }
}

@Composable
private fun SettingsError(
    messageResource: Int,
    onRetry: () -> Unit,
) {
    Column(Modifier.padding(SquishSpacing.Card)) {
        Text(
            stringResource(messageResource),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(content = { content() })
    }
}

@Composable
private fun ToggleSetting(
    titleResource: Int,
    descriptionResource: Int,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    interactive: Boolean,
) {
    ListItem(
        verticalAlignment = Alignment.CenterVertically,
        supportingContent = { Text(stringResource(descriptionResource)) },
        trailingContent = {
            Switch(
                checked = isEnabled,
                onCheckedChange = null,
                enabled = interactive,
            )
        },
        colors = settingsListItemColors(),
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = isEnabled,
                    role = Role.Switch,
                    onValueChange = onEnabledChange,
                    enabled = interactive,
                ),
    ) {
        Text(stringResource(titleResource))
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = SquishSpacing.Card),
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun settingsListItemColors() =
    ListItemDefaults.colors(
        containerColor = Color.Transparent,
    )

@Composable
private fun SettingsSectionLabel(labelResource: Int) {
    SectionLabel(
        text = stringResource(labelResource),
        color = MaterialTheme.colorScheme.secondary,
        modifier =
            Modifier.padding(
                top = SquishSpacing.Section,
                bottom = SquishSpacing.Item,
                start = SquishSpacing.Tiny,
            ),
    )
}

internal data class FeedbackActions(
    val onSoundEnabledChange: (Boolean) -> Unit,
    val onVibrationEnabledChange: (Boolean) -> Unit,
    val onRetry: () -> Unit,
)
