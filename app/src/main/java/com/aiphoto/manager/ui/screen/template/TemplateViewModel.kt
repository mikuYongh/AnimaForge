package com.aiphoto.manager.ui.screen.template

import androidx.lifecycle.ViewModel
import com.aiphoto.manager.data.model.Template
import com.aiphoto.manager.data.repository.TemplateRepository

class TemplateViewModel : ViewModel() {

    private val repository = TemplateRepository()

    val templates = repository.getTemplates()
    val categories = repository.getCategories()

    fun getTemplatesByCategory(category: String?): List<Template> {
        return if (category == null) templates
        else repository.getTemplatesByCategory(category)
    }
}
