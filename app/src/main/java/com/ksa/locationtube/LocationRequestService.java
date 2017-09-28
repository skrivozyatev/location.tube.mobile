package com.ksa.locationtube;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import ksa.location.tube.client.Result;
import ksa.location.tube.client.RetrofitClient;
import ksa.location.tube.model.Location;
import ksa.location.tube.model.UserLocation;

/**
 * Created by Sergey Krivozyatev on 19.07.2017 11:23
 */

public class LocationRequestService {

	private final Logger logger = new Logger(getClass());

	private Map<String, UserLocation> userLocationMap = new ConcurrentHashMap<>();
	private boolean active = false;
	private Timer timer;
	private ReentrantLock threadLock = new ReentrantLock();
	private Handler handler;
	private GoogleMap googleMap;

	private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();

	public LocationRequestService() {
		handler = new Handler();
	}

	public void start() {

		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (threadLock.tryLock()) {
							try {
								RetrofitClient client = new RetrofitClient(Preferences.get().getUrl());
								Result<UserLocation[]> result = client.getAllLocations();
								if (result.getStatus() == 200) {
									refreshLocations(result.getData());
									/*for (UserLocation userLocation : result.getData()) {
										userLocationMap.put(userLocation.getPhone(), userLocation);
									}*/
								}
								//Thread.sleep(Preferences.get().getLocationsRefreshInterval() * 1000);
							} catch (IOException e) {
								logger.e("Couldn't connect to the remote host to refresh locations", e);
							} finally {
								threadLock.unlock();
							}
						}
					}
				}).start();
			}
		}, 1000, Preferences.get().getLocationsRefreshInterval() * 1000);
		logger.i("Location request process started");

		/*new Thread(new Runnable() {
			@Override
			public void run() {
				active = true;
				while (active) {
					try {
						RetrofitClient client = new RetrofitClient(Preferences.get().getUrl());
						Result<UserLocation[]> result = client.getAllLocations();
						if (result.getStatus() == 200) {
							for (UserLocation userLocation : result.getData()) {
								userLocationMap.put(userLocation.getPhone(), userLocation);
							}
						}
						Thread.sleep(Preferences.get().getLocationsRefreshInterval() * 1000);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}).start();*/
	}

	private void refreshLocations(final UserLocation[] userLocations) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (googleMap != null && userLocations.length > 0) {
					googleMap.clear();
					LatLng latLng;
					for (UserLocation userLocation : userLocations) {
						if (userLocation != null) {
							Location location = userLocation.getLocation();
							latLng = new LatLng(location.getLatitude(), location.getLongitude());
							Marker marker = googleMap.addMarker(new MarkerOptions().position(latLng)
									.title(userLocation.getPhone())
									.snippet(App.getContext().getString(R.string.last_marker_date)
											+ ": " + DATE_FORMAT.format(userLocation.getDate())));
							Contact contact = Data.getContact(userLocation.getPhone());
							if (contact != null) {
								if (contact.hasBitmap()) {
									BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(contact.getBitmap());
									marker.setIcon(bitmapDescriptor);
								}
							}
						}
					}
				/*if (latLng != null) {
					mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
				}*/
				}
			}
		});
	}

	public void stop() {
		timer.cancel();
		logger.i("Location request process stopped");
	}

	public UserLocation[] getAll() {
		return userLocationMap.values().toArray(new UserLocation[userLocationMap.size()]);
	}

	public void setGoogleMap(GoogleMap googleMap) {
		this.googleMap = googleMap;
	}
}
