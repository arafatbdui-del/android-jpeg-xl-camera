package com.example.jxlcamera.ui

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.jxlcamera.ui.theme.JxlCameraTheme
import com.awxkee.jxlcoder.JxlCoder
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (exc: Exception) {
            Log.e("CameraScreen", "Use case binding failed", exc)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Capture Button
        IconButton(
            onClick = {
                captureImage(context, imageCapture, cameraExecutor)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.Red) // Vibrant color
        ) {
            Text(
                "CAPTURE",
                color = Color.White
            )
        }
    }
}

private fun captureImage(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val debugFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "debug.txt")
                    debugFile.appendText("Captured image success\n")

                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    image.close() // Close proxy as soon as possible

                    if (bitmap == null) {
                        debugFile.appendText("BitmapFactory returned null\n")
                        return
                    }

                    val jxlBytes = JxlCoder.encode(bitmap)
                    debugFile.appendText("Encoding JXL success: ${jxlBytes.size} bytes\n")

                    saveJxlToFile(context, jxlBytes, debugFile)
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Failed to process image capture", e)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Photo capture failed: ${exception.message}", exception)
            }
        }
    )
}

private fun saveJxlToFile(context: Context, jxlBytes: ByteArray, debugFile: File) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        debugFile.appendText("SDK >= Q, using MediaStore\n")
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "JXL_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jxl")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/JxlCamera")
        }

        try {
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            debugFile.appendText("Insert (Files) returned URI: $uri\n")

            uri?.let {
                resolver.openOutputStream(it).use { outputStream ->
                    outputStream?.write(jxlBytes)
                }
                debugFile.appendText("Write to URI success\n")
                Log.i("CameraScreen", "Saved JXL file to Gallery via MediaStore: $uri")
            } ?: run {
                debugFile.appendText("Insert returned null URI\n")
                Log.e("CameraScreen", "Failed to insert into MediaStore.Images, URI was null")
            }
        } catch (e: Exception) {
            debugFile.appendText("Write to MediaStore failed: ${e.message}\n")
            Log.e("CameraScreen", "Failed to write JXL to Gallery", e)
        }
    } else {
        debugFile.appendText("SDK &lt; Q, using FileOutputStream\n")
        // Fallback to private storage as before
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "JXL_$timeStamp.jxl"
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(outputDir, fileName)

        try {
            FileOutputStream(file).use { out ->
                out.write(jxlBytes)
            }
            debugFile.appendText("Write to private storage success\n")
            Log.i("CameraScreen", "Saved JXL file to private storage: ${file.absolutePath}")
        } catch (e: Exception) {
            debugFile.appendText("Write to private storage failed: ${e.message}\n")
            Log.e("CameraScreen", "Failed to write JXL to private storage", e)
        }
    }
}
