package io.github.lucarossi147.smarttourist

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
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
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import io.github.lucarossi147.smarttourist.data.model.Category
import io.github.lucarossi147.smarttourist.data.model.City
import io.github.lucarossi147.smarttourist.data.model.LoggedInUser
import io.github.lucarossi147.smarttourist.data.model.POI
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.properties.Delegates
import io.github.lucarossi147.smarttourist.Constants.ARG_USER
import io.github.lucarossi147.smarttourist.Constants.POI_VISITED_BY_USER_URL
import io.github.lucarossi147.smarttourist.Constants.getPois

private const val REQUESTING_LOCATION_UPDATES_KEY: String = "prove"
private const val REQUEST_CHECK_SETTINGS = 0x1

private const val CESENA_LAT = 44.133331
private const val CESENA_LNG = 12.233333
private const val DEFAULT_ZOOM = 14.0F

class MapsFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
    private lateinit var mapHandler: MapHandler

    private val locationCallback = object:LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            for (l in p0.locations) {
                mapHandler.updateUserPosition(l.latitude, l.longitude)
            }
        }
    }

    private class MapUI(val googleMap: GoogleMap) {
        var userMarker: Marker? = null
        var poiMarkers:List<Marker?> = emptyList()
        var cityMarkers: List<Marker?> = emptyList()

        fun drawUser(lat: Double, lng: Double) {
            val pos = LatLng(lat, lng)
            if (userMarker == null) {
                userMarker = googleMap.addMarker(MarkerOptions()
                    .position(pos)
                    .title("You are here!"))
                //get POIs next to me
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, DEFAULT_ZOOM))
            } else {
                userMarker?.position = pos
            }
        }

        fun drawPois(pois: List<POI>){
            poiMarkers = pois.map {
                googleMap.addMarker(MarkerOptions()
                    .position(LatLng(it.lat,it.lng))
                    .title(it.name)
                    .icon(when (it.visited) {
                        true -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                        false -> when (it.category) {
                            Category.FUN -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
                            Category.CULTURE -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                            Category.NATURE -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                        }
                    }))
            }
        }

        fun drawCities(cities: List<City>){
            cityMarkers = cities.map {
                googleMap.addMarker(MarkerOptions()
                    .position(LatLng(it.lat,it.lng))
                    .title(it.name)
                    .icon(
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)))
            }
        }
    }

    private class MapHandler (googleMap: GoogleMap, user:LoggedInUser){
        var mapUI: MapUI = MapUI(googleMap)
        var lastUpdate = Long.MAX_VALUE
        init{
            fetchPOIs()
        }

        var user: LoggedInUser by Delegates.observable(user) {
            _,_, updatedUser ->
            CoroutineScope(Dispatchers.Main).launch {
                mapUI.drawUser(updatedUser.lat, updatedUser.lng)
                if (System.currentTimeMillis() - lastUpdate > Constants.MINIMUM_REFRESH_TIME){
                    fetchPOIs(updatedUser.lat, updatedUser.lng)
                }
            }
        }
        var pois:List<POI> by Delegates.observable(emptyList()) {
            _,_, newPois ->
            CoroutineScope(Dispatchers.Main).launch {
                mapUI.drawPois(newPois)
            }
        }
        var cities: List<City> by Delegates.observable(emptyList()) {
                _,_, newCities ->
            CoroutineScope(Dispatchers.Main).launch {
                mapUI.drawCities(newCities)
            }
        }
        fun updateUserPosition(lat:Double, lng: Double) {
            user = user.copy(lat = lat, lng = lng)
        }
        fun fetchPOIs(lat:Double = CESENA_LAT, lng: Double = CESENA_LNG, radius: Int = 10 ){
            CoroutineScope(Dispatchers.IO).launch {
                val res = HttpClient(Android)
                    .get(getPois(lat,lng,radius))
                if (res.status.isSuccess()){
                    pois = Gson()
                        .fromJson(res.bodyAsText(), Array<POI>::class.java)
                        .toList()
                        .map { it.copy(visited = it.id in user.visitedPois) }
                    cities = pois.map { it.city }
                }
            }
        }
    }

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
                    requestPermission()
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

    private val callback = OnMapReadyCallback { googleMap ->

        val user: LoggedInUser = arguments?.getParcelable(ARG_USER) ?: return@OnMapReadyCallback
        CoroutineScope(Dispatchers.IO).launch {
            val res = HttpClient(Android).get(POI_VISITED_BY_USER_URL){
                bearerAuth(user.token)
            }
            if(res.status.isSuccess()){
                user.visitedPois = Gson().fromJson(res.bodyAsText(), Array<String>::class.java).toSet()
            }
            mapHandler = MapHandler(googleMap, user)
        }
        val activity: Activity = activity?: return@OnMapReadyCallback
        val context: Context = context?: return@OnMapReadyCallback
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermission()
        }
        val client: SettingsClient = LocationServices.getSettingsClient(activity)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
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
            val bundle = bundleOf(ARG_USER to mapHandler.user)
            view.findNavController().navigate(R.id.scanFragment, bundle)
        }
    }
}

