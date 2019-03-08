package com.robiningeglstudy

import javax.microedition.khronos.egl.EGLContext

interface RGLRender {
    fun onDrawFrame()
    fun onSurfaceChanged(width: Int, height: Int)

    fun onSurfaceCreated(eglContext: EGLContext)

    fun onSurfaceDestoryed()
}