package com.robiningeglstudy

import android.view.Surface
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGLContext

class SharedRenderImpl(private val surface: Surface, private val eglContext: EGLContext, private val textureId: Int) :
    RGLRender {
    private var glThread: GLThread? = null
    private var renderMode = RenderMode.RENDERMODE_CONTINUOUSLY

    fun setRenderMode(renderMode: RenderMode) {
        this.renderMode = renderMode
        glThread?.setRenderMode(renderMode)
    }

    override fun onDrawFrame() {

    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        glThread?.onChanged(width, height)
    }

    override fun onSurfaceCreated(eglContext: EGLContext) {
        glThread = GLThread(WeakReference(this), surface, this.eglContext)
        glThread!!.setRenderMode(renderMode)
        glThread!!.start()
    }

    override fun onSurfaceDestoryed() {
        glThread!!.exit()
    }

    fun requestRender() {
        glThread?.requestRender()
    }
}