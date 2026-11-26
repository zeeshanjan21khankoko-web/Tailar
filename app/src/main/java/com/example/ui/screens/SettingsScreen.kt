package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.LanguageMode
import com.example.model.MeasurementUnit
import com.example.model.ShopProfile
import com.example.ui.components.BilingualText
import com.example.ui.components.LanguageSelectorPills
import com.example.ui.components.UnitToggleBar

@Composable
fun SettingsScreen(
    shopProfile: ShopProfile?,
    userEmail: String?,
    languageMode: LanguageMode,
    defaultUnit: MeasurementUnit,
    isSaving: Boolean,
    onLanguageChange: (LanguageMode) -> Unit,
    onDefaultUnitChange: (MeasurementUnit) -> Unit,
    onSaveShopProfile: (
        shopName: String,
        shopPhone: String,
        shopAddress: String,
        unit: MeasurementUnit,
        lang: LanguageMode
    ) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var shopName by remember(shopProfile) {
        mutableStateOf(shopProfile?.shopName ?: "TailorBook Bespoke Studio")
    }
    var shopPhone by remember(shopProfile) {
        mutableStateOf(shopProfile?.shopPhone ?: "")
    }
    var shopAddress by remember(shopProfile) {
        mutableStateOf(shopProfile?.shopAddress ?: "")
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shop Profile Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BilingualText(
                        text = AppStrings.navSettings,
                        languageMode = languageMode,
                        primaryStyle = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it.take(100) },
                        label = { Text(AppStrings.shopName.inline(languageMode)) },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_shop_name")
                    )

                    OutlinedTextField(
                        value = shopPhone,
                        onValueChange = { shopPhone = it.take(25) },
                        label = { Text(AppStrings.shopPhone.inline(languageMode)) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_shop_phone")
                    )

                    OutlinedTextField(
                        value = shopAddress,
                        onValueChange = { shopAddress = it.take(250) },
                        label = { Text(AppStrings.shopAddress.inline(languageMode)) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        maxLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_shop_address")
                    )

                    BilingualText(
                        text = AppStrings.unitLabel,
                        languageMode = languageMode,
                        primaryStyle = MaterialTheme.typography.titleSmall
                    )
                    UnitToggleBar(
                        selectedUnit = defaultUnit,
                        onUnitSelected = onDefaultUnitChange,
                        languageMode = languageMode
                    )

                    BilingualText(
                        text = AppStrings.languageLabel,
                        languageMode = languageMode,
                        primaryStyle = MaterialTheme.typography.titleSmall
                    )
                    LanguageSelectorPills(
                        selectedMode = languageMode,
                        onSelectMode = onLanguageChange
                    )

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            onSaveShopProfile(
                                shopName.ifBlank { "TailorBook Bespoke Studio" },
                                shopPhone,
                                shopAddress,
                                defaultUnit,
                                languageMode
                            )
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_shop_profile_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = AppStrings.saveSettings.inline(languageMode),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Account Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = userEmail ?: "Google Account Connected",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = onSignOut,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_out_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(AppStrings.signOut.inline(languageMode))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
