package io.github.lucarossi147.smarttourist

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.SettingsClient

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import io.github.lucarossi147.smarttourist.data.model.Category
import io.github.lucarossi147.smarttourist.data.model.City
import io.github.lucarossi147.smarttourist.data.model.LoggedInUser
import io.github.lucarossi147.smarttourist.data.model.POI

private const val REQUESTING_LOCATION_UPDATES_KEY: String = "prove"
private const val REQUEST_CHECK_SETTINGS = 0x1

private const val ARG_USER = "user"
class MapsFragment : Fragment() {

    private var user: LoggedInUser? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mMap: GoogleMap? = null
    private var myMarker: Marker? = null
    private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
    private val city = City("idNewYork","New York", 40.730610, -73.935242)
    private var POIs:Set<POI> = setOf(
        POI(id = "1", name = "Central Park", lat = 40.771133, lng =-73.974187, city = city, category = Category.NATURE, visited = true),
        POI(id = "3", name = "Empire State Building", lat = 40.748817, lng =-73.985428, city = city, category =  Category.FUN),
        POI(id = "2", name = "Broadway", lat =40.790886, lng = -73.974709, city = city, category = Category.CULTURE)
    )
    private var markers: Set<Marker?> = emptySet()

    private lateinit var locationCallback: LocationCallback
    private val locationRequest = LocationRequest.create().apply {
        interval = 3000
        fastestInterval = 1500
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private var requestingLocationUpdates = true
    private val builder = LocationSettingsRequest.Builder()
        .addLocationRequest(locationRequest)

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            user = it.getParcelable(ARG_USER)
        }
        val activity:Activity = activity?:return
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)

        locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
//                    Log.i("PERMISSION", "FINE LOCATION")
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
//                    Log.i("PERMISSION", "COARSE LOCATION")
                }
                else -> {
//                    Log.i("PERMISSION", "NO PERMISSION GRANTED")
                }
            }
        }
        requestPermission()
        locationCallback = object :LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                for (l in p0.locations) {
                    val pos = LatLng(l.latitude,l.longitude)
                    //update UI with location data
                    if (myMarker == null) {
                        myMarker = mMap?.addMarker(MarkerOptions()
                            .position(pos)
                            .title("You are here!"))
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 14.0F))
                    } else {
                        myMarker?.position = LatLng(l.latitude,l.longitude)
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) startLocationUpdates()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates)
        super.onSaveInstanceState(outState)
    }

    private fun requestPermission() {
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun updateValuesFromBundle( savedInstanceState: Bundle?) {
        savedInstanceState ?: return

        // Update the value of requestingLocationUpdates from the Bundle.
        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            requestingLocationUpdates = savedInstanceState.getBoolean(
                REQUESTING_LOCATION_UPDATES_KEY)
        }

        // ...
        // Update UI to match restored state
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }


    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap
        val activity: Activity = activity?: return@OnMapReadyCallback
        val context: Context = context?: return@OnMapReadyCallback
        mMap = googleMap
        markers = POIs
            .map {
            mMap?.addMarker(MarkerOptions()
                .position(LatLng(it.lat,it.lng))
                .title(it.name)
                .icon(
                    when (it.visited) {
                        true -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                        false -> when (it.category) {
                            Category.FUN -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
                            Category.CULTURE -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                            Category.NATURE -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                        }
                    }))
        }.toSet()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermission()
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                // Got last known location. In some rare situations this can be null.
                val myPos = LatLng(location?.latitude?:40.730610, location?.longitude?: -73.935242)
                if (location!= null) {
                    myMarker = mMap?.addMarker(MarkerOptions().position(myPos).title("You are here!"))
                }
                mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 14.0F))

                val client: SettingsClient = LocationServices.getSettingsClient(activity)
                val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

                task.addOnSuccessListener { locationSettingsResponse ->
                    // All location settings are satisfied. The client can initialize
                    // location requests here.
                    // ...
                }

                task.addOnFailureListener { exception ->
                    if (exception is ResolvableApiException){
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            exception.startResolutionForResult(activity,
                                REQUEST_CHECK_SETTINGS)
                        } catch (sendEx: IntentSender.SendIntentException) {
                            // Ignore the error.
                        }
                    }
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
        val button: Button = view.findViewById(R.id.scan)
        button.setOnClickListener {
            view.findNavController().navigate(R.id.scanFragment)
        }
    }
}

