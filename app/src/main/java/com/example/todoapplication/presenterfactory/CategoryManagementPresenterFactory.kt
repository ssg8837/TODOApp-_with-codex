package com.example.todoapplication.presenterfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.todoapplication.application.service.CategoryService
import com.example.todoapplication.feature.category.CategoryManagementPresenter
import java.time.Clock

/** Category Service와 테스트 가능한 Clock으로 Category 관리 Presenter를 생성한다. */
class CategoryManagementPresenterFactory(
    private val categoryService: CategoryService,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CategoryManagementPresenter::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return CategoryManagementPresenter(categoryService, clock) as T
    }
}
