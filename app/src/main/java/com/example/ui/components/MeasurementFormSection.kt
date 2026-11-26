package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.localization.BiText
import com.example.model.LanguageMode
import com.example.model.MeasurementUnit
import com.example.model.ShalwarKameezMeasurement
import com.example.model.ShalwarStyleOption
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShalwarKameezMeasurementFormSection(
    measurement: ShalwarKameezMeasurement,
    onMeasurementChange: (ShalwarKameezMeasurement) -> Unit,
    languageMode: LanguageMode,
    modifier: Modifier = Modifier
) {
    val activeUnit = MeasurementUnit.fromCode(measurement.unit)
    val unitSuffix = AppStrings.unitShort(activeUnit.code, languageMode)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Unit Selector Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BilingualText(
                    text = AppStrings.unitLabel,
                    languageMode = languageMode,
                    primaryStyle = MaterialTheme.typography.titleSmall
                )
                UnitToggleBar(
                    selectedUnit = activeUnit,
                    onUnitSelected = { newUnit ->
                        onMeasurementChange(measurement.copy(unit = newUnit.code))
                    },
                    languageMode = languageMode
                )
                if (measurement.hasAnyMeasurement()) {
                    val targetUnit = if (activeUnit == MeasurementUnit.INCHES) {
                        MeasurementUnit.CENTIMETERS
                    } else {
                        MeasurementUnit.INCHES
                    }
                    OutlinedButton(
                        onClick = {
                            onMeasurementChange(measurement.convertedTo(targetUnit))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${AppStrings.convertValuesPrompt.primary(languageMode)} → ${targetUnit.code}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // Section 1: Kameez / Shirt Measurements (12 fields)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeaderPill(
                    title = AppStrings.kameezSection,
                    badge = "12 • $unitSuffix",
                    languageMode = languageMode
                )

                MeasurementPairRow(
                    leftLabel = AppStrings.kameezLength,
                    leftValue = measurement.kameezLength,
                    onLeftChange = { onMeasurementChange(measurement.copy(kameezLength = it)) },
                    leftTag = "input_kameez_length",
                    rightLabel = AppStrings.shoulder,
                    rightValue = measurement.shoulder,
                    onRightChange = { onMeasurementChange(measurement.copy(shoulder = it)) },
                    rightTag = "input_shoulder",
                    unitSuffix = unitSuffix,
                    languageMode = languageMode
                )
                MeasurementPairRow(
                    leftLabel = AppStrings.chest,
                    leftValue = measurement.chest,
                    onLeftChange = { onMeasurementChange(measurement.copy(chest = it)) },
                    leftTag = "input_chest",
                    rightLabel = AppStrings.kameezWaist,
                    rightValue = measurement.kameezWaist,
                    onRightChange = { onMeasurementChange(measurement.copy(kameezWaist = it)) },
                    rightTag = "input_kameez_waist",
                    unitSuffix = unitSuffix,
                    languageMode = languageMode
                )
                MeasurementPairRow(
                    leftLabel = AppStrings.kameezHip,
                    leftValue = measurement.kameezHip,
                    onLeftChange = { onMeasurementChange(measurement.copy(kameezHip = it)) },
                    leftTag = "input_kameez_hip",
                    rightLabel = AppStrings.sleeveLength,
                    rightValue = measurement.sleeveLength,
                    onRightChange = { onMeasurementChange(measurement.copy(sleeveLength = it)) },
                    rightTag = "input_sleeve_length",
                    unitSuffix = unitSuffix,
                    languageMode = languageMode
                )
                MeasurementPairRow(
                    leftLabel = AppStrings.armhole,
                    leftValue = measurement.armhole,
                    onLeftChange = { onMeasurementChange(measurement.copy(armhole = it)) },
                    leftTag = "input_armhole",
                    rightLabel = AppStrings.neck,
                    rightValue = measurement.neck,
                    onRightChange = { onMeasurementChange(measurement.copy(neck = it)) },
                    rightTag = "input_neck",
                    unitSuffix = unitSuffix,
                    languageMode = languageMode
                )
                MeasurementPairRow(
                    leftLabel = AppStrings.cuff,
                    leftValue = measurement.cuff,
                    onLeftChange = { onMeasurementChange(measurement.copy(cuff = it)) },
                    leftTag = "input_cuff",
                    rightLabel = AppStrings.collar,
                    rightValue = measurement.collar,
                    onRightChange = { onMeasurementChange(measurement.copy(collar = it)) },
                    rightTag = "input_collar",
                    unitSuffix = unitSuffix,
                    languageMode = languageMode
                )
                MeasurementPairRow(
                    leftLabel = AppStrings.frontLength,
                    leftValue = measurement.frontLength,
                    onLeftChange = { onMeasurementChange(measurement.copy(frontLength = it)) },
                    leftTag = "input_front_length",
                    rightLabel = AppStrings.backLength,
                    rightValue = measurement.backLength,
                    onRightChange = { onMeasurementChange(measurement.copy(backLength = it)) },
                    rightTag = "input_back_length",
                    unitSuffix = unitSuffix,
                    languageMode = languageMode
                )
            }
        }

        // Section 2: Shalwar Measurements (7 fields)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeaderPill(
                    title = AppStrings.shalwarSection,
                    badge = "7 • $unitSuffix",
                    languageMode = languageMode
                )

                MeasurementPairRow(
                    leftLabel = AppStrings.shalwarLength,
                    leftValue = measurement.shalwarLength,
                    onLeftChange = { onMeasurementChange(measurement.copy(shalwarLength = it)) },
                    leftTag = "input_shalwar_length",
                    rightLabel = AppStrings.shalwarWaist,
                    rightValue = measurement.shalwarWaist,
                    onRightChange = { onMeasurementChange(measurement.copy(shalwarWaist = it)) },
                    rightTag = "input_shalwar_waist",
                    unitSuffix = unitSuffix,
                    languageMode = languageMode
                )
                MeasurementPairRow(
                    leftLabel = AppStrings.shalwarHip,
                    leftValue = measurement.shalwarHip,
                    onLeftChange = { onMeasurementChange(measurement.copy(shalwarHip = it)) },
                    leftTag = "input_shalwar_hip",
                    rightLabel = AppStrings.thigh,
                    rightValue = measurement.thigh,
                    onRightChange = { onMeasurementChange(measurement.copy(thigh = it)) },
                    rightTag = "input_thigh",
                    unitSuffix = unitSuffix,
                    languageMode = languageMode
                )
                MeasurementPairRow(
                    leftLabel = AppStrings.knee,
                    leftValue = measurement.knee,
                    onLeftChange = { onMeasurementChange(measurement.copy(knee = it)) },
                    leftTag = "input_knee",
                    rightLabel = AppStrings.bottomPaicha,
                    rightValue = measurement.bottomPaicha,
                    onRightChange = { onMeasurementChange(measurement.copy(bottomPaicha = it)) },
                    rightTag = "input_bottom_paicha",
                    unitSuffix = unitSuffix,
                    languageMode = languageMode
                )

                Spacer(modifier = Modifier.height(2.dp))

                BilingualText(
                    text = AppStrings.shalwarStyle,
                    languageMode = languageMode,
                    primaryStyle = MaterialTheme.typography.titleSmall
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShalwarStyleOption.entries.forEach { option ->
                        val selected = measurement.shalwarStyle.equals(option.en, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { onMeasurementChange(measurement.copy(shalwarStyle = option.en)) },
                            label = {
                                Text(
                                    text = when (languageMode) {
                                        LanguageMode.EN -> option.en
                                        LanguageMode.UR -> option.ur
                                        LanguageMode.BOTH -> "${option.en} (${option.ur})"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = measurement.shalwarStyle,
                    onValueChange = { onMeasurementChange(measurement.copy(shalwarStyle = it.take(60))) },
                    label = { Text(AppStrings.shalwarStyle.inline(languageMode)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_shalwar_style")
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderPill(
    title: BiText,
    badge: String,
    languageMode: LanguageMode
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BilingualText(
            text = title,
            languageMode = languageMode,
            primaryStyle = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun MeasurementPairRow(
    leftLabel: BiText,
    leftValue: String,
    onLeftChange: (String) -> Unit,
    leftTag: String,
    rightLabel: BiText,
    rightValue: String,
    onRightChange: (String) -> Unit,
    rightTag: String,
    unitSuffix: String,
    languageMode: LanguageMode
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MeasurementField(
            label = leftLabel,
            value = leftValue,
            onValueChange = onLeftChange,
            unitSuffix = unitSuffix,
            languageMode = languageMode,
            modifier = Modifier
                .weight(1f)
                .testTag(leftTag)
        )
        MeasurementField(
            label = rightLabel,
            value = rightValue,
            onValueChange = onRightChange,
            unitSuffix = unitSuffix,
            languageMode = languageMode,
            modifier = Modifier
                .weight(1f)
                .testTag(rightTag)
        )
    }
}

@Composable
private fun MeasurementField(
    label: BiText,
    value: String,
    onValueChange: (String) -> Unit,
    unitSuffix: String,
    languageMode: LanguageMode,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val sanitized = raw.filter { it.isDigit() || it == '.' || it == '/' || it == '-' || it == ' ' }.take(12)
            onValueChange(sanitized)
        },
        label = {
            Column {
                Text(
                    text = label.primary(languageMode),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
                label.secondary(languageMode)?.let { ur ->
                    Text(
                        text = ur,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        },
        suffix = {
            Text(
                text = unitSuffix,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    )
}
