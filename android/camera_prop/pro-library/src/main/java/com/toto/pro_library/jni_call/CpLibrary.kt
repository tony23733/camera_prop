package com.toto.pro_library.jni_call

class CpLibrary {
    external fun initJNI(width: Int, height: Int)
    external fun renderFrameJNI()

    companion object {
        init {
            System.loadLibrary("camera-prop-lib")
        }
        private val instance = CpLibrary()

        fun init(width: Int, height: Int) { instance.initJNI(width, height) }
        fun renderFrame() { instance.renderFrameJNI() }
    }
}