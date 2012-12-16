package com.zw.dailyrecipe;

import java.util.ArrayList;
import java.util.List;

import com.zw.dailyrecipe.R;
import com.zw.dailyrecipe.recipe.RecipeDetail;
import com.zw.dailyrecipe.recipe.RecipeItem;
import com.zw.dailyrecipe.utils.ImageDownloader;
import com.zw.dailyrecipe.utils.Utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class RecipeAdapter extends BaseAdapter {
	@SuppressWarnings("unused")
	private final static String TAG = "RecipeAdapter";
	private LayoutInflater mInflater;
	private List<RecipeItem> mRecipeItems = new ArrayList<RecipeItem>();
	private final ImageDownloader imageDownloader;
	private Bitmap mBackgroundBmp;
	private String mPattern;

	public RecipeAdapter(Context context) {
		mInflater = (LayoutInflater) context
				.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		imageDownloader = new ImageDownloader(context);
		mBackgroundBmp = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.bg_image);
		mPattern = context.getString(R.string.pattern_time);
	}

	@Override
	public int getCount() {
		return mRecipeItems.size();
	}

	@Override
	public Object getItem(int arg0) {
		return mRecipeItems.get(arg0);
	}

	@Override
	public long getItemId(int position) {
		return mRecipeItems.indexOf(getItem(position));
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.list_item, null);
			holder = new ViewHolder();
			holder.imageView = (ImageView) convertView
					.findViewById(R.id.recipe_img);
			holder.txtTitle = (TextView) convertView
					.findViewById(R.id.recipe_title);
			holder.txtDate = (TextView) convertView
					.findViewById(R.id.recipe_pubdate);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		RecipeItem item = (RecipeItem) getItem(position);
		RecipeDetail detail = item.getRecipeDetail();

		if (detail != null) {
			imageDownloader.loadBitmap(item.getImageUrl(),
					detail.getImageUrl(), holder.imageView, mBackgroundBmp,
					holder.imageView.getWidth(), holder.imageView.getHeight());
		} else {
			imageDownloader.loadBitmap(item.getImageUrl(), null,
					holder.imageView, mBackgroundBmp,
					holder.imageView.getWidth(), holder.imageView.getHeight());
		}

		holder.txtTitle.setText(item.getTitle());

		holder.txtDate.setText(Utils.ToDateString(item.getPubDate(), mPattern));
		return convertView;
	}

	private class ViewHolder {
		ImageView imageView;
		TextView txtTitle;
		TextView txtDate;
	}

	public void setRecipelist(List<RecipeItem> list) {
		mRecipeItems = list;
		this.notifyDataSetChanged();
	}

}
