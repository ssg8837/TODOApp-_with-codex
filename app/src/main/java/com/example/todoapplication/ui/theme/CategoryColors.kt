package com.example.todoapplication.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.todoapplication.domain.model.CategoryColor

fun CategoryColor.toComposeColor(): Color = when (this) {
    CategoryColor.NEUTRAL -> Color(0xFF616161)
    CategoryColor.RED -> Color(0xFFC62828)
    CategoryColor.ORANGE -> Color(0xFFEF6C00)
    CategoryColor.YELLOW -> Color(0xFFF9A825)
    CategoryColor.GREEN -> Color(0xFF2E7D32)
    CategoryColor.BLUE -> Color(0xFF1565C0)
    CategoryColor.PURPLE -> Color(0xFF6A1B9A)
    CategoryColor.PINK -> Color(0xFFAD1457)
}
