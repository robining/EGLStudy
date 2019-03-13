package com.robiningeglstudy

import javax.microedition.khronos.egl.EGLContext

interface RGLRender {
    fun onSurfaceCreated(eglContext: EGLContext)
    fun onSurfaceChanged(width: Int, height: Int)
    fun onDrawFrame()
    fun onSurfaceDestroyed()
}