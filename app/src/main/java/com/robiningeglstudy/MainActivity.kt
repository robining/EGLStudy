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
import javax.microedition.khronos.opengles.GL10
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.Matrix
import android.os.Environment
import android.view.View
import java.io.File
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    private var mediaMuxer: MediaMuxer? = null
    private var encodec: MediaCodec? = null
    private var isRecording = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        CameraUtil.openCamera(surfaceView)

        surfaceView.setRenderMode(RenderMode.RENDERMODE_CONTINUOUSLY)
//        surfaceView.setRenders(arrayOf(PictureRender(this)))
        surfaceView.setRenders(arrayOf(BackgroundRender(), CameraRender(this, object :
            SurfaceTextureRender.SurfaceTextureListener {
            override fun onSurfaceTexureCreated(surfaceTexture: SurfaceTexture) {
                println(">>>ready to request render")
                surfaceView.requestRender()
            }
        })))

//        surfaceView.renderMode = RenderMode.RENDERMODE_WHEN_DIRTY
//        val tRender = CameraPreviewRender(this, surfaceView)
//        surfaceView.setRenders(arrayOf(tRender))
        val file = File(Environment.getExternalStorageDirectory(), "1.mp4")
        mediaMuxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
//
        val mime = "video/avc"
        val width = 1080
        val height = 1920
        val videoFormat = MediaFormat.createVideoFormat(mime, 1080, 1920)
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4)
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        encodec = MediaCodec.createEncoderByType(mime)
        encodec!!.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        val surface = encodec!!.createInputSurface()
        surfaceView.getExtraSurfaces().add(surface)

        encodec!!.start()

        var videoTrackIndex: Int? = null
        thread {
            val mediaInfo = MediaCodec.BufferInfo()
            while (true) {
                var outbufferIndex = encodec!!.dequeueOutputBuffer(mediaInfo, 10)
                if (outbufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    videoTrackIndex = mediaMuxer!!.addTrack(encodec!!.outputFormat)
                }else{
                    while (outbufferIndex >= 0) {
                        val buffer = encodec!!.outputBuffers[outbufferIndex]
                        buffer.position(mediaInfo.offset)
                        buffer.limit(mediaInfo.size + mediaInfo.offset)
                        if (isRecording) {
                            Log.e("MainActivity", "ana------:encoded ${mediaInfo.size} data")
                            mediaMuxer!!.writeSampleData(videoTrackIndex!!, buffer, mediaInfo)
                        }
                        encodec!!.releaseOutputBuffer(outbufferIndex, false)
                        outbufferIndex = encodec!!.dequeueOutputBuffer(mediaInfo, 10)
                    }
                }

            }
        }
    }

    fun startRecord(view: View) {
        mediaMuxer!!.start()
        isRecording = true
    }

    fun stopRecord(view: View) {
        isRecording = false
        mediaMuxer!!.stop()
        mediaMuxer!!.release()
    }
}
