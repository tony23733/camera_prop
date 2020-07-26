//
// Created by zhaotao on 2020/7/8.
//

#include <jni.h>
#include <string>

#include "base_renderer.h"
#include "cplog.h"

cameraprop::BaseRenderer renderer;

extern "C" {
    JNIEXPORT void JNICALL
    Java_com_toto_pro_1library_jni_1call_CpLibrary_initJNI(JNIEnv *env, jobject thiz, jint width,
    jint height) {
        renderer.init();
    }

    JNIEXPORT void JNICALL
    Java_com_toto_pro_1library_jni_1call_CpLibrary_renderFrameJNI(JNIEnv *env, jobject thiz) {
        renderer.step(nullptr);
    }
};