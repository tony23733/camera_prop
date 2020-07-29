//
// Created by zhaotao on 2020/7/7.
//

#include "base_renderer.h"

namespace cameraprop {
    constexpr float BaseRenderer::vertexPositionData[];
    constexpr float BaseRenderer::vertexCoordinateDataBack[];
    constexpr float BaseRenderer::vertexCoordinateDataFront[];

    void BaseRenderer::init() {
        assignShaderString();
        createProgram();
        createTexture();

        start = last = end = clock();
    }

    void BaseRenderer::initViewport(int width, int height) {
        glViewport(0, 0, width, height);
    }

    void BaseRenderer::leave() {
        glDetachShader(program, vertexShader);
        glDeleteShader(vertexShader);
        glDetachShader(program, fragmentShader);
        glDeleteShader(fragmentShader);
        glUseProgram(0);
        glDeleteProgram(program);
        vertexShader = fragmentShader = program = -1;
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
        glDeleteTextures(1, &oesTextureID);
    }

    void BaseRenderer::step(float *transformMatrix) {
        end = clock();
        renderFrame();

        last = end;
    }

    void BaseRenderer::assignShaderString() {

    }

    void BaseRenderer::createProgram() {
        vertexShader = loadShader(GL_VERTEX_SHADER, vertexShaderString);
        fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentShaderString);
        program = linkProgram(vertexShader, fragmentShader);

        aPositionLocation = glGetAttribLocation(program, "a_Position");
//        uTextureMatrixLocation = glGetUniformLocation(program, "u_TextureMatrix");
        aTextureCoordLocation = glGetAttribLocation(program, "a_TextureCoordinate");
//        uTextureSamplerLocation = glGetUniformLocation(program, "u_TextureSampler");
    }

    void BaseRenderer::createTexture() {
        GLuint tex[1];
        glGenTextures(1, tex);
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, tex[0]);
        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        oesTextureID = tex[0];
    }

    void BaseRenderer::renderFrame() {
        LOGI("-----BaseRenderer::renderFrame.");
        glClearColor(1, 0, 1, 1);
        glClear(GL_COLOR_BUFFER_BIT);
        glUseProgram(program);

        glActiveTexture(GL_TEXTURE_EXTERNAL_OES);
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, oesTextureID);
//        glUniform1i(uTextureSamplerLocation, 0);
//        glUniformMatrix4fv(uTextureMatrixLocation, 1, false, transformMatrix, 0);

        glVertexAttribPointer(aPositionLocation, 2, GL_FLOAT, false, 0, (const void *)vertexPositionData);
        glVertexAttribPointer(aTextureCoordLocation, 2, GL_FLOAT, false, 0, (const void *)vertexCoordinateDataFront);
        glEnableVertexAttribArray(aPositionLocation);
        glEnableVertexAttribArray(aTextureCoordLocation);

        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

        glDisableVertexAttribArray(aPositionLocation);
        glDisableVertexAttribArray(aTextureCoordLocation);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    GLuint BaseRenderer::loadShader(GLenum type, const char* shaderSource) {
        GLuint shader = glCreateShader(type);
        if (shader == 0) {
//            throw ("Create Shader Failed!" + glGetError())
            return 0;
        }
        glShaderSource(shader, 1, &shaderSource, nullptr);
        glCompileShader(shader);
        return shader;
    }

    GLuint BaseRenderer::linkProgram(GLuint vertShader, GLuint fragShader) {
        GLuint program = glCreateProgram();
        if (program == 0) {
//            throw ("Create Program Failed!" + glGetError())
            return 0;
        }
        glAttachShader(program, vertShader);
        glAttachShader(program, fragShader);
        glLinkProgram(program);
        glUseProgram(program);
        return program;
    }
}
