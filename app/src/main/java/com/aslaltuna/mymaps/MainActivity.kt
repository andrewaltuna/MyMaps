package com.aslaltuna.mymaps

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.aslaltuna.mymaps.models.Place
import com.aslaltuna.mymaps.models.UserMap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import com.google.android.gms.location.*

private const val TAG = "MainActivity"
const val EXTRA_USER_MAP = "EXTRA_USER_MAP"
const val EXTRA_MAP_TITLE = "EXTRA_MAP_TITLE"
const val FILENAME = "UserMaps.data"
const val CURRENT_LOCATION = "CURRENT_LOCATION"

class MainActivity : AppCompatActivity() {

    private lateinit var userMaps: MutableList<UserMap>
    private lateinit var mapAdapter: MapsAdapter
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var startLatLng: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userMaps = deserializeUserMaps(this).toMutableList()

        checkDatasetSize()

        // Set layout manager on recycler view
        rvMaps.layoutManager = LinearLayoutManager(this)
        // Set adapter on recycler view
        mapAdapter = MapsAdapter(this, userMaps, object: MapsAdapter.OnClickListener{
            override fun onItemClick(position: Int) {
                Log.i(TAG, "onItemClick $position")
                // Navigate to different screen once clicked
                val intent = Intent(this@MainActivity, DisplayMapActivity::class.java)
                intent.putExtra(EXTRA_USER_MAP, userMaps[position])
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        })

        rvMaps.adapter = mapAdapter

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val data = it.data
                val userMap = data?.getSerializableExtra(EXTRA_USER_MAP) as UserMap
                Log.i(TAG, "onActivityResult")
                userMaps.add(userMap)
                mapAdapter.notifyItemInserted(userMaps.size - 1)
                serializeUserMaps(this, userMaps)
            }
        }

        fabCreateMap.setOnClickListener {
            Log.i(TAG, "FAB Clicked")
            showAlertDialogue()
        }

        // Get current location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()
    }

    fun checkDatasetSize() {
        if (userMaps.size == 0) {
            tvEmptyDataset.text = "No maps to display. Start by pressing + to add a map!"
        } else {
            tvEmptyDataset.text = ""
        }
    }

    private fun serializeUserMaps(context: Context, userMaps: List<UserMap>) {
        Log.i(TAG, "serializeUserMaps")
        ObjectOutputStream(FileOutputStream(getDataFile(context))).use { it.writeObject(userMaps) }
    }

    private fun deserializeUserMaps(context: Context): List<UserMap> {
        Log.i(TAG, "deserializeUserMaps")
        val dataFile = getDataFile(context)

        if (!dataFile.exists()) {
            Log.i(TAG, "File initialization!")
            return emptyList()
        }

        ObjectInputStream(FileInputStream(dataFile)).use { return it.readObject() as List<UserMap> }
    }

    private fun getDataFile(context: Context): File {
        Log.i(TAG, "Getting file from directory ${context.filesDir}")
        return File(context.filesDir, FILENAME)
    }

    private fun showAlertDialogue() {

        val mapFormView = LayoutInflater.from(this).inflate(R.layout.dialog_create_map, null)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Map title")
            .setView(mapFormView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok", null)
            .show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val title = mapFormView.findViewById<EditText>(R.id.etMapTitle).text.toString()

            if (title.trim().isEmpty()){
                Toast.makeText(this, "All fields must be filled", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val intent = Intent(this@MainActivity, CreateMapActivity::class.java)
            intent.putExtra(EXTRA_MAP_TITLE, title)
            intent.putExtra(CURRENT_LOCATION, startLatLng)

            resultLauncher.launch(intent)

            dialog.dismiss()

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(applicationContext, "Location Services Enabled", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "Denied Location Services Access", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (checkPermission()) {
            if (isLocationEnabled()) {
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) {
                    val location = it.result
                    if (location == null) {
                        Toast.makeText(this, "Null received", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
                        startLatLng = LatLng(location.latitude, location.longitude)
                    }
                }
            } else {
                // Open settings app
                Toast.makeText(this, "Turn on Location Services", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            // Request for permissions
            requestPermission()
            Log.i(TAG, "requestPermission()")
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_ACCESS_LOCATION)
    }

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }

    private fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        return false
    }

    private fun generateSampleData(): List<UserMap> {
        return listOf(
            UserMap(
                "Memories from University",
                listOf(
                    Place("Branner Hall", "Best dorm at Stanford", 37.426, -122.163),
                    Place("Gates CS building", "Many long nights in this basement", 37.430, -122.173),
                    Place("Pinkberry", "First date with my wife", 37.444, -122.170)
                )
            ),
            UserMap("January vacation planning!",
                listOf(
                    Place("Tokyo", "Overnight layover", 35.67, 139.65),
                    Place("Ranchi", "Family visit + wedding!", 23.34, 85.31),
                    Place("Singapore", "Inspired by \"Crazy Rich Asians\"", 1.35, 103.82)
                )),
            UserMap("Singapore travel itinerary",
                listOf(
                    Place("Gardens by the Bay", "Amazing urban nature park", 1.282, 103.864),
                    Place("Jurong Bird Park", "Family-friendly park with many varieties of birds", 1.319, 103.706),
                    Place("Sentosa", "Island resort with panoramic views", 1.249, 103.830),
                    Place("Botanic Gardens", "One of the world's greatest tropical gardens", 1.3138, 103.8159)
                )
            ),
            UserMap("My favorite places in the Midwest",
                listOf(
                    Place("Chicago", "Urban center of the midwest, the \"Windy City\"", 41.878, -87.630),
                    Place("Rochester, Michigan", "The best of Detroit suburbia", 42.681, -83.134),
                    Place("Mackinaw City", "The entrance into the Upper Peninsula", 45.777, -84.727),
                    Place("Michigan State University", "Home to the Spartans", 42.701, -84.482),
                    Place("University of Michigan", "Home to the Wolverines", 42.278, -83.738)
                )
            ),
            UserMap("Restaurants to try",
                listOf(
                    Place("Champ's Diner", "Retro diner in Brooklyn", 40.709, -73.941),
                    Place("Althea", "Chicago upscale dining with an amazing view", 41.895, -87.625),
                    Place("Shizen", "Elegant sushi in San Francisco", 37.768, -122.422),
                    Place("Citizen Eatery", "Bright cafe in Austin with a pink rabbit", 30.322, -97.739),
                    Place("Kati Thai", "Authentic Portland Thai food, served with love", 45.505, -122.635)
                )
            )
        )
    }

}