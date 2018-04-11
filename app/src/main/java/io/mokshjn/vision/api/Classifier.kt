package io.mokshjn.vision.api

import android.graphics.Bitmap
import android.graphics.RectF

/**
 * Created by moksh on 12/6/17.
 */

interface Classifier {
    open class Recognition(val id: String, val title: String, val confidence: Float) {
        override fun toString() : String {
            var resultString = ""

            resultString += "$title "
            resultString += String.format("(%.1f%%) ", confidence * 100.0f)

            return resultString.trim { it <= ' ' }

        }
    }

    fun recognizeImage(bitmap: Bitmap) : List<Recognition>

    fun enableStatLogging(debug: Boolean)

    fun getStatString() : String

    fun close()
}
