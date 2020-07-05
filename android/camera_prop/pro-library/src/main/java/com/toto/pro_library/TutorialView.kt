package com.toto.pro_library

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import com.toto.pro_library.render.BaseRender
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10

class TutorialView(context: Context?): GLSurfaceView(context) {
    var redSize = 8;
    var greenSize = 8;
    var blueSize = 8 ;
    var alphaSize = 8;
    var depthSize = 16;
    var sampleSize = 4;
    var stencilSize = 0;

    private var renderer: Renderer

    val oesTextureID: Int get() = renderer.renderer.oesTextureID
    var surfaceTexture: SurfaceTexture? = null

    init {
        setEGLContextFactory(ContextFactory())
        setEGLConfigChooser(ConfigChooser())
        renderer = Renderer()
        setRenderer(renderer)
    }

    class ContextFactory: GLSurfaceView.EGLContextFactory {
        override fun createContext(egl: EGL10, display: EGLDisplay, eglConfig: EGLConfig): EGLContext {
            val EGL_CONTEXT_CLIENT_VERSION = 0x3098
            val attrib_list: IntArray = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 3, EGL10.EGL_NONE)
            val context: EGLContext = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list)
            return context
        }

        override fun destroyContext(egl: EGL10, display: EGLDisplay, context: EGLContext) {
            egl.eglDestroyContext(display, context)
        }
    }

    class ConfigChooser: GLSurfaceView.EGLConfigChooser {
        val redSize = 8
        val greenSize = 8
        val blueSize = 8
        val alphaSize = 8
        val depthSize = 16
        val sampleSize = 4
        val stencilSize = 0
        var value:IntArray = intArrayOf(1)

        private fun getConfigAttrib(egl: EGL10, display: EGLDisplay, config: EGLConfig, attribute: Int, defaultValue: Int): Int {
            if (egl.eglGetConfigAttrib(display, config, attribute, value))
                return value[0]

            return defaultValue
        }

        private fun selectConfig(egl: EGL10, display: EGLDisplay, configs: Array<EGLConfig?>): EGLConfig? {
            for (config in configs) {
                val d = getConfigAttrib(egl, display, config!!, EGL10.EGL_DEPTH_SIZE, 0)
                val s = getConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0)
                val g = getConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0)
                val r = getConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0)
                val b = getConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0)
                val a = getConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0)

                if (r == redSize && g == greenSize && b == blueSize && a == alphaSize && d >= depthSize && s >= stencilSize)
                    return config
            }

            return null
        }

        override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
            val EGL_OPENGL_ES2_BIT = 4
            val configAttributes = intArrayOf(
                EGL10.EGL_RED_SIZE, redSize,
                EGL10.EGL_GREEN_SIZE, greenSize,
                EGL10.EGL_BLUE_SIZE, blueSize,
                EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL10.EGL_SAMPLES, sampleSize,
                EGL10.EGL_DEPTH_SIZE, depthSize,
                EGL10.EGL_STENCIL_SIZE, stencilSize,
                EGL10.EGL_NONE
            )

            val num_config = intArrayOf(1)
            egl.eglChooseConfig(display, configAttributes, null, 0, num_config)

            val numConfigs = num_config[0]
            val configs : Array<EGLConfig?> = arrayOfNulls(numConfigs)
            egl.eglChooseConfig(display, configAttributes, configs, numConfigs, num_config)

            return selectConfig(egl, display, configs)!!
        }
    }

    class Renderer: GLSurfaceView.Renderer {
        val renderer: BaseRender = BaseRender()
        var surfaceTexture: SurfaceTexture? = null
        private val transformMatrix = FloatArray(16)

        override fun onDrawFrame(gl: GL10?) {
//            NativeLibrary.step()
            surfaceTexture?.updateTexImage()
            surfaceTexture?.getTransformMatrix(transformMatrix)
            renderer.step(transformMatrix)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
//            NativeLibrary.init(width, height)
            renderer.initViewport(width, height)
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            renderer.init()
        }

    }
}