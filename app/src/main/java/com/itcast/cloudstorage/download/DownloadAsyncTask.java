package com.itcast.cloudstorage.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.itcast.cloudstorage.MainActivity;
import com.itcast.cloudstorage.R;
import com.itcast.cloudstorage.bean.DownloadEntry;
import com.itcast.cloudstorage.bean.LocalFileInfo;
import com.itcast.cloudstorage.bean.TaskInfo;
import com.itcast.cloudstorage.net.CloudEngine;
import com.itcast.cloudstorage.utils.Logger;
import com.itcast.cloudstorage.utils.Utils;
import com.itcast.cloudstorage.utils.CloudDB;
import com.vdisk.android.VDiskAuthSession;
import com.vdisk.net.ProgressListener;
import com.vdisk.net.VDiskAPI;

public class DownloadAsyncTask extends AsyncTask<Void, Object, Integer> {

	private static final String TAG = DownloadAsyncTask.class.getSimpleName();

	private static final int NOTIFY_ID = 0;

	private Context mContext;
	private DownloadEntry mDownloadEntry;
	private CloudDB mVDiskDB;

	private NotificationManager mNotificationManager;
	private Notification mNotification;

	private RemoteViews mRemoteView;

	private FileOutputStream mTargetFileOS;
	private String mFilepath;
	private File mNewFile;

	// 定时器用于定时检测网络
	private Timer timer = new Timer();
	private TimerTask timerTask;

	private Handler mHandler = new Handler();

	public DownloadAsyncTask(Context ctx, DownloadEntry downloadQueue,
			CloudDB vDiskDB) {
		Logger.d(TAG, "DownloadQueue size: "
				+ DownloadManager.getInstance().getDownloadQueue().size());

		this.mContext = ctx;
		this.mDownloadEntry = downloadQueue;
		this.mVDiskDB = vDiskDB;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPreExecute() {
		Logger.d(TAG, "DownloadAsyncTask onPreExecute: entry: "
				+ mDownloadEntry);

		mFilepath = mDownloadEntry.localPath;
		Logger.d(TAG, "mFilepath = mDownloadEntry.localPath; " + mFilepath);

		mDownloadEntry.state = String.valueOf(TaskInfo.TASK_WORKING);
		mVDiskDB.updateDownloadEntry(mDownloadEntry);

		for (DownloadManager.DownloadStatusListener l : DownloadManager
				.getInstance().getDownloadStatusListeners()) {
			l.onStart(mDownloadEntry);
		}

		mNotificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		String tickerText = mDownloadEntry.name
				+ mContext.getString(R.string.start_download_label);
		mNotification = new Notification(R.drawable.notification_download_icon,
				tickerText, System.currentTimeMillis());
		mNotification.flags = Notification.FLAG_ONGOING_EVENT;
		mRemoteView = new RemoteViews(mContext.getPackageName(),
				R.layout.download_notification_layout);
		mRemoteView.setTextViewText(R.id.downloadText, mDownloadEntry.name);
		mNotification.contentView = mRemoteView;

		Intent intent = new Intent(mContext, MainActivity.class);
		intent.putExtra("downloading", true);
		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 10,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		mNotification.contentIntent = contentIntent;

		try {
			mNotificationManager.notify(NOTIFY_ID, mNotification);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	long lastBytes = -1;

	ProgressListener mProgressListener = new ProgressListener() {
		@Override
		public void onProgress(long bytes, long total) {
			if (lastBytes == -1) {
				lastBytes = bytes;
				return;
			}
			float speed = (bytes - lastBytes) * 1000 / progressInterval();
			int percent = (int) (100.0 * bytes / total + 0.5);
			Logger.d(TAG, "entry: " + mDownloadEntry.name + ", speed: " + speed
					+ ", percent: " + percent);
			Logger.d(TAG, "entry: " + mDownloadEntry.name + ", bytes: " + bytes
					+ ", total: " + total);

			percent = Math.min(percent, 100);

			publishProgress(percent, bytes, total, speed);

			lastBytes = bytes;
		}
	};

	@Override
	protected Integer doInBackground(Void... params) {
		Logger.d(TAG, "DownloadAsyncTask doInBackground entry: "
				+ mDownloadEntry);

		if (!Utils.isMountSdCard(mContext)) {
			return UpDownRet.RET_NO_SD;
		}

		if (TextUtils.isEmpty(mFilepath)) {
			return UpDownRet.RET_ERR;
		}

			VDiskAPI<VDiskAuthSession> api = CloudEngine.getInstance(mContext)
					.getApi(mContext);
			Logger.d(TAG, "api getFile: " + mDownloadEntry.pathOrCopyRef
					+ ", targetFile: " + mFilepath);
			// File newFile = api.createDownloadDirFile(mFilepath);
			File newFile = new File(mFilepath);
			if (!newFile.getParentFile().exists()) {
				newFile.getParentFile().mkdirs();
			}
			mNewFile = newFile;
			try {
				mTargetFileOS = new FileOutputStream(newFile, true);
				mDownloadEntry.targetFileStream = mTargetFileOS;
				if (DownloadEntry.SOURCE_HOME.equals(mDownloadEntry.source)) {
					api.getFile(mDownloadEntry.pathOrCopyRef, null,
							mTargetFileOS, newFile, mProgressListener
							);
				} 
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				String message = e.getMessage();
				Logger.d(TAG, "message: " + message);
				if (message != null && message.contains("ENAMETOOLONG")) {
					return UpDownRet.RET_ERR_NAME_TOO_LONG;
				}

				return UpDownRet.RET_ERR;
			} catch (Exception e) {
				Logger.d(TAG,
						"doInBackground getFile VDiskException, isCancel: "
								+ mDownloadEntry.isCancel + ", isPause: "
								+ mDownloadEntry.isPause + ", mDownloadEntry: "
								+ mDownloadEntry.name);
				e.printStackTrace();

				if (mDownloadEntry.isCancel) {
					if (mDownloadEntry.isPause) {
						return UpDownRet.RET_PAUSE;
					} else {
						return UpDownRet.RET_CANCEL;
					}
				} else {
					return UpDownRet.RET_ERR;
				}
			}

			return UpDownRet.RET_SUCC;
	}

	class UpDownRet {

		public static final int RET_SUCC = 1;
		public static final int RET_ERR = 2;
		public static final int RET_NETWORK_INTERRUPT = 3;
		public static final int RET_CANCEL = 4;
		public static final int RET_NO_SD = 5;
		public static final int RET_PAUSE = 6;
		public static final int RET_ERR_DEL = 7;
		public static final int RET_ERR_NAME_TOO_LONG = 8;

		public int retCode;
		public int errCode;
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		// percent, bytes, total, speed
		int percent = (Integer) values[0];
		long bytes = (Long) values[1];
		long total = (Long) values[2];
		float speed = (Float) values[3];

		Logger.d(TAG, "onProgressUpdate: entry: " + mDownloadEntry);

		if (percent > 100) {
			percent = 100;
		}
		if (percent < 0) {
			percent = 0;
		}

		if (mDownloadEntry.isCancel) {
			return;
		}

		try {
			mRemoteView.setTextViewText(R.id.downloadSumText, percent + "%");
			mRemoteView.setProgressBar(R.id.downloadProgress, 100,
					percent <= 100 ? percent : 100, false);
			mNotificationManager.notify(NOTIFY_ID, mNotification);
		} catch (Exception e) {
			e.printStackTrace();
		}

		mDownloadEntry.fileProgress = String.valueOf(percent);

		for (DownloadManager.DownloadStatusListener l : DownloadManager
				.getInstance().getDownloadStatusListeners()) {
			l.onProgress(mDownloadEntry, speed);
		}

		super.onProgressUpdate(values);
	}

	@Override
	protected void onPostExecute(Integer result) {
		Logger.d(TAG, "onPostExecute result: " + result + "entry: "
				+ mDownloadEntry.name);
		if (result == UpDownRet.RET_CANCEL) {
			downloadCanceled(false);
		} else if (result == UpDownRet.RET_ERR
				|| result == UpDownRet.RET_ERR_DEL
				|| result == UpDownRet.RET_ERR_NAME_TOO_LONG) {
			downloadFailed(result);
		} else if (result == UpDownRet.RET_PAUSE) {
			downloadCanceled(true);
		} else {
			downloadSuccess();
		}

		NotificationManager notificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		if (notificationManager != null) {
			notificationManager.cancel(NOTIFY_ID);
		}
	}

	private void downloadFailed(int result) {
		Logger.d(TAG, "downloadFailed: entry: " + mDownloadEntry.name
				+ ", result: " + result);

		String msg = mDownloadEntry.name
				+ mContext.getString(R.string.download_failed);
		if (result == UpDownRet.RET_ERR_NAME_TOO_LONG) {
			msg = "文件名过长，下载失败";
		}
		Utils.showToastString(mContext, msg, 0);

		if (!DownloadManager.getInstance().getDownloadQueue().isEmpty()) {
			DownloadManager.getInstance().getDownloadQueue()
					.remove(mDownloadEntry);
			mDownloadEntry.state = String.valueOf(TaskInfo.TASK_ERROR);
			mVDiskDB.updateDownloadEntry(mDownloadEntry);
		}

		for (DownloadManager.DownloadStatusListener l : DownloadManager
				.getInstance().getDownloadStatusListeners()) {
			l.onFailed(mDownloadEntry);
		}

		if (result == UpDownRet.RET_ERR_DEL) {
			if (mNewFile != null) {
				mNewFile.delete();
			}
		}

		queueNext();
	}

	private void downloadSuccess() {
		Logger.d(TAG, "downloadSuccess");

		if (mDownloadEntry.localPath
				.endsWith(VDiskAPI.DOWNLOAD_TEMP_FILE_SUFFIX)) {
			File oldFile = new File(mDownloadEntry.localPath);
			String path = mDownloadEntry.localPath.substring(0,
					mDownloadEntry.localPath.length()
							- VDiskAPI.DOWNLOAD_TEMP_FILE_SUFFIX.length());
			mDownloadEntry.localPath = path;

			Logger.d(TAG, "download finish rename newFile: " + path);

			oldFile.renameTo(new File(path));
		}

		String msg = mDownloadEntry.name
				+ mContext.getString(R.string.single_download_finish_label);
		Utils.showToastString(mContext, msg, Toast.LENGTH_SHORT);

		if (!DownloadManager.getInstance().getDownloadQueue().isEmpty()) {
			DownloadManager.getInstance().getDownloadQueue()
					.remove(mDownloadEntry);
			mDownloadEntry.fileProgress = "100";
			mDownloadEntry.state = String.valueOf(TaskInfo.TASK_FINISH);
			mVDiskDB.updateDownloadEntry(mDownloadEntry);

			LocalFileInfo localFileInfo = new LocalFileInfo();
			localFileInfo.bytes = String.valueOf(mDownloadEntry.bytes);
			localFileInfo.filename = mDownloadEntry.name;
			localFileInfo.md5 = mDownloadEntry.md5;
			localFileInfo.sha1 = mDownloadEntry.sha1;
			localFileInfo.path = mDownloadEntry.localPath;
			localFileInfo.source = LocalFileInfo.SOURCE_DOWNLOAD;
			localFileInfo.modified = mDownloadEntry.lastModifyTime;
			// localFileInfo.modified = RESTUtility.dateFormat.format(new
			// Date());
			mVDiskDB.insertLocalFile(localFileInfo);
			mVDiskDB.deleteDownloadEntry(mDownloadEntry.pathOrCopyRef);
		}

		// VDiskApplication.getInstance().mDownloadRetryCount = 0;
		// VDiskApplication.getInstance().mDownloadTimerCount = 0;

		for (DownloadManager.DownloadStatusListener l : DownloadManager
				.getInstance().getDownloadStatusListeners()) {
			l.onFinish(mDownloadEntry);
		}

		queueNext();
	}

	private void downloadCanceled(boolean isPause) {
		Logger.d(TAG, "downloadCanceled entry: " + mDownloadEntry.name
				+ ", isPause: " + isPause);

		String msg;
		if (!isPause) {
			msg = mContext.getString(R.string.download_file_canceled);
		} else {
			msg = mContext.getString(R.string.download_file_paused);
		}
		Utils.showToastString(mContext, msg, 0);

		if (!DownloadManager.getInstance().getDownloadQueue().isEmpty()) {
			if (!isPause) {
				DownloadManager.getInstance().getDownloadQueue()
						.remove(mDownloadEntry);
				mVDiskDB.deleteDownloadEntry(mDownloadEntry.pathOrCopyRef);
			}
		}

		for (DownloadManager.DownloadStatusListener l : DownloadManager
				.getInstance().getDownloadStatusListeners()) {
			if (!isPause) {
				l.onCancel(mDownloadEntry);
			} else {
				l.onPause(mDownloadEntry);
			}
		}

		queueNext();
	}

	private void queueNext() {
		if (!DownloadManager.getInstance().getDownloadQueue().isEmpty()) {
			final DownloadEntry downloadEntry = DownloadManager.getInstance()
					.getDownloadQueue().getFirst();
			Logger.d(TAG, "queueNext: entry: " + downloadEntry);
			if (downloadEntry != null
					&& !String.valueOf(TaskInfo.TASK_WORKING).equals(
							downloadEntry.state)) {
				nextEntry(downloadEntry);
			}
		} else {
			mNotificationManager.cancel(NOTIFY_ID);
		}
	}

	private void nextEntry(final DownloadEntry downloadEntry) {
		// 有网络时继续下载，无网络时使用定时器进行控制
		if (Utils.isNetworkAvailable(mContext)) {
			Logger.d(TAG, "queue nextEntry: " + downloadEntry.name);
			new DownloadAsyncTask(mContext, downloadEntry, mVDiskDB).execute();
		} else {
			timerTask = new TimerTask() {
				@Override
				public void run() {
					if (Utils.isNetworkAvailable(mContext)) {
						timer.cancel();
						timerTask.cancel();

						mHandler.post(new Runnable() {
							@Override
							public void run() {
								if (String.valueOf(TaskInfo.TASK_WAITTING)
										.equals(downloadEntry.state)) {
									new DownloadAsyncTask(mContext,
											downloadEntry, mVDiskDB).execute();
								}
							}
						});
					}
				}
			};
			timer.schedule(timerTask, 2000, 2000);
		}
	}
}
