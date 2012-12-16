package com.zw.dailyrecipe.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import java.net.URL;
import java.util.LinkedHashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import com.zw.dailyrecipe.Constants;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import android.support.v4.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

/**
 * Async image downloader
 *
 */
public class ImageDownloader {
	private final static String TAG = "ImageDownloader";
	private static Context mContext;
	public static LruCache<String, Bitmap> mLruCache;
	public static File mCacheDir;
	private int width;
	private int height;
	private static final int SOFT_CACHE_CAPACITY = 40;
	// Soft cache for bitmaps kicked out of hard cache
	private final static LinkedHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache = new LinkedHashMap<String, SoftReference<Bitmap>>(
			SOFT_CACHE_CAPACITY, 0.75f, true) {

		private static final long serialVersionUID = 1L;

		@Override
		public SoftReference<Bitmap> put(String key, SoftReference<Bitmap> value) {
			return super.put(key, value);
		}

		@Override
		protected boolean removeEldestEntry(
				Entry<String, SoftReference<Bitmap>> eldest) {
			if (size() > SOFT_CACHE_CAPACITY) {
				LOG.d(TAG, "Soft Reference limit , purge one");
				return true;
			}
			return false;

		}

	};

	public ImageDownloader(Context c) {
		mContext = c;
		final int memClass = ((ActivityManager) c
				.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

		// Use 1/8th of the available memory for this memory cache.
		final int cacheSize = 1024 * 1024 * memClass / 8;
		mCacheDir = getCacheDirectory(mContext);
		mLruCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return (bitmap.getRowBytes() * bitmap.getHeight());
			}

			@Override
			protected void entryRemoved(boolean evicted, String key,
					Bitmap oldValue, Bitmap newValue) {
				LOG.d(TAG, "hard cache is full , push to soft cache");
				sSoftBitmapCache.put(key, new SoftReference<Bitmap>(oldValue));
			}
		};

	}

	/**
	 * Download the specified image from the Internet and binds it to the
	 * provided ImageView. The binding is immediate if the image is found in the
	 * cache and will be done asynchronously otherwise. A error bitmap will be
	 * associated to the ImageView if an error occurs.
	 * 
	 * @param url
	 *            The URL of the image to download.
	 * @param optionalUrl
	 *            The URL of the image of detail recipe
	 * @param imageView
	 *            The ImageView to bind the downloaded image to.
	 * @param backgroundBmp
	 * 
	 */

	public void loadBitmap(String url, String optionalUrl, ImageView imageView,
			Bitmap backgroundBmp, int width, int height) {
		this.width = width;
		this.height = height;
		// Is the bitmap in cache ?
		final Bitmap bitmap = getBitmapFromCache(url);
		if (bitmap != null) {
			// Yes.
			cancelPotentialWork(url, imageView);
			displayImage(bitmap, imageView);
		} else {
			// Is the detail bitmap in cache
			final Bitmap fileCacheBitmap = getBitmapFromCache(optionalUrl);

			if (fileCacheBitmap != null) {
				// Yes
				cancelPotentialWork(url, imageView);
				displayImage(fileCacheBitmap, imageView);
			} else {
				// No. download from Internet
				if (cancelPotentialWork(url, imageView)) {
					final BitmapWorkerTask task = new BitmapWorkerTask(
							imageView);
					final AsyncDrawable asyncDrawable = new AsyncDrawable(task,
							backgroundBmp);
					imageView.setImageDrawable(asyncDrawable);

					task.execute(url);
				}

			}

		}
	}

	public static Bitmap getBitmapFromCache(String url) {
		if (url == null) {
			return null;
		}
		// get from memory cache
		final Bitmap bitmap = getBitmapFromMemCache(url);
		if (bitmap != null) {
			return bitmap;
		} else {
			// get from file cache
			String filename = String.valueOf(url.hashCode());
			if (mCacheDir == null) {
				mCacheDir = getCacheDirectory(mContext);
			}
			File f = new File(mCacheDir, filename);
			Bitmap fileCacheBitmap = BitmapFactory.decodeFile(f.getPath());
			if (fileCacheBitmap != null) {
				return fileCacheBitmap;
			}
		}
		return null;
	}

	private void displayImage(Bitmap bitmap, ImageView imageView) {
		if (imageView != null) {
			imageView.setImageBitmap(bitmap);
		}

		imageView.setVisibility(View.VISIBLE);
		ProgressBar progressBar = (ProgressBar) imageView.getTag();
		if (progressBar != null) {
			progressBar.setVisibility(View.GONE);
		}

	}

	private static File getCacheDirectory(Context context) {
		String sdState = android.os.Environment.getExternalStorageState();
		File cacheDir;

		if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
			File sdDir = android.os.Environment.getExternalStorageDirectory();
			cacheDir = new File(sdDir, "data/com.zw.jamieoliver.dailyrecipe");
		} else
			cacheDir = context.getCacheDir();

		if (!cacheDir.exists())
			cacheDir.mkdirs();
		return cacheDir;
	}

	private void writeFile(Bitmap bmp, File f) {
		FileOutputStream out = null;

		try {
			out = new FileOutputStream(f);
			bmp.compress(Bitmap.CompressFormat.PNG, 80, out);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (Exception ex) {
			}
		}
	}

	class BitmapWorkerTask extends AsyncTask<String, Integer, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;
		private String url;

		public BitmapWorkerTask(ImageView imageView) {
			// Use a WeakReference to ensure the ImageView can be garbage
			// collected
			imageViewReference = new WeakReference<ImageView>(imageView);
			
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		
		}

		// Decode image in background
		@Override
		protected Bitmap doInBackground(String... params) {
			
			url = params[0];
			try {

				final Bitmap bitmap = getBitmap(params[0]);
//				if(bitmap == null) {
//					String newUrl =params[0].replace("sml", "resized/por");
//					
//					final Bitmap bitmap2 = getBitmap(newUrl);
//					// Memory cache
//					//addBitmapToMemoryCache(params[0], bitmap2);
//					return bitmap2;
//				} else {
//					// Memory cache
//					addBitmapToMemoryCache(params[0], bitmap);
//					return bitmap;
//				}
//				
				// Memory cache
				addBitmapToMemoryCache(params[0], bitmap);
				return bitmap;
			

			} catch (URISyntaxException e) {
				LOG.e(TAG, "loadBitmap : URISyntaxException", e);
			} catch (MalformedURLException e) {
				LOG.e(TAG, "loadBitmap : URL error", e);
			} catch (IOException e) {
				LOG.e(TAG, "loadBitmap :connection error", e);

			} 

			return null;
		}

		
		// Once complete, see if ImageView is still around and set bitmap.
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (imageViewReference != null) {
				final ImageView imageView = (ImageView) imageViewReference
						.get();
				if (bitmap != null) {
					final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
					if (this == bitmapWorkerTask && imageView != null) {
						displayImage(bitmap, imageView);

						// file cache
						String filename = String.valueOf(url.hashCode());
						if (mCacheDir == null) {
							mCacheDir = getCacheDirectory(imageView
									.getContext());
						}
						File f = new File(mCacheDir, filename);
						writeFile(bitmap, f);

					}

				}
			
			}
		}

	}

	private Bitmap getBitmap(String url) throws URISyntaxException,
			ClientProtocolException, IOException {
		HttpGet request = null;
		InputStream is = null;
		Bitmap bitmap = null;
		try {
			request = new HttpGet(new URL(url).toURI());
			final HttpParams httpparams = new BasicHttpParams();
			httpparams.setIntParameter(AllClientPNames.CONNECTION_TIMEOUT,
					Constants.TIME_OUT);
			httpparams.setIntParameter(AllClientPNames.SO_TIMEOUT,
					Constants.TIME_OUT);

			final HttpClient client = new DefaultHttpClient(httpparams);
			
			HttpResponse response = (HttpResponse) client.execute(request);

			is = new BufferedHttpEntity(response.getEntity()).getContent();
			

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(is, null, options);
			if (height < 1 || width < 1) {
				options.inJustDecodeBounds = false;
			} else {
				final int scaleW = options.outWidth / width + 1;
				final int scaleH = options.outHeight / height + 1;
				final int scale = Math.max(scaleW, scaleH);
				if (0 < scale) {
					options.inSampleSize = scale;
					LOG.d(TAG, "inSampleSize =" + scale );
				}

				options.inJustDecodeBounds = false;
			}
			
			is.reset();
			bitmap = BitmapFactory.decodeStream(is, null, options);

		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					LOG.e(TAG,
							"loadBitmap : error occurs while closing the InputStream",
							e);
				}
			}

		}
		return bitmap;

	}

	static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(BitmapWorkerTask bitmapWorkerTask, Bitmap bmp) {
			super(bmp);
			bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(
					bitmapWorkerTask);

		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return (BitmapWorkerTask) bitmapWorkerTaskReference.get();
		}
	}

	public void addBitmapToMemoryCache(String url, Bitmap bitmap) {

		if (getBitmapFromMemCache(url) == null) {
			if (bitmap != null) {
				synchronized (mLruCache) {
					mLruCache.put(url, bitmap);
				}

			}
		}
	}

	public static Bitmap getBitmapFromMemCache(String url) {

		synchronized (mLruCache) {
			final Bitmap bitmap = mLruCache.get(url);
			if (bitmap != null)
				return bitmap;
		}

		synchronized (sSoftBitmapCache) {
			SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get(url);
			if (bitmapReference != null) {
				final Bitmap bitmap2 = bitmapReference.get();
				if (bitmap2 != null)
					return bitmap2;
				else {
					LOG.d(TAG, "soft reference has been recycled");
					sSoftBitmapCache.remove(url);
				}
			}
		}
		return null;

	}

	/**
	 * Clears the image cache used internally to improve performance. Note that
	 * for memory efficiency reasons, the cache will automatically be cleared
	 * after a certain inactivity delay.
	 */
	public static void clearCache() {

		// soft cache
		for (SoftReference<Bitmap> bm : sSoftBitmapCache.values()) {
			if (bm != null && bm.get() != null) {
				bm.get().recycle();
			}
		}
		sSoftBitmapCache.clear();

		// file cache
		if (mCacheDir != null) {
			if (mCacheDir.isDirectory()) {
				String[] children = mCacheDir.list();
				for (int i = 0; i < children.length; i++) {
					boolean b = new File(mCacheDir, children[i]).delete();
					LOG.d(TAG, children[i] + " " + b);
				}
			}
		}

	}

	public static void clearCache(String url) {
		if (url == null) {
			return;
		}

		SoftReference<Bitmap> sbm = sSoftBitmapCache.get(url);
		if (sbm != null && sbm.get() != null) {
			sbm.get().recycle();
		}
		// file cache
		if (mCacheDir != null) {
			new File(mCacheDir, String.valueOf(url.hashCode())).delete();

		}

	}

	public static boolean cancelPotentialWork(String url, ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null) {
			final String bitmapUrl = bitmapWorkerTask.url;
			if (bitmapUrl == null || (!bitmapUrl.equals(url))) {
				// Cancel previous task
				bitmapWorkerTask.cancel(true);
			} else {
				// The same work is already in progress
				return false;
			}
		}
		// No task associated with the ImageView, or an existing task was
		// cancelled
		return true;
	}

	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}
}
