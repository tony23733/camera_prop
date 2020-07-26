//
// Created by 01379869 on 2020/7/16.
//

#ifndef CAMERA_PROP_SOUL_COLOR_RENDERER_H
#define CAMERA_PROP_SOUL_COLOR_RENDERER_H

#include "base_color_renderer.h"

namespace cameraprop {
    class SoulColorRenderer: public BaseColorRenderer {
    protected:
        virtual void assignShaderString();
        virtual void createProgram();

        virtual void renderFrame();

    protected:
        GLuint uTimeLocation;
        GLuint uDurationLocation;
        GLuint uScaleLocation;

        const char* soulFragmentShader = "#version 300 es\n"
                                         "\n"
                                         "precision mediump float;\n"
                                         "\n"
                                         "uniform samplerExternalOES u_TextureSampler;\n"
                                         "uniform float u_Time;\n"
                                         "uniform float u_Duration;\n"
                                         "uniform vec2 u_Scale;\n"
                                         "in vec2 vTexCoord;\n"
                                         "out vec4 fragColor;\n"
                                         "\n"
                                         "void main()\n"
                                         "{\n"
                                         "    float timelife = mod(u_Time, u_Duration) / u_Duration;\n"
                                         "    float s = mix(u_Scale.x, u_Scale.y, timelife);\n"
                                         "    vec2 newCoord = vTexCoord - vec2(0.5,0.5);\n"
                                         "    vec2 rCoord = newCoord / s;\n"
                                         "    rCoord += vec2(0.5, 0.5);\n"
                                         "\n"
                                         "    vec4 back = texture(u_TextureSampler, vTexCoord);\n"
                                         "    vec4 soul = texture(u_TextureSampler, rCoord);\n"
                                         "    fragColor = mix(soul, back, timelife);\n"
                                         "}";
    };
}

#endif //CAMERA_PROP_SOUL_COLOR_RENDERER_H
