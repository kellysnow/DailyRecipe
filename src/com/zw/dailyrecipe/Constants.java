package com.zw.dailyrecipe;

import android.net.Uri;

public class Constants {

	public static final int TIME_OUT = 15000;
	public static final String NAME_EXTRA_DATA_TITLE = "title";
	public static final String NAME_EXTRA_DATA_LIST = "list";
	public static final String NAME_EXTRA_DATA_ITEM = "item";
	public static final String NAME_EXTRA_DATA_TYPE = "type";

	public static final int TYPE_DAILY_RECIPE = 0;
	public static final int TYPE_FAVORITES = 1;

	public static final String AUTHORITY = "com.zw.dailyrecipe.provider.RecipeContentProvider";

	// ----------------- Column Names-------
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_IMAGE = "image_url";
	public static final String COLUMN_LINK = "link";
	public static final String COLUMN_PUBDATE = "pubdate";
	public static final String COLUMN_DETAIL_IMAGE = "detail_image_url";
	public static final String COLUMN_SERVES = "serves";
	public static final String COLUMN_INGREDIENT = "ingredient";
	public static final String COLUMN_METHOD = "method";
	public static final String COLUMN_LAST_BUILD_DATE = "lastBuildDate";
	// -------Data Base-------
	public static final String RECIPES_BASE_PATH = "recipes";
	public static final String FAVORITES_BASE_PATH = "favorites";
	public static final String PATH_TRANSACTION = "transaction";
	public static final String PATH_COMMIT = "commit";
	public static final String PATH_ROLLBACK = "rollback";

	// -----------Content Provider------
	public static final Uri RECIPES_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + RECIPES_BASE_PATH);
	public static final Uri LAST_BUILD_DATE_CONTENT_URI = Uri
			.parse("content://" + AUTHORITY + "/" + RECIPES_BASE_PATH + "/"
					+ COLUMN_LAST_BUILD_DATE);
	public static final Uri FAVORITES_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + FAVORITES_BASE_PATH);
	public static final Uri TRANSACTION_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + PATH_TRANSACTION);
	public static final Uri COMMIT_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + PATH_COMMIT);
	public static final Uri ROLLBACK_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + PATH_ROLLBACK);

	// ------Time Pattern-----------
	public static final String PATTERN = "EEE, dd MMM yyyy HH:mm:ss ZZZ";

	// -----CSS------//
	public static final String CSS_RECIPE = "article#recipe";
	public static final String CSS_IMAGE = "img[width=125]";
	public static final String CSS_INGREDIENTS = "ul#list-ingredients";
	public static final String CSS_METHOD = "section#recipe-method";

	// --------RSS TAG----------//
	public static final String TAG_LAST_BUILD_DATE = "lastBuildDate";
	public static final String TAG_ITEM = "item";
	public static final String TAG_TITLE = "title";
	public static final String TAG_LINK = "link";
	public static final String TAG_GUID = "guid";
	public static final String TAG_DESC = "description";
	public static final String TAG_PUBDATE = "pubDate";

	public static final String PATTERN_URL = "src=\"(.*)\" alt";

	public static final int SUCCESS = 1;

	public static final String DUMMY_HTML = "<html><body></body></html>";
	// ---------Error Code-------------------//
	public static final int ERR_NETWORK = -100;
	public static final int ERR_PARSE = -200;

	/*
	 * Update interval 12 hours��
	 */
	public static final int UPDATE_HOUR_INTERVAL = 12; // 12 hours
	public static final int THREAD_INTERVAL = UPDATE_HOUR_INTERVAL * 60 * 60 * 1000;

}
