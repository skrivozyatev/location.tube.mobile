package com.ksa.locationtube;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import java.util.Timer;
import java.util.TimerTask;

import ksa.location.tube.client.RetrofitClient;
import ksa.location.tube.model.Location;
import ksa.location.tube.model.UserLocation;

import static com.ksa.locationtube.Common.isEmpty;
import static com.ksa.locationtube.Common.tag;

/**
 * Created by Sergey Krivozyatev on 27.07.2017 11:40
 */

public class LocationService extends Service {

	private final Logger logger = new Logger(getClass());

	private LocationManager locationManager;
	private Timer timer = new Timer(true);
	HandlerThread handlerThread = new HandlerThread("Location getter");

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		handlerThread.start();
		Looper looper = handlerThread.getLooper();

		int accuracy = Preferences.get().getMyLocationAccuracy();

		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					Preferences.get().getMyLocationScanInterval() * 1000, accuracy, locationListener, looper);
		} else {
			logger.e("No permissions to run GPS location request updates");
		}

		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
					Preferences.get().getMyLocationScanInterval() * 1000, accuracy, locationListener, looper);
		} else {
			logger.e("No permissions to run NETWORK location request updates");
		}
		/*timer.schedule(new TimerTask() {
			@Override
			public void run() {
				new Thread(new Runnable() {
					@Override
					public void run() {
					if (logger.isDebug()) {
						logger.d(new Date() + " Run the service " + LocationService.this);
					}
					RetrofitClient client = new RetrofitClient(Preferences.get().getUrl());
					String myPhoneNumber = Preferences.get().getPhoneNumber();
					if (!isEmpty(myPhoneNumber)) {
						try {
							Location location = getLocation();
							if (location != null) {
								if (logger.isDebug()) {
									logger.d("Register location (" + location.getLongitude() + ", " + location.getLatitude() + ")");
								}
								client.register(new UserLocation.Builder()
										.phone(myPhoneNumber)
										.location(location)
										.build());
							}
						} catch (Exception e) {
							Log.e(TAG, "Error sending location", e);
						}
					}
					}
				}).start();
			}
		}, 1000, Preferences.get().getMyLocationScanInterval() * 1000);*/
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		locationManager.removeUpdates(locationListener);
	}

	private Location getLocation() throws SecurityException {
		HandlerThread handlerThread = new HandlerThread("Location getter");
		handlerThread.start();
		Looper looper = handlerThread.getLooper();
		int locationSetupDuration = Preferences.get().getLocationSetupDuration() * 1000;
		try {
			int accuracy = Preferences.get().getMyLocationAccuracy();
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, locationSetupDuration/10, accuracy, locationListener, looper);
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationSetupDuration/10, accuracy, locationListener, looper);
		} catch (SecurityException e) {
			logger.e("Error requesting location updates", e);
		}
		try {
			Thread.sleep(locationSetupDuration);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			locationManager.removeUpdates(locationListener);
		}
		return currentLocation == null ? null :
				new Location.Builder().latitude(currentLocation.getLatitude()).longitude(currentLocation.getLongitude()).build();
	}

	public void stop() {
		timer.cancel();
	}

	private android.location.Location currentLocation = null;

	private LocationListener locationListener = new LocationListener() {
		@Override
		public void onLocationChanged(android.location.Location location) {
			if (logger.isDebug()) {
				logger.d("Selecting the best location of " + location.getProvider());
			}
			if (isBetterLocation(location)) {
				currentLocation = location;
				sendLocation(location);
			}
		}

		@Override
		public void onStatusChanged(String s, int i, Bundle bundle) {

		}

		@Override
		public void onProviderEnabled(String s) {

		}

		@Override
		public void onProviderDisabled(String s) {

		}
	};

	private void sendLocation(android.location.Location location) {
		RetrofitClient client = new RetrofitClient(Preferences.get().getUrl());
		String myPhoneNumber = Preferences.get().getPhoneNumber();
		if (!isEmpty(myPhoneNumber)) {
			try {
				if (location != null) {
					if (logger.isDebug()) {
						logger.d("Sending location (" + location.getLatitude() + ", " + location.getLongitude() + ")");
					}
					client.register(new UserLocation.Builder()
							.phone(myPhoneNumber)
							.location(new Location.Builder()
									.longitude(location.getLongitude())
									.latitude(location.getLatitude())
									.build())
							.build());
				}
			} catch (Exception e) {
				logger.e("Error sending location", e);
			}
		} else {
			logger.w("Location is not sent because of empty phone number");
		}
	}

	private static final int FIVE_MINUTES = 5 * 60 * 1000;

	private boolean isBetterLocation(android.location.Location location) {
		if (currentLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > FIVE_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -FIVE_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
