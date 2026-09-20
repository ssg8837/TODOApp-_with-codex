package com.example.todoapplication.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.todoapplication.R
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.ui.theme.toComposeColor

@Composable
internal fun CategoryColorPicker(
    selected: CategoryColor,
    enabled: Boolean,
    onSelected: (CategoryColor) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        TextButton(
            onClick = { expanded = !expanded },
            enabled = enabled,
            modifier = Modifier.testTag("category-palette-toggle"),
        ) {
            Box(Modifier.size(20.dp).background(selected.toComposeColor(), CircleShape))
            Text(stringResource(R.string.category_choose_color, colorName(selected)),
                modifier = Modifier.padding(start = 8.dp))
        }
        if (expanded) {
            Column(Modifier.selectableGroup()) {
                CategoryColor.entries.chunked(4).forEach { colors ->
                    Row {
                        colors.forEach { color ->
                            val name = colorName(color)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("category-palette-" + color.name)
                                    .semantics { contentDescription = name }
                                    .selectable(
                                        selected = color == selected,
                                        enabled = enabled,
                                        role = Role.RadioButton,
                                        onClick = { onSelected(color) },
                                    ),
                            ) {
                                Box(
                                    Modifier.size(28.dp)
                                        .border(
                                            if (color == selected) 3.dp else 0.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape,
                                        )
                                        .padding(4.dp)
                                        .background(color.toComposeColor(), CircleShape),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun colorName(color: CategoryColor): String = stringResource(
    when (color) {
        CategoryColor.NEUTRAL -> R.string.category_color_neutral
        CategoryColor.RED -> R.string.category_color_red
        CategoryColor.ORANGE -> R.string.category_color_orange
        CategoryColor.YELLOW -> R.string.category_color_yellow
        CategoryColor.GREEN -> R.string.category_color_green
        CategoryColor.BLUE -> R.string.category_color_blue
        CategoryColor.PURPLE -> R.string.category_color_purple
        CategoryColor.PINK -> R.string.category_color_pink
    },
)
