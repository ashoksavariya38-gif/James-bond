package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.StudyLanguage
import com.example.model.StudyModelMode
import com.example.ui.theme.LocalLuminaColors

@Composable
fun StudyTopBar(
  showDrawerToggle: Boolean,
  onOpenDrawer: () -> Unit,
  selectedMode: StudyModelMode,
  onSelectMode: (StudyModelMode) -> Unit,
  selectedLanguage: StudyLanguage,
  onSelectLanguage: (StudyLanguage) -> Unit,
  darkMode: Boolean,
  onToggleTheme: () -> Unit,
  isPluginDrawerOpen: Boolean,
  onTogglePluginDrawer: () -> Unit,
) {
  val luminaColors = LocalLuminaColors.current
  var modelMenuExpanded by remember { mutableStateOf(false) }
  var languageMenuExpanded by remember { mutableStateOf(false) }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (showDrawerToggle) {
          IconButton(
            onClick = onOpenDrawer,
            modifier = Modifier.testTag("open_sidebar_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Menu,
              contentDescription = "Open study sidebar",
              tint = MaterialTheme.colorScheme.onBackground,
            )
          }
        }

        LuminaSparkleLogo(size = 28.dp)

        // Model Selector Pill
        Box(modifier = Modifier.weight(1f, fill = false)) {
          Surface(
            color = luminaColors.elevatedCard,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, luminaColors.borderSubtle),
            modifier = Modifier
              .clickable { modelMenuExpanded = true }
              .testTag("model_selector_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = luminaColors.brandBlue,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = selectedMode.shortLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Select AI model mode",
                tint = luminaColors.textSecondary,
                modifier = Modifier.size(16.dp),
              )
            }
          }

          DropdownMenu(
            expanded = modelMenuExpanded,
            onDismissRequest = { modelMenuExpanded = false },
          ) {
            StudyModelMode.entries.forEach { mode ->
              DropdownMenuItem(
                text = {
                  Column {
                    Text(mode.label, style = MaterialTheme.typography.labelLarge)
                    Text(
                      mode.badge,
                      style = MaterialTheme.typography.labelSmall,
                      color = luminaColors.brandCyan,
                    )
                  }
                },
                trailingIcon = {
                  if (mode == selectedMode) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = luminaColors.brandBlue)
                  }
                },
                onClick = {
                  modelMenuExpanded = false
                  onSelectMode(mode)
                },
              )
            }
          }
        }

        // Language Selector Pill
        Box {
          Surface(
            color = luminaColors.elevatedCard,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, luminaColors.borderSubtle),
            modifier = Modifier
              .clickable { languageMenuExpanded = true }
              .testTag("language_selector_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "Select study language",
                tint = luminaColors.brandCyan,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = if (selectedLanguage == StudyLanguage.AUTO) "Auto" else selectedLanguage.code.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }

          DropdownMenu(
            expanded = languageMenuExpanded,
            onDismissRequest = { languageMenuExpanded = false },
          ) {
            StudyLanguage.entries.forEach { lang ->
              DropdownMenuItem(
                text = {
                  Text(
                    "${lang.displayName} (${lang.nativeName})",
                    style = MaterialTheme.typography.bodyMedium,
                  )
                },
                trailingIcon = {
                  if (lang == selectedLanguage) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = luminaColors.brandCyan)
                  }
                },
                onClick = {
                  languageMenuExpanded = false
                  onSelectLanguage(lang)
                },
              )
            }
          }
        }

        // Theme Toggle
        IconButton(
          onClick = onToggleTheme,
          modifier = Modifier.testTag("theme_toggle_button"),
        ) {
          Icon(
            imageVector = if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
            contentDescription = if (darkMode) "Switch to light mode" else "Switch to dark mode",
            tint = luminaColors.textSecondary,
          )
        }

        // Right Plugin Drawer Toggle
        Surface(
          color = if (isPluginDrawerOpen) luminaColors.brandPurple.copy(alpha = 0.22f) else luminaColors.elevatedCard,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(
            1.dp,
            if (isPluginDrawerOpen) luminaColors.brandPurple else luminaColors.borderSubtle,
          ),
          modifier = Modifier
            .clickable { onTogglePluginDrawer() }
            .testTag("plugin_drawer_toggle"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Extension,
              contentDescription = "Toggle study plugins drawer",
              tint = if (isPluginDrawerOpen) luminaColors.brandCyan else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
      HorizontalDivider(color = luminaColors.borderSubtle)
    }
  }
}
