package com.professor.baseproject.iab

data class SubscribedItem(
    val sku: String,
    val purchaseTime: Long,
    val purchaseToken: String,
    val isAutoRenewing: Boolean = false,
    val orderId: String? = null
)