package com.professor.baseproject.iab



interface ConnectResponse {
    fun onConnected(subscriptionItems: List<SubscriptionItem>)
    fun onDisconnected()
    fun onError(errorCode: Int, errorMessage: String)
}

interface PurchaseResponse {
    fun onPurchaseSuccess(productId: String)
    fun onPurchasePending()
    fun onPurchaseCancelled()
    fun onPurchaseAlreadyOwned()
    fun onPurchaseError(errorCode: Int, errorMessage: String)
}

interface QueryResponse<T> {
    fun onQuerySuccess(items: List<T>)
    fun onQueryError(errorCode: Int, errorMessage: String)
}
