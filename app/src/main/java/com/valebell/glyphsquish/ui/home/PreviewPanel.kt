package com.valebell.glyphsquish.ui.home

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valebell.glyphsquish.R
import com.valebell.glyphsquish.creature.domain.CreatureState
import com.valebell.glyphsquish.descriptionResource
import com.valebell.glyphsquish.nameResource
import com.valebell.glyphsquish.slime.SlimePhenotype
import com.valebell.glyphsquish.ui.components.SectionLabel
import com.valebell.glyphsquish.ui.slime.SlimeMatrix
import com.valebell.glyphsquish.ui.slime.SlimeMatrixState
import com.valebell.glyphsquish.ui.theme.LocalPreviewColors
import com.valebell.glyphsquish.ui.theme.SquishSpacing

@Composable
internal fun PreviewPanel(
    creature: CreatureState,
    selectedSection: SquishSection,
    previewState: SlimeMatrixState,
    previewSize: Dp,
    compact: Boolean,
    onReplayMove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val previewColors = LocalPreviewColors.current
    val replayMove = {
        if (!previewState.isMovePlaying) onReplayMove()
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = previewColors.container,
                contentColor = previewColors.content,
            ),
        border = BorderStroke(1.dp, previewColors.border),
    ) {
        Column(
            modifier = Modifier.padding(SquishSpacing.Card),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PreviewHeader(
                creature = creature,
                compact = compact,
            )
            Spacer(Modifier.height(SquishSpacing.Item))
            if (compact) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PreviewMatrix(
                        creature = creature,
                        previewState = previewState,
                        previewSize = previewSize,
                    )
                    PreviewDetails(
                        creature = creature,
                        selectedSection = selectedSection,
                        compact = true,
                        isMovePlaying = previewState.isMovePlaying,
                        onReplayMove = replayMove,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(start = SquishSpacing.Item),
                    )
                }
            } else {
                PreviewMatrix(
                    creature = creature,
                    previewState = previewState,
                    previewSize = previewSize,
                )
            }
            Spacer(Modifier.height(SquishSpacing.Compact))
            Text(
                text = stringResource(R.string.preview_hint),
                color = previewColors.muted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            if (compact) {
                CompactReplayMove(selectedSection, previewState.isMovePlaying, replayMove)
            } else {
                Spacer(Modifier.height(SquishSpacing.Item))
                PreviewDetails(
                    creature = creature,
                    selectedSection = selectedSection,
                    compact = false,
                    isMovePlaying = previewState.isMovePlaying,
                    onReplayMove = replayMove,
                )
            }
        }
    }
}

@Composable
private fun PreviewHeader(
    creature: CreatureState,
    compact: Boolean,
) {
    val previewColors = LocalPreviewColors.current
    val identity =
        stringResource(
            R.string.preview_identity_format,
            stringResource(creature.currentForm.nameResource()),
            stringResource(creature.selectedMove.nameResource()),
        )
    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SquishSpacing.Compact),
        ) {
            SectionLabel(
                text = stringResource(R.string.matrix_label),
                color = previewColors.muted,
            )
            Text(
                text = identity,
                modifier = Modifier.align(Alignment.End),
                color = previewColors.accent,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
            )
        }
    } else {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(SquishSpacing.Compact),
        ) {
            SectionLabel(
                text = stringResource(R.string.matrix_label),
                color = previewColors.muted,
            )
            Text(
                text = identity,
                color = previewColors.accent,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun PreviewMatrix(
    creature: CreatureState,
    previewState: SlimeMatrixState,
    previewSize: Dp,
) {
    SlimeMatrix(
        state = previewState,
        phenotype = SlimePhenotype.from(creature),
        contentDescription =
            stringResource(
                R.string.matrix_preview_description,
                stringResource(creature.currentForm.nameResource()),
            ),
        modifier = Modifier.size(previewSize),
        interactive = true,
    )
}

@Composable
private fun PreviewDetails(
    creature: CreatureState,
    selectedSection: SquishSection,
    compact: Boolean,
    isMovePlaying: Boolean,
    onReplayMove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val previewColors = LocalPreviewColors.current
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()
    val layoutDirection = LocalLayoutDirection.current
    AnimatedContent(
        targetState = selectedSection,
        modifier = modifier,
        transitionSpec =
            if (animationsEnabled) {
                sectionTransitionSpec(layoutDirection)
            } else {
                ReducedMotionSectionTransitionSpec
            },
        contentAlignment = Alignment.TopCenter,
        label = "preview section",
    ) { section ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text =
                    stringResource(
                        when (section) {
                            SquishSection.FORMS -> creature.currentForm.nameResource()
                            SquishSection.MOVES -> creature.selectedMove.nameResource()
                        },
                    ),
                color = previewColors.content,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            if (section == SquishSection.MOVES && !compact) {
                Text(
                    text = stringResource(creature.selectedMove.descriptionResource()),
                    modifier = Modifier.padding(top = SquishSpacing.Tiny),
                    color = previewColors.muted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
                ReplayMoveButton(
                    isPlaying = isMovePlaying,
                    onClick = onReplayMove,
                    modifier = Modifier.padding(top = SquishSpacing.Item),
                )
            }
        }
    }
}

@Composable
private fun CompactReplayMove(
    selectedSection: SquishSection,
    isMovePlaying: Boolean,
    onReplayMove: () -> Unit,
) {
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()
    AnimatedVisibility(
        visible = selectedSection == SquishSection.MOVES,
        enter =
            if (animationsEnabled) {
                fadeIn(
                    animationSpec = tween(SECTION_FADE_IN_MILLIS),
                ) +
                    expandVertically(
                        animationSpec =
                            tween(
                                durationMillis = SECTION_TRANSITION_MILLIS,
                                easing = FastOutSlowInEasing,
                            ),
                        expandFrom = Alignment.Top,
                    )
            } else {
                EnterTransition.None
            },
        exit =
            if (animationsEnabled) {
                fadeOut(
                    animationSpec = tween(SECTION_FADE_OUT_MILLIS),
                ) +
                    shrinkVertically(
                        animationSpec =
                            tween(
                                durationMillis = SECTION_TRANSITION_MILLIS,
                                easing = FastOutSlowInEasing,
                            ),
                        shrinkTowards = Alignment.Top,
                    )
            } else {
                ExitTransition.None
            },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(SquishSpacing.Item))
            ReplayMoveButton(isPlaying = isMovePlaying, onClick = onReplayMove)
        }
    }
}
