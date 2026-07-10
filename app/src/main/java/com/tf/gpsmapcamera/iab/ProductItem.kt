package com.tf.gpsmapcamera.iab

import com.android.billingclient.api.ProductDetails

abstract class ProductItem(val productDetails: ProductDetails) {
    val sku: String = productDetails.productId
    val title: String = productDetails.title
    val description: String = productDetails.description
    val name: String = productDetails.name
}
