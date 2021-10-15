package de.hbch.traewelling.ui.include.cardSearchStation

import StandardListItemAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM
import com.google.android.material.textfield.TextInputLayout.END_ICON_NONE
import de.hbch.traewelling.R
import de.hbch.traewelling.api.TraewellingApi
import de.hbch.traewelling.api.models.station.StationData
import de.hbch.traewelling.databinding.CardSearchStationBinding
import de.hbch.traewelling.ui.dashboard.DashboardFragmentDirections
import de.hbch.traewelling.ui.searchConnection.SearchConnectionFragment
import de.hbch.traewelling.ui.searchConnection.SearchConnectionFragmentDirections
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchStationCard(
        private val parent: Fragment,
        private val binding: CardSearchStationBinding,
        private val stationName: String
    ) : LocationListener {

    private lateinit var locationManager: LocationManager
    private val requestPermissionLauncher = parent.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted ->
                run {
                    if (isGranted) {
                        getCurrentLocation()
                    }
                }
    }

    val homelandStation = MutableLiveData("")

    init {
        binding.editTextSearchStation.setText(stationName)
        binding.inputLayoutStop.setEndIconOnClickListener {
            if (homelandStation.value != null && homelandStation.value!! != "")
                searchConnections(homelandStation.value!!)
        }
        binding.executePendingBindings()

        homelandStation.observe(parent.viewLifecycleOwner) { stationName ->
            binding.inputLayoutStop.endIconMode =
                when (stationName == null || stationName == "") {
                    true -> END_ICON_NONE
                    false -> END_ICON_CUSTOM
                }
        }
    }

    // Location listener
    override fun onLocationChanged(location: Location) {
        locationManager.removeUpdates(this)

        TraewellingApi.travelService.getNearbyStation(location.latitude, location.longitude)
            .enqueue(object: Callback<StationData> {
                override fun onResponse(call: Call<StationData>, response: Response<StationData>) {
                    if (response.isSuccessful) {
                        val station = response.body()?.data?.name
                        if (station != null) {
                            searchConnections(station)
                        }
                    } else {
                        Log.e("SearchStationCard", response.toString())
                    }
                }

                override fun onFailure(call: Call<StationData>, t: Throwable) {
                    Log.e("SearchStationCard", t.stackTraceToString())
                }
            })
    }

    fun findNearbyStations() {
        when (ContextCompat.checkSelfPermission(parent.requireContext(),
            android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                PackageManager.PERMISSION_GRANTED -> {
                    getCurrentLocation()
                }
                PackageManager.PERMISSION_DENIED -> {
                    if (parent.shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                        showToast("Please enable the location permission.")
                    } else {
                        requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
        }
    }

    fun searchConnections(station: String) {
        val action = when (parent is SearchConnectionFragment) {
            true -> SearchConnectionFragmentDirections.actionSearchConnectionFragmentSelf(station)
            false -> DashboardFragmentDirections.actionDashboardFragmentToSearchConnectionFragment(station)
        }
        parent.findNavController().navigate(action)
    }

    fun searchConnections() {
        val stationName = binding.editTextSearchStation.text.toString()
        searchConnections(stationName)
    }


    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        locationManager = parent.requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            LocationManager.FUSED_PROVIDER
        else
            LocationManager.GPS_PROVIDER

        locationManager.requestLocationUpdates(provider, 0L, 0F, this)
    }

    private fun showToast(text: String) {
        Toast.makeText(parent.requireContext(), text, Toast.LENGTH_SHORT).show()
    }
}