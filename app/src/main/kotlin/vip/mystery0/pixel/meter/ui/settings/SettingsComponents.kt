package vip.mystery0.pixel.meter.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import vip.mystery0.pixel.meter.R

data class SettingsRowItem(
    val key: String,
    val content: @Composable (Modifier) -> Unit,
)

enum class SettingsRowTrailing {
    NONE,
    CHEVRON,
    EXTERNAL_LINK,
}

/** Section header and grouped card container with 16dp horizontal inset. */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/**
 * Segmented rounded-card group modeled on PixelText and PixelTelo.
 * Computes 24dp outer corners and 4dp inner corners dynamically so conditional rows stay continuous.
 */
@Composable
fun SettingsGroup(
    rows: List<SettingsRowItem>,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        rows.forEachIndexed { index, item ->
            key(item.key) {
                val shape = settingsRowShape(
                    first = index == 0,
                    last = index == rows.lastIndex,
                )
                item.content(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(backgroundColor),
                )
            }
        }
    }
}

@Composable
fun SettingsRowContainer(
    first: Boolean,
    last: Boolean,
    active: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(settingsRowShape(first = first, last = last))
            .background(
                if (active) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            ),
    ) {
        content()
    }
}

private fun settingsRowShape(first: Boolean, last: Boolean): RoundedCornerShape {
    return RoundedCornerShape(
        topStart = if (first) 24.dp else 4.dp,
        topEnd = if (first) 24.dp else 4.dp,
        bottomStart = if (last) 24.dp else 4.dp,
        bottomEnd = if (last) 24.dp else 4.dp,
    )
}

@Composable
fun SettingsItemRow(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    summaryContent: (@Composable () -> Unit)? = null,
    icon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailing: SettingsRowTrailing? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    summaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
) {
    val hasSummary = !summary.isNullOrEmpty() || summaryContent != null
    val effectiveTrailing = trailing ?: if (onClick != null) {
        SettingsRowTrailing.CHEVRON
    } else {
        SettingsRowTrailing.NONE
    }
    val textOpacity = if (enabled) 1f else 0.6f
    val iconOpacity = if (enabled) 1f else 0.38f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .defaultMinSize(minHeight = if (hasSummary) 72.dp else 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null || leadingContent != null) {
            SettingsLeadingIcon(
                icon = icon,
                enabled = enabled,
                leadingContent = leadingContent,
                tint = iconTint,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(textOpacity),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
            )
            if (!summary.isNullOrEmpty()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = summaryColor,
                )
            }
            summaryContent?.invoke()
        }
        if (trailingContent != null) {
            Box(modifier = Modifier.alpha(iconOpacity)) {
                trailingContent()
            }
        } else {
            SettingsTrailingIcon(
                trailing = effectiveTrailing,
                enabled = enabled,
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    checked: Boolean,
    icon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val textOpacity = if (enabled) 1f else 0.6f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .defaultMinSize(minHeight = if (summary.isNullOrEmpty()) 56.dp else 72.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null || leadingContent != null) {
            SettingsLeadingIcon(
                icon = icon,
                enabled = enabled,
                leadingContent = leadingContent,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(textOpacity),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!summary.isNullOrEmpty()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/**
 * Android System Settings-style two-target row: tapping the left body navigates to detail settings,
 * while a vertical divider separates an independent toggleable Switch target on the right.
 */
@Composable
fun SettingsTwoTargetSwitchRow(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    switchEnabled: Boolean = enabled,
) {
    val textOpacity = if (enabled) 1f else 0.6f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .defaultMinSize(minHeight = if (summary.isNullOrEmpty()) 56.dp else 72.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (icon != null || leadingContent != null) {
                SettingsLeadingIcon(
                    icon = icon,
                    enabled = enabled,
                    leadingContent = leadingContent,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(textOpacity),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!summary.isNullOrEmpty()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .semantics { contentDescription = title }
                .toggleable(
                    value = checked,
                    enabled = switchEnabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = switchEnabled,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

@Composable
fun <T> SettingsChoiceRow(
    modifier: Modifier = Modifier,
    value: T,
    options: List<T>,
    onValueChange: (T) -> Unit,
    title: String,
    summary: String? = null,
    summaryContent: (@Composable () -> Unit)? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    optionLabel: @Composable (T) -> String,
) {
    val active = LocalNavigationActive.current
    var showDialog by remember { mutableStateOf(false) }

    SettingsItemRow(
        modifier = modifier,
        title = title,
        summary = summary,
        summaryContent = summaryContent,
        icon = icon,
        enabled = enabled,
        onClick = { showDialog = true },
    )

    if (active && showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .selectableGroup(),
                ) {
                    options.forEach { option ->
                        val selected = value == option
                        val selectOption = {
                            onValueChange(option)
                            showDialog = false
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = selectOption,
                                )
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                            )
                            Text(
                                text = optionLabel(option),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun SettingsSliderRow(
    modifier: Modifier = Modifier,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    summary: String? = null,
    icon: ImageVector? = null,
    steps: Int = 0,
    enabled: Boolean = true,
) {
    var sliderPosition by remember(value) { mutableFloatStateOf(value.coerceIn(valueRange)) }
    val textOpacity = if (enabled) 1f else 0.6f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (icon != null) {
                SettingsLeadingIcon(icon = icon, enabled = enabled)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(textOpacity),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                if (!summary.isNullOrEmpty()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Slider(
            value = sliderPosition,
            onValueChange = { nextValue ->
                sliderPosition = nextValue
                onValueChange(nextValue)
            },
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (icon != null) 48.dp else 0.dp),
        )
    }
}

@Composable
fun SettingsTextFieldRow(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val active = LocalNavigationActive.current
    var showDialog by remember { mutableStateOf(false) }

    SettingsItemRow(
        modifier = modifier,
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        onClick = { showDialog = true },
    )

    if (active && showDialog) {
        var draft by remember(value) { mutableStateOf(value) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(draft)
                        showDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsLeadingIcon(
    icon: ImageVector?,
    enabled: Boolean,
    leadingContent: (@Composable () -> Unit)? = null,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .alpha(if (enabled) 1f else 0.38f),
        contentAlignment = Alignment.Center,
    ) {
        when {
            leadingContent != null -> leadingContent()
            icon != null -> Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = tint,
            )
        }
    }
}

@Composable
private fun SettingsTrailingIcon(
    trailing: SettingsRowTrailing,
    enabled: Boolean,
) {
    val opacity = if (enabled) 1f else 0.38f
    when (trailing) {
        SettingsRowTrailing.NONE -> Unit
        SettingsRowTrailing.CHEVRON -> Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .alpha(opacity),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SettingsRowTrailing.EXTERNAL_LINK -> Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = stringResource(R.string.settings_open_external_cd),
            modifier = Modifier
                .size(24.dp)
                .alpha(opacity),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
