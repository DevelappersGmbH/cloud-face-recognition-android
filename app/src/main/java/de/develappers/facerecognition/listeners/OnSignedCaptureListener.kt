package de.develappers.facerecognition.listeners

import android.graphics.Bitmap

interface OnSignedCaptureListener {
    fun onSignatureCaptured(bitmap: Bitmap, fileUri: String)
}