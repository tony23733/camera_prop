package com.toto.camera_prop

import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.toto.pro_library.camera2.Camera2GLSurfaceView

class CameraActivity : AppCompatActivity() {

    lateinit var graphicsView: Camera2GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_camera2)

        graphicsView = Camera2GLSurfaceView(this)
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
}