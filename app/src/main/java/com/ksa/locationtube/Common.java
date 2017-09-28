package com.ksa.locationtube;

/**
 * Created by Sergey Krivozyatev on 01.08.2017 14:11
 */

public class Common {

	public static boolean isEmpty(String value) {

		return value == null || value.trim().isEmpty();
	}

	public static String tag(String localTag) {
		return "LocationTube:" + localTag;
	}
}
