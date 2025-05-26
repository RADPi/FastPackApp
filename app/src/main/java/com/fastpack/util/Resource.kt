package com.fastpack.util

import android.net.Uri
import androidx.compose.runtime.saveable.Saver

sealed class Resource<T>(val data: T? = null, val message: String? = null, val code: Int? = null) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, code: Int? = null, data: T? = null) : Resource<T>(data, message, code)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}

val UriSaver = Saver<Uri?, String>(
    save = { uri -> uri?.toString() ?: "" }, // Convierte Uri a String para guardarlo
    restore = { value ->                   // Convierte String de nuevo a Uri para restaurarlo
        if (value.isNotEmpty()) {
            Uri.parse(value)
        } else {
            null
        }
    }
)