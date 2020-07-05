package com.toto.pro_library.render

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20.*
import androidx.core.graphics.translationMatrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


class BaseRender {
    lateinit var context: Context

    var vertexShader = -1
    var fragmentShader = -1
    var program = -1
    var oesTextureID = -1
    var aPositionLocation = -1
    var aTextureCoordLocation = -1
    var uTextureMatrixLocation = -1
    var uTextureSamplerLocation = -1

    val vertexShaderString = "attribute vec4 a_Position;\n" +
            "uniform mat4 u_TextureMatrix;\n" +
            "attribute vec4 a_TextureCoordinate;\n" +
            "varying vec2 v_TexCoord;\n" +
            "void main()\n" +
            "{\n" +
            "  v_TexCoord = (u_TextureMatrix * a_TextureCoordinate).xy;\n" +
            "  gl_Position = a_Position;\n" +
            "}"
//    val fragmentShaderString = "#extension GL_OES_EGL_image_external : require\n" +
//            "precision mediump float;\n" +
//            "uniform samplerExternalOES u_TextureSampler;\n" +
//            "varying vec2 v_TextureCoord;\n" +
//            "void main()\n" +
//            "{\n" +
//            "  vec4 v_CameraColor = texture2D(u_TextureSampler, v_TextureCoord);\n" +
//            "  float fGrayColor = (0.3*v_CameraColor.r + 0.59*v_CameraColor.g + 0.11*v_CameraColor.b);\n" +
//            "  gl_FragColor = vec4(fGrayColor, fGrayColor, fGrayColor, 1.0);\n" +
//            "}\n"

    val fragmentShaderString = (""+
    "#extension GL_OES_EGL_image_external : require\n" +
    "precision mediump float;" +
    "varying vec2 textureCoordinate;\n" +
    "uniform samplerExternalOES u_TextureSampler;\n" +
    "void main() {" +
    "  gl_FragColor = texture2D( u_TextureSampler, textureCoordinate );\n" +
    "}")

    private lateinit var vertexPositionBuffer: FloatBuffer
    private lateinit var vertexCoordinateBuffer: FloatBuffer

    private val vertexPositionData: FloatArray = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    private var vertexCoordinateData: FloatArray = floatArrayOf(
        1f, 1f,
        1f, 0f,
        0f, 1f,
        1f, 1f
    )

    fun init() {
        createProgram()
        createTexture()
        createBuffer()
    }

    fun initViewport(width: Int, height: Int) {
        glViewport(0, 0, width, height)
    }

    fun step(transformMatrix: FloatArray?) {
        renderFrame(transformMatrix)
    }

    private fun createProgram() {
        vertexShader = loadShader(GL_VERTEX_SHADER, vertexShaderString)
        fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentShaderString)
        program = linkProgram(vertexShader, fragmentShader)

        aPositionLocation = glGetAttribLocation(program, "a_Position")
        uTextureMatrixLocation = glGetUniformLocation(program, "u_TextureMatrix")
        aTextureCoordLocation = glGetAttribLocation(program, "a_TextureCoordinate")
        uTextureSamplerLocation = glGetUniformLocation(program, "u_TextureSampler")
    }

    private fun createTexture() {
        val tex = intArrayOf(0)
        glGenTextures(1, tex, 0)
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0])
        glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        oesTextureID = tex[0]
    }

    private fun createBuffer() {
        vertexPositionBuffer = ByteBuffer.allocateDirect(vertexPositionData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexPositionBuffer.put(vertexPositionData).position(0)

        vertexCoordinateBuffer = ByteBuffer.allocateDirect(vertexCoordinateData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexCoordinateBuffer.put(vertexCoordinateData).position(0)
    }

    private fun renderFrame(transformMatrix: FloatArray?) {
        if (transformMatrix == null) {
            return
        }
        glClearColor(1f, 1f, 1f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)
        glUseProgram(program)

        glActiveTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureID)
        glUniform1i(uTextureSamplerLocation, 0)
        glUniformMatrix4fv(uTextureMatrixLocation, 1, false, transformMatrix, 0)

        vertexPositionBuffer.position(0)
        vertexCoordinateBuffer.position(0)
        glVertexAttribPointer(aPositionLocation, 2, GL_FLOAT, false, 0, vertexPositionBuffer)
        glVertexAttribPointer(aTextureCoordLocation, 2, GL_FLOAT, false, 0, vertexCoordinateBuffer)
        glEnableVertexAttribArray(aPositionLocation)
        glEnableVertexAttribArray(aTextureCoordLocation)

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)

        glDisableVertexAttribArray(aPositionLocation)
        glDisableVertexAttribArray(aTextureCoordLocation)
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private fun loadShader(type: Int, shaderSource: String?): Int {
        val shader = glCreateShader(type)
        if (shader == 0) {
            throw RuntimeException("Create Shader Failed!" + glGetError())
        }
        glShaderSource(shader, shaderSource)
        glCompileShader(shader)
        return shader
    }

    private fun linkProgram(verShader: Int, fragShader: Int): Int {
        val program = glCreateProgram()
        if (program == 0) {
            throw java.lang.RuntimeException("Create Program Failed!" + glGetError())
        }
        glAttachShader(program, verShader)
        glAttachShader(program, fragShader)
        glLinkProgram(program)
        glUseProgram(program)
        return program
    }
}