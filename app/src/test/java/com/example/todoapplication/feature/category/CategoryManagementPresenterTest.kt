package com.example.todoapplication.feature.category

import com.example.todoapplication.application.service.CategoryService
import com.example.todoapplication.application.service.ServiceError
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.domain.model.Category
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.domain.validation.CategoryValidationError
import com.example.todoapplication.presenterfactory.CategoryManagementPresenterFactory
import com.example.todoapplication.test.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryManagementPresenterTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-09-20T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun observesCategoriesInServiceOrderIncludingSystemCategory() = runTest {
        val service = FakeCategoryManagementService()
        val presenter = CategoryManagementPresenter(service, clock)
        service.emit(listOf(user(2, "둘", 2), system(), user(1, "하나", 1)))
        advanceUntilIdle()

        assertEquals(listOf("둘", "일반", "하나"), presenter.state.value.categories.map { it.name })
        assertTrue(presenter.state.value.categories[1].isSystem)
        assertFalse(presenter.state.value.isLoading)
    }

    @Test
    fun createDialogNameAndSuccessfulCreateUseNeutralWithoutMutatingList() = runTest {
        val service = FakeCategoryManagementService()
        val presenter = loadedPresenter(service)

        presenter.onEvent(CategoryManagementEvent.RequestCreate)
        presenter.onEvent(CategoryManagementEvent.CategoryNameChanged("업무"))
        presenter.onEvent(CategoryManagementEvent.SaveCategory)
        advanceUntilIdle()

        assertEquals("업무", service.created.single().name)
        assertEquals(CategoryColor.NEUTRAL, service.created.single().color)
        assertEquals(Instant.now(clock), service.created.single().createdAt)
        assertEquals(CategoryEditorMode.NONE, presenter.state.value.editorMode)
    }

    @Test
    fun blankNameDoesNotCallService() = runTest {
        val service = FakeCategoryManagementService()
        val presenter = loadedPresenter(service)
        presenter.onEvent(CategoryManagementEvent.RequestCreate)
        presenter.onEvent(CategoryManagementEvent.CategoryNameChanged("  "))
        presenter.onEvent(CategoryManagementEvent.SaveCategory)

        assertEquals(CategoryNameValidationError.REQUIRED, presenter.state.value.validationError)
        assertTrue(service.created.isEmpty())
    }

    @Test
    fun reservedAndDuplicateNamesRemainInEditorWithMeaningfulErrors() = runTest {
        val service = FakeCategoryManagementService()
        val presenter = loadedPresenter(service)
        presenter.onEvent(CategoryManagementEvent.RequestCreate)
        presenter.onEvent(CategoryManagementEvent.CategoryNameChanged("일반"))
        service.createResult = ServiceResult.Failure(
            ServiceError.InvalidCategory(setOf(CategoryValidationError.RESERVED_SYSTEM_NAME)),
        )
        presenter.onEvent(CategoryManagementEvent.SaveCategory)
        advanceUntilIdle()
        assertEquals(CategoryNameValidationError.RESERVED, presenter.state.value.validationError)
        assertEquals("일반", presenter.state.value.categoryNameInput)

        service.createResult = ServiceResult.Failure(ServiceError.CategoryNameDuplicated)
        presenter.onEvent(CategoryManagementEvent.CategoryNameChanged("업무"))
        presenter.onEvent(CategoryManagementEvent.SaveCategory)
        advanceUntilIdle()
        assertEquals(CategoryNameValidationError.DUPLICATED, presenter.state.value.validationError)
    }

    @Test
    fun duplicateSaveIsBlockedWhileCreateIsRunning() = runTest {
        val service = FakeCategoryManagementService().apply { createGate = CompletableDeferred() }
        val presenter = loadedPresenter(service)
        presenter.onEvent(CategoryManagementEvent.RequestCreate)
        presenter.onEvent(CategoryManagementEvent.CategoryNameChanged("업무"))
        presenter.onEvent(CategoryManagementEvent.SaveCategory)
        advanceUntilIdle()
        presenter.onEvent(CategoryManagementEvent.SaveCategory)

        assertEquals(1, service.created.size)
        assertTrue(presenter.state.value.isSaving)
        service.createGate?.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun editUsesExistingCategoryAndKeepsDialogOnFailure() = runTest {
        val service = FakeCategoryManagementService()
        val existing = user(4, "업무", 1, CategoryColor.BLUE)
        val presenter = loadedPresenter(service, listOf(system(), existing))
        presenter.onEvent(CategoryManagementEvent.RequestEdit(4))
        assertEquals("업무", presenter.state.value.categoryNameInput)
        presenter.onEvent(CategoryManagementEvent.CategoryNameChanged("회사"))
        service.updateResult = ServiceResult.Failure(ServiceError.PersistenceFailure)
        presenter.onEvent(CategoryManagementEvent.SaveCategory)
        advanceUntilIdle()

        assertEquals(CategoryColor.BLUE, service.updated.single().color)
        assertEquals(CategoryEditorMode.EDIT, presenter.state.value.editorMode)
        assertEquals("회사", presenter.state.value.categoryNameInput)
        assertEquals(CategoryManagementError.PERSISTENCE_FAILURE, presenter.state.value.error)
    }

    @Test
    fun successfulEditPreservesColorAndClosesDialog() = runTest {
        val service = FakeCategoryManagementService()
        val presenter = loadedPresenter(
            service,
            listOf(system(), user(4, "업무", 1, CategoryColor.BLUE)),
        )
        presenter.onEvent(CategoryManagementEvent.RequestEdit(4))
        presenter.onEvent(CategoryManagementEvent.CategoryNameChanged("회사"))
        presenter.onEvent(CategoryManagementEvent.SaveCategory)
        advanceUntilIdle()

        assertEquals("회사", service.updated.single().name)
        assertEquals(CategoryColor.BLUE, service.updated.single().color)
        assertEquals(CategoryEditorMode.NONE, presenter.state.value.editorMode)
        assertNull(presenter.state.value.error)
    }

    @Test
    fun systemCategoryEditAndDeleteAreBlocked() = runTest {
        val service = FakeCategoryManagementService()
        val presenter = loadedPresenter(service)
        presenter.onEvent(CategoryManagementEvent.RequestEdit(1))
        assertEquals(CategoryManagementError.SYSTEM_OPERATION_PROHIBITED, presenter.state.value.error)
        presenter.onEvent(CategoryManagementEvent.RequestDelete(1))
        assertEquals(CategoryManagementError.SYSTEM_OPERATION_PROHIBITED, presenter.state.value.error)
        assertTrue(service.deleted.isEmpty())
    }

    @Test
    fun deleteRequiresConfirmationAndCancelDoesNotCallService() = runTest {
        val service = FakeCategoryManagementService()
        val presenter = loadedPresenter(service, listOf(system(), user(2, "업무", 1)))
        presenter.onEvent(CategoryManagementEvent.RequestDelete(2))
        assertTrue(presenter.state.value.showDeleteConfirmation)
        assertTrue(service.deleted.isEmpty())
        presenter.onEvent(CategoryManagementEvent.CancelDelete)
        assertFalse(presenter.state.value.showDeleteConfirmation)
        assertTrue(service.deleted.isEmpty())
    }

    @Test
    fun confirmDeleteCallsServiceOnceAndBlocksDuplicate() = runTest {
        val service = FakeCategoryManagementService().apply { deleteGate = CompletableDeferred() }
        val presenter = loadedPresenter(service, listOf(system(), user(2, "업무", 1)))
        presenter.onEvent(CategoryManagementEvent.RequestDelete(2))
        presenter.onEvent(CategoryManagementEvent.ConfirmDelete)
        advanceUntilIdle()
        presenter.onEvent(CategoryManagementEvent.ConfirmDelete)
        assertEquals(listOf(2L), service.deleted)
        service.deleteGate?.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun deleteFailureClosesConfirmationAndReportsError() = runTest {
        val service = FakeCategoryManagementService().apply {
            deleteResult = ServiceResult.Failure(ServiceError.PersistenceFailure)
        }
        val presenter = loadedPresenter(service, listOf(system(), user(2, "업무", 1)))
        presenter.onEvent(CategoryManagementEvent.RequestDelete(2))
        presenter.onEvent(CategoryManagementEvent.ConfirmDelete)
        advanceUntilIdle()
        assertFalse(presenter.state.value.showDeleteConfirmation)
        assertEquals(CategoryManagementError.PERSISTENCE_FAILURE, presenter.state.value.error)
    }

    @Test
    fun factoryCreatesPresenterUsingOnlyCategoryService() {
        val presenter = CategoryManagementPresenterFactory(
            categoryService = FakeCategoryManagementService(),
            clock = clock,
        ).create(CategoryManagementPresenter::class.java)

        assertEquals(CategoryManagementPresenter::class.java, presenter.javaClass)
    }

    private suspend fun loadedPresenter(
        service: FakeCategoryManagementService,
        categories: List<Category> = listOf(system()),
    ): CategoryManagementPresenter {
        val presenter = CategoryManagementPresenter(service, clock)
        service.emit(categories)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        return presenter
    }

    private fun system() = Category(1, "일반", CategoryColor.NEUTRAL, 0, true, Instant.EPOCH)
    private fun user(
        id: Long,
        name: String,
        order: Int,
        color: CategoryColor = CategoryColor.RED,
    ) = Category(id, name, color, order, false, Instant.EPOCH)

    @Test
    fun reorderUsesServiceOnceAndOnlyFlowChangesConfirmedOrder() = runTest {
        val service = FakeCategoryManagementService().apply { reorderGate = CompletableDeferred() }
        val original = listOf(system(), user(2, "업무", 1), user(3, "개인", 2))
        val presenter = loadedPresenter(service, original)
        presenter.onEvent(CategoryManagementEvent.ReorderCategories(listOf(3, 2)))
        presenter.onEvent(CategoryManagementEvent.ReorderCategories(listOf(2, 3)))
        presenter.onEvent(CategoryManagementEvent.RequestCreate)
        advanceUntilIdle()
        assertEquals(listOf(listOf(3L, 2L)), service.reordered)
        assertTrue(presenter.state.value.isReordering)
        assertEquals(listOf(1L, 2L, 3L), presenter.state.value.categories.map { it.id })
        service.emit(listOf(original[0], original[2], original[1]))
        service.reorderGate?.complete(Unit)
        advanceUntilIdle()
        assertFalse(presenter.state.value.isReordering)
        assertEquals(listOf(1L, 3L, 2L), presenter.state.value.categories.map { it.id })
    }

    @Test
    fun systemReorderAndColorEditAreBlockedWithoutServiceWrites() = runTest {
        val service = FakeCategoryManagementService()
        val presenter = loadedPresenter(service)
        presenter.onEvent(CategoryManagementEvent.ReorderCategories(listOf(1)))
        assertEquals(CategoryManagementError.SYSTEM_OPERATION_PROHIBITED, presenter.state.value.error)
        presenter.onEvent(CategoryManagementEvent.RequestEdit(1))
        presenter.onEvent(CategoryManagementEvent.CategoryColorChanged(CategoryColor.BLUE))
        presenter.onEvent(CategoryManagementEvent.SaveCategory)
        advanceUntilIdle()
        assertTrue(service.reordered.isEmpty())
        assertTrue(service.updated.isEmpty())
        assertEquals(CategoryColor.NEUTRAL, presenter.state.value.selectedCategoryColor)
    }

    @Test
    fun invalidOrdersAndPersistenceFailureKeepConfirmedOrder() = runTest {
        val service = FakeCategoryManagementService()
        val presenter = loadedPresenter(service, listOf(system(), user(2, "업무", 1)))
        for (ids in listOf(listOf(99L), listOf(2L, 2L), emptyList())) {
            service.reorderResult = ServiceResult.Failure(ServiceError.InvalidCategoryOrder)
            presenter.onEvent(CategoryManagementEvent.ReorderCategories(ids))
            advanceUntilIdle()
            assertEquals(CategoryManagementError.INVALID_ORDER, presenter.state.value.error)
            assertEquals(listOf(1L, 2L), presenter.state.value.categories.map { it.id })
        }
        service.reorderResult = ServiceResult.Failure(ServiceError.PersistenceFailure)
        presenter.onEvent(CategoryManagementEvent.ReorderCategories(listOf(2)))
        advanceUntilIdle()
        assertEquals(CategoryManagementError.PERSISTENCE_FAILURE, presenter.state.value.error)
        assertFalse(presenter.state.value.isReordering)
    }

    @Test
    fun createUsesSelectedColorAndNextEditorResetsToNeutral() = runTest {
        val service = FakeCategoryManagementService()
        val presenter = loadedPresenter(service)
        presenter.onEvent(CategoryManagementEvent.RequestCreate)
        presenter.onEvent(CategoryManagementEvent.CategoryNameChanged("색상"))
        presenter.onEvent(CategoryManagementEvent.CategoryColorChanged(CategoryColor.PINK))
        presenter.onEvent(CategoryManagementEvent.SaveCategory)
        advanceUntilIdle()
        assertEquals(CategoryColor.PINK, service.created.single().color)
        presenter.onEvent(CategoryManagementEvent.RequestCreate)
        assertEquals(CategoryColor.NEUTRAL, presenter.state.value.selectedCategoryColor)
    }

    @Test
    fun colorOnlyEditPreservesIdentityAndFailurePreservesInput() = runTest {
        val service = FakeCategoryManagementService()
        val original = user(2, "업무", 4, CategoryColor.RED)
        val presenter = loadedPresenter(service, listOf(system(), original))
        presenter.onEvent(CategoryManagementEvent.RequestEdit(2))
        assertEquals(CategoryColor.RED, presenter.state.value.selectedCategoryColor)
        presenter.onEvent(CategoryManagementEvent.CategoryColorChanged(CategoryColor.BLUE))
        service.updateResult = ServiceResult.Failure(ServiceError.PersistenceFailure)
        presenter.onEvent(CategoryManagementEvent.SaveCategory)
        advanceUntilIdle()
        assertEquals(original.copy(color = CategoryColor.BLUE), service.updated.single())
        assertEquals(CategoryColor.BLUE, presenter.state.value.selectedCategoryColor)
        assertEquals("업무", presenter.state.value.categoryNameInput)
        assertEquals(CategoryEditorMode.EDIT, presenter.state.value.editorMode)
        service.updateResult = null
        presenter.onEvent(CategoryManagementEvent.SaveCategory)
        advanceUntilIdle()
        assertEquals(CategoryEditorMode.NONE, presenter.state.value.editorMode)
    }
}

private class FakeCategoryManagementService : CategoryService {
    private val stream = MutableSharedFlow<ServiceResult<List<Category>>>(replay = 1)
    val created = mutableListOf<Category>()
    val updated = mutableListOf<Category>()
    val deleted = mutableListOf<Long>()
    var createResult: ServiceResult<Category>? = null
    var updateResult: ServiceResult<Category>? = null
    var deleteResult: ServiceResult<Unit> = ServiceResult.Success(Unit)
    var createGate: CompletableDeferred<Unit>? = null
    var deleteGate: CompletableDeferred<Unit>? = null
    var reorderGate: CompletableDeferred<Unit>? = null
    var reorderResult: ServiceResult<Unit> = ServiceResult.Success(Unit)
    val reordered = mutableListOf<List<Long>>()

    suspend fun emit(categories: List<Category>) {
        stream.emit(ServiceResult.Success(categories))
    }

    override fun observeAll(): Flow<ServiceResult<List<Category>>> = stream

    override suspend fun create(category: Category): ServiceResult<Category> {
        created += category
        createGate?.await()
        return createResult ?: ServiceResult.Success(category.copy(id = 10))
    }

    override suspend fun update(category: Category): ServiceResult<Category> {
        updated += category
        return updateResult ?: ServiceResult.Success(category)
    }

    override suspend fun delete(categoryId: Long): ServiceResult<Unit> {
        deleted += categoryId
        deleteGate?.await()
        return deleteResult
    }

    override suspend fun getById(categoryId: Long): ServiceResult<Category> = error("unused")
    override suspend fun reorder(categoryIds: List<Long>): ServiceResult<Unit> {
        reordered += categoryIds
        reorderGate?.await()
        return reorderResult
    }
}
