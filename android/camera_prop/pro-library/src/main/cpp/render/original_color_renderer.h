//
// Created by 01379869 on 2020/7/15.
//

#ifndef CAMERA_PROP_ORIGINAL_COLOR_RENDERER_H
#define CAMERA_PROP_ORIGINAL_COLOR_RENDERER_H

#include "base_color_renderer.h"

namespace cameraprop {
    class OriginalColorRenderer: public BaseColorRenderer {
    protected:
        virtual void assignShaderString();

    protected:
        const char* originalFragmentShader = "#extension GL_OES_EGL_image_external : require\n"
                   "precision mediump float;\n"
                   "varying vec2 textureCoordinate;\n"
                   "uniform samplerExternalOES u_TextureSampler;\n"
                   "void main() {\n"
                   "  gl_FragColor = texture2D( u_TextureSampler, textureCoordinate );\n"
                   "}";
    };
}

#endif //CAMERA_PROP_ORIGINAL_COLOR_RENDERER_H
