/* NES emulator By ArDev */
package com.example.nes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.nes.data.AspectRatioMode
import com.example.nes.data.ControlSize
import com.example.nes.data.EmulatorSettings
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    settings: EmulatorSettings,
    onSaveSettings: (EmulatorSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var aspect by remember { mutableStateOf(settings.aspectRatio) }
    var smooth by remember { mutableStateOf(settings.smoothFilter) }
    var audio by remember { mutableStateOf(settings.audioEnabled) }
    var volume by remember { mutableStateOf(settings.audioVolume) }
    var opacity by remember { mutableStateOf(settings.controlsOpacity) }
    var ctrlSize by remember { mutableStateOf(settings.controlSize) }
    var haptic by remember { mutableStateOf(settings.hapticFeedback) }
    var arabic by remember { mutableStateOf(settings.languageArabic) }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NesSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NesBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (arabic) "إعدادات المحاكي" else "Emulator Settings",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "NES emulator By ArDev",
                            style = MaterialTheme.typography.labelSmall,
                            color = NesGold,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NesTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Video
                Text(
                    text = if (arabic) "العرض والفيديو" else "Video & Display",
                    style = MaterialTheme.typography.titleMedium,
                    color = NesCyan
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Aspect Ratio Selector
                Text(
                    text = if (arabic) "نسبة العرض إلى الارتفاع:" else "Aspect Ratio:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NesTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AspectRatioMode.values().forEach { mode ->
                        val isSelected = aspect == mode
                        val label = when (mode) {
                            AspectRatioMode.ORIGINAL_4_3 -> "4:3 NES"
                            AspectRatioMode.SQUARE_1_1 -> "1:1 Square"
                            AspectRatioMode.FULL_STRETCH -> if (arabic) "ملء الشاشة" else "Stretch"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NesRed else NesSurfaceCard)
                                .clickable { aspect = mode }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else NesTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Smooth Filter Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (arabic) "تنعيم البكسل (Smooth Filter)" else "Smooth Filter",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NesTextPrimary
                        )
                        Text(
                            text = if (arabic) "فلتر ثنائي خطي للصورة" else "Bilinear image filtering",
                            style = MaterialTheme.typography.bodySmall,
                            color = NesTextSecondary
                        )
                    }
                    Switch(
                        checked = smooth,
                        onCheckedChange = { smooth = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NesRed, checkedTrackColor = NesRedDark)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 14.dp), color = NesBorder)

                // Section: Audio
                Text(
                    text = if (arabic) "الصوتيات" else "Audio",
                    style = MaterialTheme.typography.titleMedium,
                    color = NesCyan
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (arabic) "تفعيل صوت المحاكي" else "Enable Audio",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NesTextPrimary
                    )
                    Switch(
                        checked = audio,
                        onCheckedChange = { audio = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NesRed, checkedTrackColor = NesRedDark)
                    )
                }

                if (audio) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${if (arabic) "مستوى الصوت:" else "Volume:"} ${(volume * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = NesTextSecondary
                    )
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = NesRed, activeTrackColor = NesRed)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 14.dp), color = NesBorder)

                // Section: Controls
                Text(
                    text = if (arabic) "التحكم باللمس" else "Touch Controls",
                    style = MaterialTheme.typography.titleMedium,
                    color = NesCyan
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Control Size Selector
                Text(
                    text = if (arabic) "حجم أزرار التحكم:" else "Button Size:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NesTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ControlSize.values().forEach { size ->
                        val isSelected = ctrlSize == size
                        val label = when (size) {
                            ControlSize.SMALL -> if (arabic) "صغير" else "Small"
                            ControlSize.NORMAL -> if (arabic) "متوسط" else "Normal"
                            ControlSize.LARGE -> if (arabic) "كبير" else "Large"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NesRed else NesSurfaceCard)
                                .clickable { ctrlSize = size }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else NesTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Opacity Slider
                Text(
                    text = "${if (arabic) "شفافية الأزرار:" else "Controls Opacity:"} ${(opacity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = NesTextSecondary
                )
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    valueRange = 0.2f..1f,
                    colors = SliderDefaults.colors(thumbColor = NesCyan, activeTrackColor = NesCyan)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Haptic feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (arabic) "الاهتزاز عند اللمس (Haptic)" else "Haptic Feedback",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NesTextPrimary
                        )
                        Text(
                            text = if (arabic) "استجابة اهتزازية عند ضغط الأزرار" else "Vibrate slightly on button press",
                            style = MaterialTheme.typography.bodySmall,
                            color = NesTextSecondary
                        )
                    }
                    Switch(
                        checked = haptic,
                        onCheckedChange = { haptic = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NesRed, checkedTrackColor = NesRedDark)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 14.dp), color = NesBorder)

                // Section: Language
                Text(
                    text = if (arabic) "اللغة (Language)" else "Language",
                    style = MaterialTheme.typography.titleMedium,
                    color = NesCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (arabic) NesRed else NesSurfaceCard)
                            .clickable { arabic = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "العربية",
                            color = if (arabic) Color.White else NesTextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!arabic) NesRed else NesSurfaceCard)
                            .clickable { arabic = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "English",
                            color = if (!arabic) Color.White else NesTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Watermark & Mapper Info Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NesSurfaceCard)
                        .border(1.dp, NesBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "NES emulator By ArDev",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (arabic) "يدعم Mappers: NROM (0), MMC1 (1), UxROM (2), CNROM (3)"
                            else "Supports Mappers: NROM (0), MMC1 (1), UxROM (2), CNROM (3)",
                            fontSize = 11.sp,
                            color = NesTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save Button
                Button(
                    onClick = {
                        val newSettings = settings.copy(
                            aspectRatio = aspect,
                            smoothFilter = smooth,
                            audioEnabled = audio,
                            audioVolume = volume,
                            controlsOpacity = opacity,
                            controlSize = ctrlSize,
                            hapticFeedback = haptic,
                            languageArabic = arabic
                        )
                        onSaveSettings(newSettings)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NesRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (arabic) "حفظ التغييرات" else "Save Settings",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
