package com.valebell.glyphsquish.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.valebell.glyphsquish.R
import com.valebell.glyphsquish.creature.domain.CreatureState
import com.valebell.glyphsquish.ui.components.FailurePane
import com.valebell.glyphsquish.ui.components.LoadingPane
import com.valebell.glyphsquish.ui.components.TopBarFade
import com.valebell.glyphsquish.ui.components.screenEdgeInsets
import com.valebell.glyphsquish.ui.slime.SlimeMatrixState

enum class SquishSection {
    FORMS,
    MOVES,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    creature: CreatureState?,
    loadFailure: String?,
    selectedSection: SquishSection,
    previewState: SlimeMatrixState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val edgeInsets = screenEdgeInsets()
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = actions.onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.open_settings),
                                tint = MaterialTheme.colorScheme.onSurface,
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
        when {
            creature != null -> {
                HomeContent(
                    creature = creature,
                    selectedSection = selectedSection,
                    previewState = previewState,
                    edgeInsets = edgeInsets,
                    topInset = contentPadding.calculateTopPadding(),
                    modifier = Modifier.fillMaxSize(),
                    actions = actions,
                )
            }

            loadFailure != null -> {
                FailurePane(
                    message = stringResource(R.string.load_error),
                    retryLabel = stringResource(R.string.retry),
                    onRetry = actions.onRetry,
                    modifier =
                        Modifier.padding(
                            start = edgeInsets.start,
                            top = contentPadding.calculateTopPadding(),
                            end = edgeInsets.end,
                            bottom = edgeInsets.bottom,
                        ),
                )
            }

            else -> {
                LoadingPane(
                    message = stringResource(R.string.creature_loading),
                    modifier =
                        Modifier.padding(
                            start = edgeInsets.start,
                            top = contentPadding.calculateTopPadding(),
                            end = edgeInsets.end,
                            bottom = edgeInsets.bottom,
                        ),
                )
            }
        }
    }
}
