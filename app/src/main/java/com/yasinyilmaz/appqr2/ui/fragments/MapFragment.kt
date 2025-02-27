package com.yasinyilmaz.appqr2.ui.fragments

import android.app.AlertDialog
import android.content.res.Configuration
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.glance.visibility
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import com.yasinyilmaz.appqr2.BuildConfig
import com.yasinyilmaz.appqr2.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var geoApiContext: GeoApiContext
    private lateinit var editTextOrigin: MaterialAutoCompleteTextView
    private lateinit var editTextDestination: MaterialAutoCompleteTextView
    private lateinit var buttonCalculateRoute: MaterialButton
    private lateinit var selectOriginLocationButton: MaterialButton
    private lateinit var selectDestinationLocationButton: MaterialButton
    private lateinit var selectLocationButton: FloatingActionButton
    private lateinit var centerMarker: ImageView

    private var currentMarker: Marker? = null
    private var originLocationSelectionMode = false
    private var destinationLocationSelectionMode = false
    private var selectedOriginLocation: LatLng? = null
    private var selectedDestinationLocation: LatLng? = null

    // Normal ArrayAdapter kullanıyoruz
    private val adapter by lazy {
        ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        initializeDependencies(view)
        setupClickListeners()
        return view
    }

    private fun initializeDependencies(view: View) {
        Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY)
        placesClient = Places.createClient(requireContext())
        geoApiContext = GeoApiContext.Builder().apiKey(BuildConfig.MAPS_API_KEY).build()

        editTextOrigin = view.findViewById(R.id.editTextOrigin)
        editTextDestination = view.findViewById(R.id.editTextDestination)
        buttonCalculateRoute = view.findViewById(R.id.buttonCalculateRoute)
        selectOriginLocationButton = view.findViewById(R.id.selectOriginLocationButton)
        selectDestinationLocationButton = view.findViewById(R.id.selectDestinationLocationButton)
        selectLocationButton = view.findViewById(R.id.selectLocationButton)
        centerMarker = view.findViewById(R.id.centerMarker)

        (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
    }

    private fun setupClickListeners() {
        buttonCalculateRoute.setOnClickListener { calculateRoute() }
        selectOriginLocationButton.setOnClickListener { updateLocationSelectionUI(true, true) }
        selectDestinationLocationButton.setOnClickListener { updateLocationSelectionUI(true, false) }
        selectLocationButton.setOnClickListener { onLocationSelected() }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap.apply {
            moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(39.9334, 32.8597), 10f))
        }
        setupAutoComplete(editTextOrigin)
        setupAutoComplete(editTextDestination)
    }

    private fun setupAutoComplete(autoCompleteTextView: MaterialAutoCompleteTextView) {
        val token = AutocompleteSessionToken.newInstance()
        autoCompleteTextView.setAdapter(adapter)

        // Karanlık tema kontrolü ve popupBackground ayarı
        val popupBackground = if (isDarkTheme()) {
            ContextCompat.getDrawable(requireContext(), R.color.black)
        } else {
            ContextCompat.getDrawable(requireContext(), R.color.white)
        }
        autoCompleteTextView.setDropDownBackgroundDrawable(popupBackground)

        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.takeIf { it.isNotEmpty() }?.let { fetchAutocompletePredictions(it, token) }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        autoCompleteTextView.setOnFocusChangeListener { _, hasFocus ->
            val isOrigin = autoCompleteTextView == editTextOrigin
            selectOriginLocationButton.visibility = if (hasFocus && isOrigin) View.VISIBLE else View.GONE
            selectDestinationLocationButton.visibility = if (hasFocus && !isOrigin) View.VISIBLE else View.GONE
        }
    }

    private fun fetchAutocompletePredictions(searchText: String, token: AutocompleteSessionToken) {
        placesClient.findAutocompletePredictions(
            FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(searchText)
                .build()
        ).addOnSuccessListener { response ->
            val predictions = response.autocompletePredictions.map { it.getFullText(null).toString() }
            requireActivity().runOnUiThread {
                adapter.apply {
                    clear()
                    addAll(predictions)
                    notifyDataSetChanged()
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("MapFragment", "Error fetching autocomplete predictions: ${exception.message}")
        }
    }

    private fun calculateRoute() {
        val origin = editTextOrigin.text.toString()
        val destination = editTextDestination.text.toString()

        if (origin.isEmpty() || destination.isEmpty()) {
            showError("Lütfen başlangıç ve bitiş noktalarını girin.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val drivingDirectionsResult = getDirections(origin, destination, TravelMode.DRIVING)
                val walkingDirectionsResult = getDirections(origin, destination, TravelMode.WALKING)

                withContext(Dispatchers.Main) {
                    if (drivingDirectionsResult.routes.isNotEmpty() && walkingDirectionsResult.routes.isNotEmpty()) {
                        showRoute(drivingDirectionsResult)
                        showTravelTimes(drivingDirectionsResult, walkingDirectionsResult)
                    } else {
                        showError("Yol bulunamadı")
                    }
                }
            } catch (e: Exception) {
                showError("Rota hesaplanırken bir hata oluştu")
                Log.e("MapFragment", "Error calculating route", e)
            }
        }
    }

    private suspend fun getDirections(origin: String, destination: String, mode: TravelMode): DirectionsResult {
        return DirectionsApi.newRequest(geoApiContext)
            .origin(origin)
            .destination(destination)
            .mode(mode)
            .await()
    }

    private fun showTravelTimes(drivingResult: DirectionsResult, walkingResult: DirectionsResult) {
        AlertDialog.Builder(requireContext())
            .setTitle("Yol Süreleri")
            .setMessage(
                "Araba ile: ${drivingResult.routes[0].legs[0].duration.humanReadable}\nYürüyerek: ${walkingResult.routes[0].legs[0].duration.humanReadable}"
            )
            .setPositiveButton("Tamam") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showRoute(directionsResult: DirectionsResult) {
        mMap.clear()
        currentMarker = null
        val route = directionsResult.routes[0]
        val path = route.overviewPolyline.decodePath()
        val startLocation = LatLng(path.first().lat, path.first().lng)
        val endLocation = LatLng(path.last().lat, path.last().lng)

        val routeColor = if (isDarkTheme()) {
            requireContext().getColor(R.color.colorRoute)
        } else {
            requireContext().getColor(R.color.colorRoute)
        }

        mMap.addPolyline(PolylineOptions().apply {
            path.forEach { add(LatLng(it.lat, it.lng)) }
            color(routeColor)
            width(10f)
        })
        mMap.addMarker(MarkerOptions().position(startLocation).title("Başlangıç"))
        mMap.addMarker(MarkerOptions().position(endLocation).title("Bitiş"))

        val bounds = LatLngBounds.Builder().apply {
            path.forEach { include(LatLng(it.lat, it.lng)) }
        }.build()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    private fun updateLocationSelectionUI(isSelecting: Boolean, isOrigin: Boolean) {
        originLocationSelectionMode = isSelecting && isOrigin
        destinationLocationSelectionMode = isSelecting && !isOrigin

        selectOriginLocationButton.visibility = if (isSelecting && isOrigin) View.VISIBLE else View.GONE
        selectDestinationLocationButton.visibility = if (isSelecting && !isOrigin) View.VISIBLE else View.GONE
        selectLocationButton.visibility = if (isSelecting) View.VISIBLE else View.GONE
        centerMarker.visibility = if (isSelecting) View.VISIBLE else View.GONE
        editTextOrigin.isEnabled = !isSelecting
        editTextDestination.isEnabled = !isSelecting
        buttonCalculateRoute.isEnabled = !isSelecting
    }

    private suspend fun getAddressFromLocation(latLng: LatLng): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        continuation.resume(addresses[0].getAddressLine(0))
                    } else {
                        continuation.resume("")
                    }
                }
            } else {
                try {
                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    continuation.resume(addresses?.firstOrNull()?.getAddressLine(0) ?: "")
                } catch (e: Exception) {
                    Log.e("MapFragment", "Error getting address", e)
                    continuation.resume("")
                }
            }
        }
    }

    private fun onLocationSelected() {
        CoroutineScope(Dispatchers.Main).launch {
            val selectedLatLng = when {
                originLocationSelectionMode -> {
                    selectedOriginLocation = mMap.projection.visibleRegion.latLngBounds.center
                    selectedOriginLocation
                }
                destinationLocationSelectionMode -> {
                    selectedDestinationLocation = mMap.projection.visibleRegion.latLngBounds.center
                    selectedDestinationLocation
                }
                else -> null
            }

            selectedLatLng?.let {
                val address = getAddressFromLocation(it)
                if (originLocationSelectionMode) {
                    editTextOrigin.setText(address)
                } else if (destinationLocationSelectionMode) {
                    editTextDestination.setText(address)
                }
            }
            updateLocationSelectionUI(false, false)
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun isDarkTheme(): Boolean {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }
}