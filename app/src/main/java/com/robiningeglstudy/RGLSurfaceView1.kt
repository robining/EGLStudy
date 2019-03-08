package com.robiningeglstudy

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLSurface
import javax.microedition.khronos.opengles.GL10

class RGLSurfaceView1(context: Context, attrs: AttributeSet?, defStyle: Int) : SurfaceView(context, attrs, defStyle),
    SurfaceHolder.Callback, RGLRender {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private val TAG = "RGLSurfaceView"
    private var glThread: GLThread? = null
    private val extraSurfaces: ArrayList<Surface> = arrayListOf()
    private val extraSharedRenders: ArrayList<RGLRender> = arrayListOf()
    private var renders: Array<GLSurfaceView.Renderer>? = null
    private var renderMode: RenderMode = RenderMode.RENDERMODE_CONTINUOUSLY

    init {
        holder.addCallback(this)
    }

    fun getExtraSurfaces(): ArrayList<Surface> {
        return extraSurfaces
    }

    fun setRenders(renders: Array<GLSurfaceView.Renderer>) {
        if (glThread != null) {
            throw IllegalStateException("need call serRenders before surfaceCreated")
        }
        this.renders = renders
    }

    fun requestRender() {
        glThread?.requestRender()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        glThread?.onChanged(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        glThread?.exit()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        glThread = GLThread(WeakReference(this), null, null)
        glThread!!.setRenderMode(renderMode)
        glThread!!.start()
    }

    override fun onSurfaceDestoryed() {

    }

    override fun onDrawFrame() {
        if (renders != null) {
            for (render in renders!!) {
                render.onDrawFrame(null)
            }
        }
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        if (renders != null) {
            for (render in renders!!) {
                render.onSurfaceChanged(null, width, height)
            }
        }
    }

    override fun onSurfaceCreated(eglContext: EGLContext) {


        extraSharedRenders.clear()
        for (surface in extraSurfaces) {
            val sharedRender = SharedRenderImpl(surface, eglContext,0)
            extraSharedRenders.add(sharedRender)
            sharedRender.setRenderMode(RenderMode.RENDERMODE_WHEN_DIRTY)
        }

        if (renders != null) {
            for (render in renders!!) {
                render.onSurfaceCreated(null, null)
            }
        }
    }

    fun setRenderMode(renderMode: RenderMode) {
        this.renderMode = renderMode
        glThread?.setRenderMode(renderMode)
    }
}