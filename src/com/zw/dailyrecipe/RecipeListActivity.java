package com.zw.dailyrecipe;

import java.util.ArrayList;
import java.util.List;

import com.zw.dailyrecipe.R;
import com.zw.dailyrecipe.parsertask.LoadHtmlTask;
import com.zw.dailyrecipe.parsertask.OnFinishListener;
import com.zw.dailyrecipe.parsertask.ParseRssTask;
import com.zw.dailyrecipe.recipe.RecipeDetail;
import com.zw.dailyrecipe.recipe.RecipeItem;
import com.zw.dailyrecipe.utils.ImageDownloader;

import android.os.AsyncTask;
import android.os.Bundle;

import android.app.Activity;
import android.app.ProgressDialog;

import android.content.Intent;
import android.database.Cursor;

import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Common list class (Recipes & Favorites)
 * <p>
 * <b>Intent parameters:</b>
 * <dl>
 * <dt>{@link  Constants#NAME_EXTRA_DATA_TYPE}:String
 * <dd>List Type (Recipes or Favorites).
 * </dl>
 */
public class RecipeListActivity extends Activity {
	private static final String TAG = "RecipeListActivity";

	private List<RecipeItem> mRecipeList;
	private RecipeAdapter mAdapter;
	private int mType;
	private String mRssUrl;
	private ImageView mImgRefresh;
	private ProgressBar mProgressBar;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list);

		mRssUrl = getString(R.string.rss_daily_recipes);
		//Get list type from intent.
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mType = extras.getInt(Constants.NAME_EXTRA_DATA_TYPE,
					Constants.TYPE_DAILY_RECIPE);
		} else {
			mType = Constants.TYPE_DAILY_RECIPE;
		}
		
		initViews();


	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			if (mType == Constants.TYPE_DAILY_RECIPE) {				
				finish();
				moveTaskToBack(true);

				return true;
			}

		}
		return super.onKeyDown(keyCode, event);
	}
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		// update list
		if (mType == Constants.TYPE_FAVORITES) {
			mImgRefresh.setVisibility(View.INVISIBLE);
			mProgressBar.setVisibility(View.VISIBLE);
			GetFavoritesTask task = new GetFavoritesTask();
			task.execute();
		} else {
			mRecipeList = getRecipeListFromDB();
			mAdapter.setRecipelist(mRecipeList);
		}
		super.onResume();
	}

	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
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

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
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


	/*
	 * List item click listener
	 */
	private OnItemClickListener clickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Intent i = new Intent(view.getContext(), RecipeDetailActivity.class);
			i.putExtra(Constants.NAME_EXTRA_DATA_ITEM,
					mRecipeList.get(position));
			i.putExtra(Constants.NAME_EXTRA_DATA_TYPE, mType);
			startActivity(i);

		}

	};

	/*
	 * Finishing listener of parsing RSS task 
	 * Invoke when parsing RSS task is finished.
	 */
	private OnFinishListener finishListener = new OnFinishListener() {

		@Override
		public void onFinish() {
			//refresh list
			mRecipeList = getRecipeListFromDB();
			mAdapter.setRecipelist(mRecipeList);

			mImgRefresh.setVisibility(View.VISIBLE);
			mProgressBar.setVisibility(View.INVISIBLE);
		}

	};

	

	/*
	 * initialize views
	 */
	private void initViews() {
		// title
		TextView textView = (TextView) findViewById(R.id.textview_title);
		// list view
		ListView listView = (ListView) findViewById(R.id.listView_recipe);
		// adapter
		mAdapter = new RecipeAdapter(this);
		listView.setAdapter(mAdapter);
		listView.setOnItemClickListener(clickListener);
		
		// [refresh] button image
		mImgRefresh = (ImageView) findViewById(R.id.image_refresh);
		mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
		FrameLayout btn_refresh = (FrameLayout) findViewById(R.id.btn_refresh_layout);
		
		// left button image
		ImageView leftImage = (ImageView) findViewById(R.id.imageview_left);
		FrameLayout btn_left = (FrameLayout) findViewById(R.id.btn_left);
		

		switch (mType) {
		case Constants.TYPE_DAILY_RECIPE:
			// Set Title
			textView.setText(R.string.title_activity_list);
			// set the image of left button to display [ favorite list]
			leftImage.setBackgroundResource(R.drawable.btn_favorite_list);
			// Get recipe from DB
			mRecipeList = getRecipeListFromDB();
			//display recipe
			mAdapter.setRecipelist(mRecipeList);
			break;
		case Constants.TYPE_FAVORITES:
			// Set Title
			textView.setText(R.string.title_activity_favorites);
			// set the image of left button to display [ back to recipe list]
			leftImage.setBackgroundResource(R.drawable.btn_back);
			
			// start a task to get favorite list from DB
			GetFavoritesTask task = new GetFavoritesTask();
			task.execute();
			break;
		default:
			break;
		}

		btn_left.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				switch (mType) {
				case Constants.TYPE_DAILY_RECIPE:
					// open favorite list
					openFavorites();
					break;
				case Constants.TYPE_FAVORITES:
					//back to previous view
					RecipeListActivity.this.finish();
					break;
				default:
					break;
				}

			}

		});

		btn_refresh.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// button invisible/visible
				mImgRefresh.setVisibility(View.INVISIBLE);
				mProgressBar.setVisibility(View.VISIBLE);
				refreshList();

			}

		});

	}

	private void openFavorites() {
		Intent i = new Intent(this, RecipeListActivity.class);
		i.putExtra(Constants.NAME_EXTRA_DATA_TYPE, Constants.TYPE_FAVORITES);
		startActivity(i);
	}

	private void refreshList() {
		switch (mType) {
		case Constants.TYPE_DAILY_RECIPE:
			// show dialog
			ParseRssTask downloadTask = new ParseRssTask(this,  true,
					finishListener);
			downloadTask.execute(mRssUrl);
			break;
		case Constants.TYPE_FAVORITES:
			GetFavoritesTask task = new GetFavoritesTask();
			task.execute();
			break;
		default:
			break;
		}

	}

	private ArrayList<RecipeItem> getRecipeListFromDB() {

		Cursor cursor = getContentResolver().query(
				Constants.RECIPES_CONTENT_URI, null, null, null,
				Constants.COLUMN_PUBDATE + " DESC");
		return readFromCursor(cursor);
	}

	private ArrayList<RecipeItem> readFromCursor(Cursor c) {
		if (c != null) {
			ArrayList<RecipeItem> list = new ArrayList<RecipeItem>();
			c.moveToFirst();
			int count = c.getCount();
			for (int i = 0; i < count; i++) {
				RecipeItem item = new RecipeItem();
				item.setTitle(c.getString(c
						.getColumnIndex(Constants.COLUMN_TITLE)));
				item.setImageUrl(c.getString(c
						.getColumnIndex(Constants.COLUMN_IMAGE)));
				item.setLink(c.getString(c
						.getColumnIndex(Constants.COLUMN_LINK)));
				item.setPubdate(c.getLong(c
						.getColumnIndex(Constants.COLUMN_PUBDATE)));
				String detail_image_url = c.getString(c
						.getColumnIndex(Constants.COLUMN_DETAIL_IMAGE));
				String serves = c.getString(c
						.getColumnIndex(Constants.COLUMN_SERVES));
				String ingredients = c.getString(c
						.getColumnIndex(Constants.COLUMN_INGREDIENT));
				String method = c.getString(c
						.getColumnIndex(Constants.COLUMN_METHOD));
				if (detail_image_url == null || ingredients == null
						|| method == null) {
					// Start parsing Html;
					LoadHtmlTask htmlTask = new LoadHtmlTask(this, 
							false, null);
					htmlTask.execute(item);

				} else {
					item.setRecipeDetail(new RecipeDetail(detail_image_url,
							serves, ingredients, method));
				}

				// Add to list
				list.add(item);
				c.moveToNext();
			}
			c.close();
			return list;
		}
		return null;
	}

	// ------Get Favorites list --------//
	class GetFavoritesTask extends AsyncTask<Void, Void, List<RecipeItem>> {
		private ProgressDialog progressDialog = new ProgressDialog(
				RecipeListActivity.this);

		@Override
		protected void onPreExecute() {
			progressDialog.setMessage(RecipeListActivity.this.getString(R.string.message_progress));
			progressDialog.show();
		}

		@Override
		protected List<RecipeItem> doInBackground(Void... params) {
			return getFavoriteRecipe();

		}

		@Override
		protected void onPostExecute(List<RecipeItem> result) {
			if (result != null) {
				mRecipeList = result;
				mAdapter.setRecipelist(result);
			}
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
			
			mImgRefresh.setVisibility(View.VISIBLE);
			mProgressBar.setVisibility(View.INVISIBLE);
		}

	}

	/*
	 * Get favorites list from DB
	 */
	private List<RecipeItem> getFavoriteRecipe() {
		Cursor cursor = getContentResolver().query(
				Constants.FAVORITES_CONTENT_URI, null, null, null,
				Constants.COLUMN_ID + " DESC");
		return readFromCursor(cursor);
	}



}
