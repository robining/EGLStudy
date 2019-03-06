package com.robiningeglstudy

import android.app.Activity
import android.graphics.SurfaceTexture
import android.util.Log

class CameraPreviewRender(private val activity: Activity, private val surfaceView: RGLSurfaceView) :
    SurfaceTextureRender(activity),
    SurfaceTextureRender.SurfaceTextureListener {
    override fun onSurfaceTexureCreated(surfaceTexture: SurfaceTexture) {
        surfaceTexture.setOnFrameAvailableListener {
            Log.e("CameraPreviewRender", "ana------:on frame available")
            surfaceView.requestRender()
        }
        CameraUtil.openCamera(0, surfaceTexture, activity)
    }

    init {
        setSurfaceTexureListener(this)
    }
}