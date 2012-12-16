package com.zw.dailyrecipe.recipe;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 
 * Recipe detail Class
 *
 */
public class RecipeDetail implements Parcelable {
	private String mIngredientsHtml;
	private String mMethodHtml;
	private String mServes;
	private String mImageUrl;

	public RecipeDetail() {
	}

	public RecipeDetail(String imageUrl, String serves, String ingredientsHtml,
			String methodHtml) {
		mImageUrl = imageUrl;
		mServes = serves;
		mIngredientsHtml = ingredientsHtml;
		mMethodHtml = methodHtml;

	}

	public String getIngredients() {
		return mIngredientsHtml;
	}

	public String getMethod() {
		return mMethodHtml;
	}

	public String getServes() {
		return mServes;
	}

	public String getImageUrl() {
		return mImageUrl;
	}

	public void setIngredientsHtml(String ingredientsHtml) {
		mIngredientsHtml = ingredientsHtml;
	}

	public void setMethodHtml(String methodHtml) {
		mMethodHtml = methodHtml;
	}

	public void setServes(String serves) {
		mServes = serves;
	}

	public void setImageUrl(String url) {
		mImageUrl = url;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mIngredientsHtml);
		dest.writeString(mMethodHtml);
		dest.writeString(mServes);
		dest.writeString(mImageUrl);
	}

	public RecipeDetail(Parcel in) {
		mIngredientsHtml = in.readString();
		mMethodHtml = in.readString();
		mServes = in.readString();
		mImageUrl = in.readString();

	}

	public static final Parcelable.Creator<RecipeDetail> CREATOR = new Parcelable.Creator<RecipeDetail>() {
		public RecipeDetail createFromParcel(Parcel in) {
			return new RecipeDetail(in);
		}

		public RecipeDetail[] newArray(int size) {
			return new RecipeDetail[size];
		}
	};
}
