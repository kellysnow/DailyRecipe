package com.zw.dailyrecipe.parsertask;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.os.AsyncTask;
import android.widget.Toast;

import com.zw.dailyrecipe.Constants;
import com.zw.dailyrecipe.RecipeListActivity;
import com.zw.dailyrecipe.recipe.RecipeItem;
import com.zw.dailyrecipe.utils.ImageDownloader;
import com.zw.dailyrecipe.utils.LOG;
import com.zw.dailyrecipe.utils.Utils;
import com.zw.dailyrecipe.R;

/**
 * 
 * RSS Parser
 * 
 */
public class ParseRssTask extends AsyncTask<String, Void, Integer> {
	private static final String TAG = "ParseRssTask";
	private Context mContext;
	private boolean mUpdate;
	private ProgressDialog progressDialog;
	private OnFinishListener mListener;
	private Pattern mPattern = Pattern.compile(Constants.PATTERN_URL);

	/**
	 * 
	 * @param c
	 *            Context
	 * @param isRefresh
	 *            false: first start, do not show dialog, go to list view after
	 *            parsing is finished, show dialog otherwise.
	 * @param listener
	 *            call back method when task is finish;
	 */
	public ParseRssTask(Context c, boolean isRefresh, OnFinishListener listener) {
		mContext = c;
		mUpdate = isRefresh;
		mListener = listener;
		if (isRefresh) {
			progressDialog = new ProgressDialog(c, android.R.style.Theme_Panel);
			progressDialog.setMessage(c.getString(R.string.message_progress));
			progressDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					if (mListener != null) {
						mListener.onFinish();
					}
				}

			});
			progressDialog.show();

		}
	}

	@Override
	protected Integer doInBackground(String... params) {
		String strUrl = params[0];
		HttpURLConnection httpConnection = null;
		try {
			URL url = new URL(strUrl);
			httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setRequestMethod("GET");
			httpConnection.setConnectTimeout(Constants.TIME_OUT);
			httpConnection.setReadTimeout(Constants.TIME_OUT);
			httpConnection.connect();
			return parse(httpConnection);
		} catch (MalformedURLException e) {
			LOG.e(TAG, "URL Error", e);
			return Constants.ERR_PARSE;
		} catch (IOException e) {
			LOG.e(TAG, "Connection Error", e);
			return Constants.ERR_NETWORK;
		} catch (XmlPullParserException e) {
			LOG.e(TAG, "Parse Error", e);
			return Constants.ERR_PARSE;
		} finally {
			if (httpConnection != null) {
				httpConnection.disconnect();
				httpConnection = null;
			}
		}
	}

	protected void onPostExecute(Integer result) {
		if (result == Constants.ERR_NETWORK) {
			Toast.makeText(mContext,
					mContext.getString(R.string.message_network_error),
					Toast.LENGTH_LONG).show();
		}

		if (!mUpdate) {
			// if fisrt start app, go to recipe list view.
			Intent i = new Intent(mContext, RecipeListActivity.class);
			i.putExtra(Constants.NAME_EXTRA_DATA_TYPE,
					Constants.TYPE_DAILY_RECIPE);
			mContext.startActivity(i);
		} else {
			// notify the list view to refresh.
			mListener.onFinish();
		}
		// close progress dialog.
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}

	private int parse(HttpURLConnection con) throws IOException,
			XmlPullParserException {

		InputStream is = null;
		try {
			int responseCode = con.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				is = con.getInputStream();
				return parseRecipe(is);
			}
		} finally {
			if (is != null) {
				is.close();
			}
		}
		return Constants.ERR_PARSE;
	}

	private int parseRecipe(InputStream in) throws XmlPullParserException,
			IOException {
		List<RecipeItem> recipeList = new ArrayList<RecipeItem>();
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		XmlPullParser pullParser = factory.newPullParser();
		pullParser.setInput(in, "UTF-8");
		int eventType = pullParser.getEventType();
		long newLastBuildData = 0L;
		RecipeItem item = null;
		while (eventType != XmlPullParser.END_DOCUMENT) {
			String tagName;

			if (eventType == XmlPullParser.START_TAG) {
				tagName = pullParser.getName();
				if (tagName.equals(Constants.TAG_LAST_BUILD_DATE)) {
					// get last build date of RSS
					// [lastBuildDate]
					String strData = pullParser.nextText();
					newLastBuildData = Utils.StringToLong(strData,
							Constants.PATTERN);

					long oldDate = getLastBuildDateInDB();
					LOG.d(TAG, "now BuildData =" + newLastBuildData);
					LOG.d(TAG, "old BuildData =" + oldDate);
					// if RSS data has updated then parse
					if (newLastBuildData <= oldDate) {
						break;
					}
				} else if (tagName.equals(Constants.TAG_ITEM)) {
					item = new RecipeItem();
				} else if (tagName.equals(Constants.TAG_TITLE)) {
					if (item != null) {
						String tmpTitle = pullParser.nextText();

						String title = tmpTitle.replace("'", "''");
						if (title.endsWith("...")) {
							int end = title.lastIndexOf("...");
							title.subSequence(0, end);
							String newTitle = title.subSequence(0, end)
									.toString();
							item.setTitle(newTitle);
						} else {
							item.setTitle(title);
						}

					}

				} else if (tagName.equals(Constants.TAG_GUID)) {
					if (item != null) {
						String link = pullParser.nextText();
						item.setLink(link);
					}
				} else if (tagName.equals(Constants.TAG_DESC)) {
					if (item != null) {
						String des = pullParser.nextText();
						Matcher m = mPattern.matcher(des);
						if (m.find()) {
							String imageUrl = m.group(1);
							item.setImageUrl(imageUrl);
						}
					}
				} else if (tagName.equals(Constants.TAG_PUBDATE)) {
					if (item != null) {
						String strData = pullParser.nextText();
						long pubDate = Utils.StringToLong(strData,
								Constants.PATTERN);
						item.setPubdate(pubDate);
					}

				}
			} else if (eventType == XmlPullParser.END_TAG) {
				tagName = pullParser.getName();
				if (tagName.equals(Constants.TAG_ITEM)) {
					item.setLastBuildData(newLastBuildData);
					if (item.getTitle().length() != 0) {
						recipeList.add(item);

					}
					item = null;

				}
			}
			eventType = pullParser.next();
		}

		if (recipeList.size() != 0) {
			// insert to DB
			insertToDatabase(recipeList);
			// Delete old Date
			deleteOldRecipes();
		}

		return Constants.SUCCESS;

	}

	private void insertToDatabase(List<RecipeItem> items) {
		if (items != null) {
			mContext.getContentResolver().insert(
					Constants.TRANSACTION_CONTENT_URI, null);
			int cnt = items.size();
			for (int i = 0; i < cnt; i++) {
				insertToDatabase(items.get(i));
			}
			mContext.getContentResolver().insert(Constants.COMMIT_CONTENT_URI,
					null);
		}

	}

	private void insertToDatabase(RecipeItem item) {
		// try to update lastBuildDate
		String where = Constants.COLUMN_TITLE + " = '" + item.getTitle()
				+ "' AND " + Constants.COLUMN_LINK + " = '" + item.getLink()
				+ "'";
		ContentValues updateValues = new ContentValues();
		updateValues.put(Constants.COLUMN_LAST_BUILD_DATE,
				item.getLastBuildData());
		int num = mContext.getContentResolver().update(
				Constants.RECIPES_CONTENT_URI, updateValues, where, null);
		// if there is no matched recipe then insert to DB
		if (num == 0) {
			// insert to DB
			ContentValues values = new ContentValues();
			values.put(Constants.COLUMN_TITLE, item.getTitle());
			values.put(Constants.COLUMN_IMAGE, item.getImageUrl());
			values.put(Constants.COLUMN_LINK, item.getLink());
			values.put(Constants.COLUMN_PUBDATE, item.getPubDate());
			values.put(Constants.COLUMN_LAST_BUILD_DATE,
					item.getLastBuildData());

			values.putNull(Constants.COLUMN_DETAIL_IMAGE);
			values.putNull(Constants.COLUMN_SERVES);
			values.putNull(Constants.COLUMN_INGREDIENT);
			values.putNull(Constants.COLUMN_METHOD);

			// RecipeDetail detail = item.getRecipeDetail();
			// if (detail != null) {
			// values.put(Constants.COLUMN_DETAIL_IMAGE, detail.getImageUrl());
			// values.put(Constants.COLUMN_SERVES, detail.getServes());
			// values.put(Constants.COLUMN_INGREDIENT, detail.getIngredients());
			// values.put(Constants.COLUMN_METHOD, detail.getMethod());
			// } else {
			//
			// }

			mContext.getContentResolver().insert(Constants.RECIPES_CONTENT_URI,
					values);
		}

	}

	private long getLastBuildDateInDB() {
		Cursor cursor = mContext.getContentResolver().query(
				Constants.LAST_BUILD_DATE_CONTENT_URI,
				new String[] { Constants.COLUMN_LAST_BUILD_DATE }, null, null,
				Constants.COLUMN_LAST_BUILD_DATE + " DESC");
		if (cursor != null) {
			cursor.moveToFirst();

			if (cursor.getCount() != 0) {
				return cursor.getLong(0);
			}
		}
		return -1L;
	}

	/*
	 * Delete old recipes by lastBuildDate. Delete the recipe data with smaller
	 * lastBuildDate.
	 */
	private void deleteOldRecipes() {
		long oldBuildDate = 0L;
		Cursor cursor = mContext.getContentResolver().query(
				Constants.LAST_BUILD_DATE_CONTENT_URI,
				new String[] { Constants.COLUMN_LAST_BUILD_DATE }, null, null,
				Constants.COLUMN_LAST_BUILD_DATE + " ASC");
		if (cursor != null) {
			cursor.moveToFirst();

			if (cursor.getCount() > 1) {
				oldBuildDate = cursor.getLong(0);
				String where = Constants.COLUMN_LAST_BUILD_DATE + " = "
						+ oldBuildDate;
				Cursor c = mContext.getContentResolver().query(
						Constants.RECIPES_CONTENT_URI,
						new String[] { Constants.COLUMN_IMAGE,
								Constants.COLUMN_DETAIL_IMAGE }, where, null,
						null);
				if (c != null) {
					c.moveToFirst();
					int count = c.getCount();
					for (int i = 0; i < count; i++) {
						String image_url = c.getString(c
								.getColumnIndex(Constants.COLUMN_IMAGE));
						String detail_image_url = c.getString(c
								.getColumnIndex(Constants.COLUMN_DETAIL_IMAGE));
						deleteCache(image_url);
						deleteCache(detail_image_url);
					}
				}

				mContext.getContentResolver().delete(
						Constants.RECIPES_CONTENT_URI, where, null);
			}
		}

	}

	private void deleteCache(String url) {
		if (url != null) {
			ImageDownloader.clearCache(url);
		}
	}

}
