package com.itcast.cloudstorage.upload.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

import com.itcast.cloudstorage.R;
import com.itcast.cloudstorage.bean.Event;
import com.itcast.cloudstorage.net.CloudEngine;
import com.itcast.cloudstorage.net.ExceptionHandler;
import com.itcast.cloudstorage.utils.CloudAsyncTask;
import com.itcast.cloudstorage.utils.Logger;
import com.vdisk.net.ProgressListener;
import com.vdisk.net.VDiskAPI;
import com.vdisk.net.VDiskAPI.Entry;
import com.vdisk.net.VDiskAPI.UploadRequest;
import com.vdisk.net.exception.VDiskException;
import com.vdisk.net.exception.VDiskPartialFileException;

/**
 * Here we show uploading a file in a background thread, trying to show typical
 * exception handling and flow of control for an app that uploads a file from
 * VDisk.
 */
public class SimpleUploadAsyncTask extends
		CloudAsyncTask<Object[], Long, Integer> {
	private VDiskAPI<?> mApi;
	private String mPath;
	private File mFile;

	private long mFileLen;
	private UploadRequest mRequest;
	private Context mContext;

	private String mErrorMsg;
	private SimpleUploadTask mSimpleUploadTask;
	private Notification mUploadNotification;
	private NotificationManager mUploadNotificationManager;
	private RemoteViews mContentView;

	private long mCurrentLength;

	private static final int UPLOAD_NOTIFY_ID = 1;
	private static final int UPLOAD_CANCELLED = 2;
	private static final int UPLOAD_SUCCESS = 3;
	private static final int UPLOAD_FAILED = 4;
	private static final int PROGRESS_INTERVAL = 500;

	private static final String TAG = "SimpleUploadAsyncTask";

	private long mSpeed;
	private boolean mIsBackup;
	private int mErrorCode;
	private Entry mEntry;

	public SimpleUploadAsyncTask(Context ctx) {
		this.mContext = ctx.getApplicationContext();
		this.mApi = CloudEngine.getInstance(mContext).getApi(mContext);
	}

	@Override
	protected Integer doInBackground(Object[]... params) {
		mSimpleUploadTask = (SimpleUploadTask) params[0][0];
		mUploadNotification = (Notification) params[0][1];
		mUploadNotificationManager = (NotificationManager) params[0][2];

		if (mUploadNotification == null || mUploadNotificationManager == null) {
			mIsBackup = true;
		} else {
			mContentView = mUploadNotification.contentView;
		}

		mFileLen = mSimpleUploadTask.fileSize;
		mPath = mSimpleUploadTask.desPath;
		mFile = new File(mSimpleUploadTask.srcPath);

		try {
			// By creating a request, we get a handle to the putFile operation,
			// so we can cancel it later if we want to
			FileInputStream fis = new FileInputStream(mFile);

			if (!mPath.endsWith("/")) {
				mPath = mPath + "/";
			}

			String path = mPath + UploadManager.fileNameFilter(mFile.getName());
			mRequest = mApi.putFileOverwriteRequest(path, fis, mFile.length(),
					new ProgressListener() {
						@Override
						public long progressInterval() {
							// Update the progress bar every half-second or so
							return PROGRESS_INTERVAL;
						}

						@Override
						public void onProgress(long bytes, long total) {
							Logger.d("Test", bytes + "/" + total);
							// if (!mIsBackup) {
							publishProgress(bytes);
							// }
						}
					});

			mSimpleUploadTask.assertCanceled();
			if (mRequest != null) {
				mEntry = mRequest.upload();
				Log.e("UPLOAD", mEntry.path);
				return UPLOAD_SUCCESS;
			}
		} catch (VDiskPartialFileException e) {
			// We canceled the operation
			mErrorMsg = "Upload canceled";
			return UPLOAD_CANCELLED;
		} catch (VDiskException e) {
			e.printStackTrace();
			Event event = CloudEngine.updateEvent(mContext, e, null);
			mErrorMsg = ExceptionHandler.getErrMsgByErrCode(event.errCode,
					mContext);
			mErrorCode = event.errCode;
		} catch (FileNotFoundException e) {
			mErrorMsg = mContext
					.getString(R.string.local_source_file_is_deleted);
		}

		return UPLOAD_FAILED;
	}

	@Override
	protected void onProgressUpdate(Long... progress) {
		int percent = (int) (100.0 * (double) progress[0] / mFileLen + 0.5);
		Logger.d("Test", "percent-->" + percent);

		if (percent >= 98) {
			percent = 98;
		}

		if (mCurrentLength > 0 && mCurrentLength < progress[0]) {
			mSpeed = (progress[0] - mCurrentLength) * 1000 / PROGRESS_INTERVAL;
		}

		mCurrentLength = progress[0];

		mContentView.setTextViewText(R.id.downloadSumText, percent + "%");
		mContentView.setProgressBar(R.id.downloadProgress, 100, percent, false);
		mUploadNotificationManager
				.notify(UPLOAD_NOTIFY_ID, mUploadNotification);

		// 发送当前进度
		mSimpleUploadTask.fileprogress = percent;

		if (mSpeed != 0) {
			for (UploadManager.UploadStatusListener l : UploadManager
					.getInstance(mContext).getUploadStatusListeners()) {
				l.onProgress(mSimpleUploadTask, mSpeed);
			}
		}

		super.onProgressUpdate(progress);
	}

	@Override
	protected void onPostExecute(Integer result) {
		switch (result) {
		case UPLOAD_SUCCESS:
			uploadSuccess();
			break;
		case UPLOAD_CANCELLED:
			uploadCancelled();
			break;
		case UPLOAD_FAILED:
			uploadFailed();
			break;
		default:
			break;
		}
	}

	public void cancel() {
		if (mRequest != null) {
			// 4.0系统网络操作需放到子线程执行，避免发生NetworkOnMainThreadException异常。
			new Thread() {
				@Override
				public void run() {
					mRequest.abort();
				}
			}.start();
		}
	}

	private void uploadCancelled() {
		Logger.d(TAG, "simple upload canceled");
		if (!mIsBackup) {
			// String tips = String.format(
			// mContext.getString(R.string.upload_cancel_toast),
			// mSimpleUploadTask.filename);

			UploadManager.getInstance(mContext).startNextUpload(
					mSimpleUploadTask, mUploadNotification,
					mUploadNotificationManager);

			for (UploadManager.UploadStatusListener l : UploadManager
					.getInstance(mContext).getUploadStatusListeners()) {
				l.onCancel(mSimpleUploadTask);
			}

			// Utils.showToastString(mContext, tips, 0);
		}
	}

	private void uploadFailed() {
		Logger.d(TAG, "simple upload failed");
		UploadManager.getInstance(mContext).uploadRetry(mSimpleUploadTask,
				mUploadNotification, mUploadNotificationManager, mErrorMsg);

	}

	private void uploadSuccess() {
		Logger.d(TAG, "simple upload success");
		mContentView.setTextViewText(R.id.downloadSumText, 100 + "%");
		mContentView.setProgressBar(R.id.downloadProgress, 100, 100, false);
		mUploadNotificationManager
				.notify(UPLOAD_NOTIFY_ID, mUploadNotification);
		// 发送当前进度
		mSimpleUploadTask.fileprogress = 100;
		mSimpleUploadTask.sha1 = mEntry.sha1;

		UploadManager.getInstance(mContext).uploadCallback(mSimpleUploadTask,
				UploadManager.UPLOAD_SUCCESS);

		UploadManager.getInstance(mContext).startNextUpload(mSimpleUploadTask,
				mUploadNotification, mUploadNotificationManager);

		for (UploadManager.UploadStatusListener l : UploadManager.getInstance(
				mContext).getUploadStatusListeners()) {
			l.onSuccess(mEntry, mSimpleUploadTask);
		}
	}
}
