//
// Created by 01379869 on 2020/7/16.
//

#include "soul_color_renderer.h"

namespace cameraprop {
    void cameraprop::SoulColorRenderer::assignShaderString() {
        fragmentShaderString = soulFragmentShader;
    }

    void SoulColorRenderer::createProgram() {
        BaseRenderer::createProgram();

        uTimeLocation = glGetUniformLocation(program, "u_Time");
        uDurationLocation = glGetUniformLocation(program, "u_Duration");
        uScaleLocation = glGetUniformLocation(program, "u_Scale");
    }

    void SoulColorRenderer::renderFrame() {
        glClearColor(1, 1, 1, 1);
        glClear(GL_COLOR_BUFFER_BIT);
        glUseProgram(program);

        glActiveTexture(GL_TEXTURE_EXTERNAL_OES);
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, oesTextureID);

        glVertexAttribPointer(aPositionLocation, 2, GL_FLOAT, false, 0, (const void *)vertexPositionData);
        glVertexAttribPointer(aTextureCoordLocation, 2, GL_FLOAT, false, 0, (const void *)vertexCoordinateDataFront);
        glEnableVertexAttribArray(aPositionLocation);
        glEnableVertexAttribArray(aTextureCoordLocation);

        // 设置参数
        glUniform1f(uTimeLocation, 0.8);
        glUniform1f(uDurationLocation, 2);
        glUniform2f(uScaleLocation, 2, 1, 2);

        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

        glDisableVertexAttribArray(aPositionLocation);
        glDisableVertexAttribArray(aTextureCoordLocation);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
}