package io.mokshjn.vision

import android.Manifest
import android.animation.Animator
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Button
import android.widget.ImageView
import com.flurgle.camerakit.CameraListener
import com.flurgle.camerakit.CameraView
import android.widget.TextView
import io.mokshjn.vision.api.Classifier
import io.mokshjn.vision.api.TensorFlowImageClassifier
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
    private var classifier: Classifier? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        camera = findViewById(R.id.camera_view) as CameraView

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), RC_CAMERA)
        }

        findViewById(R.id.floatingActionButton).setOnClickListener { _: View? ->
            camera?.captureImage()
        }

        camera?.setCameraListener(object : CameraListener() {
            override fun onPictureTaken(jpeg: ByteArray?) {
                super.onPictureTaken(jpeg)

                var result: Bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg?.size!!)
                result = Bitmap.createScaledBitmap(result, INPUT_SIZE, INPUT_SIZE, false)

                val results = classifier?.recognizeImage(result)

                (findViewById(R.id.result) as TextView).text = results.toString()

                animateView()
            }
        })

        initTensorFlow()
    }

    private fun animateView() {
        val resultView = findViewById(R.id.resultCard)
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
            classifier = TensorFlowImageClassifier.create(
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
        executor.execute { classifier?.close() }
    }
}