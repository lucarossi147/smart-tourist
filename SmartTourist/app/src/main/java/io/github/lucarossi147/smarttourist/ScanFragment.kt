package io.github.lucarossi147.smarttourist

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.google.android.gms.maps.model.LatLng
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.lucarossi147.smarttourist.data.model.Category
import io.github.lucarossi147.smarttourist.databinding.FragmentScanBinding
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.properties.Delegates

class ScanFragment : Fragment() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewBinding: FragmentScanBinding
    private lateinit var myContext: Context

    private class QrObservable (val view: View?,) {
        val poi = POI("1","monsampietro morico", LatLng(45.0,45.0),
            pictures = listOf(
                "https://placedog.net/15",
                "https://placedog.net/13",
                "https://placedog.net/14",
                "https://placedog.net/16",
                "https://placedog.net/17",
                "https://placedog.net/18",
                ),
            category = Category.CULTURE,
            snippet = "I live here",
            visited = true)

        var qr:String by Delegates.observable(""){
            _, _, newValue ->
            // TODO: deserialize poi from the response and pass it to the fragment
            val bundle = bundleOf("poi" to poi)
            view?.findNavController()?.navigate(R.id.poiFragment, bundle )
        }
    }

    // TODO: Consider using glider instead of picasso
    // TODO: BUG WITH PERMISSION FIRST TIME USER USES CAMERA
    private class QrScanner(val qrObservable: QrObservable ) : ImageAnalysis.Analyzer {
        val client = HttpClient(Android)
        override fun analyze(imageProxy: ImageProxy) {
            @androidx.camera.core.ExperimentalGetImage
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                // Pass image to an ML Kit Vision API
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE)
                    .build()

                val scanner = BarcodeScanning.getClient(options)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            // See API reference for complete list of supported types
                            when (barcode.valueType) {
                                Barcode.TYPE_URL -> {
                                    Log.d("Camera", "barcode found")
//                                    val title = barcode.url!!.title
                                    val url = barcode.url!!.url?:"none"
                                    runBlocking {
                                        Log.d("Camera", "inside runBlocking")
                                        val res = client.get(url)
                                        if (res.status.isSuccess()){
                                            qrObservable.qr = url
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Task failed with an exception
                    }
            }
            imageProxy.close()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            myContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        myContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding = FragmentScanBinding.bind(view)
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                context as Activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(myContext)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QrScanner(QrObservable(view)))
                }
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer)
            } catch(exc: Exception) {

            }
        }, ContextCompat.getMainExecutor(myContext))
    }

}