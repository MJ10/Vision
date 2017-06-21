# Vision

## About
Vision is an app that recognizes objects in images. The application uses Google's ImageNet based Inception dataset.

This application is certainly not the best way to implement object recognition since the space occupied by the dataset is quite large and the computation is also quite complex to perform on the device itself. This project just demonstrates that it can be done on the device.

This project is quite similar to and is inspired by the TF Classify app in the TensorFlow Android examples.

## Building
- Clone the repository
- Download [libandroid_tensorflow_inference_java.jar](http://ci.tensorflow.org/view/Nightly/job/nightly-android/lastSuccessfulBuild/artifact/out/libandroid_tensorflow_inference_java.jar) and [libtensorflow_inference.so](http://ci.tensorflow.org/view/Nightly/job/nightly-android/lastSuccessfulBuild/artifact/out/native/libtensorflow_inference.so/armeabi-v7a/libtensorflow_inference.so) from the TensorFlow nightly builds.
- Move `libandroid_tensorflow_inference_java.jar` to `/app/libs` and `libtensorflow_inference.so` to `/app/src/main/jniLibs/armeabi-v7a`
- Download Google's [Inception Dataset](https://storage.googleapis.com/download.tensorflow.org/models/inception5h.zip)
- Extract the contents of `inception5h.zip` to `/app/src/main/assets`
- You can now open the project in Android Studio and build and run the application.

## Libraries
- [TensorFlow](https://tensorflow.org)
- [Anko](https://github.com/Kotlin/anko)

## License
This project is licensed under the [MIT License](https://github.com/MJ10/Vision/blob/master/LICENSE.md)
