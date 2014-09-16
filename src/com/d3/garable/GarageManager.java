package com.d3.garable;

import java.util.UUID;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.bluetooth.*;

public class GarageManager implements BluetoothAdapter.LeScanCallback {

	private static final String TAG = "GarageManager Bluetooth LE";

	private static final String DEVICE_NAME = "Garable";

	/* Garage Service */
	private static final UUID GARAGE_SERVICE = UUID.fromString("13e3b8e0-22df-11e4-8c21-0800200c9a66");
	/* Characteristic to open the garage door */
	private static final UUID ACTIVATE_DESCRIPTOR = UUID
			.fromString("37193b60-173a-11e4-8fd1-0002a5d5c51b");

	
	private BluetoothAdapter mBluetoothAdapter;
	private SparseArray<BluetoothDevice> mDevices;

	private BluetoothGatt mConnectedGatt;
	private BluetoothDevice device;

	private ProgressDialog mProgress;

	private static GarageManager instance = null;
	
	private boolean connected = false;

	protected GarageManager() {
		
	}

	public static GarageManager getInstance() {
		if (instance == null) {
			instance = new GarageManager();
		}
		return instance;
	}
	
	public void start(){
		
		/*
		 * Bluetooth in Android 4.3 is accessed via the BluetoothManager, rather
		 * than the old static BluetoothAdapter.getInstance()
		 */
		BluetoothManager manager = (BluetoothManager) GarableApplication.getContextApp().getSystemService(GarableApplication.getContextApp().BLUETOOTH_SERVICE);
		mBluetoothAdapter = manager.getAdapter();

		mDevices = new SparseArray<BluetoothDevice>();
		/*
		 * A progress dialog will be needed while the connection process is
		 * taking place
		 */
		mProgress = new ProgressDialog(GarableApplication.getActivity());
		mProgress.setIndeterminate(true);
		mProgress.setCancelable(false);
		
		mDevices.clear();
		startScan();
		
	}
	
	
	/* Methods to schedule and control Scan tasks */
	private Runnable mStopRunnable = new Runnable() {
		@Override
		public void run() {
			stopScan();
		}
	};
	private Runnable mStartRunnable = new Runnable() {
		@Override
		public void run() {
			startScan();
		}
	};

	private void startScan() {
		mBluetoothAdapter.startLeScan(this);
		GarableApplication.setProgressBarIndeterminateVisibility(true);

		mHandler.postDelayed(mStopRunnable, 2500);
	}

	private void stopScan() {
		
		mBluetoothAdapter.stopLeScan(this);
		GarableApplication.setProgressBarIndeterminateVisibility(false);
		
	}
	/*End Scan tasks */

	
	/* BluetoothAdapter.LeScanCallback */
	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
		/*
		 * We are looking for "Garable" devices only, so validate the name that
		 * each device reports before adding it to our collection
		 */
		if (DEVICE_NAME.equals(device.getName())) {

			this.device = device;
			mDevices.put(device.hashCode(), device);
			// btnActivate.setEnabled(true);

			Log.i(TAG, "New LE Device Added 7777: " + device.getName() + " @ "
					+ rssi);

			// stop scanning devices when we get one
			stopScan();

			GarableApplication.getCallBack().onGarableFound();
			
		}
	}

	
	/*
	 * In this callback, we control all notifications and connectivity with our BluetoothLE
	 */
	private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			
			Log.d(TAG, "Connection State Change: " + status + " -> " + connectionState(newState));
			
			if (status == BluetoothGatt.GATT_SUCCESS
					&& newState == BluetoothProfile.STATE_CONNECTED) {
				/*
				 * Once successfully connected, we must next discover all the
				 * services on the device before we can read and write their
				 * characteristics.
				 */
				gatt.discoverServices();
				mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Discovering Services..."));
				
			} else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
				/*
				 * If at any point we disconnect, send a message to clear the
				 * weather values out of the UI
				 */
				mHandler.sendEmptyMessage(MSG_CLEAR);
				
			} else if (status != BluetoothGatt.GATT_SUCCESS) {
				/*
				 * If there is a failure at any stage, simply disconnect
				 */
				gatt.disconnect();
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			
			Log.d(TAG, "Services Discovered: " + status);
			mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Activating Sensors..."));
			
			
			activateSensor(gatt);
			
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {

		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {

		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {

		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			Log.d(TAG, "Remote RSSI: " + rssi);
		}

		private String connectionState(int status) {
			switch (status) {
			case BluetoothProfile.STATE_CONNECTED:
				return "Connected";
			case BluetoothProfile.STATE_DISCONNECTED:
				return "Disconnected";
			case BluetoothProfile.STATE_CONNECTING:
				return "Connecting";
			case BluetoothProfile.STATE_DISCONNECTING:
				return "Disconnecting";
			default:
				return String.valueOf(status);
			}
		}
	};

	/*
	 * We have a Handler to process event results on the main thread
	 */
	private static final int ACTIVATE_GARAGE = 101;
	private static final int MSG_PROGRESS = 201;
	private static final int MSG_DISMISS = 202;
	private static final int MSG_CLEAR = 301;
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			BluetoothGattCharacteristic characteristic;

			switch (msg.what) {
			case MSG_PROGRESS:
				mProgress.setMessage((String) msg.obj);
				if (!mProgress.isShowing()) {
					mProgress.show();
				}
				break;
			case MSG_DISMISS:
				mProgress.hide();
				break;
			}
		}
	};

	public void activateSensor(BluetoothGatt gatt) {
        BluetoothGattCharacteristic characteristic;
        
        System.out.println(gatt);
        
        System.out.println(GARAGE_SERVICE);
        System.out.println(ACTIVATE_DESCRIPTOR);
        
        characteristic = gatt.getService(GARAGE_SERVICE).getCharacteristic(ACTIVATE_DESCRIPTOR);
        characteristic.setValue(new byte[] {0x02});
                
        gatt.writeCharacteristic(characteristic);
        
        mHandler.sendMessage(Message.obtain(null, MSG_DISMISS,""));
        
    }
	
	public void openGarage(){
		
		if(mConnectedGatt==null){
        	
        	// Obtain the discovered device to connect with
			//BluetoothDevice device = mDevices.get(0);
			Log.i(TAG, "Connecting to " + device.getName());
			/*
			 * Make a connection with the device using the special LE-specific
			 * connectGatt() method, passing in a callback for GATT events
			*/ 
			mConnectedGatt = device.connectGatt(GarableApplication.getContextApp(), false, mGattCallback);
			// Display progress UI
			mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS,
					"Connecting to " + device.getName() + "..."));
			
    	}else {
    		
    		if(mConnectedGatt.getDevice()!=null && (mConnectedGatt.getDevice().getName().equals(DEVICE_NAME))){
    			
    			activateSensor(mConnectedGatt);
    			
    		}
    		
    		
    	}
	}
	
	/* Start of getter and setters */
	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}
	
	public void stop(){
		// Disconnect from any active tag connection
		if (mConnectedGatt != null) {
			mConnectedGatt.disconnect();
			mConnectedGatt = null;
		}
	}
	
	public void pause(){
		// Make sure dialog is hidden
		mProgress.dismiss();
		// Cancel any scans in progress
		mHandler.removeCallbacks(mStopRunnable);
		mHandler.removeCallbacks(mStartRunnable);
		mBluetoothAdapter.stopLeScan(this);
	}

	public boolean isBluetoothActivated(){
		
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			return false;
		}
		
		return true;
	}
	
	
	

	
}
