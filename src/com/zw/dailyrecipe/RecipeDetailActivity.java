package com.zw.dailyrecipe;
import com.zw.dailyrecipe.R;
import com.zw.dailyrecipe.parsertask.LoadHtmlTask;
import com.zw.dailyrecipe.parsertask.OnFinishListener;
import com.zw.dailyrecipe.recipe.RecipeDetail;
import com.zw.dailyrecipe.recipe.RecipeItem;
import com.zw.dailyrecipe.utils.ImageDownloader;
import com.zw.dailyrecipe.utils.Utils;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
/**
 * 
 * Recipe detail view
 * <p>
 * <b>Intent parameters:</b>
 * <dl>
 * <dt>{@link  Constants#NAME_EXTRA_DATA_TYPE}: {@link RecipeItem}
 * <dd>recipe data which is selected.
 * <dt>{@link  Constants#NAME_EXTRA_DATA_ITEM}:String
 * <dd>List Type (Recipes or Favorites).
 * </dl>
 */
public class RecipeDetailActivity extends Activity {
	private static final String TAG = "RecipeDetailActivity";
	private RecipeItem mRecipeItem = null;
	private ImageDownloader imageLoader;
	private TextView titleView;
	private CheckBox mBtnFavorites;
	private ImageView mImageView;
	private TextView mTextviewServes;
	private WebView ingredientsView;
	private WebView methodView;
	private ProgressBar mProgressBar;
	private ImageView mImgRefresh;
	private boolean mIsFavorites;
	private LoadHtmlTask mTask;
	private Bitmap mBackgroundBmp;
	private int mType;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recipe_detail);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mRecipeItem = extras.getParcelable(Constants.NAME_EXTRA_DATA_ITEM);
			mType = extras.getInt(Constants.NAME_EXTRA_DATA_TYPE,
					Constants.TYPE_DAILY_RECIPE);
		}

		imageLoader = new ImageDownloader(this);
		mBackgroundBmp = BitmapFactory.decodeResource(getResources(),
				R.drawable.bg_image);

		final boolean isLandscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? true
				: false);
		initializeUI(isLandscape);

		RecipeDetail recipeDetail = getDetailFromDB(mRecipeItem.getTitle(),
				mRecipeItem.getLink());

		if (recipeDetail != null) {
			mRecipeItem.setRecipeDetail(recipeDetail);
			String ingre = recipeDetail.getIngredients();
			String method = recipeDetail.getMethod();
			int dummyLen = Constants.DUMMY_HTML.length();
			if (ingre.length() == dummyLen && method.length() == dummyLen) {
				refresh();
			} else {
				mIsFavorites = isFavorites(mRecipeItem);
				updateViewContent(mIsFavorites, recipeDetail);
			}

		} else {
			refresh();
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		switch (mType) {
		case Constants.TYPE_DAILY_RECIPE:
			getMenuInflater().inflate(R.menu.menu_main, menu);
			break;
		case Constants.TYPE_FAVORITES:
			getMenuInflater().inflate(R.menu.menu_default, menu);
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_favorites:
			openFavorites();
			return true;
		case R.id.menu_clear:
			ImageDownloader.clearCache();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.activity_recipe_detail);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			initializeUI(true);
		} else {
			initializeUI(false);
		}

		if (mTask != null && mTask.isRunning()) {
			Log.d(TAG, "task is running");
			mImgRefresh.setVisibility(View.INVISIBLE);
			mProgressBar.setVisibility(View.VISIBLE);
		}
		updateViewContent(mIsFavorites, mRecipeItem.getRecipeDetail());
	}

	private void initializeUI(boolean isLandscape) {
		titleView = (TextView) findViewById(R.id.recipe_title);
		titleView.setText(mRecipeItem.getTitle());
		mBtnFavorites = (CheckBox) this.findViewById(R.id.button_favorite);
		mImageView = (ImageView) findViewById(R.id.recipe_image);
		mTextviewServes = (TextView) findViewById(R.id.textview_serves);
		ingredientsView = (WebView) this.findViewById(R.id.redicpe_ingredients);
		methodView = (WebView) this.findViewById(R.id.redicpe_method);
		mBtnFavorites.setOnClickListener(clickListener);

		mImgRefresh = (ImageView) findViewById(R.id.image_refresh);
		mProgressBar = (ProgressBar) findViewById(R.id.progressbar);

		FrameLayout btn_left = (FrameLayout) findViewById(R.id.btn_left);
		FrameLayout btn_refresh = (FrameLayout) findViewById(R.id.btn_refresh_layout);

		WindowManager windowManager = (WindowManager) this
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = windowManager.getDefaultDisplay();
		DisplayMetrics displayMetrics = new DisplayMetrics();
		display.getMetrics(displayMetrics);
		int display_width = displayMetrics.widthPixels;

		LayoutParams params = mImageView.getLayoutParams();
		if (isLandscape) {
			params.height = display_width / 2;
			params.width = (int) params.width * 408 / 326;

		} else {
			params.width = display_width;
			params.height = (int) params.width * 408 / 326;
		}

		mImageView.setLayoutParams(params);

		if (mType == Constants.TYPE_FAVORITES) {
			btn_refresh.setVisibility(View.INVISIBLE);
		}

		btn_refresh.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// button invisible/visible

				refresh();

			}

		});

		btn_left.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				RecipeDetailActivity.this.finish();

			}

		});

	}

	private void openFavorites() {
		Intent i = new Intent(this, RecipeListActivity.class);
		i.putExtra(Constants.NAME_EXTRA_DATA_TYPE, Constants.TYPE_FAVORITES);
		startActivity(i);
	}

	private void refresh() {
		mImgRefresh.setVisibility(View.INVISIBLE);
		mProgressBar.setVisibility(View.VISIBLE);
		boolean isConnected = Utils.checkNetwork(this);
		if (!isConnected) {
			mImgRefresh.setVisibility(View.VISIBLE);
			mProgressBar.setVisibility(View.INVISIBLE);
			Toast.makeText(this,
					this.getString(R.string.message_network_error),
					Toast.LENGTH_LONG).show();
		} else {
			mTask = new LoadHtmlTask(this, true, finishListener);
			mTask.execute(mRecipeItem);
		}

	}

	private OnClickListener clickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (((CheckBox) v).isChecked()) {
				// Add to favorites
				addToFavorites(mRecipeItem);
			} else {
				// remove from favorites
				removeFromFavorites(mRecipeItem);
			}

		}

	};

	private OnFinishListener finishListener = new OnFinishListener() {

		@Override
		public void onFinish() {
			RecipeDetail recipeDetail = getDetailFromDB(mRecipeItem.getTitle(),
					mRecipeItem.getLink());
			mRecipeItem.setRecipeDetail(recipeDetail);
			mIsFavorites = isFavorites(mRecipeItem);
			updateViewContent(mIsFavorites, recipeDetail);
			mImgRefresh.setVisibility(View.VISIBLE);
			mProgressBar.setVisibility(View.INVISIBLE);

		}

	};

	private RecipeDetail getDetailFromDB(String title, String link) {
		String selection = Constants.COLUMN_TITLE + " = '" + title + "' AND "
				+ Constants.COLUMN_LINK + " = '" + link + "'";
		Cursor c = null;
		switch (mType) {
		case Constants.TYPE_DAILY_RECIPE:
			c = getContentResolver().query(
					Constants.RECIPES_CONTENT_URI,
					new String[] { Constants.COLUMN_DETAIL_IMAGE,
							Constants.COLUMN_SERVES,
							Constants.COLUMN_INGREDIENT,
							Constants.COLUMN_METHOD }, selection, null, null);
			break;
		case Constants.TYPE_FAVORITES:
			c = getContentResolver().query(
					Constants.FAVORITES_CONTENT_URI,
					new String[] { Constants.COLUMN_DETAIL_IMAGE,
							Constants.COLUMN_SERVES,
							Constants.COLUMN_INGREDIENT,
							Constants.COLUMN_METHOD }, selection, null, null);
		}

		if (c != null) {

			c.moveToFirst();
			String image_url = c.getString(c
					.getColumnIndex(Constants.COLUMN_DETAIL_IMAGE));
			String serves = c.getString(c
					.getColumnIndex(Constants.COLUMN_SERVES));
			String ingredients = c.getString(c
					.getColumnIndex(Constants.COLUMN_INGREDIENT));
			String method = c.getString(c
					.getColumnIndex(Constants.COLUMN_METHOD));
			if (ingredients != null || method != null) {
				RecipeDetail detail = new RecipeDetail(image_url, serves,
						ingredients, method);
				return detail;

			}
			c.close();
		}
		return null;

	}

	private boolean isFavorites(RecipeItem item) {
		String selection = Constants.COLUMN_TITLE + " = '" + item.getTitle()
				+ "' AND " + Constants.COLUMN_LINK + " = '" + item.getLink()
				+ "'";
		Cursor c = getContentResolver().query(Constants.FAVORITES_CONTENT_URI,
				new String[] { Constants.COLUMN_TITLE }, selection, null, null);
		if (c != null && c.getCount() != 0) {
			return true;
		}
		return false;
	}

	private void updateViewContent(boolean isFavorite, RecipeDetail recipeDetail) {

		mBtnFavorites.setChecked(isFavorite);
		if (recipeDetail != null) {
			String ingre = recipeDetail.getIngredients();
			String method = recipeDetail.getMethod();
			mTextviewServes.setText(recipeDetail.getServes());

			imageLoader.loadBitmap(recipeDetail.getImageUrl(), null,
					mImageView, mBackgroundBmp, mImageView.getWidth(),
					mImageView.getHeight());
			ingredientsView.loadDataWithBaseURL("", ingre, "text/html",
					"UTF-8", "");

			methodView
					.loadDataWithBaseURL("", method, "text/html", "UTF-8", "");
			int dummyLen = Constants.DUMMY_HTML.length();
			if (ingre.length() == dummyLen && method.length() == dummyLen) {
				mBtnFavorites.setVisibility(View.INVISIBLE);
				Toast.makeText(this,
						this.getString(R.string.message_get_detail_error),
						Toast.LENGTH_LONG).show();

			} else {
				mBtnFavorites.setVisibility(View.VISIBLE);
			}

		}

	}

	private boolean addToFavorites(RecipeItem item) {
		ContentValues values = new ContentValues();
		values.put(Constants.COLUMN_TITLE, item.getTitle());
		values.put(Constants.COLUMN_IMAGE, item.getImageUrl());
		values.put(Constants.COLUMN_LINK, item.getLink());
		values.put(Constants.COLUMN_PUBDATE, item.getPubDate());

		RecipeDetail detail = item.getRecipeDetail();
		if (detail != null) {
			values.put(Constants.COLUMN_DETAIL_IMAGE, detail.getImageUrl());
			values.put(Constants.COLUMN_SERVES, detail.getServes());
			values.put(Constants.COLUMN_INGREDIENT, detail.getIngredients());
			values.put(Constants.COLUMN_METHOD, detail.getMethod());
		} else {
			values.putNull(Constants.COLUMN_DETAIL_IMAGE);
			values.putNull(Constants.COLUMN_SERVES);
			values.putNull(Constants.COLUMN_INGREDIENT);
			values.putNull(Constants.COLUMN_METHOD);
		}
		Uri uri = getContentResolver().insert(Constants.FAVORITES_CONTENT_URI,
				values);
		if (uri != null) {
			return true;
		}
		return false;
	}

	private boolean removeFromFavorites(RecipeItem item) {
		String where = Constants.COLUMN_TITLE + " = '" + item.getTitle()
				+ "' AND " + Constants.COLUMN_LINK + " = '" + item.getLink()
				+ "'";
		int cnt = getContentResolver().delete(Constants.FAVORITES_CONTENT_URI,
				where, null);
		if (cnt != 0) {
			return true;
		}
		return false;
	}

}
