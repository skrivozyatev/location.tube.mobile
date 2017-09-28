package com.ksa.locationtube;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.widget.Toast;

import ksa.location.tube.client.RetrofitClient;
import ksa.location.tube.model.Location;
import ksa.location.tube.model.UserLocation;

import static com.ksa.locationtube.Common.isEmpty;

/**
 * Created by Sergey Krivozyatev on 03.08.2017 15:40
 */

public class LocationJobService extends JobService {

	private final Logger logger = new Logger(getClass());

	private Context context;
	private Handler handler;
	private android.location.Location currentLocation;

	@Override
	public boolean onStartJob(final JobParameters params) {
		context = getApplicationContext();
		handler = new Handler();
		new Thread(new Runnable() {
			@Override
			public void run() {

			try {
				logger.i("Job started with accuracy: " + Preferences.get().getMyLocationAccuracy());
				RetrofitClient client = new RetrofitClient(Preferences.get().getUrl());
				String myPhoneNumber = Preferences.get().getPhoneNumber();
				if (!isEmpty(myPhoneNumber)) {
					try {
						Location location = getLocation();
						if (location != null) {
							logger.i("Registering a location (" + location.getLatitude() + ", " + location.getLongitude() + ")");
							client.register(new UserLocation.Builder()
									.phone(myPhoneNumber)
									.location(location)
									.build());
						}
					} catch (Exception e) {
						logger.e("Error registering location", e);
					}
				}
			} finally {
				jobFinished(params, false);
			}
			}
		}).start();
		return true;
	}

	@Override
	public boolean onStopJob(JobParameters params) {
		return false;
	}

	private Location getLocation() throws SecurityException {
		debugMessage("Sending my location");
		final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		if (currentLocation == null) {
			logger.i("Get last known location");
			currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			android.location.Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (isBetterLocation(gpsLocation)) {
				currentLocation = gpsLocation;
			}
		}
		HandlerThread handlerThread = new HandlerThread("Location getter");
		handlerThread.start();
		Looper looper = handlerThread.getLooper();
		int locationSetupDuration = Preferences.get().getLocationSetupDuration() * 1000;
		try {
			int accuracy = Preferences.get().getMyLocationAccuracy();
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, locationSetupDuration/10, accuracy, locationListener, looper);
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationSetupDuration/10, accuracy, locationListener, looper);
		} catch (SecurityException e) {
			logger.e("Couldn't start location updates", e);
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

	private void debugMessage(final String message) {
		if (BuildConfig.DEBUG) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
				}
			});
		}
	}

	private LocationListener locationListener = new LocationListener() {
		@Override
		public void onLocationChanged(android.location.Location location) {
			logger.i("Checking better location");
			if (isBetterLocation(location)) {
				currentLocation = location;
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
}
