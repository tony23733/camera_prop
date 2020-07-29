//
// Created by zhaotao on 2020/7/8.
//

#include <jni.h>
#include <string>
#include <soul_color_renderer.h>
#include <original_color_renderer.h>

#include "base_renderer.h"
#include "base_color_renderer.h"
#include "cplog.h"

cameraprop::BaseColorRenderer renderer;

extern "C" {
    JNIEXPORT void JNICALL
    Java_com_toto_pro_1library_jni_1call_CpLibrary_initJNI(JNIEnv *env, jobject thiz) {
        renderer.init();
    }

    JNIEXPORT void JNICALL
    Java_com_toto_pro_1library_jni_1call_CpLibrary_initViewportJNI(JNIEnv *env, jobject thiz,
                                                                   jint width, jint height) {
        renderer.initViewport(width, height);
    }

    JNIEXPORT void JNICALL
    Java_com_toto_pro_1library_jni_1call_CpLibrary_renderFrameJNI(JNIEnv *env, jobject thiz) {
        renderer.step(nullptr);
    }

    JNIEXPORT jint JNICALL
    Java_com_toto_pro_1library_jni_1call_CpLibrary_getOesTextureIDJNI(JNIEnv *env, jobject thiz) {
        return renderer.getOesTextureID();
    }
};