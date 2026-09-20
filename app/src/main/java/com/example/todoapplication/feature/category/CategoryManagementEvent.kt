package com.example.todoapplication.feature.category

sealed interface CategoryManagementEvent {
    data object RequestCreate : CategoryManagementEvent
    data class RequestEdit(val categoryId: Long) : CategoryManagementEvent
    data class CategoryNameChanged(val value: String) : CategoryManagementEvent
    data object SaveCategory : CategoryManagementEvent
    data object CancelEdit : CategoryManagementEvent
    data class RequestDelete(val categoryId: Long) : CategoryManagementEvent
    data object ConfirmDelete : CategoryManagementEvent
    data object CancelDelete : CategoryManagementEvent
}
