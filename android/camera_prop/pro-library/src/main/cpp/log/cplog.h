//
// Created by zhaotao on 2020/7/8.
//

#ifndef CAMERA_PROP_CPLOG_H
#define CAMERA_PROP_CPLOG_H

#include <android/log.h>

#define LOG_TAG "prop-library"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#endif //CAMERA_PROP_CPLOG_H
