package io.mokshjn.vision.api

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.os.Trace
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by moksh on 18/11/17.
 */
class ImageClassifier() : Classifier {

    companion object {
        private val TAG = "ImageClassifier"

        private val MAX_RESULTS = 3
        private val THRESHOLD = 0.1f

        fun create(assetManager: AssetManager,
                          modelFilename: String,
                          labelFilename: String,
                          inputSize: Int,
                          imageMean: Int,
                          imageStd: Float,
                          inputName: String,
                          outputName: String): Classifier {
            val classifier = ImageClassifier()
            classifier.inputName = inputName
            classifier.outputName = outputName

            val filename = labelFilename.split("file:///android_asset/")[1]
            val br = assetManager.open(filename).bufferedReader()

            br.useLines {
                lines -> lines.forEach { classifier.labels.add(it) }
            }

            classifier.inferenceInterface = TensorFlowInferenceInterface(assetManager,
                                                                            modelFilename)
            val operation = classifier.inferenceInterface.graphOperation(outputName)

            val numClasses = operation.output(0).shape().size(1).toInt()

            classifier.inputSize = inputSize
            classifier.imageMean = imageMean
            classifier.imageStd = imageStd

            classifier.outputNames = arrayOf(outputName)
            classifier.intValues = IntArray(inputSize * inputSize)
            classifier.floatValues = FloatArray(inputSize * inputSize * 3)
            classifier.outputs = FloatArray(numClasses)
            return classifier
        }
    }

    private lateinit var inputName: String
    private lateinit var outputName: String
    private var imageMean = 0
    private var imageStd = 0f
    private var inputSize = 0

    private var labels: ArrayList<String> = ArrayList()
    private lateinit var intValues: IntArray
    private lateinit var floatValues: FloatArray
    private lateinit var outputs: FloatArray
    private lateinit var outputNames: Array<String>

    private lateinit var inferenceInterface: TensorFlowInferenceInterface

    private var logStats = false



    override fun recognizeImage(bitmap: Bitmap): List<Classifier.Recognition> {
        Trace.beginSection("recognizeImage")

        Trace.beginSection("preprocessBitmap")

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0,
                        bitmap.width, bitmap.height)

        for (i in 0 until intValues.size){
            val int = intValues[i]
            floatValues[i * 3] = (((int shr 16) and 0xFF) - imageMean) / imageStd
            floatValues[i * 3] = (((int shr 8) and 0xFF) - imageMean) / imageStd
            floatValues[i * 3] = ((int and 0xFF) - imageMean) / imageStd
        }

        Trace.endSection()

        Trace.beginSection("feed")
        inferenceInterface.feed(inputName, floatValues, 1L, inputSize.toLong(), inputSize.toLong(), 3L)
        Trace.endSection()

        Trace.beginSection("run")
        inferenceInterface.run(outputNames, logStats)
        Trace.endSection()

        Trace.beginSection("fetch")
        inferenceInterface.fetch(outputName, outputs)
        Trace.endSection()

        val priorityQueue = PriorityQueue<Classifier.Recognition>(3,
                kotlin.Comparator { o1, o2 -> (o1.confidence - o2.confidence).toInt() })

        (0 until outputs.size)
                .filter { outputs[it] > THRESHOLD }
                .mapTo(priorityQueue) {
                    Classifier.Recognition("$it",
                            labels[it], outputs[it], null)
                }
        val recognitions = ArrayList<Classifier.Recognition>()
        val recogSize = minOf(priorityQueue.size, MAX_RESULTS)
        for (i in 0 until recogSize) {
            recognitions.add(priorityQueue.poll())
        }
        Trace.endSection()
        return recognitions
    }

    override fun enableStatLogging(debug: Boolean) {
        logStats = debug
    }

    override fun getStatString(): String {
        return inferenceInterface.statString
    }

    override fun close() {
        inferenceInterface.close()
    }
}