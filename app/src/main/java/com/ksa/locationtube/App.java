package com.ksa.locationtube;

import android.app.Application;
import android.content.Context;

/**
 * Created by Sergey Krivozyatev on 31.07.2017 13:49
 */

public class App extends Application {

	private static Context context;

	public static Context getContext() {
		return context;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		context = getApplicationContext();
	}
}
