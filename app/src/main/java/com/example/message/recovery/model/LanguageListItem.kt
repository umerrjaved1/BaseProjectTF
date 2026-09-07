package com.example.message.recovery.model

sealed class LanguageListItem {
    data class Header(val title: String) : LanguageListItem()
    data class Language(val model: LanguageModel) : LanguageListItem()
}
