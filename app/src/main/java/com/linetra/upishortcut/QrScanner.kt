package com.linetra.upishortcut

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume

sealed interface ScanResult {
    data class Found(val text: String) : ScanResult
    data object Cancelled : ScanResult
    data class Failed(val message: String) : ScanResult
}

object QrScanner {

    /** Opens Google's full-screen QR scanner (runs in Play services; needs no camera permission). */
    suspend fun scanCamera(context: Context): ScanResult = suspendCancellableCoroutine { cont ->
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { code ->
                val text = code.rawValue
                cont.resume(if (text.isNullOrEmpty()) ScanResult.Failed("That QR code is empty") else ScanResult.Found(text))
            }
            .addOnCanceledListener { cont.resume(ScanResult.Cancelled) }
            .addOnFailureListener { cont.resume(ScanResult.Failed("Scanner unavailable: ${it.message}")) }
    }

    /** Decodes a QR code from an image (gallery pick or shared screenshot), preferring a UPI one. */
    suspend fun decodeImage(context: Context, uri: Uri): ScanResult {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            return ScanResult.Failed("Couldn't open that image")
        }
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
        return suspendCancellableCoroutine { cont ->
            scanner.process(image)
                .addOnSuccessListener { codes ->
                    val texts = codes.mapNotNull { it.rawValue }
                    val text = texts.firstOrNull { UpiLink.find(it) != null } ?: texts.firstOrNull()
                    cont.resume(if (text == null) ScanResult.Failed("No QR code found in that image") else ScanResult.Found(text))
                }
                .addOnFailureListener { cont.resume(ScanResult.Failed("Couldn't read QR code: ${it.message}")) }
                .addOnCompleteListener { scanner.close() }
        }
    }
}
