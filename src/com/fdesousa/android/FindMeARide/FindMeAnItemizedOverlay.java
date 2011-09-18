package com.fdesousa.android.FindMeARide;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class FindMeAnItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private Context mContext;

	public FindMeAnItemizedOverlay(Drawable defaultMarker) {
		super(boundCenter(defaultMarker));
	}

	public FindMeAnItemizedOverlay(Drawable defaultMarker, Context context) {
		super(boundCenter(defaultMarker));
		mContext = context;
	}

	/**
	 *	Add an OverlayItem instance to the list of OverlayItems
	 *	@param overlay - the OverlayItem instance to add to the list
	 */
	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}
	
	/**
	 *	Handles a tap on the marker by showing title and snippet
	 *+	specified within the given OverlayItem instance 
	 */
	@Override
	protected boolean onTap(int index) {
		OverlayItem item = mOverlays.get(index);
		AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
		dialog.setTitle(item.getTitle());
		dialog.setMessage(item.getSnippet());
		dialog.show();
		return true;
	}
	
	/**
	 *	Convenience method for clearing the overlays, instead of
	 *	requiring another constructor call
	 */
	public void clearOverlays() {
		mOverlays.clear();
	}
	
	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}
	
	@Override
	public int size() {
		return mOverlays.size();
	}
}
