package com.fdesousa.android.FindMeARide;

import java.io.IOException;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class FindMeARide extends MapActivity {
	//	Static and final declarations
	/**	Activity-specific tag for logging purposes						*/
	private final static String TAG = "FindMeARide:MapActivity";
	/**	Minimum distance change in metres; 10 metres					*/
	static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 10;
	/**	Minimum time between updates in milliseconds; 30 seconds		*/
	static final long MINIMUM_TIME_BETWEEN_UPDATES = 30000;
	/**	The default message and snippet text for an overlay item		*/
	static final String DEFAULT_OVERLAY_ITEM_MESSAGE = "You're here!";

	//	Declarations relating to the use of the MapView
	/**	MapView instance for displaying Google Maps of location			*/
	MapView mapView;
	/**	MapController to manage panning and zooming of the MapView		*/
	MapController mapController;
	/**	mapOverlays will store the Overlays used with the MapView		*/
	List<Overlay> mapOverlays;
	/**	itemizedOverlay will be used for individual OverlayItems		*/
	FindMeAnItemizedOverlay itemizedOverlay;

	//	Declarations relating to the current or user-entered location
	/**	Location for a constant record of last-known location of device	*/
	Location lastKnownLoc;
	/**	LocationManager provides updates on the current location		*/
	LocationManager locationManager;
	/**	For convenience with location updates, store a LocationListener	*/
	LocationListener locationListener;
	/**	GeoPoint will contain the latitude/longitude pairing of Address	*/
	GeoPoint geoPoint;
	/**	Provides address lookup and reverse-address lookup of location	*/
	Geocoder geocoder;
	/**	Stores Address information acquired from Geocoder instance		*/
	Address address;

	//	Private declarations relating to visible controls on Main
	/**	Button instance for the Current/Custom Location button			*/
	private Button locationButton;

	//	Private declarations relating to visible controls on Dialog
	/**	Handles a custom Dialog for this application					*/
	private Dialog dialog;
	/**	Checkbox that determines whether user wants to be auto-located	*/
	private CheckBox autoLocate;
	/**	EditText instance for the custom location text in Dialog		*/
	private EditText customLocation;

	//	Other private declarations
	/**	Store the marker drawable we'll use on the map overlay			*/
	private Drawable marker;
	/**	Used to determine whether to use lastKnownLoc data or not		*/
	private boolean auto_locate_user;

	/** Called when the activity is first created.	*/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//	main.xml contains our MapView and Buttons
		setContentView(R.layout.main);

		//	Set window preferences
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		//	Extract the Button from layout as the text will be updated later
		locationButton = (Button) findViewById(R.id.CurrentLocationButton);
		locationButton.setText("Manually Set Location");

		//	We want to automatically locate the user by default
		auto_locate_user = true;

		//	Initialise our custom Dialog for later use
		dialog = new Dialog(this);
		dialog.setTitle("Set Custom Location");
		dialog.setCancelable(true);
		//	new_location_dialog.xml contains Buttons, Checkbox and EditText
		dialog.setContentView(R.layout.new_location_dialog);

		autoLocate = (CheckBox) findViewById(R.id.auto_locate);
		//	Checkbox should have same checked state as boolean auto_locate_user
		autoLocate.setChecked(auto_locate_user);

		//	Instantiate the EditText with newLocationText control
		customLocation = (EditText) findViewById(R.id.newLocationText);
		//	Disabled when auto_locate_user is true and vice versa
		customLocation.setEnabled(!auto_locate_user);

		//	Instantiate the new LocationListener to use with LocationManager
		locationListener = new FindMeALocationListener(this);
		//	Get a LocationManager from the system to get updates on the
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		//	android.R.drawable.ic_menu_mylocation refers to the 
		//+	standard GPS tracking icon in Android
		marker = getResources().getDrawable(android.R.drawable.ic_menu_mylocation);
		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());

		// Extract the MapView from layout to work with it
		mapView = (MapView) findViewById(R.id.MapViewControl);
		//	Add on the zoom controls
		mapView.setBuiltInZoomControls(true);
		//	Make sure the map is pan and zoom enabled
		mapController = mapView.getController();
		//	We need access to Overlays for adding custom map points
		mapOverlays = mapView.getOverlays();
		//	Instantiate with our ItemizedOverlay subclass
		itemizedOverlay = new FindMeAnItemizedOverlay(marker, this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		//	By default, we want to get location updates
		requestLocationUpdates();

		//	Get a new lastKnownLoc for convenience, within onResume rather than onCreate
		//	Just in case LocationManager doesn't provide lastKnownLoc in
		//+	time for it to be called first-time (rather likely to happen)
		long startTime = System.nanoTime();
		float runningTime = 0;
		while (lastKnownLoc == null) {
			//	For safety, keep checking its status, and attempt to get while it's null
			lastKnownLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (lastKnownLoc != null) break; // Just in case the lastKnownLoc is updated before threshold

			//	Check how long this loop has been running for, otherwise we might need to quit
			runningTime += (System.nanoTime() - startTime) / 1000000000.0f;
			if (runningTime >= 10.0) {			//	Taken 10 seconds? Warn the user that's not normal
				Toast.makeText(this, "Taking longer than expected to lock on location", Toast.LENGTH_SHORT);
			} else if (runningTime >= 20.0) {	//	Taken 20 seconds? Warn the user again
				Toast.makeText(this, "Cannot get a GPS location. Taking longer than expected", Toast.LENGTH_LONG);
			} else if (runningTime >= 30.0) {	//	If it's been over 30 seconds, user may not have data connection
				Toast.makeText(this, "GPS taking too long. May not have GPS enabled/Data connection active", Toast.LENGTH_LONG);
			}
		}

		//	call convenience method that looks up the address
		if (auto_locate_user)
			getAddressFromLocation();

		// call convenience method that zooms map on our location
		zoomToMyLocation();
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		//	Ignore orientation changes
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPause() {
		super.onPause();
		locationManager.removeUpdates(locationListener);
	}

	/**
	 *	Convenience method for requesting location updates<br/>
	 *	To stop the Location updates simply call:<ul>
	 *	<li>locationManager.removeUpdates(locationListener)</li></ul>
	 */
	public void requestLocationUpdates() {
		//	This can take an awfully long time to get the initial location data
		locationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER,		//	Updates provided by GPS
				MINIMUM_TIME_BETWEEN_UPDATES,		//	Minimum time between updates
				MINIMUM_DISTANCE_CHANGE_FOR_UPDATES,//	Minimum distance change between updates
				locationListener);					//	Using our LocationListener instance
	}

	/**
	 *	onClick for CurrentLocationButton calls this method
	 *	@param view - Required View instance
	 */
	public void changeLocation(View view) {
		dialog.show();
	}

	/**
	 *	onClick for FindMeARideButton calls this method
	 */
	public void runSearch(View view) {
		//	Should start up the next Activity, which doesn't exist yet
		//+	but then this button isn't even visible as yet...
	}

	/**
	 *	onClick for new_location_dialog.ok_button calls this method
	 */
	public void confirmCustomLocation(View view) {
		EditText customLocationText = (EditText) view.findViewById(R.id.newLocationText);
		String customAddress = customLocationText.getText().toString();

		//	If user has auto_locate checked:
		if (auto_locate_user && customAddress != null) {
			//	Call a convenience method to find the address for us
			getAddressFromLocationName(customAddress);			
		} else {
			//	Otherwise, get from user's current location
			getAddressFromLocation();
		}
		//	Then update the map by zooming to the location
		zoomToMyLocation();
	}

	/**
	 *	onClick for new_location_dialog.cancel_button calls this method
	 */
	public void cancelCustomLocation(View view) {
		//	Quite simply cancel the current dialog, we don't need it
		dialog.cancel();
	}

	/**
	 *	onClick for new_location_dialog.auto_locate calls this method
	 */
	public void autoLocate(View view) {
		CheckBox auto_locate = (CheckBox) view.findViewById(R.id.auto_locate);
		EditText customLocationText = (EditText) view.findViewById(R.id.newLocationText);

		if (auto_locate.isChecked()) {
			//	User wants to be automatically located from now on
			auto_locate_user = true;
			//	Disable the EditText control so they can't change the location
			customLocationText.setEnabled(!auto_locate_user);	//	Always opposite of auto_locate_user
			//	Try getting the latest location fix and zoom to it
			getAddressFromLocation();
			zoomToMyLocation();
		} else {
			//	User wants to manually set the current location
			auto_locate_user = false;
			//	Enable the EditText control so the user can enter a location
			customLocationText.setEnabled(!auto_locate_user);	//	Always opposite of auto_locate_user
			//	No more operations here as getAddressFromLocationName is called
			//+	only when the user presses the OK button
		}
	}

	/**
	 *	This method performs lookup of the address from current location 
	 */
	public void getAddressFromLocation() {
		String result = null;
		geocoder = new Geocoder(this);

		//	Should be contained within a new thread, but as the operation
		//+	is short, I've foregone the thread for now. Will re-add later
		try {
			List<Address> addressList = geocoder.getFromLocation(
					lastKnownLoc.getLatitude(),
					lastKnownLoc.getLongitude(), 1);
			if (addressList != null && addressList.size() > 0) {
				address = addressList.get(0);
				//	We want the result to be postal code and locality only
				result = address.getPostalCode() + ", " + address.getLocality();
				//	Must call zoomToMyLocation() afterwards to zoom in to new
				//+	location if needed, by using our Address instance
			}
		} catch (IOException e) {
			Log.e(TAG, "Impossible to connect to Geocoder", e);
		} finally {
			Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
			locationButton.setText(result);
		}
	}

	/**
	 *	This method is similar to getAddressFromLocation, but uses an address
	 *+	that the user supplied, or an address fragment, to find a location
	 */
	public void getAddressFromLocationName(final String addressFragment) {
		String result = null;
		geocoder = new Geocoder(this);

		//	Should be contained within a new thread, but as the operation
		//+	is short, I've foregone the thread for now. Will re-add later
		try {
			List<Address> addressList = geocoder.getFromLocationName(
					addressFragment, 1);
			if (addressList != null && addressList.size() > 0) {
				address = addressList.get(0);
				//	We want the result to be postal code and locality only
				result = address.getPostalCode() + ", " + address.getLocality();
				//	Must call zoomToMyLocation() afterwards to zoom in to new
				//+	location if needed, by using our Address instance
			}
		} catch (IOException e) {
			Log.e(TAG, "Impossible to connect to Geocoder", e);
		} finally {
			Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
			locationButton.setText(result);
		}
	}

	/**
	 * This method zooms to the user's location with a zoom level of 10.
	 */
	private void zoomToMyLocation() {
		//	If the location exists, zoom into it at zoom level 10
		if (address != null) {
			//	For convenience use a GeoPoint instance to store lat & long
			geoPoint = new GeoPoint(
					(int)(address.getLatitude() * 1E6), 	//	Latitude in microdegrees
					(int)(address.getLongitude() * 1E6));	//	Longitude in microdegrees
			//	Now zoom into the current location on the map
			mapController.animateTo(geoPoint);
			mapController.setZoom(15);
			//	Clear the overlays to make sure we only ever have one icon
			mapOverlays.clear();
			itemizedOverlay.clearOverlays();
			//	Now add a new icon for the overlay
			OverlayItem overlayItem = new OverlayItem(geoPoint, DEFAULT_OVERLAY_ITEM_MESSAGE, DEFAULT_OVERLAY_ITEM_MESSAGE);
			itemizedOverlay.addOverlay(overlayItem);
			mapOverlays.add(itemizedOverlay);
			mapView.postInvalidate();
		} else {
			Toast.makeText(this, "Cannot determine location", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 *	Convenience method for FindMeALocationListener instance
	 *	@param location - the new Location instance to set lastKnownLoc as
	 */
	public void setLastKnownLocation(Location location) {
		lastKnownLoc = location;
	}

	@Override
	protected boolean isRouteDisplayed() { return false; }
}