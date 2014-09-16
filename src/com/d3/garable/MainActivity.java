package com.d3.garable;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;
import android.bluetooth.*;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * Created by Leandro Salas 
 * MainActivity
 */
public class MainActivity extends ActionBarActivity implements View.OnTouchListener, EventsCallback {

	private ImageView btnActivate;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);

		setProgressBarIndeterminate(true);
		
		if(savedInstanceState==null){
			
			GarableApplication.setCallBack(this);
			
			btnActivate = (ImageView) findViewById(R.id.btnActivate);
			btnActivate.setOnTouchListener(this);
			GarageManager.getInstance().start();	
			
		}

	}

	@Override
	protected void onResume() {
		super.onResume();

		/*
		 * We need to enforce that Bluetooth is first enabled, and take the user
		 * to settings to enable it if they have not done so.
		 */
		
		//call isBlueoothActivated
		if(!GarageManager.getInstance().isBluetoothActivated()){
			
			// Bluetooth is disabled
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivity(enableBtIntent);
			finish();
			
			return;
		}

		/*
		 * Check for Bluetooth LE Support. In production, our manifest entry
		 * will keep this from installing on these devices, but this will allow
		 * test devices or other sideloads to report whether or not the feature
		 * exists.
		 */
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		
		GarageManager.getInstance().pause();
		
		finish();
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Disconnect from any active tag connection
		
		GarageManager.getInstance().stop();
		
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		
		return super.onOptionsItemSelected(item);
	}



	
	// ImageView event
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch(event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            
        	btnActivate.setImageResource(R.drawable.remote_over);
        	
        	GarageManager.getInstance().openGarage();
        	
            return true;
        case MotionEvent.ACTION_UP:
            
        	btnActivate.setImageResource(R.drawable.remote);
        	
            return true; 
		}
		
		return false;
	}
	
	public void activateButton(boolean activate){
		btnActivate.setEnabled(activate);
	}

	@Override
	public void onGarableFound() {
		btnActivate.setEnabled(true);
	}
	
}
