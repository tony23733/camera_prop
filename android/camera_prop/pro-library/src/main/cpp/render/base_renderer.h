//
// Created by zhaotao on 2020/7/7.
//

#ifndef CAMERA_PROP_BASE_RENDERER_H
#define CAMERA_PROP_BASE_RENDERER_H

#include <string>
#include <GLES3/gl3.h>
#include <GLES2/gl2ext.h>

#include "cplog.h"

namespace cameraprop {
    class BaseRenderer {
    public:
        virtual void init();
        virtual void initViewport(int width, int height);
        virtual void leave();
        void step(float transformMatrix[]);
        GLuint getOesTextureID() { return oesTextureID; }

    protected:
        virtual void assignShaderString();
        virtual void createProgram();
        void createTexture();
        virtual void renderFrame();
        /**
         * 获取此渲染器启动至现在的时间消耗，单位s。
         * @return 时间消耗，单位s。
         */
        inline float getElapseTime() { return (float)((end - start) / CLOCKS_PER_SEC); }
        inline float getDeltaTime() { return (float)((end - last) / CLOCKS_PER_SEC); }

        static GLuint loadShader(GLenum type, const char* shaderSource);
        static GLuint linkProgram(GLuint vertShader, GLuint fragShader);

    protected:
        int vertexShader = -1;
        int fragmentShader = -1;
        GLuint program = -1;
        GLuint oesTextureID = 0;
        GLuint aPositionLocation = 0;
        GLuint aTextureCoordLocation = 0;

        const char* vertexShaderString = "\n"
                                         "attribute vec4 a_Position;\n"
                                         "attribute vec2 a_TextureCoordinate;\n"
                                         "varying vec2 textureCoordinate;\n"
                                         "void main()\n"
                                         "{\n"
                                         "gl_Position = a_Position;\n"
                                         "textureCoordinate = a_TextureCoordinate;\n"
                                         "}";
        const char* fragmentShaderString = "\n"
                        "#extension GL_OES_EGL_image_external : require\n"
                        "precision mediump float;\n"
                        "varying vec2 textureCoordinate;\n"
                        "uniform samplerExternalOES u_TextureSampler;\n"
                        "void main() {\n"
                        "  gl_FragColor = texture2D( u_TextureSampler, textureCoordinate );\n"
                        "}";

        static constexpr float vertexPositionData[] = {
            -1.0f, -1.0f, // 左下
            1.0f, -1.0f, // 右下
            1.0f, 1.0f, // 右上
            -1.0f, 1.0f // 左上
            };
        static constexpr float vertexCoordinateDataBack[] {
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
            0.0f, 1.0f
        };
        static constexpr float vertexCoordinateDataFront[] {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
        };

    private:
        clock_t start, last, end;
    };
}


#endif //CAMERA_PROP_BASE_RENDERER_H
