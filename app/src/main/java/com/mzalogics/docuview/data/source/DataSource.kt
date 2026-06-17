package com.mzalogics.docuview.data.source

import android.content.Context
import java.lang.reflect.Type

/**

Created by Umer Javed
Senior Android Developer
Created on 17/06/2025 3:07 pm
Email: umerr8019@gmail.com

 */
interface DataSource {
    suspend fun <T> loadData(context: Context, keyOrFile: String, type: Type): List<T>
}