package com.kslimweb.testfacematching.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.media.ThumbnailUtils
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * Collection of image reading and manipulation utilities in the form of static functions.
 *
 * Created by Arindam Karmakar on 9/5/19.
 */
abstract class ImageUtils {

    companion object {

        /**
         * Helper function used to convert an EXIF orientation enum into a transformation matrix
         * that can be applied to a bitmap.
         *
         * @param orientation - One of the constants from [ExifInterface]
         */
        private fun decodeExifOrientation(orientation: Int): Matrix {
            val matrix = Matrix()
            // Apply transformation corresponding to declared EXIF orientation
            when (orientation) {
                ExifInterface.ORIENTATION_NORMAL -> Unit
                ExifInterface.ORIENTATION_UNDEFINED -> Unit
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1F, 1F)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1F, -1F)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postScale(-1F, 1F)
                    matrix.postRotate(270F)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postScale(-1F, 1F)
                    matrix.postRotate(90F)
                }

                // Error out if the EXIF orientation is invalid
                else -> throw IllegalArgumentException("Invalid orientation: $orientation")
            }

            // Return the resulting matrix
            return matrix
        }

        /**
         * Decode a bitmap from a file and apply the transformations described in its EXIF data
         *
         * @param file - The image file to be read using [BitmapFactory.decodeFile]
         */
        fun decodeBitmap(file: File): Bitmap {
            // First, decode EXIF data and retrieve transformation matrix
            val exif = ExifInterface(file.absolutePath)
            val transformation = decodeExifOrientation(
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90
                )
            )

            // Read bitmap using factory methods, and transform it using EXIF data
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            return Bitmap.createBitmap(
                BitmapFactory.decodeFile(file.absolutePath),
                0, 0, bitmap.width, bitmap.height, transformation, true
            )
        }

        fun decodeBitmap(image: ImageProxy): Bitmap {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.capacity()).also { buffer.get(it) }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        /**
         * This function cuts out a circular thumbnail from the provided bitmap. This is done by
         * first scaling the image down to a square with width of [diameter], and then marking all
         * pixels outside of the inner ic_shutter_video as transparent.
         *
         * @param bitmap - The [Bitmap] to be taken a thumbnail of
         * @param diameter - Size in pixels for the diameter of the resulting ic_shutter_video
         */
        fun cropCircularThumbnail(bitmap: Bitmap, diameter: Int = 128): Bitmap {
            // Extract a much smaller bitmap to serve as thumbnail
            val thumbnail = ThumbnailUtils.extractThumbnail(bitmap, diameter, diameter)

            // Create an additional bitmap of same size as thumbnail to carve a ic_shutter_video out of
            val circular = Bitmap.createBitmap(
                diameter, diameter, Bitmap.Config.ARGB_8888
            )

            // Paint will be used as a mask to cut out the ic_shutter_video
            val paint = Paint().apply {
                color = Color.BLACK
            }

            Canvas(circular).apply {
                drawARGB(0, 0, 0, 0)
                drawCircle(diameter / 2F, diameter / 2F, diameter / 2F - 8, paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                val rect = Rect(0, 0, diameter, diameter)
                drawBitmap(thumbnail, rect, rect, paint)
            }

            return circular
        }
    }
}
