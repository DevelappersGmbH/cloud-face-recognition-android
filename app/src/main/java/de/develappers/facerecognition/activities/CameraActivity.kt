package de.develappers.facerecognition.activities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.develappers.facerecognition.FaceApp
import de.develappers.facerecognition.REQUEST_CAMERA_PERMISSION
import de.develappers.facerecognition.REQUEST_STORAGE_PERMISSION
import de.develappers.facerecognition.utils.CompareSizesByArea
import de.develappers.facerecognition.utils.ImageSaver
import de.develappers.facerecognition.utils.signatureView.AutoFitTextureView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

open class CameraActivity: AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {


    protected lateinit var textureView: AutoFitTextureView
    protected val galleryFolder: File =
        FaceApp.galleryFolder
    private lateinit var file: File
    private lateinit var cameraId: String
    private var captureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private lateinit var previewSize: Size
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewRequest: CaptureRequest
    private var state =
        STATE_PREVIEW
    private var imageReader: ImageReader? = null
    private var flashSupported = false
    private var sensorOrientation = 0
    private val cameraOpenCloseLock =
        Semaphore(1) //A [Semaphore] to prevent the app from exiting before closing the camera.
    /**
     * This a callback object for the [ImageReader]. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        backgroundHandler?.post(ImageSaver(it.acquireLatestImage(), file))
    }

    protected val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit

    }


    protected lateinit var finalCaptureCallback : CameraCaptureSession.CaptureCallback

        /**
         * Lock the focus as the first step for a still image capture.
         */
        protected fun lockFocus() {
            try {
                // This is how to tell the camera to lock focus.
                previewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START
                )
                // Tell #captureCallback to wait for the lock.
                state =
                    STATE_WAITING_LOCK
                captureSession?.capture(
                    previewRequestBuilder.build(), captureCallback,
                    backgroundHandler
                )
            } catch (e: CameraAccessException) {
                Log.e(TAG, e.toString())
            }

        }

        protected fun captureStillPicture() {
            try {
                if (cameraDevice == null) return
                val rotation = windowManager.defaultDisplay.rotation
                var angle: Int = 0
                if (sensorOrientation == 270){
                    angle = 90
                } else if (sensorOrientation == 0) {
                    angle = 270
                }

                // This is the CaptureRequest.Builder that we use to take a picture.
                val captureBuilder = cameraDevice?.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )?.apply {
                    addTarget(imageReader?.surface!!)

                    // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
                    // We have to take that into account and rotate JPEG properly.
                    // For devices with orientation of 90, we return our mapping from ORIENTATIONS.
                    // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.

                    set(
                        CaptureRequest.JPEG_ORIENTATION,
                        (ORIENTATIONS.get(rotation) + sensorOrientation + angle) % 360
                    )

                    // Use the same AE and AF modes as the preview.
                    set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                }?.also { setAutoFlash(it) }

                val captureCallback = finalCaptureCallback

                captureSession?.apply {
                    stopRepeating()
                    abortCaptures()
                    capture(captureBuilder?.build()!!, captureCallback, null)
                }
            } catch (e: CameraAccessException) {
                Log.e(TAG, e.toString())
            }

        }

        /**
         * Unlock the focus. This method should be called when still image capture sequence is
         * finished.
         */
        protected fun unlockFocus() {
            try {
                // Reset the auto-focus trigger
                previewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
                )
                setAutoFlash(previewRequestBuilder)
                captureSession?.capture(
                    previewRequestBuilder.build(), captureCallback,
                    backgroundHandler
                )
                // After this, the camera will go back to the normal state of preview.
                state =
                    STATE_PREVIEW
                captureSession?.setRepeatingRequest(
                    previewRequest, captureCallback,
                    backgroundHandler
                )
            } catch (e: CameraAccessException) {
                Log.e(TAG, e.toString())
            }

        }


        protected fun setUpCameraOutputs(width: Int, height: Int) {
            val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                for (cameraId in manager.cameraIdList) {
                    val characteristics = manager.getCameraCharacteristics(cameraId)

                    // We use a front facing camera
                    val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (cameraDirection == null ||
                        cameraDirection != CameraCharacteristics.LENS_FACING_FRONT
                    ) {
                        continue
                    }

                    val map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                    ) ?: continue

                    // For still image captures, we use the largest available size.
                    val largest = Collections.max(
                        Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
                        CompareSizesByArea()
                    )
                    imageReader = ImageReader.newInstance(
                        largest.width, largest.height,
                        ImageFormat.JPEG, /*maxImages*/ 2
                    ).apply {
                        setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
                    }

                    // Find out if we need to swap dimension to get the preview size relative to sensor
                    // coordinate.
                    val displayRotation = windowManager.defaultDisplay.rotation

                    sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
                    val swappedDimensions = areDimensionsSwapped(displayRotation)

                    val displaySize = Point()
                    windowManager.defaultDisplay.getSize(displaySize)
                    val rotatedPreviewWidth = if (swappedDimensions) height else width
                    val rotatedPreviewHeight = if (swappedDimensions) width else height
                    var maxPreviewWidth = if (swappedDimensions) displaySize.y else displaySize.x
                    var maxPreviewHeight = if (swappedDimensions) displaySize.x else displaySize.y

                    if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth =
                        MAX_PREVIEW_WIDTH
                    if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight =
                        MAX_PREVIEW_HEIGHT

                    // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                    // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                    // garbage capture data.
                    previewSize =
                        chooseOptimalSize(
                            map.getOutputSizes(SurfaceTexture::class.java),
                            rotatedPreviewWidth, rotatedPreviewHeight,
                            maxPreviewWidth, maxPreviewHeight,
                            largest
                        )

                    // We fit the aspect ratio of TextureView to the size of preview we picked.
                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        textureView.setAspectRatio(previewSize.width, previewSize.height)
                    } else {
                        textureView.setAspectRatio(previewSize.height, previewSize.width)
                    }

                    // Check if the flash is supported.
                    flashSupported =
                        characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

                    this.cameraId = cameraId

                    // We've found a viable camera and finished setting up member variables,
                    // so we don't need to iterate through other available cameras.
                    return
                }
            } catch (e: CameraAccessException) {
                Log.e(TAG, e.toString())
            } catch (e: NullPointerException) {
            }

        }

        /**
         * Determines if the dimensions are swapped given the phone's current rotation.
         *
         * @param displayRotation The current rotation of the display
         *
         * @return true if the dimensions are swapped, false otherwise.
         */
        protected fun areDimensionsSwapped(displayRotation: Int): Boolean {
            var swappedDimensions = false
            when (displayRotation) {
                Surface.ROTATION_0, Surface.ROTATION_180 -> {
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        swappedDimensions = true
                    }
                }
                Surface.ROTATION_90, Surface.ROTATION_270 -> {
                    if (sensorOrientation == 0 || sensorOrientation == 180) {
                        swappedDimensions = true
                    }
                }
                else -> {
                    Log.e(TAG, "Display rotation is invalid: $displayRotation")
                }
            }
            return swappedDimensions
        }

        /**
         * Opens the camera specified by [Camera2BasicFragment.cameraId].
         */
        protected fun openCamera(width: Int, height: Int) {
            val permissionCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            val permissionStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission()
                return
            }
            if (permissionStorage != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermission()
                return
            }
            setUpCameraOutputs(width, height)
            configureTransform(width, height)
            val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                // Wait for camera to open - 2.5 seconds is sufficient
                if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw RuntimeException("Time out waiting to lock camera opening.")
                }
                manager.openCamera(cameraId, stateCallback, backgroundHandler)
            } catch (e: CameraAccessException) {
                Log.e(TAG, e.toString())
            } catch (e: InterruptedException) {
                throw RuntimeException("Interrupted while trying to lock camera opening.", e)
            }

        }

        /**
         * Closes the current [CameraDevice].
         */
        protected fun closeCamera() {
            try {
                cameraOpenCloseLock.acquire()
                captureSession?.close()
                captureSession = null
                cameraDevice?.close()
                cameraDevice = null
                imageReader?.close()
                imageReader = null
            } catch (e: InterruptedException) {
                throw RuntimeException("Interrupted while trying to lock camera closing.", e)
            } finally {
                cameraOpenCloseLock.release()
            }
        }

        /**
         * Starts a background thread and its [Handler].
         */
        protected fun startBackgroundThread() {
            backgroundThread = HandlerThread("CameraBackground").also { it.start() }
            backgroundHandler = Handler(backgroundThread?.looper)
        }

        /**
         * Stops the background thread and its [Handler].
         */
        protected fun stopBackgroundThread() {
            backgroundThread?.quitSafely()
            try {
                backgroundThread?.join()
                backgroundThread = null
                backgroundHandler = null
            } catch (e: InterruptedException) {
                Log.e(TAG, e.toString())
            }

        }

        /**
         * Creates a new [CameraCaptureSession] for camera preview.
         */
        protected fun createCameraPreviewSession() {
            try {
                val texture = textureView.surfaceTexture

                // We configure the size of default buffer to be the size of camera preview we want.
                texture.setDefaultBufferSize(previewSize.width, previewSize.height)

                // This is the output Surface we need to start preview.
                val surface = Surface(texture)

                // We set up a CaptureRequest.Builder with the output Surface.
                previewRequestBuilder = cameraDevice!!.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW
                )
                previewRequestBuilder.addTarget(surface)

                // Here, we create a CameraCaptureSession for camera preview.
                cameraDevice?.createCaptureSession(
                    Arrays.asList(surface, imageReader?.surface),
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            // The camera is already closed
                            if (cameraDevice == null) return

                            // When the session is ready, we start displaying the preview.
                            captureSession = cameraCaptureSession
                            try {
                                // Auto focus should be continuous for camera preview.
                                previewRequestBuilder.set(
                                    CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                )
                                // Flash is automatically enabled when necessary.
                                setAutoFlash(previewRequestBuilder)

                                // Finally, we start displaying the camera preview.
                                previewRequest = previewRequestBuilder.build()
                                captureSession?.setRepeatingRequest(
                                    previewRequest,
                                    captureCallback, backgroundHandler
                                )
                            } catch (e: CameraAccessException) {
                                Log.e(TAG, e.toString())
                            }

                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                        }
                    }, null
                )
            } catch (e: CameraAccessException) {
                Log.e(TAG, e.toString())
            }

        }

        /**
         * Configures the necessary [android.graphics.Matrix] transformation to `textureView`.
         * This method should be called after the camera preview size is determined in
         * setUpCameraOutputs and also the size of `textureView` is fixed.
         *
         * @param viewWidth  The width of `textureView`
         * @param viewHeight The height of `textureView`
         */
        protected fun configureTransform(viewWidth: Int, viewHeight: Int) {
            val rotation = windowManager.defaultDisplay.rotation
            val matrix = Matrix()
            val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
            val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
            val centerX = viewRect.centerX()
            val centerY = viewRect.centerY()

            if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
                val scale = Math.max(
                    viewHeight.toFloat() / previewSize.height,
                    viewWidth.toFloat() / previewSize.width
                )
                with(matrix) {
                    setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                    postScale(scale, scale, centerX, centerY)
                    postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
                }
            } else if (Surface.ROTATION_180 == rotation) {
                matrix.postRotate(180f, centerX, centerY)
            }
            textureView.setTransform(matrix)
        }


        protected fun setAutoFlash(requestBuilder: CaptureRequest.Builder) {
            if (flashSupported) {
                requestBuilder.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                )
            }
        }




        protected val stateCallback = object : CameraDevice.StateCallback() {

            override fun onOpened(cameraDevice: CameraDevice) {
                cameraOpenCloseLock.release()
                this@CameraActivity.cameraDevice = cameraDevice
                createCameraPreviewSession()
            }

            override fun onDisconnected(cameraDevice: CameraDevice) {
                cameraOpenCloseLock.release()
                cameraDevice.close()
                this@CameraActivity.cameraDevice = null
            }

            override fun onError(cameraDevice: CameraDevice, error: Int) {
                onDisconnected(cameraDevice)
                this@CameraActivity.finish()
            }

        }


        protected val captureCallback = object : CameraCaptureSession.CaptureCallback() {

            protected fun process(result: CaptureResult) {
                when (state) {
                    STATE_PREVIEW -> Unit // Do nothing when the camera preview is working normally.
                    STATE_WAITING_LOCK -> capturePicture(result)
                    STATE_WAITING_PRECAPTURE -> {
                        // CONTROL_AE_STATE can be null on some devices
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED
                        ) {
                            state =
                                STATE_WAITING_NON_PRECAPTURE
                        }
                    }
                    STATE_WAITING_NON_PRECAPTURE -> {
                        // CONTROL_AE_STATE can be null on some devices
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                            state =
                                STATE_PICTURE_TAKEN
                            captureStillPicture()
                        }
                    }
                }
            }


            override fun onCaptureProgressed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                partialResult: CaptureResult
            ) {
                process(partialResult)
            }

            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                process(result)
            }

        }

        protected fun capturePicture(result: CaptureResult) {
            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
            if (afState == null) {
                captureStillPicture()
            } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
                || CaptureResult.CONTROL_AF_STATE_INACTIVE == afState
            ) {
                // CONTROL_AE_STATE can be null on some devices
                val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    state =
                        STATE_PICTURE_TAKEN
                    captureStillPicture()
                } else {
                    runPrecaptureSequence()
                }
            }
        }

        /**
         * Run the precapture sequence for capturing a still image. This method should be called when
         * we get a response in [.captureCallback] from [.lockFocus].
         */
        protected fun runPrecaptureSequence() {
            try {
                // This is how to tell the camera to trigger.
                previewRequestBuilder.set(
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
                )
                // Tell #captureCallback to wait for the precapture sequence to be set.
                state =
                    STATE_WAITING_PRECAPTURE
                captureSession?.capture(
                    previewRequestBuilder.build(), captureCallback,
                    backgroundHandler
                )
            } catch (e: CameraAccessException) {
                Log.e(TAG, e.toString())
            }

        }

        companion object {

        /**
         * Conversion from screen rotation to JPEG orientation.
         */
        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        /**
         * Tag for the [Log].
         */
        private val TAG = "Camera2BasicFragment"

        /**
         * Camera state: Showing camera preview.
         */
        private val STATE_PREVIEW = 0

        /**
         * Camera state: Waiting for the focus to be locked.
         */
        private val STATE_WAITING_LOCK = 1

        /**
         * Camera state: Waiting for the exposure to be precapture state.
         */
        private val STATE_WAITING_PRECAPTURE = 2

        /**
         * Camera state: Waiting for the exposure state to be something other than precapture.
         */
        private val STATE_WAITING_NON_PRECAPTURE = 3

        /**
         * Camera state: Picture was taken.
         */
        private val STATE_PICTURE_TAKEN = 4


        /**
         * Max preview width that is guaranteed by Camera2 API
         */
        private val MAX_PREVIEW_WIDTH = 1920

        /**
         * Max preview height that is guaranteed by Camera2 API
         */
        private val MAX_PREVIEW_HEIGHT = 1080

        /**
         * Given `choices` of `Size`s supported by a camera, choose the smallest one that
         * is at least as large as the respective texture view size, and that is at most as large as
         * the respective max size, and whose aspect ratio matches with the specified value. If such
         * size doesn't exist, choose the largest one that is at most as large as the respective max
         * size, and whose aspect ratio matches with the specified value.
         *
         * @param choices           The list of sizes that the camera supports for the intended
         *                          output class
         * @param textureViewWidth  The width of the texture view relative to sensor coordinate
         * @param textureViewHeight The height of the texture view relative to sensor coordinate
         * @param maxWidth          The maximum width that can be chosen
         * @param maxHeight         The maximum height that can be chosen
         * @param aspectRatio       The aspect ratio
         * @return The optimal `Size`, or an arbitrary one if none were big enough
         */
        @JvmStatic
        private fun chooseOptimalSize(
            choices: Array<Size>,
            textureViewWidth: Int,
            textureViewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            aspectRatio: Size
        ): Size {

            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w
                ) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            if (bigEnough.size > 0) {
                return Collections.min(bigEnough, CompareSizesByArea())
            } else if (notBigEnough.size > 0) {
                return Collections.max(notBigEnough, CompareSizesByArea())
            } else {
                Log.e(TAG, "Couldn't find any suitable preview size")
                return choices[0]
            }
        }

    }


    protected fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    protected fun requestStoragePermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
        } else {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
        }
    }



    protected fun createImageFile(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "IMG_" + timeStamp
        file = File(galleryFolder,"$imageFileName.jpg")
        file.createNewFile()
        //file = File.createTempFile(imageFileName, ".jpg", galleryFolder)
        return file.path
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }
}