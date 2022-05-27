package io.github.lucarossi147.smarttourist

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.lucarossi147.smarttourist.Constants.ARG_USER
import io.github.lucarossi147.smarttourist.data.model.LoggedInUser
import io.github.lucarossi147.smarttourist.data.model.POI
import io.github.lucarossi147.smarttourist.databinding.FragmentScanBinding
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.properties.Delegates

class ScanFragment : Fragment() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewBinding: FragmentScanBinding
    private lateinit var myContext: Context

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                view?.findNavController()?.navigate(R.id.mapsFragment)
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }

    private class ProgressBarHandler(val progressBar: ProgressBar?) {
        var analyzing: Boolean by Delegates.observable(false) {
            _,_, newValue ->
                if (newValue) {
                    progressBar?.visibility = View.VISIBLE
                } else {
                    progressBar?.visibility = View.INVISIBLE
                }
        }
    }

    private class QrObservable (val view: View?, user:LoggedInUser? ) {
        var poiDeserializer :String? by Delegates.observable(null){
            _, _, newValue ->
            val poi = Gson().fromJson(newValue,POI::class.java)
            try {
                //could go wrong if poi isn't serialized correctly
                if (user!=null) {
                    val bundle = bundleOf(
                        "poi" to poi.copy(visited = poi.id in user.visitedPois),
                        ARG_USER to user,
                    )
                    view?.findNavController()?.navigate(R.id.poiFragment, bundle)
                }
            }catch (e:java.lang.Exception) {
                //don't do anything
            }
        }
    }

    // TODO: Consider using glider instead of picasso
    private class QrScanner(val qrObservable: QrObservable, val pbh: ProgressBarHandler ) : ImageAnalysis.Analyzer {
        val client = HttpClient(Android)

        override fun analyze(imageProxy: ImageProxy) {
            @androidx.camera.core.ExperimentalGetImage
            val mediaImage = imageProxy.image

            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                // Pass image to an ML Kit Vision API
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()

                val scanner = BarcodeScanning.getClient(options)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            // See API reference for complete list of supported types
                            when (barcode.valueType) {
                                Barcode.TYPE_URL -> {
//                                    val title = barcode.url!!.title
                                    val url = barcode.url?.url!!
                                    pbh.analyzing = true
                                    CoroutineScope(context = Dispatchers.IO).launch {
                                        val res = client.get(url)
                                        if (res.status.isSuccess()){
                                            //needs to run on main thread or Android throws a tantrum
                                            CoroutineScope(Dispatchers.Main).launch {
                                                qrObservable.poiDeserializer = res.body()
                                            }
                                        } else {
                                            if (res.status.value == 404) {
                                                // TODO: maybe make a toast to user 
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    pbh.analyzing = false
                                                }
                                            }
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

    private fun requestPermission() {
        val activity = activity?:return
        when {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                //permission granted
                startCamera()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA
            ) -> {
                //additional rationale
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                //not been asked yet
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding = FragmentScanBinding.bind(view)
        requestPermission()
        cameraExecutor = Executors.newSingleThreadExecutor()
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
                    it.setAnalyzer(cameraExecutor,
                        QrScanner(QrObservable(view, arguments?.getParcelable(ARG_USER)),
                            ProgressBarHandler(view?.findViewById(R.id.scanProgressBar))))
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