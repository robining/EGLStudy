package com.robiningeglstudy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val backgroundRender = BackgroundRender()
        val pictureRender = PictureRender(this)
        surfaceView.setRenders(arrayOf(backgroundRender,pictureRender))
    }

    class BackgroundRender : GLSurfaceView.Renderer{
        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glClearColor(1.0f,0.0f,0.0f,1.0f)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        }

    }

    class PictureRender(private val context: Context) : GLSurfaceView.Renderer{
        private var vPositionId : Int? = null
        private var fPositionId : Int? = null
        private var samplerTextureId : Int? = null
        private var programId : Int? = null
        private var textureId : Int? = null
        private var imgTextureId : Int? = null //若使用fbo缓冲，那么只有imgTextureId里面有图，textureId是初始化的空的
        private var vertexVboId : Int? = null
        private var textureVertexVboId : Int? =null
        private var textureFboId : Int? = null
        private var isCreatedSuccess = false

        private val vertexArray = floatArrayOf(
            -0.5f,-0.5f,
            0.5f,-0.5f,
            -0.5f,0.5f,
            0.5f,0.5f
        )

        private val textureVertexArray = floatArrayOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
        )
        private val vertexBuffer : FloatBuffer
        private val fragmentBuffer : FloatBuffer
        init {
            vertexBuffer = ByteBuffer.allocateDirect(4 * vertexArray.size)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexArray);
            vertexBuffer.position(0)

            fragmentBuffer = ByteBuffer.allocateDirect(4 * textureVertexArray.size)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureVertexArray)
            fragmentBuffer.position(0)

        }

        override fun onDrawFrame(gl: GL10?) {
            if(!isCreatedSuccess){
                return
            }

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,textureFboId!!)

            GLES20.glUseProgram(programId!!)

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,imgTextureId!!)
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
//            GLES20.glUniform1i(samplerTextureId!!,0)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,vertexVboId!!)
            GLES20.glEnableVertexAttribArray(vPositionId!!)
            GLES20.glVertexAttribPointer(vPositionId!!,2,GLES20.GL_FLOAT,false,8,0)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,textureVertexVboId!!)
            GLES20.glEnableVertexAttribArray(fPositionId!!)
            GLES20.glVertexAttribPointer(fPositionId!!,2,GLES20.GL_FLOAT,false,8,0)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0,0,width,height)
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            val vertexShaderId = GlesUtil.createVertexShader(context,R.raw.gl_vertext_shader)
            val fragmentShaderId = GlesUtil.createFragmentShader(context,R.raw.gl_fragment_shader)
            programId = GlesUtil.createProgram(vertexShaderId,fragmentShaderId)

            vPositionId = GLES20.glGetAttribLocation(programId!!,"vPosition")
            fPositionId = GLES20.glGetAttribLocation(programId!!,"fPosition")
            samplerTextureId = GLES20.glGetUniformLocation(programId!!,"samplerTexture")

            vertexVboId = createVboBuffer(vertexArray,vertexBuffer)
            textureVertexVboId = createVboBuffer(textureVertexArray,fragmentBuffer)

//            val result  = createFboBuffer()
//            textureFboId = result[0]
//            textureId = result[1]

            val fbos = IntArray(1)
            GLES20.glGenBuffers(1, fbos, 0)
            textureFboId = fbos[0]
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, textureFboId!!)


            val textureIds = IntArray(1)
            GLES20.glGenTextures(1, textureIds, 0)
            textureId = textureIds[0]

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId!!)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glUniform1i(samplerTextureId!!, 0)

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_RGBA,
                720,
                1280,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                null
            )
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D,
                textureId!!,
                0
            )
            if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                Log.e("ywl5320", "fbo wrong")
            } else {
                Log.e("ywl5320", "fbo success")
            }

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

            val bitmap : Bitmap = BitmapFactory.decodeResource(context.resources,R.mipmap.ic_launcher)
            imgTextureId = createTexture(bitmap)
            isCreatedSuccess = true
        }


        private fun createVboBuffer(data : FloatArray,buffer : FloatBuffer) : Int{
            val bufferIds = IntArray(1)
            GLES20.glGenBuffers(1,bufferIds,0)
            val bufferId = bufferIds[0]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,bufferId)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,data.size * 4,null,GLES20.GL_STATIC_DRAW)
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER,0,data.size * 4,buffer)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0)
            return bufferId
        }

        private fun createFboBuffer() : IntArray{
            val bufferIds = IntArray(1)
            GLES20.glGenBuffers(1,bufferIds,0)
            val bufferId = bufferIds[0]
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,bufferId)

            val fboTextureId = createTexture(null)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,fboTextureId)
            GLES20.glActiveTexture(fboTextureId)
            GLES20.glUniform1i(samplerTextureId!!,0)
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 720, 1280, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)

            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,GLES20.GL_TEXTURE_2D,fboTextureId,0)
            if(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE){
                throw IllegalStateException("create fbo failed")
            }
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0)

            return intArrayOf(bufferId,fboTextureId)
        }

        private fun createTexture(bitmap: Bitmap?) : Int{
            val textureIds = IntArray(1)
            GLES20.glGenTextures(1,textureIds,0)
            val textureId = textureIds[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId!!)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

            if(bitmap != null){
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0)
                bitmap.recycle()
            }

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)

            return textureId
        }
    }
}