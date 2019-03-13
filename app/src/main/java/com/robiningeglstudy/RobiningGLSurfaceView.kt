package com.robiningeglstudy

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLContext

class RobiningGLSurfaceView(context: Context, attrs: AttributeSet?, defStyle: Int) :
    SurfaceView(context, attrs, defStyle),
    SurfaceHolder.Callback, RGLRender {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private var glThread: GLThread? = null
    private var surface: Surface? = null

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
    private var programId: Int? = null
    private var vertexAttrId: Int? = null
    private var textureVertexAttrId: Int? = null
    private var samplerTextureUniformId: Int? = null
    private var localTextureId : Int? = null
    private var render : GLSurfaceView.Renderer? = null

    init {
        render = PictureRender(context)
        holder.addCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        glThread?.onChanged(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        glThread?.exit()
        glThread = null
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        if (surface == null) {
            surface = holder!!.surface
        }

        glThread = GLThread(WeakReference(this), surface, null)
        glThread!!.start()
    }

    override fun onSurfaceCreated(eglContext: EGLContext) {
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
        val fragmentShaderId = GlesUtil.createFragmentShader(context, R.raw.gl_samper2d_fragment_shader)
        programId = GlesUtil.createProgram(vertexShaderId, fragmentShaderId)

        vertexAttrId = GLES20.glGetAttribLocation(programId!!, "vPosition")
        textureVertexAttrId = GLES20.glGetAttribLocation(programId!!, "fPosition")
        samplerTextureUniformId = GLES20.glGetUniformLocation(programId!!, "samplerTexture")

        localTextureId = GlesUtil.createTexture(BitmapFactory.decodeResource(context.resources,R.drawable.b))

//        render!!.onSurfaceCreated(null,null)
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
//        render!!.onSurfaceChanged(null,width,height)
    }

    override fun onDrawFrame() {
        render!!.onDrawFrame(null)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)

        GLES20.glUseProgram(programId!!)

        GLES20.glEnableVertexAttribArray(vertexAttrId!!)
        GLES20.glVertexAttribPointer(vertexAttrId!!, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer)

        GLES20.glEnableVertexAttribArray(textureVertexAttrId!!)
        GLES20.glVertexAttribPointer(textureVertexAttrId!!, 2, GLES20.GL_FLOAT, false, 8, textureVertexBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, localTextureId!!)
        GLES20.glUniform1i(samplerTextureUniformId!!, 1)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    override fun onSurfaceDestoryed() {

    }
}