package com.professor.baseproject.remoteconfig.data


data class AssetsConfigData(
    val category: String = "",
) {
    fun getString(key: String): String {
        return when (key) {

            else -> ""
        }
    }
}
