package com.valebell.glyphsquish.ui.forms

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.valebell.glyphsquish.R
import com.valebell.glyphsquish.creature.domain.CreatureForm
import com.valebell.glyphsquish.creature.domain.CreatureForms
import com.valebell.glyphsquish.creature.domain.CreatureState
import com.valebell.glyphsquish.nameResource
import com.valebell.glyphsquish.ui.components.Badge
import com.valebell.glyphsquish.ui.components.GalleryCard
import com.valebell.glyphsquish.ui.components.GalleryHeader
import com.valebell.glyphsquish.ui.components.LockFeedbackIcon
import com.valebell.glyphsquish.ui.slime.FormThumbnail
import com.valebell.glyphsquish.ui.theme.SquishSpacing

internal fun LazyStaggeredGridScope.formsGalleryItems(
    creature: CreatureState,
    onSelectForm: (CreatureForm) -> Unit,
    newForms: Set<CreatureForm>,
    visibleForms: Set<CreatureForm>,
) {
    val availableForms = CreatureForms.availableFor(creature.unlockedMoves)
    item(
        key = "forms-header",
        span = StaggeredGridItemSpan.FullLine,
    ) {
        GalleryHeader(
            title = stringResource(R.string.form_gallery_title),
            progress =
                pluralStringResource(
                    R.plurals.form_gallery_discovered_format,
                    availableForms.size,
                    availableForms.size,
                    CreatureForm.entries.size,
                ),
            modifier = Modifier.animateItem(),
        )
    }
    items(
        items = CreatureForm.entries,
        key = { "form-$it" },
    ) { form ->
        val unlocked = form in availableForms
        FormCard(
            form = form,
            unlocked = unlocked,
            isNew = form in newForms && form != creature.currentForm,
            visible = form in visibleForms,
            current = form == creature.currentForm,
            onClick = { onSelectForm(form) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateItem(),
        )
    }
}

@Composable
private fun FormCard(
    form: CreatureForm,
    unlocked: Boolean,
    isNew: Boolean,
    visible: Boolean,
    current: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val revealProgress = rememberFormRevealProgress(isNew, visible)
    var lockFeedbackTrigger by remember { mutableIntStateOf(0) }
    val status = if (unlocked) null else formRequirement(form)
    val description =
        stringResource(
            if (unlocked) {
                R.string.form_gallery_card_description_unlocked_format
            } else {
                R.string.form_gallery_card_description_locked_format
            },
            stringResource(form.nameResource()),
            status.orEmpty(),
        )

    val newLabel = stringResource(R.string.form_gallery_new_description)
    val onLockedClick = {
        lockFeedbackTrigger += 1
        onClick()
    }
    GalleryCard(
        onClick = if (unlocked) onClick else onLockedClick,
        description = description,
        selected = current,
        actionLabel = if (unlocked) stringResource(R.string.select_form_action) else null,
        modifier =
            modifier.formDiscoveryEffect(isNew, visible, revealProgress).semantics {
                if (isNew) stateDescription = newLabel
            },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(SquishSpacing.Card),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FormDiscoveryThumbnail(
                form,
                unlocked,
                revealProgress,
                Modifier.size(72.dp),
                lockFeedbackTrigger,
            )
            FormCardLabels(form, status, current)
        }
    }
}

@Composable
private fun FormCardLabels(
    form: CreatureForm,
    status: String?,
    current: Boolean,
) {
    Text(
        text = stringResource(form.nameResource()),
        modifier = Modifier.padding(top = SquishSpacing.Compact),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    if (status != null) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = SquishSpacing.Tiny),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = status,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                minLines = 2,
            )
        }
    }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = FORM_BADGE_SLOT_HEIGHT),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (current) {
            Badge(
                text = stringResource(R.string.form_gallery_current_badge),
                emphasized = true,
            )
        }
    }
}

@Composable
private fun formRequirement(form: CreatureForm): String =
    stringResource(
        R.string.form_gallery_status_locked_format,
        stringResource(requireNotNull(CreatureForms.requiredMove(form)).nameResource()),
    )

@Composable
internal fun FormDiscoveryThumbnail(
    form: CreatureForm,
    unlocked: Boolean,
    revealProgress: State<Float>,
    modifier: Modifier = Modifier,
    lockFeedbackTrigger: Int = 0,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (unlocked) {
            FormThumbnail(
                form = form,
                contentDescription = null,
                modifier =
                    Modifier.fillMaxSize().graphicsLayer {
                        val reveal = revealProgress.value
                        val fraction = FastOutSlowInEasing.transform(((reveal - 0.25f) / 0.75f).coerceIn(0f, 1f))
                        alpha = fraction
                        scaleX = 0.78f + 0.22f * fraction
                        scaleY = scaleX
                    },
            )
        }
        if (!unlocked || revealProgress.value < 1f) {
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val fraction = if (unlocked) (revealProgress.value / 0.55f).coerceIn(0f, 1f) else 0f
                    alpha = 1f - FastOutSlowInEasing.transform(fraction)
                    scaleX = 1f - 0.16f * fraction
                    scaleY = scaleX
                },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {}
                LockFeedbackIcon(
                    feedbackTrigger = lockFeedbackTrigger,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

internal val FORM_CARD_MIN_WIDTH = 156.dp
internal val TWO_COLUMN_FORMS_MIN_WIDTH =
    FORM_CARD_MIN_WIDTH * 2 + SquishSpacing.Item
private val FORM_BADGE_SLOT_HEIGHT = 28.dp
