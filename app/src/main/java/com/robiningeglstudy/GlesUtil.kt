package com.robiningeglstudy

import android.content.Context
import android.opengl.GLES20
import android.support.annotation.IdRes
import android.support.annotation.RawRes

object GlesUtil {
    fun readStringFromRaw(context: Context, @RawRes idRes: Int): String {
        val inputStream = context.resources.openRawResource(idRes)
        val buffer = ByteArray(1024)
        var length = inputStream.read(buffer)
        val stringBuilder = StringBuilder()
        while (length != -1) {
            stringBuilder.append(String(buffer, 0, length))
            length = inputStream.read(buffer)
        }

        return stringBuilder.toString()
    }

    fun createVertexShader(context: Context, @RawRes idRes: Int): Int {
        val shaderStr = readStringFromRaw(context, idRes)
        return GlesUtil.createShader(GLES20.GL_VERTEX_SHADER,shaderStr)
    }

    fun createFragmentShader(context: Context,@RawRes idRes: Int) : Int{
        val shaderStr = readStringFromRaw(context, idRes)
        return GlesUtil.createShader(GLES20.GL_FRAGMENT_SHADER,shaderStr)
    }

    fun createShader(shaderType: Int, shaderStr: String): Int {
        val shaderId = GLES20.glCreateShader(shaderType)
        GLES20.glShaderSource(shaderId, shaderStr)
        GLES20.glCompileShader(shaderId)

        val compileResult = IntArray(1)
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, compileResult, 0)
        if (compileResult[0] != GLES20.GL_TRUE) {
            throw IllegalStateException("create shader failed")
        }

        return shaderId
    }

    fun createProgram(vararg shaders:Int) : Int{
        val programId = GLES20.glCreateProgram()
        for(shader in shaders){
            GLES20.glAttachShader(programId,shader)
        }
        GLES20.glLinkProgram(programId)

        val linkResult = IntArray(1)
        GLES20.glGetProgramiv(programId,GLES20.GL_LINK_STATUS,linkResult,0)
        if(linkResult[0] != GLES20.GL_TRUE){
            throw IllegalStateException("create program failed")
        }

        return programId
    }
}