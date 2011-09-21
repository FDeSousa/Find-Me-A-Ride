package com.fdesousa.android.FindMeARide;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.widget.Toast;

public class FindMeALocationListener implements LocationListener {
	private final FindMeARide findMeARide;

	public FindMeALocationListener(FindMeARide findMeARide) {
		this.findMeARide = findMeARide;
	}

	@Override
	public void onLocationChanged(Location location) {
		//  We just want to update lastKnownLoc for the moment
		findMeARide.setLastKnownLocation(location);
	}

	@Override
	public void onProviderDisabled(String provider) {
		//  GPS disabled, so send out informational message
		Toast.makeText(findMeARide, "GPS turned off", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onProviderEnabled(String provider) {
		//  GPS enabled, so send out informational message
		Toast.makeText(findMeARide, "GPS turned on", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		//  Unsure of what a status change could encapsulate
		Toast.makeText(findMeARide, "Provider status changed", Toast.LENGTH_LONG).show();
	}

}