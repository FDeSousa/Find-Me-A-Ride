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
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView.BufferType;
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
	/**	Minimum distance change in metres; 1 metres						*/
	static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1;
	/**	Minimum time between updates in milliseconds; 2 minutes			*/
	static final long MINIMUM_TIME_BETWEEN_UPDATES = 1000 * 60 * 2;
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
	/**	Used to provide location updates in conjunction with Listener	*/
	LocationManager locationManager;
	/**	LocationListener for receiving regular location updates			*/
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
	/**	OnCheckedChangedListener for enabling/disabling EditText		*/
	private OnCheckedChangeListener customLocationOnChecked;
	/**	Instance of the PositiveButton from Dialog						*/
	private Button positiveButton;
	/**	Instance of the NegativeButton from Dialog						*/
	private Button negativeButton;
	/**	OnClickListener for the positiveButton in Dialog				*/
	private OnClickListener positiveButtonOnClick;
	/**	OnClickListener for the negativeButton in Dialog				*/
	private OnClickListener negativeButtonOnClick;

	//	Other private declarations
	/**	Store the marker drawable we'll use on the map overlay			*/
	private Drawable marker;
	/**	Used to determine whether to use lastKnownLoc data or not		*/
	private boolean auto_locate_user = true;

	/** Called when the activity is first created.	*/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//	Set window preferences
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		//	main.xml contains our MapView and Buttons
		setContentView(R.layout.main);

		//	Setup the main activity and its visible controls where necessary
		setupMain();
		//	Setup our custom Dialog and its controls
		setupDialog();
		//	Setup our MapView and the related other classes
		setupMapView();
	}

	@Override
	protected void onResume() {
		super.onResume();

		//	Should add the below code to a new Thread later on
		//	If user wants to be automatically located, do so
		if (auto_locate_user) {
			//	We want to request that Location be updated, use convenience method for this
			requestLocationUpdates();

			//	Get a new lastKnownLoc for convenience, within onResume rather than onCreate
			for (int i = 0; i < 50; i++) {
				lastKnownLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (lastKnownLoc != null)
					break;
			}

			if (lastKnownLoc == null) {
				//	Last resort method for getting location fix
				lastKnownLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}

			//	call convenience method that looks up the address
			if (lastKnownLoc != null) {
				//	If it couldn't get a location fix by now, we don't want to run this here
				getAddressFromLocation();
			}
		}

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
		//	Stop receiving updates while paused
		locationManager.removeUpdates(locationListener);
	}

	/**
	 *	Convenience method for setting up the main activity (this one)
	 *	and its related views, controls, etc.
	 */
	private void setupMain() {
		//	Extract the Button from layout as the text will be updated later
		locationButton = (Button) findViewById(R.id.CurrentLocationButton);
		locationButton.setText("Manually Set Location");
		// Extract the MapView from layout to work with it
		mapView = (MapView) findViewById(R.id.MapViewControl);

		locationListener = new FindMeALocationListener(this);
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	}

	/**
	 *	Convenience method for setting up the custom Dialog and its controls
	 */
	private void setupDialog() {
		//	Initialise our custom Dialog for later use
		dialog = new Dialog(this);
		dialog.setTitle("Set Custom Location");
		dialog.setCancelable(true);
		//	new_location_dialog.xml contains Buttons, Checkbox and EditText
		dialog.setContentView(R.layout.new_location_dialog);

		//	Instantiate the EditText with newLocationText control
		customLocation = (EditText) dialog.findViewById(R.id.newLocationText);
		//	Instantiate the CheckBox with auto_locate control
		autoLocate = (CheckBox) dialog.findViewById(R.id.auto_locate);
		//	Instantiate the Button with positiveButton
		positiveButton = (Button) dialog.findViewById(R.id.positiveButton);
		//	Instantiate the Button with negativeButton
		negativeButton = (Button) dialog.findViewById(R.id.negativeButton);

		//	Setup the OnClickListener and set it for Dialog.positiveButton
		positiveButtonOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				String customAddress = customLocation.getText().toString();

				auto_locate_user = autoLocate.isChecked();

				//	If user has auto_locate checked:
				if (!auto_locate_user && customAddress != null) {
					//	Call a convenience method to find the address for us
					getAddressFromLocationName(customAddress);
				} else {
					//	Otherwise, get from user's current location
					getAddressFromLocation();
				}
				//	Then update the map by zooming to the location
				zoomToMyLocation();

				dialog.hide();
			}
		};
		if (positiveButton != null)
			positiveButton.setOnClickListener(positiveButtonOnClick);
		
		//	Setup the OnClickListener and set it for Dialog.negativeButton
		negativeButtonOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.hide();
			}
		};
		if (negativeButton != null)
			negativeButton.setOnClickListener(negativeButtonOnClick);

		//	Setup the OnCheckedChangeListener and set it for Dialog.auto_locate
		customLocationOnChecked = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				toggleCustomLocation(isChecked);
			}
		};
		if (autoLocate != null)
			autoLocate.setOnCheckedChangeListener(customLocationOnChecked);
	}

	private void setupMapView() {
		//	android.R.drawable.ic_menu_mylocation refers to the 
		//+	standard GPS tracking icon in Android
		marker = getResources().getDrawable(android.R.drawable.ic_menu_mylocation);
		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());

		//	Add on the zoom controls
		mapView.setBuiltInZoomControls(true);
		//	Make sure the map is pan and zoom enabled
		mapController = mapView.getController();
		//	We need access to Overlays for adding custom map points
		mapOverlays = mapView.getOverlays();
		//	Instantiate with our ItemizedOverlay subclass
		itemizedOverlay = new FindMeAnItemizedOverlay(marker, this);
	}

	/**
	 *	onClick for CurrentLocationButton calls this method
	 *	@param view - Required View instance
	 */
	public void currentLocationButton(View v) {
		autoLocate.setChecked(auto_locate_user);
		dialog.show();
	}

	/**
	 *	onClick for FindMeARideButton calls this method
	 */
	public void findMeARideButton(View v) {
		//	Should start up the next Activity, which doesn't exist yet
		//+	but then this button isn't even visible as yet...
		Toast.makeText(this, "Find Me A Ride! button clicked", Toast.LENGTH_LONG);
	}

	/**
	 *	onClick for new_location_dialog.auto_locate calls this method
	 */
	public void autoLocate(View v) {
		CheckBox auto_locate = (CheckBox) v.findViewById(R.id.auto_locate);
		EditText customLocationText = (EditText) v.findViewById(R.id.newLocationText);

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
	 *	Toggles whether the user is using a Custom Location or an Automatically
	 *	tracked Location.<br />
	 *	If isChecked is true, should disable editing of the text in EditText, but
	 *	otherwise it should enable editing the EditText control
	 *
	 *	@param isChecked - the new state for whether or not to enable EditText
	 */
	public void toggleCustomLocation(boolean isChecked) {
		if (customLocation != null) {
			//	Enable it if it's not checked and vice versa
			customLocation.setEnabled(!isChecked);
			customLocation.setClickable(!isChecked);
			customLocation.setFocusable(!isChecked);
			customLocation.setFocusableInTouchMode(!isChecked);

			if (isChecked) {
				customLocation.setText(customLocation.getText().toString(), BufferType.NORMAL);
			} else {
				customLocation.setText(customLocation.getText().toString(), BufferType.EDITABLE);
				customLocation.requestFocus();
			}
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
			Toast.makeText(this, "Impossible to connect to Geocoder", Toast.LENGTH_SHORT);
		} catch (NullPointerException e) {
			if (lastKnownLoc == null)
				Log.e(TAG, e.getMessage() + "\tLast Known Location is likely null", e);
		} finally {
			//	Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
			if (result != null)
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
			mapController.setZoom(18);
			//	Clear the overlays to make sure we only ever have one icon
			mapOverlays.clear();
			itemizedOverlay.clearOverlays();
			//	Now add a new icon for the overlay
			OverlayItem overlayItem = new OverlayItem(geoPoint, DEFAULT_OVERLAY_ITEM_MESSAGE, DEFAULT_OVERLAY_ITEM_MESSAGE);
			itemizedOverlay.addOverlay(overlayItem);
			mapOverlays.add(itemizedOverlay);
			mapView.postInvalidate();
		} else {
			if (auto_locate_user) {
				getAddressFromLocation();
				zoomToMyLocation();
			}
		}
	}

	@Override
	protected boolean isRouteDisplayed() { return false; }

	/**
	 *	Convenience method for requesting that we receive Location updates<br />
	 *	To stop these updates simply call:<ul>
	 *	<li>locationManager.removeUpdates(locationListener)</li></ul>
	 */
	private void requestLocationUpdates() {
		locationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER, 
				MINIMUM_TIME_BETWEEN_UPDATES, 
				MINIMUM_DISTANCE_CHANGE_FOR_UPDATES, 
				locationListener);
	}

	/**
	 *	Linked with use in FindMeALocationListener class, used to
	 *	update lastKnownLoc when a newer location is available, but
	 *	depending upon new location data being more accurate/newer
	 *	@param location
	 */
	public void setLastKnownLocation(Location location) {
		//if (lastKnownLoc == null) {
		//	For now, do no checks on the location data, simply overwrite the previous one
		//	Any location data is better than none at all
		synchronized (location) {
			lastKnownLoc = location;				
		}
		//}

		if (auto_locate_user) {
			getAddressFromLocation();
			zoomToMyLocation();
		}
	}
}