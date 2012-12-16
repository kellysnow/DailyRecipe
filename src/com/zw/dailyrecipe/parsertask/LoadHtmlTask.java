package com.zw.dailyrecipe.parsertask;

import java.io.IOException;
import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.zw.dailyrecipe.Constants;
import com.zw.dailyrecipe.recipe.RecipeItem;
import com.zw.dailyrecipe.utils.LOG;
import com.zw.dailyrecipe.R;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.widget.Toast;

/**
 * 
 * HTML parser task
 * 
 */
public class LoadHtmlTask extends AsyncTask<RecipeItem, Void, Boolean> {
	private static final String TAG = "LoadHtmlTask";
	private String mImageUrl = null;
	private String mIngredientHtml = null;
	private String mMethodHtml = null;
	private String mServes = null;
	private Context mContext;
	private RecipeItem mRecipeItem;
	private ProgressDialog progressDialog;
	private OnFinishListener mListener;
	private boolean mBShowDialog;
	private boolean isRunning = false;
	public static HashMap<String, LoadHtmlTask> taskMap = new HashMap<String, LoadHtmlTask>();

	/**
	 * 
	 * @param c
	 *            Context
	 * @param showDialog
	 *            true: show progress dialog, otherwise do not show progress
	 *            dialog
	 * @param listener
	 *            call back method when task is finished
	 */
	public LoadHtmlTask(Context c, boolean showDialog, OnFinishListener listener) {
		mContext = c;
		mBShowDialog = showDialog;
		mListener = listener;
		if (showDialog) {
			progressDialog = new ProgressDialog(c);
			progressDialog.setMessage(c.getString(R.string.message_progress));
			progressDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					LoadHtmlTask.this.cancel(true);
					isRunning = false;
					if (mListener != null) {
						mListener.onFinish();
					}
					taskMap.remove(mRecipeItem.getLink());
				}

			});
			progressDialog.show();
		}
	}

	@Override
	protected Boolean doInBackground(RecipeItem... params) {
		isRunning = true;
		mRecipeItem = params[0];
		String link = mRecipeItem.getLink();
		if (link != null) {
			if (taskMap.containsKey(link)) {
				LoadHtmlTask task = taskMap.get(link);
				task.cancel(true);
				taskMap.remove(link);
			}
			taskMap.put(link, this);
			try {
				Document doc = Jsoup.connect(link).timeout(Constants.TIME_OUT)
						.get();
				Elements elements = doc.select(Constants.CSS_RECIPE);

				// image
				Element imgElement = elements.select(Constants.CSS_IMAGE)
						.first();
				if (imgElement != null) {
					mImageUrl = imgElement.attr("src");
				}
				// Serves:
				Element servesElement = elements.select("p").first();
				if (servesElement != null) {
					mServes = servesElement.text();

				}
				// ingredients
				Element ingreElement = elements.select(
						Constants.CSS_INGREDIENTS).first();
				if (ingreElement != null) {
					String tmp = ingreElement.html();
					mIngredientHtml = "<html><body>" + tmp + "</body></html>";

				}

				// method
				Element methodElement = elements.select(Constants.CSS_METHOD)
						.first();
				if (methodElement != null) {
					String tmp = methodElement.html();
					mMethodHtml = "<html><body>" + tmp + "</body></html>";

				}
				return true;
			} catch (IOException e) {
				LOG.e(TAG, "parse html error " + mRecipeItem.getTitle(), e);

			}
		}

		return false;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		isRunning = false;
		// update to DB
		if (result) {
			updateDB(mRecipeItem.getTitle(), mRecipeItem.getLink(), mImageUrl,
					mServes, mIngredientHtml, mMethodHtml);
		} else {
			if (mBShowDialog) {
				Toast.makeText(mContext,
						mContext.getString(R.string.message_get_detail_error),
						Toast.LENGTH_LONG).show();
			}

		}
		if (mListener != null) {
			mListener.onFinish();
		}
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		taskMap.remove(mRecipeItem.getLink());

	}

	public boolean isRunning() {
		return isRunning;
	}

	public void updateDB(String title, String link, String url, String serves,
			String ingredient, String method) {
		String where = Constants.COLUMN_TITLE + " = '" + title + "' AND "
				+ Constants.COLUMN_LINK + " = '" + link + "'";
		ContentValues values = new ContentValues();

		values.put(Constants.COLUMN_DETAIL_IMAGE, url);
		values.put(Constants.COLUMN_SERVES, serves);
		values.put(Constants.COLUMN_INGREDIENT, ingredient);
		values.put(Constants.COLUMN_METHOD, method);
		mContext.getContentResolver().insert(Constants.TRANSACTION_CONTENT_URI,
				null);
		mContext.getContentResolver().update(Constants.RECIPES_CONTENT_URI,
				values, where, null);
		mContext.getContentResolver()
				.insert(Constants.COMMIT_CONTENT_URI, null);
	}

}
