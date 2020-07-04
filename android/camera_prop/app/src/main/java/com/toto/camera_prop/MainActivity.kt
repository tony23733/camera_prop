package com.toto.camera_prop

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.toto.pro_library.TutorialView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var graphicsView: TutorialView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)

//        // Example of a call to a native method
//        sample_text.text = stringFromJNI()


        graphicsView = TutorialView(application)
        setContentView(graphicsView)
    }

    override fun onPause() {
        super.onPause()
        graphicsView.onPause()
    }

    override fun onResume() {
        super.onResume()
        graphicsView.onResume()
    }

//    /**
//     * A native method that is implemented by the 'native-lib' native library,
//     * which is packaged with this application.
//     */
//    external fun stringFromJNI(): String
//
//    companion object {
//        // Used to load the 'native-lib' library on application startup.
//        init {
//            System.loadLibrary("native-lib")
//        }
//    }
}
