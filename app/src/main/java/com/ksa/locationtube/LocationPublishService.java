package com.ksa.locationtube;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Timer;
import java.util.TimerTask;

import ksa.location.tube.client.RetrofitClient;
import ksa.location.tube.model.Location;
import ksa.location.tube.model.UserLocation;

import static com.ksa.locationtube.Common.isEmpty;

/**
 * Created by Sergey Krivozyatev on 10.08.2017 9:49
 */

public class LocationPublishService extends Service {

	private final Logger logger = new Logger(getClass());

	private static boolean running = false;

	long scanInterval;
	private Timer timer;

	public static boolean isRunning() {
		return running;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logger.i("Starting publish service");
		restartTimer();
		running = true;
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		try {
			if (timer != null) {
				logger.i("Cancelling publish service timer");
				timer.cancel();
			}
		} finally {
			running = false;
		}
		logger.i("Stopping publish service");
		super.onDestroy();
	}

	private void restartTimer() {
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer();
		scanInterval = Preferences.get().getMyLocationScanInterval() * 1000;
		timer.schedule(new LocationScanTimerTask(getApplicationContext(), scanInterval), 0, scanInterval);
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private static class LocationScanTimerTask extends TimerTask {

		private final Logger logger = new Logger(getClass());

		private FusedLocationProviderClient fusedLocationProviderClient;
		private android.location.Location currentLocation = null;
		private Context context;
		private long scanInterval;

		private LocationScanTimerTask(Context context, long scanInterval) {
			this.context = context;
			this.scanInterval = scanInterval;
			fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
		}

		@Override
		public void run() {
			new Thread(new Runnable() {
				@Override
				public void run() {
					//if (lock.tryLock()) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							logger.i("Process location");
							processLocation();
						}
					}).start();
					try {
						Thread.sleep(Preferences.get().getLocationSetupDuration() * 1000);
							/*while (lock.isLocked()) {
								Thread.sleep(200);
							}*/
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						logger.i("End location processing");
						fusedLocationProviderClient.removeLocationUpdates(locationListener);
					}
					//}
				}
			}).start();
		}

		private void processLocation() {
			try {
				HandlerThread handlerThread = new HandlerThread("Location getter");
				handlerThread.start();
				Looper looper = handlerThread.getLooper();
				LocationRequest locationRequest = new LocationRequest();
				locationRequest.setInterval(scanInterval / 10);
				locationRequest.setFastestInterval(scanInterval / 10 / 10);
				locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
				if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
						|| ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
					fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationListener, looper);
					logger.i("Location requests started");
				}
			} finally {
				//lock.unlock();
			}
		}

		private LocationCallback locationListener = new LocationCallback() {
			@Override
			public void onLocationResult(LocationResult locationResult) {
				for (android.location.Location location : locationResult.getLocations()) {
					logger.i("Checking better location for (" + location.getLongitude() + ", " + location.getLatitude() + ")");
					if (isBetterLocation(location)) {
						currentLocation = location;
					}
				}
				if (currentLocation != null) {
					logger.i("Sending (" + currentLocation.getLatitude() + ", " + currentLocation.getLongitude() + ")");
					sendLocation(currentLocation);
				}
			}
		};

		private void sendLocation(android.location.Location location) {
			RetrofitClient client = new RetrofitClient(Preferences.get().getUrl());
			String myPhoneNumber = Preferences.get().getPhoneNumber();
			if (!isEmpty(myPhoneNumber)) {
				try {
					if (location != null) {
						logger.i("Send as phone " + myPhoneNumber);
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
	}
}
