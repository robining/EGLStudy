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

class RGLSurfaceView(context: Context, attrs: AttributeSet?, defStyle: Int) : SurfaceView(context, attrs, defStyle),
    SurfaceHolder.Callback, GLSurfaceView.Renderer {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private val TAG = "RGLSurfaceView"
    private var surface: Surface? = null
    private var eglContext: EGLContext? = null
    private var glThread: GLThread? = null
    private val extraSurfaces: ArrayList<Surface> = arrayListOf()
    private val extraEglSurfaces: ArrayList<EGLSurface> = arrayListOf()
    private var renders: Array<GLSurfaceView.Renderer>? = null
    var renderMode: RenderMode = RenderMode.RENDERMODE_CONTINUOUSLY

    init {
        holder.addCallback(this)
    }

    fun setSurface(surface: Surface) {
        this.surface = surface
    }

    fun setEglContext(eglContext: EGLContext) {
        this.eglContext = eglContext
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
        if (surface == null) {
            surface = holder!!.surface
        }

        glThread = GLThread(WeakReference(this))
        glThread!!.start()
    }


    override fun onDrawFrame(gl: GL10?) {
        if (renders != null) {
            for (render in renders!!) {
                render.onDrawFrame(gl)
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        if (renders != null) {
            for (render in renders!!) {
                render.onSurfaceChanged(gl, width, height)
            }
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        if (renders != null) {
            for (render in renders!!) {
                render.onSurfaceCreated(gl, config)
            }
        }
    }

    fun onInitExtraEglHelper(eglContextHelper: EglContextHelper) {
        extraEglSurfaces.clear()
        for (surface in extraSurfaces) {
            val eglSurface = eglContextHelper.createEglSurface(surface)
            if(eglSurface != null) {
                extraEglSurfaces.add(eglSurface)
                eglContextHelper.bindTo(eglSurface)
            }
        }
    }

    fun onSwapExtraBuffer(eglContextHelper: EglContextHelper) {
        for (eglSurface in extraEglSurfaces) {
            Log.e(TAG,"ana------:extra egl surface...swap")
//            eglContextHelper.bindTo(eglSurface)
            eglContextHelper.swapBuffers(eglSurface)
//            eglContextHelper.bindTo(eglContextHelper.currentSurface!!)
        }
    }

    fun onDestroyExtraEglContextHelper(eglContextHelper: EglContextHelper) {
        extraEglSurfaces.clear()
    }


    class GLThread(private val glSurfaceViewRef: WeakReference<RGLSurfaceView>) : Thread() {
        private var isDestoryed = false
        private var isChanged = false
        private var width: Int = 0
        private var height: Int = 0
        private var eglContextHelper = EglContextHelper()
        private val lock = Object()
        private var isStarted = false

        override fun run() {
            super.run()
            if (glSurfaceViewRef.get() != null && glSurfaceViewRef.get()!!.surface != null) {
                eglContextHelper.init(glSurfaceViewRef.get()!!.surface!!, glSurfaceViewRef.get()!!.eglContext)
                glSurfaceViewRef.get()!!.onInitExtraEglHelper(eglContextHelper)
                glSurfaceViewRef.get()!!.onSurfaceCreated(null, eglContextHelper.currentGlConfig)
            } else {
                return
            }

            while (!isDestoryed) {
                val glSurfaceView = glSurfaceViewRef.get()
                if (glSurfaceView == null) {
                    isDestoryed = true
                    break
                }

                if (isChanged) {
                    isChanged = false
                    glSurfaceView.onSurfaceChanged(null, width, height)
                }

                if (glSurfaceView.renderMode == RenderMode.RENDERMODE_WHEN_DIRTY) {
                    try {
                        Log.e("RGLSurfaceView","ana------:wait request render")
                        synchronized(lock){
                            lock.wait()
                        }
                        Log.e("RGLSurfaceView","ana------:requested a render,continue")
                    } catch (ex: InterruptedException) {
                    }
                }else{
                    try {
                        Thread.sleep(1000 / 60)
                    } catch (ex: InterruptedException) {
                    }
                }


                glSurfaceView.onDrawFrame(null)
                eglContextHelper.swapBuffers()
                glSurfaceView.onSwapExtraBuffer(eglContextHelper)

                isStarted = true
            }

            glSurfaceViewRef.get()?.onDestroyExtraEglContextHelper(eglContextHelper)
            eglContextHelper.destoryEGL()
        }

        fun onChanged(width: Int, height: Int) {
            isChanged = true
            this.width = width
            this.height = height
        }

        fun exit() {
            isDestoryed = true
        }

        fun requestRender() {
            synchronized(lock){
                lock.notifyAll()
            }
        }
    }

    enum class RenderMode {
        RENDERMODE_WHEN_DIRTY, RENDERMODE_CONTINUOUSLY
    }
}