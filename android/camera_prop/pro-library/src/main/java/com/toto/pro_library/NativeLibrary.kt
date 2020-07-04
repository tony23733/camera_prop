package com.toto.pro_library

class NativeLibrary {
    external fun initJNI(width: Int, height: Int)
    external fun stepJNI()

    companion object {
        // Used to load the 'camera-pro-lib' library on application startup.
        init {
            System.loadLibrary("camera-pro-lib")
        }
        val instance = NativeLibrary()

        fun init(width: Int, height: Int) { instance.initJNI(width, height) }
        fun step() { instance.stepJNI() }
    }
}