package com.toto.pro_library.render

import com.toto.pro_library.jni_call.CpLibrary

class RenderManager {
    /**
     * 使用渲染器。
     * todo：需完善动态加载渲染器，创建销毁管理
     */
    fun init(index:Int) {
        CpLibrary.init()
    }

    fun initViewport(width: Int, height: Int) {
        CpLibrary.initViewport(width, height)
    }

    fun renderFrame(transformMatrix: FloatArray?) {
        CpLibrary.renderFrame();
    }

    fun getOesTextureID(): Int {
        return CpLibrary.getOesTextureID();
    }
}