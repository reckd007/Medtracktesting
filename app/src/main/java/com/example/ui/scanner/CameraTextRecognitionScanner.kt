package com.example.ui.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

/**
 * Real-Time On-Device Text Recognition (OCR) Camera View using Google ML Kit.
 * Replaces legacy barcode scanning to extract medicine brand/generic name,
 * strength/dosage, and form factor directly from labels, strips, and bottles.
 */
@Composable
fun CameraTextRecognitionScannerView(
    onMedicineDetailsExtracted: (ExtractedMedicineDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var cameraFailed by remember { mutableStateOf(false) }
    var currentDetectedDetails by remember { mutableStateOf<ExtractedMedicineDetails?>(null) }
    var liveDetectedRawText by remember { mutableStateOf("") }
    var noTextWarningMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Camera Viewfinder Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A))
                .testTag("ocr_camera_scanner_container"),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission && !cameraFailed) {
                CameraPreviewWithTextRecognition(
                    onTextDetected = { rawText, details ->
                        liveDetectedRawText = rawText
                        currentDetectedDetails = details
                        if (noTextWarningMessage != null && details != null) {
                            noTextWarningMessage = null
                        }
                    },
                    onError = { cameraFailed = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Camera permission prompt or hardware-less fallback
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MedTealSoft,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = if (!hasCameraPermission) "Camera Permission Required" else "Camera Viewfinder Ready",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (!hasCameraPermission) "Grant camera permission to scan medicine labels" else "Use emulator quick samples below or point camera at a box",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    if (!hasCameraPermission) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Grant Permission", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Scanning Overlay: Viewfinder frame
            Box(
                modifier = Modifier
                    .size(width = 250.dp, height = 140.dp)
                    .border(2.dp, Color(0xFF14B8A6), RoundedCornerShape(14.dp))
            )

            // Scanning Laser Guide
            HorizontalDivider(
                color = Color(0xFF10B981),
                thickness = 2.dp,
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .shadow(4.dp, shape = RoundedCornerShape(1.dp))
            )

            // Live HUD: Shows recognized name & strength as camera streams
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xCC0F172A),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (currentDetectedDetails != null) Icons.Default.CheckCircle else Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = if (currentDetectedDetails != null) MedSuccess else Color(0xFF14B8A6),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = currentDetectedDetails?.let {
                            "${it.name} • ${it.dosage} • ${it.formFactor}"
                        } ?: if (liveDetectedRawText.isNotBlank()) "Analyzing text..." else "Align medicine label in frame",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        // Warning banner when no text is detected
        if (noTextWarningMessage != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFEF3C7),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFDE68A))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(16.dp)
                    )
                    noTextWarningMessage?.let { msg ->
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Live Recognition Card with "Capture / Auto-Fill" Primary Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Medicine Label Text Recognition",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTextPrimary
                    )
                }

                // Extracted Details Preview Fields
                val detected = currentDetectedDetails
                if (detected != null) {
                    val details = detected
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF86EFAC))
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Name: ${details.name}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF166534)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Dosage: ${details.dosage}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                                Text(
                                    text = "• Form: ${details.formFactor}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Point camera at any medicine label to extract Name, Strength & Form Factor.",
                        fontSize = 12.sp,
                        color = MedTextSecondary
                    )
                }

                // Capture / Auto-Fill Primary Action Button
                Button(
                    onClick = {
                        val details = currentDetectedDetails
                        if (details != null && details.name.isNotBlank()) {
                            onMedicineDetailsExtracted(details)
                        } else {
                            noTextWarningMessage = "Position medicine label clearly in front of the camera"
                            Toast.makeText(
                                context,
                                "Position medicine label clearly in front of the camera",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("ocr_capture_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary)
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentDetectedDetails != null) "Capture & Auto-Fill Medicine" else "Capture / Auto-Fill",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Quick OCR Medicine Packaging Samples (Instant testing on emulators without webcam!)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Medicine Label Test Samples:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MedTextPrimary
                )
                Text(
                    text = "Tap to simulate OCR frame",
                    fontSize = 11.sp,
                    color = MedTextSecondary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("Paracetamol 650mg Tablets IP\nBatch: B402\nExp: 10/2027", "Paracetamol 650mg", "Tablet"),
                    Triple("Dolo 650 Tab\nMicro Labs Ltd\n650 mg", "Dolo 650", "Tablet"),
                    Triple("Augmentin 625 Duo Tablets\nAmoxicillin 500mg + Clavulanic 125mg", "Augmentin 625", "Tablet"),
                    Triple("Benadryl Cough Syrup\nDiphenhydramine HCl\n100 ml Oral Liquid", "Benadryl 100ml Syrup", "Liquid"),
                    Triple("Insulin Glargine 100 IU/ml\nInjectable Solution 10ml Vial", "Insulin 100 IU Vial", "Injection"),
                    Triple("Voltaren 1% Gel\nDiclofenac Sodium Topical Ointment 50g", "Voltaren 1% Gel", "Topical")
                ).forEach { (rawText, label, category) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                        ),
                        modifier = Modifier
                            .clickable {
                                val parsed = MedicineTextParser.parse(rawText)
                                if (parsed != null) {
                                    currentDetectedDetails = parsed
                                    liveDetectedRawText = rawText
                                    onMedicineDetailsExtracted(parsed)
                                }
                            }
                            .testTag("test_ocr_sample_${label.replace(" ", "_")}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when (category) {
                                    "Liquid" -> Icons.Default.WaterDrop
                                    "Injection" -> Icons.Default.Vaccines
                                    "Topical" -> Icons.Default.Healing
                                    else -> Icons.Default.Medication
                                },
                                contentDescription = null,
                                tint = MedTealPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * CameraX ImageAnalysis stream connected to Google ML Kit Text Recognition client.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewWithTextRecognition(
    onTextDetected: (rawText: String, details: ExtractedMedicineDetails?) -> Unit,
    onError: (Exception) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            textRecognizer.close()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            textRecognizer.process(image)
                                .addOnSuccessListener { visionText ->
                                    val rawText = visionText.text
                                    if (rawText.isNotBlank()) {
                                        val details = MedicineTextParser.parse(rawText)
                                        ContextCompat.getMainExecutor(ctx).execute {
                                            onTextDetected(rawText, details)
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    // Non-fatal recognition pass failure, continue streaming
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = when {
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                        else -> null
                    }

                    if (cameraSelector == null) {
                        onError(IllegalStateException("No camera sensor detected on this emulator"))
                        return@addListener
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (t: Throwable) {
                    onError(Exception(t.message ?: "Camera initialization error", t))
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}
