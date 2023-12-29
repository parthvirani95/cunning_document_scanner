package com.websitebeaver.documentscanner.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * This class contains a helper function for opening the camera.
 *
 * @param activity current activity
 * @param onPhotoCaptureSuccess gets called with photo file path when photo is ready
 * @param onCancelPhoto gets called when user cancels out of camera
 * @constructor creates camera util
 */
class CameraUtil(
    private val activity: ComponentActivity,
    private val onPhotoCaptureSuccess: (photoFilePath: String) -> Unit,
    private val onCancelPhoto: () -> Unit
) {
    /**
     * @property photoFilePath the photo file path
     */
    private lateinit var photoFilePath: String
    private var file: File? = null
    /**
     * @property startForResult used to launch camera
     */

    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        var filePath: String? = null
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                it.moveToFirst()
                val columnIndex = it.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                filePath = it.getString(columnIndex)
            }
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            filePath = uri.path
        }
        return filePath
    }

    fun copyFile(sourceFile: File, destinationFile: File) {
        try {

            sourceFile.let { s-> s.copyTo(destinationFile,true) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private val startForResult = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                Log.d("Result", result.toString());

                val selectedImageUri = result.data?.data;
                if (selectedImageUri != null) {

                     var filePath : String? = getFilePathFromUri(activity,selectedImageUri);

                    Log.d("File Path",filePath.toString());

                    if(filePath!=null && file!=null){
                        val sourcePath = File(filePath);
                        copyFile(sourcePath, file!!)
                    }



                    Log.d("Result final", file.toString());
                    Log.d("Result final", file?.toURI().toString());


                    // Process the selected image URI here
                    // You can do whatever you want with the selected image URI
                    // For instance, pass it to a method or update UI
                  onPhotoCaptureSuccess(photoFilePath)
                }else{
                    // send back photo file path on capture success
                    onPhotoCaptureSuccess(photoFilePath)
                }
            }
            Activity.RESULT_CANCELED -> {
                // delete the photo since the user didn't finish taking the photo
                File(photoFilePath).delete()
                onCancelPhoto()
            }
        }
    }

    /**
     * open the camera by launching an image capture intent
     *
     * @param pageNumber the current document page number
     */
    @Throws(IOException::class)
    fun openCamera(pageNumber: Int) {
        // create intent to launch camera
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // create new file for photo
        val photoFile: File = FileUtil().createImageFile(activity, pageNumber)

        // store the photo file path, and send it back once the photo is saved
        photoFilePath = photoFile.absolutePath

        // photo gets saved to this file path
        val photoURI: Uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.DocumentScannerFileProvider",
            photoFile
        )
        Log.d("Result1",photoURI.toString());

        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

        // open camera
        startForResult.launch(takePictureIntent)
    }

    fun openGallery(pageNumber: Int) {

        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)

        val photoFile: File = FileUtil().createImageFile(activity, pageNumber)

        file = photoFile

        photoFilePath = photoFile.absolutePath

        val photoURI: Uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.DocumentScannerFileProvider",
            photoFile
        )

        galleryIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

        startForResult.launch(galleryIntent)
    }
}