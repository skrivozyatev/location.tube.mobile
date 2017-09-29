package com.ksa.locationtube;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Arrays;
import java.util.List;

import ksa.location.tube.model.Location;
import ksa.location.tube.model.UserLocation;

import static android.app.job.JobScheduler.RESULT_SUCCESS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ksa.locationtube.Common.isEmpty;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

	private final Logger logger = new Logger(getClass());

	private static final int JOB_ID = 1;
	private static final int REQUIRED_PERMISSIONS = 1;
	private static final String[] APP_PERMISSIONS = new String[] {
			Manifest.permission.READ_SMS,
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.READ_CONTACTS
	};

	private GoogleMap mMap;
	private LocationRequestService locationRequestService;
	private FusedLocationProviderClient fusedLocationProviderClient;

	private static final float MARKER_ZOOM_LEVEL = 16f;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
		//PhoneService.create(this.getApplicationContext());
		//startService(new Intent(this, LocationTubeService.class));
		//startService(new Intent(this, LocationService.class));
		if (permissionsGranted()) {
			startServices();
		} else {
			ActivityCompat.requestPermissions(this, APP_PERMISSIONS, REQUIRED_PERMISSIONS);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case REQUIRED_PERMISSIONS:
				List<String> permissionsList = Arrays.asList(permissions);
				if (hasPermissions(APP_PERMISSIONS, permissionsList, grantResults)) {
					startServices();
				} else {
					logger.w("Required permissions were not granted");
					Toast.makeText(this, "Required permissions were not granted", Toast.LENGTH_LONG).show();
				}
		}
	}

	private void startServices() {
		logger.i("Starting services...");
		locationRequestService = new LocationRequestService();
		locationRequestService.setGoogleMap(mMap);
		locationRequestService.start();
		if (Preferences.get().getPublishSwitch() && !LocationPublishService.isRunning()) {
			logger.i("Starting the Location Publish Service");
			startService(new Intent(this, LocationPublishService.class));
		}
		//startService(new Intent(this, LocationService.class));
		//runLocationServiceJob();
	}

	private boolean hasPermissions(String[] permissionsToCheck, List<String> permissions, int[] grantResults) {
		for (String permission : permissionsToCheck) {
			if (!permissions.contains(permission)) {
				return false;
			}
			if (grantResults[permissions.indexOf(permission)] != PERMISSION_GRANTED) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected void onStart() {
		if (locationRequestService != null) {
			logger.i("Starting Location Request Service");
			locationRequestService.start();
		}
		super.onStart();
	}

	@Override
	protected void onStop() {
		if (locationRequestService != null) {
			logger.i("Stopping Location Request Service");
			locationRequestService.stop();
		}
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		logger.i("Application competed");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menuItemSettings:
				openSettings();
				return true;
			case R.id.menuItemLog:
				openLog();
				return true;
			case R.id.contacts_menu_item:
				openContacts();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void runLocationServiceJob() {
		long scanInterval = Preferences.get().getMyLocationScanInterval() * 1000;
		JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(getApplicationContext(), LocationJobService.class))
				.setPeriodic(scanInterval)
				.build();
		JobScheduler jobScheduler = (JobScheduler) getApplicationContext().getSystemService(JOB_SCHEDULER_SERVICE);
		int result = jobScheduler.schedule(jobInfo);
		if (result == RESULT_SUCCESS) {
			logger.i("Location Service has been scheduled");
		} else {
			logger.e("Location Service scheduling failed");
		}
	}

	private void openSettings() {
		//Intent intent = new Intent(this, SettingsActivity.class);
		Intent intent = new Intent(this, LocationTubePreferencesActivity.class);
		startActivity(intent);
	}

	private void openLog() {
		startActivity(new Intent(this, LogActivity.class));
	}

	private void openContacts() {
		startActivity(new Intent(this, ContactsActivity.class));
	}

	private boolean permissionsGranted() {
		for (String permission : APP_PERMISSIONS) {
			if (!permissionGranted(permission)) {
				return false;
			}
		}
		return true;
	}

	private boolean permissionGranted(String permission) {
		return ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED;
	}

	private void magnifyMarkers() {
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
	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;
		mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
			@Override
			public boolean onMarkerClick(Marker marker) {
				if (mMap.getCameraPosition().zoom < MARKER_ZOOM_LEVEL) {
					mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), MARKER_ZOOM_LEVEL));
				} else {
					marker.showInfoWindow();
				}
				return true;
			}
		});
		if (locationRequestService != null) {
			locationRequestService.setGoogleMap(googleMap);
			logger.i("Request markers process started");
		}
	}
}
