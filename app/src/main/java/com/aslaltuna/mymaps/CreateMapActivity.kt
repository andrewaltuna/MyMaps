package com.aslaltuna.mymaps

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentProviderClient
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.aslaltuna.mymaps.databinding.ActivityCreateMapBinding
import com.aslaltuna.mymaps.models.Place
import com.aslaltuna.mymaps.models.UserMap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.Marker
import com.google.android.material.snackbar.Snackbar

private const val TAG = "CreateMapActivity"

class CreateMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityCreateMapBinding

    // Collates user markers for UserMap creation
    private var markers: MutableList<Marker> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreateMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = intent.getStringExtra(EXTRA_MAP_TITLE)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mapFragment.view?.let {
            Snackbar.make(it, "Long press to add a marker.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Ok", null)
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_create_map, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Check that item selected is save button
        if (item.itemId == R.id.miSave) {
            if (markers.isEmpty()) {
                Toast.makeText(this, "There must be at least one marker on the map", Toast.LENGTH_LONG).show()
                return true
            }
            val places = markers.map { marker -> Place(marker.title,  marker.snippet, marker.position.latitude, marker.position.longitude) }
            val userMap = intent.getStringExtra(EXTRA_MAP_TITLE)?.let { UserMap(it, places) }
            val data = Intent()
            data.putExtra(EXTRA_USER_MAP, userMap)
            setResult(Activity.RESULT_OK, data)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setOnInfoWindowClickListener {
            markers.remove(it)
            it.remove()
        }

        mMap.setOnMapLongClickListener {
            Log.i(TAG, "$it")
            showAlertDialogue(it)
        }
        // Add a marker in Sydney and move the camera
        val philippines = LatLng(12.87, 121.77)

        val startLatLng = intent.getParcelableExtra<LatLng>(CURRENT_LOCATION)

        Log.i(TAG, "$startLatLng")

        val defaultZoom = startLatLng ?: LatLng(0.0,0.0)

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultZoom, 10f))
    }

    private fun showAlertDialogue(LatLng: LatLng) {

        val placeFormView = LayoutInflater.from(this).inflate(R.layout.dialog_create_place, null)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Create a marker")
            .setView(placeFormView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok", null)
            .show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val title = placeFormView.findViewById<EditText>(R.id.etPlaceTitle).text.toString()
            val desc = placeFormView.findViewById<EditText>(R.id.etPlaceDescription).text.toString()

            if (title.trim().isEmpty() || desc.trim().isEmpty()){
                Toast.makeText(this, "All fields must be filled", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            } else {
                val marker = mMap.addMarker(MarkerOptions().position(LatLng).title(title).snippet(desc))
                markers.add(marker)
                dialog.dismiss()
            }
        }
    }
}