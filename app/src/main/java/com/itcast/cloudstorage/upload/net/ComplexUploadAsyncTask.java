package com.itcast.cloudstorage.upload.net;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

import com.itcast.cloudstorage.R;
import com.itcast.cloudstorage.bean.Event;
import com.itcast.cloudstorage.bean.TaskInfo;
import com.itcast.cloudstorage.bean.UploadTask;
import com.itcast.cloudstorage.net.CloudEngine;
import com.itcast.cloudstorage.net.ExceptionHandler;
import com.itcast.cloudstorage.utils.CloudAsyncTask;
import com.itcast.cloudstorage.utils.CloudDB;
import com.itcast.cloudstorage.utils.Logger;
import com.vdisk.android.ComplexUploadHandler;
import com.vdisk.net.VDiskAPI;
import com.vdisk.net.VDiskAPI.Entry;
import com.vdisk.net.VDiskAPI.VDiskUploadFileInfo;
import com.vdisk.net.exception.VDiskException;
import com.vdisk.net.exception.VDiskPartialFileException;
import com.vdisk.utils.Digest;

/**
 * Here we show uploading a large file in a background thread, trying to show
 * typical exception handling and flow of control for an app that uploads a file
 * from VDisk.
 */
public class ComplexUploadAsyncTask extends
		CloudAsyncTask<Object[], Long, Integer> {

	private static final String TAG = "ComplexUploadAsyncTask";

	private VDiskAPI<?> mApi;
	private String mPath;

	private long mFileLen;
	private Context mContext;

	private CloudDB mDb;

	private String mErrorMsg;
	private String mSrcPath;
	private ComplexUploadHandler handler;

	private UploadTask mComplexUploadTask;

	private Notification mUploadNotification;
	private NotificationManager mUploadNotificationManager;
	private RemoteViews mContentView;
	// private UploadStatusListener mProgressListener;

	private static int UPLOAD_NOTIFY_ID = 1;
	private static final int UPLOAD_CANCELLED = 2;
	private static final int UPLOAD_SUCCESS = 3;
	private static final int UPLOAD_FAILED = 4;

	private static final int PROGRESS_INTERVAL = 500;

	private long mCurrentLength;
	private long mSpeed;

	private boolean mIsBackup;
	private int mErrorCode;
	private Entry mEntry;

	public ComplexUploadAsyncTask(Context context, UploadTask complexUploadTask) {
		// We set the context this way so we don't accidentally leak activities
		mContext = context.getApplicationContext();

		mComplexUploadTask = complexUploadTask;

		mSrcPath = mComplexUploadTask.srcPath;

		mFileLen = mComplexUploadTask.fileSize;
		mApi = CloudEngine.getInstance(mContext).getApi(mContext);

		mPath = mComplexUploadTask.desPath;

		mDb = CloudDB.getInstance(mContext);
	}

	@Override
	protected Integer doInBackground(Object[]... params) {
		mUploadNotification = (Notification) params[0][0];
		mUploadNotificationManager = (NotificationManager) params[0][1];

		if (mUploadNotification == null || mUploadNotificationManager == null) {
			mIsBackup = true;
		} else {
			mContentView = mUploadNotification.contentView;
		}

		try {
			if (!mPath.endsWith("/")) {
				mPath = mPath + "/";
			}

			String desPath = mPath
					+ UploadManager.fileNameFilter(mComplexUploadTask.filename);

			handler = new MyComplexUploadHandler(mContext);

			mApi.putLargeFileRequest(mSrcPath, desPath,
					mComplexUploadTask.fileSize, -1, false, null, handler);

			return UPLOAD_SUCCESS;

		} catch (VDiskPartialFileException e) {
			// We canceled the operation
			mErrorMsg = "Upload canceled";
			mComplexUploadTask.isCancel = true;
			return UPLOAD_CANCELLED;
		} catch (VDiskException e) {
			Event event = CloudEngine.updateEvent(mContext, e, null);
			mErrorMsg = ExceptionHandler.getErrMsgByErrCode(event.errCode,
					mContext);
			mErrorCode = event.errCode;
			e.printStackTrace();
		}

		return UPLOAD_FAILED;
	}

	@Override
	protected void onProgressUpdate(Long... progress) {
		int percent = (int) (100.0 * (double) progress[0] / mFileLen + 0.5);
		Log.d(TAG, "upload progress-->" + progress[0] + "/" + mFileLen);

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

		mComplexUploadTask.fileprogress = percent;

		Logger.d("Test", "complex percent-->" + mComplexUploadTask.fileprogress);
		if (mSpeed != 0) {
			// 发送当前进度
			for (UploadManager.UploadStatusListener l : UploadManager
					.getInstance(mContext).getUploadStatusListeners()) {
				l.onProgress(mComplexUploadTask, mSpeed);
			}
		}
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

	private void uploadCancelled() {
		if (!mIsBackup) {
			UploadManager.getInstance(mContext).startNextUpload(
					mComplexUploadTask, mUploadNotification,
					mUploadNotificationManager);

			for (UploadManager.UploadStatusListener l : UploadManager
					.getInstance(mContext).getUploadStatusListeners()) {
				l.onCancel(mComplexUploadTask);
			}
		}
	}

	private void uploadSuccess() {
		mContentView.setTextViewText(R.id.downloadSumText, "100%");
		mContentView.setProgressBar(R.id.downloadProgress, 100, 100, false);
		mUploadNotificationManager
				.notify(UPLOAD_NOTIFY_ID, mUploadNotification);

		mComplexUploadTask.fileprogress = 100;

		UploadManager.getInstance(mContext).uploadCallback(mComplexUploadTask,
				UploadManager.UPLOAD_SUCCESS);

		UploadManager.getInstance(mContext).startNextUpload(mComplexUploadTask,
				mUploadNotification, mUploadNotificationManager);

		for (UploadManager.UploadStatusListener l : UploadManager.getInstance(
				mContext).getUploadStatusListeners()) {
			l.onSuccess(mEntry, mComplexUploadTask);
		}
	}

	private void uploadFailed() {
		UploadManager.getInstance(mContext).uploadRetry(mComplexUploadTask,
				mUploadNotification, mUploadNotificationManager, mErrorMsg);
	}

	public void cancel() {
		if (handler != null) {
			handler.abort();
		}
	}

	class MyComplexUploadHandler extends ComplexUploadHandler {

		public MyComplexUploadHandler(Context ctx) {
			super(ctx);
		}

		@Override
		public void onProgress(long bytes, long total) {
			Logger.d("Test", bytes + "/" + total);
			// if (!mIsBackup) {
			publishProgress(bytes);
			// }
		}

		@Override
		public long progressInterval() {
			return PROGRESS_INTERVAL;
		}

		@Override
		public void startedWithStatus(ComplexUploadStatus status) {
			switch (status) {
			case ComplexUploadStatusLocateHost:
				Log.d(TAG, "Getting the nearest host...");
				mComplexUploadTask.state = String
						.valueOf(TaskInfo.TASK_GET_HOST);
				break;
			case ComplexUploadStatusCreateFileSHA1:
				Log.d(TAG, "Creating the sha1 of file");
				mComplexUploadTask.state = String
						.valueOf(TaskInfo.TASK_CREATE_SHA1);
				break;
			case ComplexUploadStatusInitialize:
				Log.d(TAG, "Signing each segment of file...");
				mComplexUploadTask.state = String
						.valueOf(TaskInfo.TASK_BATCH_SIGN);
				break;
			case ComplexUploadStatusCreateFileMD5s:
				Log.d(TAG, "Creating each segment's md5...");
				mComplexUploadTask.state = String
						.valueOf(TaskInfo.TASK_CREATE_MD5S);
				break;
			case ComplexUploadStatusUploading:
				Log.d(TAG, "Uploading one segment...");
				mComplexUploadTask.state = String
						.valueOf(TaskInfo.TASK_WORKING);
				break;
			case ComplexUploadStatusMerging:
				Log.d(TAG, "File Merging...");
				mComplexUploadTask.state = String
						.valueOf(TaskInfo.TASK_WORKING);
				break;
			default:
				break;
			}
		}

		@Override
		public void finishedWithMetadata(Entry metadata) {
			Log.d(TAG, "Upload success : " + metadata.fileName());
			mEntry = metadata;
		}

		@Override
		public VDiskUploadFileInfo readUploadFileInfo(String srcPath,
				String desPath) throws VDiskException {
			String fileId = Digest.md5String(srcPath + desPath);
			String serStr = null;
			serStr = mDb.readUploadFileInfo(mComplexUploadTask.taskid, fileId);

			if (serStr != null) {
				VDiskUploadFileInfo fileInfo = (VDiskUploadFileInfo) deserialize(serStr);
				Log.d(TAG, "readUploadFileInfo-->" + fileInfo.point);
				return fileInfo;
			}

			return null;
		}

		@Override
		public void updateUploadFileInfo(VDiskUploadFileInfo fileInfo)
				throws VDiskException {
			String serStr = serialize(fileInfo);
			mDb.updateUploadFileInfo(mComplexUploadTask.taskid, fileInfo.id,
					serStr);
		}

		@Override
		public void deleteUploadFileInfo(VDiskUploadFileInfo fileInfo) {
			mComplexUploadTask.sha1 = fileInfo.sha1;// 文件上传成功后会将sha1值赋给task，
			mDb.deleteUploadFileInfo(mComplexUploadTask.taskid);
		}
	}
}
