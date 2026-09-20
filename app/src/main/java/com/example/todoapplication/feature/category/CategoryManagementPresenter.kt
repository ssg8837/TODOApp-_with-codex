package com.example.todoapplication.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapplication.application.service.CategoryService
import com.example.todoapplication.application.service.ServiceError
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.domain.validation.CategoryValidationError
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoryManagementPresenter(
    private val categoryService: CategoryService,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(CategoryManagementUiState())
    private var categoriesById: Map<Long, Category> = emptyMap()

    val state: StateFlow<CategoryManagementUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryService.observeAll().collect(::applyCategories)
        }
    }

    fun onEvent(event: CategoryManagementEvent) {
        when (event) {
            CategoryManagementEvent.RequestCreate -> requestCreate()
            is CategoryManagementEvent.RequestEdit -> requestEdit(event.categoryId)
            is CategoryManagementEvent.CategoryNameChanged -> nameChanged(event.value)
            CategoryManagementEvent.SaveCategory -> saveCategory()
            CategoryManagementEvent.CancelEdit -> cancelEdit()
            is CategoryManagementEvent.RequestDelete -> requestDelete(event.categoryId)
            CategoryManagementEvent.ConfirmDelete -> confirmDelete()
            CategoryManagementEvent.CancelDelete -> cancelDelete()
        }
    }

    private fun applyCategories(result: ServiceResult<List<Category>>) {
        when (result) {
            is ServiceResult.Success -> {
                categoriesById = result.value.associateBy(Category::id)
                mutableState.update {
                    it.copy(
                        categories = result.value.map { it.toUiModel() },
                        isLoading = false,
                        error = null,
                    )
                }
            }
            is ServiceResult.Failure -> mutableState.update {
                it.copy(isLoading = false, error = result.error.toPresentationError())
            }
        }
    }

    private fun requestCreate() {
        if (isBusy()) return
        mutableState.update {
            it.copy(
                editorMode = CategoryEditorMode.CREATE,
                editingCategoryId = null,
                categoryNameInput = "",
                validationError = null,
                error = null,
            )
        }
    }

    private fun requestEdit(categoryId: Long) {
        if (isBusy()) return
        val category = categoriesById[categoryId]
        if (category == null) {
            setError(CategoryManagementError.CATEGORY_NOT_FOUND)
        } else if (category.isSystem) {
            setError(CategoryManagementError.SYSTEM_OPERATION_PROHIBITED)
        } else {
            mutableState.update {
                it.copy(
                    editorMode = CategoryEditorMode.EDIT,
                    editingCategoryId = category.id,
                    categoryNameInput = category.name,
                    validationError = null,
                    error = null,
                )
            }
        }
    }

    private fun nameChanged(value: String) {
        if (mutableState.value.isSaving) return
        mutableState.update { it.copy(categoryNameInput = value, validationError = null) }
    }

    private fun saveCategory() {
        val current = mutableState.value
        if (isBusy() || current.editorMode == CategoryEditorMode.NONE) return
        if (current.categoryNameInput.isBlank()) {
            mutableState.update { it.copy(validationError = CategoryNameValidationError.REQUIRED) }
            return
        }

        val category = when (current.editorMode) {
            CategoryEditorMode.CREATE -> Category(
                id = 0,
                name = current.categoryNameInput,
                color = CategoryColor.NEUTRAL,
                sortOrder = 1,
                isSystem = false,
                createdAt = Instant.now(clock),
            )
            CategoryEditorMode.EDIT -> current.editingCategoryId
                ?.let(categoriesById::get)
                ?.takeUnless(Category::isSystem)
                ?.copy(name = current.categoryNameInput)
                ?: run {
                    setError(CategoryManagementError.CATEGORY_NOT_FOUND)
                    return
                }
            CategoryEditorMode.NONE -> return
        }

        mutableState.update { it.copy(isSaving = true, validationError = null, error = null) }
        viewModelScope.launch {
            val result = if (current.editorMode == CategoryEditorMode.CREATE) {
                categoryService.create(category)
            } else {
                categoryService.update(category)
            }
            when (result) {
                is ServiceResult.Success -> closeEditor()
                is ServiceResult.Failure -> mutableState.update {
                    it.copy(
                        isSaving = false,
                        validationError = result.error.toValidationError(),
                        error = if (result.error.toValidationError() == null) {
                            result.error.toPresentationError()
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    private fun cancelEdit() {
        if (!mutableState.value.isSaving) closeEditor()
    }

    private fun closeEditor() {
        mutableState.update {
            it.copy(
                editorMode = CategoryEditorMode.NONE,
                editingCategoryId = null,
                categoryNameInput = "",
                isSaving = false,
                validationError = null,
                error = null,
            )
        }
    }

    private fun requestDelete(categoryId: Long) {
        if (isBusy()) return
        val category = categoriesById[categoryId]
        if (category == null) {
            setError(CategoryManagementError.CATEGORY_NOT_FOUND)
        } else if (category.isSystem) {
            setError(CategoryManagementError.SYSTEM_OPERATION_PROHIBITED)
        } else {
            mutableState.update {
                it.copy(
                    showDeleteConfirmation = true,
                    deleteTargetCategoryId = categoryId,
                    error = null,
                )
            }
        }
    }

    private fun confirmDelete() {
        val current = mutableState.value
        val categoryId = current.deleteTargetCategoryId ?: return
        if (isBusy() || !current.showDeleteConfirmation) return
        mutableState.update { it.copy(isDeleting = true, error = null) }
        viewModelScope.launch {
            when (val result = categoryService.delete(categoryId)) {
                is ServiceResult.Success -> closeDeleteDialog()
                is ServiceResult.Failure -> mutableState.update {
                    it.copy(
                        showDeleteConfirmation = false,
                        deleteTargetCategoryId = null,
                        isDeleting = false,
                        error = result.error.toPresentationError(),
                    )
                }
            }
        }
    }

    private fun cancelDelete() {
        if (!mutableState.value.isDeleting) closeDeleteDialog()
    }

    private fun closeDeleteDialog() {
        mutableState.update {
            it.copy(
                showDeleteConfirmation = false,
                deleteTargetCategoryId = null,
                isDeleting = false,
                error = null,
            )
        }
    }

    private fun isBusy(): Boolean = mutableState.value.run { isSaving || isDeleting }

    private fun setError(error: CategoryManagementError) {
        mutableState.update { it.copy(error = error) }
    }

    private fun Category.toUiModel() = CategoryManagementItemUiModel(
        id = id,
        name = name,
        color = color,
        isSystem = isSystem,
    )

    private fun ServiceError.toValidationError(): CategoryNameValidationError? = when (this) {
        ServiceError.CategoryNameDuplicated -> CategoryNameValidationError.DUPLICATED
        is ServiceError.InvalidCategory -> when {
            CategoryValidationError.BLANK_NAME in errors -> CategoryNameValidationError.REQUIRED
            CategoryValidationError.RESERVED_SYSTEM_NAME in errors -> CategoryNameValidationError.RESERVED
            else -> CategoryNameValidationError.INVALID
        }
        else -> null
    }

    private fun ServiceError.toPresentationError(): CategoryManagementError = when (this) {
        ServiceError.CategoryNotFound -> CategoryManagementError.CATEGORY_NOT_FOUND
        ServiceError.SystemCategoryOperationProhibited ->
            CategoryManagementError.SYSTEM_OPERATION_PROHIBITED
        ServiceError.PersistenceFailure -> CategoryManagementError.PERSISTENCE_FAILURE
        else -> CategoryManagementError.OPERATION_FAILED
    }
}
