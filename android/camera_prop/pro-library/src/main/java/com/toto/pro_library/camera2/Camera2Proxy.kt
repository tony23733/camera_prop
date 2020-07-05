package com.toto.pro_library.camera2

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.params.MeteringRectangle
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.view.SurfaceHolder
import java.util.*
import kotlin.math.abs


class Camera2Proxy @TargetApi(Build.VERSION_CODES.M) constructor(private val activity: Activity) {
    private var cameraId = CameraCharacteristics.LENS_FACING_FRONT // 要打开的摄像头ID
    private lateinit var previewSize: Size // 预览大小
    private val cameraManager: CameraManager // 相机管理者
    private lateinit var cameraCharacteristics: CameraCharacteristics // 相机属性
    private var cameraDevice: CameraDevice? = null // 相机对象
    private var captureSession: CameraCaptureSession? = null
    private lateinit var previewRequestBuilder: CaptureRequest.Builder // 相机预览请求的构造器
    private lateinit var previewRequest: CaptureRequest
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private var imageReader: ImageReader? = null
    private var previewSurface: Surface? = null
    private var previewSurfaceTexture: SurfaceTexture? = null
    private val orientationEventListener: OrientationEventListener
    private var displayRotate = 0
    private var deviceOrientation = 0 // 设备方向，由相机传感器获取
    private var zoom = 1 // 缩放

    fun getPreviewSize(): Size {
        return previewSize
    }

    /**
     * 打开摄像头的回调
     */
    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "onOpened")
            cameraDevice = camera
            initPreviewRequest()
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "onDisconnected")
            releaseCamera()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(TAG, "Camera Open failed, error: $error")
            releaseCamera()
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(width: Int, height: Int) {
        Log.v(TAG, "openCamera")
        startBackgroundThread() // 对应 releaseCamera() 方法中的 stopBackgroundThread()
        orientationEventListener.enable()
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId.toString())
            val map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            // 拍照大小，选择能支持的一个最大的图片大小
            val largest = Collections.max(
                Arrays.asList(*map!!.getOutputSizes(ImageFormat.JPEG)),
                CompareSizesByArea()
            )
            Log.d(TAG, "picture size: " + largest.width + "*" + largest.height)
            imageReader = ImageReader.newInstance(largest.width, largest.height, ImageFormat.JPEG, 2)
            // 预览大小，根据上面选择的拍照图片的长宽比，选择一个和控件长宽差不多的大小
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java), width, height, largest)
            Log.d(TAG, "preview size: " + previewSize.width + "*" + previewSize.height)
            // 打开摄像头
            cameraManager.openCamera(cameraId.toString(), stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun releaseCamera() {
        Log.v(TAG, "releaseCamera")
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
        orientationEventListener.disable()
        stopBackgroundThread() // 对应 openCamera() 方法中的 startBackgroundThread()
    }

    fun setImageAvailableListener(onImageAvailableListener: OnImageAvailableListener) {
        imageReader?.setOnImageAvailableListener(onImageAvailableListener, null)
    }

    fun setPreviewSurface(holder: SurfaceHolder) {
        previewSurface = holder.surface
    }

    fun setPreviewSurface(surfaceTexture: SurfaceTexture) {
        previewSurfaceTexture = surfaceTexture
    }

    private fun initPreviewRequest() {
        try {
            if (previewSurfaceTexture != null && previewSurface == null) {
                previewSurfaceTexture!!.setDefaultBufferSize(previewSize.width, previewSize.height)
                previewSurface = Surface(previewSurfaceTexture)
            }

            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(previewSurface!!) // 设置预览输出的 Surface
            cameraDevice!!.createCaptureSession(Arrays.asList(previewSurface, imageReader?.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        // 设置连续自动对焦
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        // 设置自动曝光
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                        // 设置完后自动开始预览
                        previewRequest = previewRequestBuilder.build()
                        startPreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "ConfigureFailed. session: mCaptureSession")
                    }
                }, backgroundHandler
            ) // handle 传入 null 表示使用当前线程的 Looper
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun startPreview() {
        try {
            // 开始预览，即一直发送预览的请求
            captureSession?.setRepeatingRequest(previewRequest, null, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun stopPreview() {
        try {
            captureSession?.stopRepeating()
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun captureStillPicture() {
        try {
            if (cameraDevice == null) return
            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(imageReader!!.surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(deviceOrientation))
            }
            // 预览如果有放大，拍照的时候也应该保存相同的缩放
            val zoomRect: Rect? = previewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION)
            zoomRect?.apply { captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect) }

            captureSession?.apply {
                stopRepeating()
                abortCaptures()
                val time = System.currentTimeMillis()
                capture(captureBuilder.build(), object : CaptureCallback() {
                    override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                        Log.w(TAG, "onCaptureCompleted, time: " + (System.currentTimeMillis() - time))
                        try {
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
                            captureSession?.capture(previewRequestBuilder.build(), null, backgroundHandler)
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                        startPreview()
                    }
                }, backgroundHandler)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun getJpegOrientation(deviceOrientation: Int): Int {
        var deviceOrientation = deviceOrientation
        if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) return 0
        val sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90
        // Reverse device orientation for front-facing cameras
        val facingFront = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) deviceOrientation = -deviceOrientation
        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360
    }

    val isFrontCamera: Boolean
        get() = cameraId == CameraCharacteristics.LENS_FACING_BACK

    fun switchCamera(width: Int, height: Int) {
        cameraId = cameraId xor 1
        releaseCamera()
        openCamera(width, height)
    }

    private fun chooseOptimalSize(sizes: Array<Size>, viewWidth: Int, viewHeight: Int, pictureSize: Size): Size {
        val totalRotation = rotation
        val swapRotation = totalRotation == 90 || totalRotation == 270
        val width = if (swapRotation) viewHeight else viewWidth
        val height = if (swapRotation) viewWidth else viewHeight
        return getSuitableSize(sizes, width, height, pictureSize)
    }

    private val rotation: Int
        get() {
            var displayRotation = activity.windowManager.defaultDisplay.rotation
            when (displayRotation) {
                Surface.ROTATION_0 -> displayRotation = 90
                Surface.ROTATION_90 -> displayRotation = 0
                Surface.ROTATION_180 -> displayRotation = 270
                Surface.ROTATION_270 -> displayRotation = 180
            }
            val sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
            displayRotate = (displayRotation + sensorOrientation + 270) % 360
            return displayRotate
        }

    private fun getSuitableSize(sizes: Array<Size>, width: Int, height: Int, pictureSize: Size): Size {
        var minDelta = Int.MAX_VALUE // 最小的差值，初始值应该设置大点保证之后的计算中会被重置
        var index = 0 // 最小的差值对应的索引坐标
        val aspectRatio = pictureSize.height * 1.0f / pictureSize.width
        Log.d(TAG,"getSuitableSize. aspectRatio: $aspectRatio")
        for (i in sizes.indices) {
            val size = sizes[i]
            // 先判断比例是否相等
            if (size.width * aspectRatio == size.height.toFloat()) {
                val delta = abs(width - size.width)
                if (delta == 0) {
                    return size
                }
                if (minDelta > delta) {
                    minDelta = delta
                    index = i
                }
            }
        }
        return sizes[index]
    }

    fun handleZoom(isZoomIn: Boolean) {
        if (cameraDevice == null) {
            return
        }
        var maxZoom = (cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)?.toInt()?.times(10))
        maxZoom = maxZoom ?: 100 // nullable set 100
        val rect = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        if (isZoomIn && zoom < maxZoom) {
            zoom++
        } else if (zoom > 1) {
            zoom--
        }
        val minW = rect!!.width() / maxZoom
        val minH = rect.height() / maxZoom
        val difW = rect.width() - minW
        val difH = rect.height() - minH
        var cropW = difW * zoom / 100
        var cropH = difH * zoom / 100
        cropW -= cropW and 3
        cropH -= cropH and 3
        val zoomRect = Rect(cropW, cropH, rect.width() - cropW, rect.height() - cropH)
        previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
        previewRequest = previewRequestBuilder.build()
        startPreview() // 需要重新 start preview 才能生效
    }

    fun focusOnPoint(x: Double, y: Double, width: Int, height: Int) {
        var x = x
        var y = y
        if (cameraDevice == null) {
            return
        }
        // 1. 先取相对于view上面的坐标
        var previewWidth = previewSize.width
        var previewHeight = previewSize.height
        if (displayRotate == 90 || displayRotate == 270) {
            previewWidth = previewSize.height
            previewHeight = previewSize.width
        }
        // 2. 计算摄像头取出的图像相对于view放大了多少，以及有多少偏移
        val tmp: Double
        var imgScale: Double
        var verticalOffset = 0.0
        var horizontalOffset = 0.0
        if (previewHeight * width > previewWidth * height) {
            imgScale = width * 1.0 / previewWidth
            verticalOffset = (previewHeight - height / imgScale) / 2
        } else {
            imgScale = height * 1.0 / previewHeight
            horizontalOffset = (previewWidth - width / imgScale) / 2
        }
        // 3. 将点击的坐标转换为图像上的坐标
        x = x / imgScale + horizontalOffset
        y = y / imgScale + verticalOffset
        if (90 == displayRotate) {
            tmp = x
            x = y
            y = previewSize.height - tmp
        } else if (270 == displayRotate) {
            tmp = x
            x = previewSize.width - y
            y = tmp
        }
        // 4. 计算取到的图像相对于裁剪区域的缩放系数，以及位移
        var cropRegion =
            previewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION)
        if (cropRegion == null) {
            Log.w(TAG, "can't get crop region")
            cropRegion = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        }
        val cropWidth = cropRegion!!.width()
        val cropHeight = cropRegion.height()
        if (previewSize.height * cropWidth > previewSize.width * cropHeight) {
            imgScale = cropHeight * 1.0 / previewSize.height
            verticalOffset = 0.0
            horizontalOffset = (cropWidth - imgScale * previewSize.width) / 2
        } else {
            imgScale = cropWidth * 1.0 / previewSize.width
            horizontalOffset = 0.0
            verticalOffset = (cropHeight - imgScale * previewSize.height) / 2
        }
        // 5. 将点击区域相对于图像的坐标，转化为相对于成像区域的坐标
        x = x * imgScale + horizontalOffset + cropRegion.left
        y = y * imgScale + verticalOffset + cropRegion.top
        val tapAreaRatio = 0.1
        val rect = Rect()
        rect.left = clamp((x - tapAreaRatio / 2 * cropRegion.width()).toInt(), 0, cropRegion.width())
        rect.right = clamp((x + tapAreaRatio / 2 * cropRegion.width()).toInt(), 0, cropRegion.width())
        rect.top = clamp((y - tapAreaRatio / 2 * cropRegion.height()).toInt(), 0, cropRegion.height())
        rect.bottom = clamp((y + tapAreaRatio / 2 * cropRegion.height()).toInt(), 0, cropRegion.height())
        // 6. 设置 AF、AE 的测光区域，即上述得到的 rect
        previewRequestBuilder.set(
            CaptureRequest.CONTROL_AF_REGIONS,
            arrayOf(MeteringRectangle(rect, 1000))
        )
        previewRequestBuilder.set(
            CaptureRequest.CONTROL_AE_REGIONS,
            arrayOf(MeteringRectangle(rect, 1000))
        )
        previewRequestBuilder.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_AUTO
        )
        previewRequestBuilder.set(
            CaptureRequest.CONTROL_AF_TRIGGER,
            CameraMetadata.CONTROL_AF_TRIGGER_START
        )
        previewRequestBuilder.set(
            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
            CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START
        )
        try {
            // 7. 发送上述设置的对焦请求，并监听回调
            captureSession!!.capture(
                previewRequestBuilder.build(),
                mAfCaptureCallback,
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val mAfCaptureCallback: CaptureCallback = object : CaptureCallback() {
        private fun process(result: CaptureResult) {
            val state = result.get(CaptureResult.CONTROL_AF_STATE) ?: return
            if (state == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || state == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                previewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
                )
                previewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                previewRequestBuilder.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.FLASH_MODE_OFF
                )
                startPreview()
            }
        }

        override fun onCaptureProgressed(session: CameraCaptureSession, request: CaptureRequest, partialResult: CaptureResult) {
            process(partialResult)
        }

        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            process(result)
        }
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null || backgroundHandler == null) {
            backgroundThread = HandlerThread("CameraBackground")
            backgroundThread!!.start()
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun clamp(x: Int, min: Int, max: Int): Int {
        if (x > max) return max
        return if (x < min) min else x
    }

    /**
     * Compares two `Size`s based on their areas.
     */
    internal class CompareSizesByArea : Comparator<Size> {
        override fun compare(lhs: Size, rhs: Size): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
        }
    }

    companion object {
        private const val TAG = "Camera2Proxy"
    }

    init {
        cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        orientationEventListener = object : OrientationEventListener(activity) {
            override fun onOrientationChanged(orientation: Int) {
                deviceOrientation = orientation
            }
        }
    }
}