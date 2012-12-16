package com.zw.dailyrecipe.provider;


import com.zw.dailyrecipe.Constants;
import com.zw.dailyrecipe.utils.LOG;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class RecipeContentProvider extends ContentProvider {
	private static final String DEBUG_TAG = "RecipeContentProvider";
	private static final String DATABASE_NAME = "recipes.db";
	private static final int DATABASE_VERSION = 1;
	private static final String RECIPES_TABLE_NAME = "recipes";
	private static final String FAVORITES_TABLE_NAME = "favorites";
	private DatabaseHelper mDBHelper;
	private SQLiteDatabase db;
	private static final int TYPE_RECIPES = 0;
	private static final int TYPE_FAVORITES = 1;
	private static final int TYPE_LAST_BUILD_DATE = 2;
	private static final int TYPE_TRANSACTION = 3;
	private static final int TYPE_COMMIT = 4;
	private static final int TYPE_ROLLBACK =5;
	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(Constants.AUTHORITY, RECIPES_TABLE_NAME,
				TYPE_RECIPES);
		sURIMatcher.addURI(Constants.AUTHORITY, RECIPES_TABLE_NAME + "/"
				+ Constants.COLUMN_LAST_BUILD_DATE, TYPE_LAST_BUILD_DATE);
		sURIMatcher.addURI(Constants.AUTHORITY, FAVORITES_TABLE_NAME,
				TYPE_FAVORITES);
		sURIMatcher.addURI(Constants.AUTHORITY, Constants.PATH_TRANSACTION,
				TYPE_TRANSACTION);
		sURIMatcher.addURI(Constants.AUTHORITY, Constants.PATH_COMMIT,
				TYPE_COMMIT);
		sURIMatcher.addURI(Constants.AUTHORITY, Constants.PATH_ROLLBACK,
				TYPE_ROLLBACK);
		
	}

	private class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// create the recipe database
			db.execSQL("CREATE TABLE " + RECIPES_TABLE_NAME + " (" + Constants.COLUMN_ID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ Constants.COLUMN_TITLE + " text not null, " 
					+ Constants.COLUMN_IMAGE + " text not null, "
					+ Constants.COLUMN_LINK + " text not null, "
					+ Constants.COLUMN_PUBDATE + " long not null, "
					+ Constants.COLUMN_DETAIL_IMAGE + " text, "
					+ Constants.COLUMN_SERVES + " text, "
					+ Constants.COLUMN_INGREDIENT + " text, " 
					+ Constants.COLUMN_METHOD + " text,"
					+ Constants.COLUMN_LAST_BUILD_DATE + " LONG not null );");
			// create the Favorites database
			db.execSQL("CREATE TABLE " + FAVORITES_TABLE_NAME + " (" + Constants.COLUMN_ID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ Constants.COLUMN_TITLE + " text not null, " 
					+ Constants.COLUMN_IMAGE + " text not null, "
					+ Constants.COLUMN_LINK + " text not null, "
					+ Constants.COLUMN_PUBDATE + " long not null, "
					+ Constants.COLUMN_DETAIL_IMAGE + " text, "
					+ Constants.COLUMN_SERVES + " text, "
					+ Constants.COLUMN_INGREDIENT + " text, " 
					+ Constants.COLUMN_METHOD + " text);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVertion, int newVertion) {
			LOG.w(DEBUG_TAG,
					"Upgrading database. Existing contents will be lost. ["
							+ oldVertion + "]->[" + newVertion + "]");
			db.execSQL("DROP TABLE IF EXISTS " + RECIPES_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + FAVORITES_TABLE_NAME);
			onCreate(db);
		}

	}

	@Override
	public int delete(Uri uri, String whereClause, String[] whereArgs) {
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case TYPE_RECIPES:
			return db.delete(RECIPES_TABLE_NAME, whereClause, whereArgs);
		case TYPE_FAVORITES:
			return db.delete(FAVORITES_TABLE_NAME, whereClause, whereArgs);
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long rowId = 0;
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case TYPE_RECIPES:
			rowId = db.insert(RECIPES_TABLE_NAME, null, values);
			if (rowId > 0) {
				return ContentUris.withAppendedId(
						Constants.RECIPES_CONTENT_URI, rowId);
			}else{
				return null;
			}
		
		case TYPE_FAVORITES:
			rowId = db.insert(FAVORITES_TABLE_NAME, null, values);
			if (rowId > 0) {
				return ContentUris.withAppendedId(
						Constants.FAVORITES_CONTENT_URI, rowId);
			}else{
				return null;
			}
		case TYPE_TRANSACTION:
			db.beginTransaction();
			break;
		case TYPE_COMMIT:
			db.setTransactionSuccessful();
			db.endTransaction();
			break;
		case TYPE_ROLLBACK:
			db.endTransaction();
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return null;
	}

	@Override
	public boolean onCreate() {
		mDBHelper = new DatabaseHelper(getContext());
		db = mDBHelper.getWritableDatabase();

		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case TYPE_RECIPES:
			return db.query(RECIPES_TABLE_NAME, null, selection, selectionArgs,
					null, null, sortOrder);
		case TYPE_FAVORITES:
			return db.query(FAVORITES_TABLE_NAME, null, selection,
					selectionArgs, null, null, sortOrder);
		case TYPE_LAST_BUILD_DATE:
			return db.query(true, RECIPES_TABLE_NAME,
					new String[] { Constants.COLUMN_LAST_BUILD_DATE },
					selection, selectionArgs, null, null, sortOrder, null);

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case TYPE_RECIPES:
			return db.update(RECIPES_TABLE_NAME, values, selection,
					selectionArgs);

		case TYPE_FAVORITES:
			return db.update(FAVORITES_TABLE_NAME, values, selection,
					selectionArgs);

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

}
