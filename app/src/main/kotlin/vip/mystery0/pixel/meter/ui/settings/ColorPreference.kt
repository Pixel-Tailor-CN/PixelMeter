package vip.mystery0.pixel.meter.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.AlphaTile
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import vip.mystery0.pixel.meter.R

@Composable
fun ColorPreference(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    showAlpha: Boolean = true,
    onColorSelected: (Color) -> Unit,
) {
    val active = LocalNavigationActive.current
    var showDialog by remember { mutableStateOf(false) }

    SettingsItemRow(
        modifier = modifier,
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        trailingContent = {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    ),
            )
        },
        onClick = { showDialog = true },
    )

    if (active && showDialog) {
        val controller = rememberColorPickerController()
        var selectedColor by remember { mutableStateOf(color) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.color_picker_title)) },
            text = {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HsvColorPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        controller = controller,
                        initialColor = color,
                        onColorChanged = { envelope ->
                            selectedColor = envelope.color
                        },
                    )
                    if (showAlpha) {
                        AlphaSlider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            controller = controller,
                        )
                    }
                    BrightnessSlider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        controller = controller,
                    )
                    if (showAlpha) {
                        AlphaTile(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(MaterialTheme.shapes.medium),
                            controller = controller,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onColorSelected(selectedColor)
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
