package com.tf.phonecleaner.booster.cache

data class CacheEntry<T>(
    val data: List<T>,
    val timestamp: Long // time in milliseconds
)
