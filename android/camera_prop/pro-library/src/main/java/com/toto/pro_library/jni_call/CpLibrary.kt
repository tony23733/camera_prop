package com.toto.pro_library.jni_call

class CpLibrary {
    external fun initJNI()
    external fun initViewportJNI(width: Int, height: Int);
    external fun renderFrameJNI()
    external fun getOesTextureIDJNI(): Int;

    companion object {
        init {
            System.loadLibrary("camera-prop-lib")
        }
        private val instance = CpLibrary()

        fun init() { instance.initJNI() }
        fun initViewport(width: Int, height: Int) { instance.initViewportJNI(width, height) }
        fun renderFrame() { instance.renderFrameJNI() }
        fun getOesTextureID(): Int { return instance.getOesTextureIDJNI(); }
    }
}