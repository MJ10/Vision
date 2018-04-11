package io.mokshjn.vision.api

import android.content.res.AssetManager
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.experimental.and

/**
 * Created by moksh on 18/11/17.
 */
class ImageClassifier() : Classifier {

    companion object {
        private val TAG = "ImageClassifier"

        private val MAX_RESULTS = 3
        private val THRESHOLD = 0.1f
        private val BATCH_SIZE = 1
        private val PIXEL_SIZE = 3

        fun create(assetManager: AssetManager,
                          modelFilename: String,
                          labelFilename: String,
                          inputSize: Int): Classifier {
            val classifier = ImageClassifier()
            classifier.interpreter = Interpreter(classifier.loadModelFile(assetManager,
                    modelFilename))
            classifier.labelList = classifier.loadLabelList(assetManager, labelFilename)
            classifier.inputSize = inputSize

            return classifier
        }
    }

    private var interpreter: Interpreter? = null
    private var inputSize: Int = 0
    private lateinit var labelList: List<String>

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength)
    }

    private fun loadLabelList(assetManager: AssetManager, labelPath: String): List<String> {
        val list = ArrayList<String>()
        val bufferReader = BufferedReader(InputStreamReader(assetManager.open(labelPath)))
        var line: String?
        line = bufferReader.readLine()
        while (line != null) {
            list.add(line)
            line = bufferReader.readLine()
        }
        bufferReader.close()
        return list
    }

    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(
                BATCH_SIZE * inputSize * inputSize * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i: Int in 0 until inputSize)
            for (j: Int in 0 until inputSize) {
                val intVal = intValues[pixel++]
                byteBuffer.put(((intVal shr 16) and 0xFF).toByte())
                byteBuffer.put(((intVal shr 8) and 0xFF).toByte())
                byteBuffer.put((intVal and 0xFF).toByte())
            }
        return byteBuffer
    }

    private fun getSortedResult(labelProbArray: Array<ByteArray>): List<Classifier.Recognition> {
        val pq = PriorityQueue<Classifier.Recognition>(MAX_RESULTS,
                kotlin.Comparator { t1, t2 ->
                    (t1.confidence - t2.confidence).toInt()
                })
        (0 until labelProbArray.size)
                .filter { (labelProbArray[0][it] and 0xFF.toByte()).toFloat() / 225.0 > THRESHOLD }
                .mapTo(pq) {
                    Classifier.Recognition("$it",
                            labelList[it],
                            ((labelProbArray[0][it] and 0xFF.toByte()).toFloat() / 225.0).toFloat())
                }
        val recognitions = ArrayList<Classifier.Recognition>()
        val recogSize = minOf(pq.size, MAX_RESULTS)
        for (i in 0 until recogSize) {
            recognitions.add(pq.poll())
        }
        return recognitions
    }

    override fun recognizeImage(bitmap: Bitmap): List<Classifier.Recognition> {
        var byteBuffer = convertBitmapToByteArray(bitmap)
        var result = Array(1, { ByteArray(labelList.size) })
        interpreter?.run(byteBuffer, result)
        return getSortedResult(result)
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
    }

}