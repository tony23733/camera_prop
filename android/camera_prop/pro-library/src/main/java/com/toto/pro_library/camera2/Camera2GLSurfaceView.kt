package com.toto.pro_library.camera2

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.toto.pro_library.render.BaseRender
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt


class Camera2GLSurfaceView(context: Context?, attrs: AttributeSet?) :
    GLSurfaceView(context, attrs), GLSurfaceView.Renderer, OnFrameAvailableListener {
    private lateinit var cameraProxy: Camera2Proxy
    private lateinit var surfaceTexture: SurfaceTexture
    private lateinit var renderer: BaseRender
    private var ratioWidth = 0
    private var ratioHeight = 0
    private var oldDistance = 0f
    private var oesTextureId = -1

    constructor(context: Context?) : this(context, null) {}

    private fun init(context: Context?) {
        cameraProxy = Camera2Proxy((context as Activity?)!!)
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        renderer = BaseRender()
        renderer.init()
        oesTextureId = renderer.oesTextureID
        surfaceTexture = SurfaceTexture(oesTextureId)
        surfaceTexture.setOnFrameAvailableListener(this)
        cameraProxy.setPreviewSurface(surfaceTexture)

        Log.d(TAG, "onSurfaceCreated. width: $width, height: $height")
        cameraProxy.openCamera(width, height)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        Log.d(TAG,"onSurfaceChanged. thread: " + Thread.currentThread().name)
        Log.d(TAG,"onSurfaceChanged. width: $width, height: $height")
        val previewWidth: Int = cameraProxy.getPreviewSize().width
        val previewHeight: Int = cameraProxy.getPreviewSize().height

        if (width > height) {
            setAspectRatio(previewWidth, previewHeight)
        } else {
            setAspectRatio(previewHeight, previewWidth)
        }
        renderer.initViewport(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        super.surfaceDestroyed(holder)

        cameraProxy.releaseCamera()
    }

    override fun onDrawFrame(gl: GL10) {
//        GLES20.glClearColor(0f, 0f, 0f, 0f)
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        surfaceTexture.updateTexImage()
//        mDrawer.draw(oesTextureId, cameraProxy.isFrontCamera)
        val transformMatrix = FloatArray(16)
        surfaceTexture.getTransformMatrix(transformMatrix)
        renderer.step(transformMatrix)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        requestRender()
    }

    private fun setAspectRatio(width: Int, height: Int) {
        require(!(width < 0 || height < 0)) { "Size cannot be negative." }
        ratioWidth = width
        ratioHeight = height
        post {
            requestLayout() // must run in UI thread
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (0 == ratioWidth || 0 == ratioHeight) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * ratioWidth / ratioHeight) {
                setMeasuredDimension(width, width * ratioHeight / ratioWidth)
            } else {
                setMeasuredDimension(height * ratioWidth / ratioHeight, height)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount == 1) {
            // 点击聚焦
            cameraProxy.focusOnPoint(event.x.toDouble(), event.y.toDouble(), width, height)
            return true
        }
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_POINTER_DOWN -> oldDistance = getFingerSpacing(event)
            MotionEvent.ACTION_MOVE -> {
                val newDistance = getFingerSpacing(event)
                if (newDistance > oldDistance) {
                    cameraProxy.handleZoom(true)
                } else if (newDistance < oldDistance) {
                    cameraProxy.handleZoom(false)
                }
                oldDistance = newDistance
            }
        }
        return super.onTouchEvent(event)
    }

    companion object {
        private const val TAG = "Camera2GLSurfaceView"
        private fun getFingerSpacing(event: MotionEvent): Float {
            val x = event.getX(0) - event.getX(1)
            val y = event.getY(0) - event.getY(1)
            return sqrt(x * x + y * y.toDouble()).toFloat()
        }
    }

    init {
        init(context)
    }
}
