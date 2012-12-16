package com.zw.dailyrecipe.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Utils {

	private static Date StringToDate(String value, String pattern) {
		if (value == null) {
			return null;
		}
		SimpleDateFormat format = new SimpleDateFormat(pattern);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
			return format.parse(value);
		} catch (ParseException e) {
			return null;
		}

	}

	/**
	 * Convert String Date value to long value
	 * @param value		Date String 
	 * @param pattern	the pattern. 
	 * @return long value
	 */
	public static long StringToLong(String value, String pattern) {
		if (value == null ) {
			return 0;
		}
		Date date = StringToDate(value, pattern);
		if(date != null){
			return date.getTime();
		} else {
			return 0;
		}
		

	}

	/**
	 * Convert long value to String date
	 * @param millis	milliseconds of time
	 * @param pattern	the pattern
	 * @return	String Date
	 */
	public static String ToDateString(long millis, String pattern) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millis);
		SimpleDateFormat format = new SimpleDateFormat(pattern);
		return format.format(c.getTime());

	}
	
	/**
	 * Check the network
	 * @param c	Context
	 * @return true if network is connected, false otherwise.
	 */
	public static boolean checkNetwork(Context c){
		final ConnectivityManager conMgr =  (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
		if(activeNetwork != null && activeNetwork.isConnected()){
			return true;
		}
		
		return false;
	}
}
