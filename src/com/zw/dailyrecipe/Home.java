package com.zw.dailyrecipe;

import com.zw.dailyrecipe.R;
import com.zw.dailyrecipe.parsertask.ParseRssTask;

import android.app.Activity;


import android.os.Bundle;
/**
 * 
 * First View Parse Daily Recipe RSS data
 * 
 */
public class Home extends Activity {
	//force to parse RSS
	private boolean mForce = false;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.home);
		parseRss();

	}

	 @Override
	 protected void onResume(){
	 super.onResume();
	 if(mForce) {
		 parseRss();
	 }
	
	 mForce = false;
	 }

	 @Override
	 protected void onPause(){
	 super.onResume();
	 	mForce = true;
	 }
	private void parseRss() {
		String url = this.getString(R.string.rss_daily_recipes);
		ParseRssTask downloadTask = new ParseRssTask(this, false, null);
		downloadTask.execute(url);
	}
}
