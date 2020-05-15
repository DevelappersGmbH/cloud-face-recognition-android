package de.develappers.facerecognition.serviceAI

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import de.develappers.facerecognition.FaceApp
import de.develappers.facerecognition.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ImageHelper() {

    companion object {
        // The maximum side length of the image to detect, to keep the size of image less than 4MB.
// Resize the image if its side length is larger than the maximum.
        private val IMAGE_MAX_SIDE_LENGTH = 1280

        // Ratio to scale a detected face rectangle, the face rectangle scaled up looks more natural.
        private val FACE_RECT_SCALE_RATIO = 1.3

        // Decode image from imageUri, and resize according to the expectedMaxImageSideLength
// If expectedMaxImageSideLength is
//     (1) less than or equal to 0,
//     (2) more than the actual max size length of the bitmap
//     then return the original bitmap
// Else, return the scaled bitmap
        fun loadSizeLimitedBitmapFromUri(
            imageUri: Uri?,
            context: Context
        ): Bitmap? {
            return try { // Load the image into InputStream.
                //var imageInputStream = context.contentResolver.openInputStream(imageUri!!)

                val galleryFolder = FaceApp.galleryFolder
                var imageInputStream  = FileInputStream(File(galleryFolder, imageUri.toString().substringAfter("${context.getString(R.string.app_name)}/")))
                // For saving memory, only decode the image meta and get the side length.
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                val outPadding = Rect()
                BitmapFactory.decodeStream(imageInputStream, outPadding, options)
                // Calculate shrink rate when loading the image into memory.
                var maxSideLength = if (options.outWidth > options.outHeight) options.outWidth else options.outHeight
                options.inSampleSize = 1
                options.inSampleSize = calculateSampleSize(maxSideLength, IMAGE_MAX_SIDE_LENGTH)
                options.inJustDecodeBounds = false
                imageInputStream?.close()
                // Load the bitmap and resize it to the expected size length
                //imageInputStream = context.contentResolver.openInputStream(imageUri)
                imageInputStream  = FileInputStream(File(galleryFolder, imageUri.toString().substringAfter("${context.getString(R.string.app_name)}/")))
                var bitmap = BitmapFactory.decodeStream(imageInputStream, outPadding, options)
                maxSideLength = if (bitmap!!.width > bitmap.height) bitmap.width else bitmap.height
                val ratio = IMAGE_MAX_SIDE_LENGTH / maxSideLength.toDouble()
                if (ratio < 1) {
                    bitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * ratio).toInt(),
                        (bitmap.height * ratio).toInt(),
                        false
                    )
                }
                rotateBitmap(bitmap!!, getImageRotationAngle(imageUri!!, context.contentResolver))
            } catch (e: Exception) {
                Log.d("Exception", "exception")
                null
            }
        }

        // Return the number of times for the image to shrink when loading it into memory.
// The SampleSize can only be a final value based on powers of 2.
        private fun calculateSampleSize(maxSideLength: Int, expectedMaxImageSideLength: Int): Int {
            var maxSideLength = maxSideLength
            var inSampleSize = 1
            while (maxSideLength > 2 * expectedMaxImageSideLength) {
                maxSideLength /= 2
                inSampleSize *= 2
            }
            return inSampleSize
        }

        // Get the rotation angle of the image taken.
        @Throws(IOException::class)
        private fun getImageRotationAngle(
            imageUri: Uri, contentResolver: ContentResolver
        ): Int {
            var angle = 0
            val cursor = contentResolver.query(
                imageUri, arrayOf(
                    MediaStore.Images.ImageColumns.ORIENTATION
                ), null, null, null
            )
            if (cursor != null) {
                if (cursor.count == 1) {
                    cursor.moveToFirst()
                    angle = cursor.getInt(0)
                }
                cursor.close()
            } else {
                val exif = ExifInterface(imageUri.path)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_270 -> angle = 270
                    ExifInterface.ORIENTATION_ROTATE_180 -> angle = 180
                    ExifInterface.ORIENTATION_ROTATE_90 -> angle = 90
                    else -> {
                    }
                }
            }
            return angle
        }

        // Rotate the original bitmap according to the given orientation angle
        private fun rotateBitmap(
            bitmap: Bitmap,
            angle: Int
        ): Bitmap? { // If the rotate angle is 0, then return the original image, else return the rotated image
            return if (angle != 0) {
                val matrix = Matrix()
                matrix.postRotate(angle.toFloat())
                Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
            } else {
                bitmap
            }
        }

        //save the image from assets to local storage
        @Throws(IOException::class)
        fun saveVisitorPhotoLocally(context: Context, lastName: String, galleryFolder : File, databaseFolder: String): File {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            //val newFile = File.createTempFile("IMG_${lastName}_$timeStamp", ".jpg", galleryFolder)
            val newFile = File(galleryFolder,"IMG_${lastName}_$timeStamp.jpg")
            newFile.createNewFile()
            try {
                val imageFileName = "$databaseFolder/$lastName/$lastName.${Random().nextInt(20) + 1}.jpg"
                println(newFile.absolutePath)
                val inputStream = context.assets.open(imageFileName)
                val outputStream = FileOutputStream(newFile)
                try {
                    inputStream.copyTo(outputStream)
                } finally {
                    inputStream.close()
                    outputStream.close()
                }
            } catch (e: IOException) {
                throw IOException("Could not open file", e)
            }
            return newFile
        }
    }


}