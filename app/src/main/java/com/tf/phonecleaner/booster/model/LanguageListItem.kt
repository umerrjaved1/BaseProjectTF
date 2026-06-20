package com.tf.phonecleaner.booster.model

sealed class LanguageListItem {
    data class Header(val title: String) : LanguageListItem()
    data class Language(val model: LanguageModel) : LanguageListItem()
}
