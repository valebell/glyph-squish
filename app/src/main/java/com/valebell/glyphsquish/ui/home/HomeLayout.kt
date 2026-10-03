package com.valebell.glyphsquish.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valebell.glyphsquish.creature.domain.CreatureState
import com.valebell.glyphsquish.ui.components.ScreenEdgeInsets
import com.valebell.glyphsquish.ui.components.TopBarFadeHeight
import com.valebell.glyphsquish.ui.forms.TWO_COLUMN_FORMS_MIN_WIDTH
import com.valebell.glyphsquish.ui.slime.SlimeMatrixState
import com.valebell.glyphsquish.ui.theme.SquishSpacing

@Composable
internal fun HomeContent(
    creature: CreatureState,
    selectedSection: SquishSection,
    previewState: SlimeMatrixState,
    actions: HomeActions,
    edgeInsets: ScreenEdgeInsets,
    topInset: Dp,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val fontScale = LocalDensity.current.fontScale
        val previewScrollState = rememberScrollState()
        val availableHeight = maxOf(0.dp, maxHeight - topInset)
        val compactPreview =
            availableHeight < COMPACT_LAYOUT_MAX_HEIGHT || fontScale >= COMPACT_FONT_SCALE
        val twoPaneHorizontalPadding =
            if (compactPreview) {
                SquishSpacing.Screen
            } else {
                SquishSpacing.ScreenWide
            }
        val previewPaneWidth =
            if (compactPreview) {
                COMPACT_PREVIEW_PANE_WIDTH
            } else {
                EXPANDED_PREVIEW_PANE_WIDTH
            }
        val boundedWidth = minOf(maxWidth, SquishSpacing.ContentMaxWidth)
        val twoPaneContentWidth =
            boundedWidth -
                edgeInsets.start -
                edgeInsets.end -
                twoPaneHorizontalPadding * 2
        val splitLayout =
            twoPaneContentWidth >=
                previewPaneWidth + SquishSpacing.Section + TWO_COLUMN_FORMS_MIN_WIDTH * fontScale.coerceAtLeast(1f)
        val horizontalPadding =
            if (splitLayout) {
                twoPaneHorizontalPadding
            } else {
                SquishSpacing.Screen
            }
        val previewSize =
            when {
                compactPreview -> {
                    minOf(
                        COMPACT_ROW_PREVIEW_SIZE,
                        (availableHeight * COMPACT_ROW_PREVIEW_HEIGHT_FRACTION)
                            .coerceAtLeast(MINIMUM_COMPACT_PREVIEW_SIZE),
                    )
                }

                splitLayout -> {
                    WIDE_PREVIEW_SIZE
                }

                else -> {
                    minOf(
                        COMPACT_PREVIEW_SIZE,
                        (maxWidth - edgeInsets.start - edgeInsets.end - horizontalPadding * 2)
                            .coerceAtLeast(MINIMUM_PREVIEW_SIZE),
                        (availableHeight * COMPACT_PREVIEW_HEIGHT_FRACTION)
                            .coerceAtLeast(MINIMUM_PREVIEW_SIZE),
                    )
                }
            }

        if (splitLayout) {
            Row(
                modifier =
                    Modifier
                        .widthIn(max = SquishSpacing.ContentMaxWidth)
                        .fillMaxSize()
                        .align(Alignment.TopCenter)
                        .padding(
                            start = edgeInsets.start + horizontalPadding,
                            top = maxOf(0.dp, topInset - TopBarFadeHeight),
                            end = edgeInsets.end + horizontalPadding,
                        ),
                horizontalArrangement = Arrangement.spacedBy(SquishSpacing.Section),
            ) {
                Column(
                    modifier =
                        Modifier
                            .width(previewPaneWidth)
                            .fillMaxHeight()
                            .verticalScroll(previewScrollState)
                            .padding(
                                top = SquishSpacing.Item,
                                bottom = edgeInsets.bottom + FLOATING_CONTROL_CLEARANCE,
                            ),
                ) {
                    PreviewPanel(
                        creature = creature,
                        selectedSection = selectedSection,
                        previewState = previewState,
                        previewSize = previewSize,
                        compact = compactPreview,
                        onReplayMove = actions.onReplayMove,
                    )
                }
                Gallery(
                    creature = creature,
                    selectedSection = selectedSection,
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    actions = actions.gallery,
                    layout =
                        GalleryLayout(
                            animateSectionChange = true,
                            contentPadding =
                                PaddingValues(
                                    top = SquishSpacing.Item,
                                    bottom = edgeInsets.bottom + FLOATING_CONTROL_CLEARANCE,
                                ),
                        ),
                )
            }
        } else {
            Gallery(
                creature = creature,
                selectedSection = selectedSection,
                modifier =
                    Modifier
                        .widthIn(max = SquishSpacing.PhoneContentMaxWidth)
                        .fillMaxSize()
                        .align(Alignment.TopCenter),
                actions = actions.gallery,
                layout =
                    GalleryLayout(
                        animateSectionChange = false,
                        scrollToPreviewOnSelection = maxHeight > maxWidth,
                        contentPadding =
                            PaddingValues(
                                start = edgeInsets.start + horizontalPadding,
                                top = topInset + SquishSpacing.Item,
                                end = edgeInsets.end + horizontalPadding,
                                bottom = edgeInsets.bottom + FLOATING_CONTROL_CLEARANCE,
                            ),
                        leadingContent = {
                            Column {
                                PreviewPanel(
                                    creature = creature,
                                    selectedSection = selectedSection,
                                    previewState = previewState,
                                    previewSize = previewSize,
                                    compact = compactPreview,
                                    onReplayMove = actions.onReplayMove,
                                )
                                Spacer(Modifier.height(SquishSpacing.Section - SquishSpacing.Item))
                            }
                        },
                    ),
            )
        }

        ExpressiveSectionControl(
            selectedSection = selectedSection,
            onSelectSection = actions.onSelectSection,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = edgeInsets.start + SquishSpacing.Screen,
                        end = edgeInsets.end + SquishSpacing.Screen,
                        bottom = edgeInsets.bottom + SquishSpacing.Item,
                    ),
        )
    }
}

private val COMPACT_LAYOUT_MAX_HEIGHT = 600.dp
private const val COMPACT_FONT_SCALE = 1.3f
private val WIDE_PREVIEW_SIZE = 340.dp
private val EXPANDED_PREVIEW_PANE_WIDTH = WIDE_PREVIEW_SIZE + SquishSpacing.Card * 2
private val COMPACT_PREVIEW_PANE_WIDTH = 280.dp
private val COMPACT_PREVIEW_SIZE = 220.dp
private val COMPACT_ROW_PREVIEW_SIZE = 150.dp
private val MINIMUM_PREVIEW_SIZE = 160.dp
private val MINIMUM_COMPACT_PREVIEW_SIZE = 112.dp
private const val COMPACT_PREVIEW_HEIGHT_FRACTION = 0.32f
private const val COMPACT_ROW_PREVIEW_HEIGHT_FRACTION = 0.44f
private val FLOATING_CONTROL_CLEARANCE = 84.dp
