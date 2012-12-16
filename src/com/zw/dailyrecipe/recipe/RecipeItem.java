package com.zw.dailyrecipe.recipe;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 
 * Recipe Item class
 *
 */
public class RecipeItem implements Parcelable {
	private String mImageUrl;
	private String mTitle;
	private String mLink;
	private RecipeDetail mRecipeDetail = null;
	private long mLastBuildData;
	private long mPubDate;
	
	public RecipeItem(){}
	public String getImageUrl() {
		return mImageUrl;
	}

	public String getTitle() {
		return mTitle;
	}

	public String getLink() {
		return mLink;
	}

	public RecipeDetail getRecipeDetail() {
		return mRecipeDetail;
	}

	public long getLastBuildData(){
		return mLastBuildData;
	}
	
	public long getPubDate(){
		return mPubDate;
	}
	
	
	public void setImageUrl(String url) {
		mImageUrl = url;
	}

	public void setTitle(String title) {
		mTitle = title;
	}

	public void setLink(String link) {
		mLink = link;
	}

	public void setRecipeDetail(RecipeDetail detail) {
		mRecipeDetail = null;
		mRecipeDetail = detail;
	}

	public void setLastBuildData(long data){
		mLastBuildData = data;
	}
	
	public void setPubdate(long  date){
		mPubDate = date;
	}
	

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(mImageUrl);
		out.writeString(mTitle);
		out.writeString(mLink);
		out.writeParcelable(mRecipeDetail, flags);
		out.writeLong(mLastBuildData);
		out.writeLong(mPubDate);

		
		


	}

	private RecipeItem(Parcel in) {
       mImageUrl = in.readString();
       mTitle = in.readString();
       mLink = in.readString();
       mRecipeDetail = in.readParcelable(RecipeDetail.class.getClassLoader());
       mLastBuildData = in.readLong();
       mPubDate = in.readLong();
      
    }
	
	

	public static final Parcelable.Creator<RecipeItem> CREATOR = new Parcelable.Creator<RecipeItem>() {
		public RecipeItem createFromParcel(Parcel in) {
			return new RecipeItem(in);
		}

		public RecipeItem[] newArray(int size) {
			return new RecipeItem[size];
		}
	};
}
