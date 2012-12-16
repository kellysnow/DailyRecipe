package com.zw.dailyrecipe.utils;

import android.util.Log;

public class LOG {
	public static final boolean DEBUG = true;
	
	public static void d(String tag, String msg){
		if(DEBUG){
			Log.d(tag, msg);
		}
	}
	
	public static void e(String tag, String msg){
		if(DEBUG){
			Log.e(tag, msg);
		}
	}
	
	public static void e(String tag, String msg, Throwable tr){
		if(DEBUG){
			Log.e(tag, msg, tr);
		}
	}
	
	public static void w(String tag, String msg){
		if(DEBUG){
			Log.w(tag, msg);
		}
	}
}
