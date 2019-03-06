package com.robiningeglstudy

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

open class SurfaceTextureRender(private val context: Context) : GLSurfaceView.Renderer {
    private var programId: Int? = null
    private var vertexAttrId: Int? = null
    private var textureVertexAttrId: Int? = null
    private var samplerTextureUniformId: Int? = null

    private val vertexPositions = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    private val textureVertexPositions = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    private var vertexBuffer: FloatBuffer? = null
    private var textureVertexBuffer: FloatBuffer? = null
    private var textureId: Int? = null
    private var testPictureTextureId : Int? = null

    private var surfaceTexture: SurfaceTexture? = null
    private var surfaceTextureListener: SurfaceTextureListener? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig) {
        vertexBuffer = ByteBuffer.allocateDirect(4 * vertexPositions.size)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexPositions)
        vertexBuffer!!.position(0)

        textureVertexBuffer = ByteBuffer.allocateDirect(4 * textureVertexPositions.size)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(textureVertexPositions)
        textureVertexBuffer!!.position(0)

        val vertexShaderId = GlesUtil.createVertexShader(context, R.raw.gl_vertext_shader)
        val fragmentShaderId = GlesUtil.createFragmentShader(context, R.raw.gl_oes_fragment_shader)
        programId = GlesUtil.createProgram(vertexShaderId, fragmentShaderId)

        vertexAttrId = GLES20.glGetAttribLocation(programId!!, "vPosition")
        textureVertexAttrId = GLES20.glGetAttribLocation(programId!!, "fPosition")
        samplerTextureUniformId = GLES20.glGetUniformLocation(programId!!, "samplerTexture")

        textureId = GlesUtil.createOesTexture()
        surfaceTexture = SurfaceTexture(textureId!!)
        testPictureTextureId = GlesUtil.createTexture(BitmapFactory.decodeResource(context.resources,R.mipmap.a))

        if (surfaceTextureListener != null) {
            surfaceTextureListener!!.onSurfaceTexureCreated(surfaceTexture!!)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        Log.e("SurfaceTextureRender", "ana------:on draw frame")
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)

        GLES20.glUseProgram(programId!!)
        surfaceTexture?.updateTexImage()

        GLES20.glEnableVertexAttribArray(vertexAttrId!!)
        GLES20.glVertexAttribPointer(vertexAttrId!!, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer)

        GLES20.glEnableVertexAttribArray(textureVertexAttrId!!)
        GLES20.glVertexAttribPointer(textureVertexAttrId!!, 2, GLES20.GL_FLOAT, false, 8, textureVertexBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId!!)
        GLES20.glUniform1i(samplerTextureUniformId!!, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    fun setSurfaceTexureListener(listener: SurfaceTextureRender.SurfaceTextureListener) {
        this.surfaceTextureListener = listener
    }

    interface SurfaceTextureListener {
        fun onSurfaceTexureCreated(surfaceTexture: SurfaceTexture)
    }
}
