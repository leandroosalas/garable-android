package com.d3.garable;

import android.R.bool;
import android.app.Application;
import android.content.Context;

public class GarableApplication extends Application {
	
	private static Context contextApp;
	private static EventsCallback callBack;
	
	public void onCreate(){
		super.onCreate();
		contextApp = this.getApplicationContext();
	}
	
	public static Context getContextApp(){
		return contextApp;
	}

	public static EventsCallback getCallBack() {
		return callBack;
	}

	public static void setCallBack(EventsCallback callBack) {
		GarableApplication.callBack = callBack;
	}
	
	public static void setProgressBarIndeterminateVisibility(boolean status){
		((MainActivity)callBack).setProgressBarIndeterminateVisibility(status);
	}
	
	public static MainActivity getActivity(){
		return (MainActivity)callBack;
	}
	
}
