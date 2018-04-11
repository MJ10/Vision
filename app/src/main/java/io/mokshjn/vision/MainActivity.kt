package io.mokshjn.vision

import android.Manifest
import android.animation.Animator
import android.content.pm.PackageManager
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.TextView
import com.wonderkiln.camerakit.CameraView
import io.mokshjn.vision.api.Classifier
import io.mokshjn.vision.api.ImageClassifier
import org.jetbrains.anko.doAsync
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    val RC_CAMERA = 100
    private val INPUT_SIZE = 224

    private val IMAGE_MEAN = 117
    private val IMAGE_STD = 1f
    private val INPUT_NAME = "input"
    private val OUTPUT_NAME = "output"

    private val MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb"
    private val LABEL_FILE = "file:///android_asset/imagenet_comp_graph_label_strings.txt"

    var camera: CameraView? = null
    private lateinit var classifier: Classifier
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        camera = findViewById(R.id.camera_view)

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), RC_CAMERA)
        }

//        camera?.

//        camera?.(object : CameraKitEventListener() {
//            override fun onPictureTaken(jpeg: ByteArray?) {
//                super.onPictureTaken(jpeg)
//
//                var result: Bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg?.size!!)
//                result = Bitmap.createScaledBitmap(result, INPUT_SIZE, INPUT_SIZE, false)
//
//                doAsync {
//                    val results = classifier.recognizeImage(result)
//                    runOnUiThread {
//                        (findViewById<TextView>(R.id.result)).text = results.toString()
//                        animateView()
//                    }
//                }
//            }
//        })

        doAsync {
            initTensorFlow()

            runOnUiThread {
                findViewById<FloatingActionButton>(R.id.floatingActionButton).setOnClickListener {
                    camera?.captureImage {
                        val result = it.bitmap
                        doAsync {
                            val results = classifier.recognizeImage(result)
                            runOnUiThread {
                                (findViewById<TextView>(R.id.result)).text = results.toString()
                                animateView()
                            }
                        }

                    }
                }
            }
        }
    }

    private fun animateView() {
        val resultView = findViewById<CardView>(R.id.resultCard)
        val cx = (resultView.left + resultView.right) / 2
        val cy = (resultView.top + resultView.bottom) / 2
        val finalRadius = Math.max(resultView.width, resultView.height)

        val anim: Animator = ViewAnimationUtils.createCircularReveal(resultView, cx, cy, 0f, finalRadius.toFloat())
        resultView.visibility = View.VISIBLE
        resultView.setBackgroundColor(Color.parseColor("#FFFFFF"))
        anim.duration = 350
        anim.start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            RC_CAMERA -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
                    finish()
            }
        }
    }

    private fun initTensorFlow() {
        executor.execute({
            classifier = ImageClassifier.create(
                    assets,
                    MODEL_FILE,
                    LABEL_FILE,
                    INPUT_SIZE,
                    IMAGE_MEAN,
                    IMAGE_STD,
                    INPUT_NAME,
                    OUTPUT_NAME)
        })
    }

    override fun onResume() {
        super.onResume()
        camera?.start()
    }

    override fun onPause() {
        camera?.stop()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.execute { classifier.close() }
    }
}