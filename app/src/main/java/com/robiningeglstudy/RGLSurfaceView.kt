package com.robiningeglstudy

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.opengles.GL10

class RGLSurfaceView(context: Context, attrs: AttributeSet?, defStyle: Int) : SurfaceView(context, attrs, defStyle),
    SurfaceHolder.Callback,GLSurfaceView.Renderer {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private val TAG = "RGLSurfaceView"
    private var surface: Surface? = null
    private var eglContext: EGLContext? = null
    private var glThread : GLThread? =null
    private val extraSurfaces : ArrayList<Surface> = arrayListOf()
    private val extraEglHelpers : ArrayList<EglContextHelper> = arrayListOf()
    private var renders : Array<GLSurfaceView.Renderer>? = null

    init {
        holder.addCallback(this)
    }

    fun setSurface(surface: Surface){
        this.surface = surface
    }

    fun setEglContext(eglContext: EGLContext){
        this.eglContext = eglContext
    }

    fun getExtraSurfaces() : List<Surface>{
        return extraSurfaces
    }

    fun setRenders(renders : Array<GLSurfaceView.Renderer>){
        if(glThread != null){
            throw IllegalStateException("need call serRenders before surfaceCreated")
        }
        this.renders = renders
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        glThread?.onChanged(width,height)
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
        if(renders != null){
            for(render in renders!!){
                render.onDrawFrame(gl)
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        if(renders != null){
            for(render in renders!!){
                render.onSurfaceChanged(gl,width,height)
            }
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        if(renders != null){
            for(render in renders!!){
                render.onSurfaceCreated(gl,config)
            }
        }
    }

    fun onInitExtraEglHelper(eglContext: EGLContext){
        extraEglHelpers.clear()
        for(surface in extraSurfaces){
            val contextHelper = EglContextHelper()
            contextHelper.init(surface,eglContext)
            extraEglHelpers.add(contextHelper)
        }
    }

    fun onSwapExtraBuffer(){
        for(helper in extraEglHelpers){
            helper.swapBuffers()
        }
    }

    fun onDestroyExtraEglContextHelper(){
        for(helper in extraEglHelpers){
            helper.destoryEGL()
        }
        extraEglHelpers.clear()
    }


    class GLThread(private val glSurfaceViewRef: WeakReference<RGLSurfaceView>) : Thread() {
        var isDestoryed = false
        var isChanged = false
        var width: Int = 0
        var height: Int = 0
        var eglContextHelper = EglContextHelper()

        override fun run() {
            super.run()
            if (glSurfaceViewRef.get() != null && glSurfaceViewRef.get()!!.surface != null) {
                eglContextHelper.init(glSurfaceViewRef.get()!!.surface!!, glSurfaceViewRef.get()!!.eglContext)
                glSurfaceViewRef.get()!!.onSurfaceCreated(null,eglContextHelper.currentGlConfig)
                glSurfaceViewRef.get()!!.onInitExtraEglHelper(eglContextHelper.currentEglContext!!)
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
                    glSurfaceView.onSurfaceChanged(null,width,height)
                }

                glSurfaceView.onDrawFrame(null)
                eglContextHelper.swapBuffers()
                glSurfaceView.onSwapExtraBuffer()

                try {
                    Thread.sleep(1000 / 60)
                } catch (ex: InterruptedException) {
                }
            }

            eglContextHelper.destoryEGL()
            glSurfaceViewRef.get()?.onDestroyExtraEglContextHelper()
        }

        fun onChanged(width: Int,height: Int){
            isChanged = true
            this.width = width
            this.height = height
        }

        fun exit(){
            isDestoryed = true
        }
    }
}