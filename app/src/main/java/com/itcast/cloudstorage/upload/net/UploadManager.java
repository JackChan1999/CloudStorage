package com.itcast.cloudstorage.upload.net;

import java.io.BufferedOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.SystemClock;
import android.widget.RemoteViews;

import com.itcast.cloudstorage.MainActivity;
import com.itcast.cloudstorage.R;
import com.itcast.cloudstorage.bean.LocalFileInfo;
import com.itcast.cloudstorage.bean.TaskInfo;
import com.itcast.cloudstorage.bean.UploadTask;
import com.itcast.cloudstorage.utils.CloudDB;
import com.itcast.cloudstorage.utils.Logger;
import com.itcast.cloudstorage.utils.Utils;
import com.vdisk.net.VDiskAPI.Entry;

public class UploadManager {

	private static final String TAG = UploadManager.class.getSimpleName();

	private static UploadManager instance = null;

	private ArrayList<UploadTask> queue = new ArrayList<UploadTask>();

	private Notification uploadNotification;
	private NotificationManager uploadNotificationManager;
	private RemoteViews contentView = null;

	private static Context sContext;

	public static int UPLOAD_NOTIFY_ID = 1;
	public static String UPLOAD_FINISH = "com.itcast.cloudstorage.ACTION_UPLOAD_FINISH";
	public static final int UPLOAD_SUCCESS = 3;
	public static final int UPLOAD_FAILED = 4;
	
	public static final int PHOTO_CAMERA = 11;
	public static final int VIDEO_CAMERA = 12;
	
	// upload file size//文件大小分割点
	public static final long UPLOAD_FILE_POINT = 4 * 1048576;// 2M,2097152//3M,3145728

	TimerTask timerTask = null;
	Timer timer = null;

	private static final int UPLOAD_RETRY_MAX_TIMES = 3;
	private static final int UPLOAD_RETRY_DELAY = 1000;

	private int uploadRetryTimes = 0;

	UploadStatusListener listener;

	public static UploadManager getInstance(Context ctx) {
		if (instance == null) {
			instance = new UploadManager();
			sContext = ctx.getApplicationContext();
		}
		return instance;
	}

	public int queueSize() {
		return queue.size();
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

	public void clearUploadQueue() {
		queue.clear();
	}

	public UploadTask getUploadTask(int index) {
		return queue.get(index);
	}

	public ArrayList<UploadTask> getUploadQueue() {
		return queue;
	}

	/**
	 * @param ctx
	 * @param handler
	 * @param db
	 * @param queue
	 */
	public void startUpload() {
		// 开启queue, 启动上传队列
		Logger.e(TAG, TAG + ":startUpload");

		UploadTask uploader = null;
		if (queue != null && !queue.isEmpty()) {
			if (uploadNotificationManager == null || uploadNotification == null) {
				initNotificationBar();
			}
			uploader = queue.get(0);

			if (uploader != null) {
				if(!uploader.isCancel) {
					updateNotificationBar(uploader);

					uploader.state = String.valueOf(TaskInfo.TASK_WORKING);
					uploader.fileprogress = 0;
					CloudDB.getInstance(sContext).updateUploadTable(uploader);

					uploader.startUpload(sContext, uploadNotification,
							uploadNotificationManager);
				}
			}
		}
	}

	private void startScheduleUploadTask() {
		if (timer == null || timerTask == null) {
			timer = new Timer();
			timerTask = new TimerTask() {

				@Override
				public void run() {
					if (Utils.isNetworkAvailable(sContext)) {
						timer.cancel();
						timer = null;
						timerTask.cancel();
						timerTask = null;
						startUpload();
					}
				}
			};

			timer.schedule(timerTask, 2000, 2000);
		}
	}

	private void initNotificationBar() {
		// notification
		uploadNotificationManager = (NotificationManager) sContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		uploadNotification = new Notification(
				R.drawable.notification_upload_icon, "",
				System.currentTimeMillis());

		Intent intnt = new Intent(sContext, MainActivity.class);
		intnt.putExtra("uploading", true);
		PendingIntent contentIntent = PendingIntent.getActivity(sContext, 0,
				intnt, PendingIntent.FLAG_UPDATE_CURRENT);
		uploadNotification.contentIntent = contentIntent;
		contentView = new RemoteViews(sContext.getPackageName(),
				R.layout.upload_notification_layout);

		uploadNotification.flags = Notification.FLAG_ONGOING_EVENT;
		uploadNotification.contentView = contentView;
		uploadNotificationManager.notify(UPLOAD_NOTIFY_ID, uploadNotification);
	}

	private void updateNotificationBar(UploadTask uploadTask) {
		contentView.setTextViewText(R.id.downloadText, uploadTask.filename);
		contentView.setTextViewText(R.id.downloadSumText, 0 + "%");
		contentView.setProgressBar(R.id.downloadProgress, 100, 0, true);
		uploadNotification.tickerText = uploadTask.filename
				+ sContext.getString(R.string.start_upload_label);
		uploadNotificationManager.notify(UPLOAD_NOTIFY_ID, uploadNotification);
	}

	public void startNextUpload(UploadTask uploadTask,
			final Notification uploadNotification,
			final NotificationManager uploadNotificationManager) {
		uploadRetryTimes = 0;

		if (queue != null && !queue.isEmpty()) {
			queue.remove(0);
		}

		if (queue != null && !queue.isEmpty()) {
			if (Utils.isNetworkAvailable(sContext)) {
				startUpload();
			} else {
				startScheduleUploadTask();
			}
		} else {
			uploadNotificationManager.cancel(UPLOAD_NOTIFY_ID);

			Intent i = new Intent(UPLOAD_FINISH);
			i.putExtra("path", uploadTask.desPath);
			sContext.sendBroadcast(i);
		}
	}

	public void uploadRetry(UploadTask uploadTask,
			final Notification uploadNotification,
			final NotificationManager uploadNotificationManager, String errorMsg) {
		if (uploadRetryTimes++ < UPLOAD_RETRY_MAX_TIMES) {
			SystemClock.sleep(UPLOAD_RETRY_DELAY);

			if (queue != null && !queue.isEmpty()) {
				Logger.d("UploadManager", "upload retry-->" + uploadRetryTimes);
				startUpload();
			}
		} else {
			Logger.d("UploadManager", "upload failed!");
			uploadTask.fileprogress = 0;
			uploadCallback(uploadTask, UploadManager.UPLOAD_FAILED);

			startNextUpload(uploadTask, uploadNotification,
					uploadNotificationManager);

			for (UploadStatusListener l : getUploadStatusListeners()) {
				l.onFailed(uploadTask);
			}

			Utils.showToastString(sContext, errorMsg, 0);
		}
	}

	ExecutorService executor = Executors.newSingleThreadExecutor();

	public boolean judgeTransfer(final BufferedOutputStream bos,
			final byte[] buffer, final int len) throws Exception {

		Future<String> result = executor.submit(new Callable<String>() {

			@Override
			public String call() throws Exception {

				bos.write(buffer, 0, len);
				bos.flush();

				return "success";
			}
		});

		String str = result.get(20, TimeUnit.SECONDS);

		if (str != null) {
			return true;
		}
		return false;
	}

	private ArrayList<UploadStatusListener> mUploadStatusListeners = new ArrayList<UploadStatusListener>();

	public void regiserObserver(UploadStatusListener listener) {
		mUploadStatusListeners.add(listener);
	}

	public void unregiserObserver(UploadStatusListener listener) {
		mUploadStatusListeners.remove(listener);
	}

	public ArrayList<UploadStatusListener> getUploadStatusListeners() {
		return mUploadStatusListeners;
	}

	public interface UploadStatusListener {

		public void onProgress(UploadTask task, long speed);

		public void onCancel(UploadTask task);

		public void onSuccess(Entry entry, UploadTask uploadTask);

		public void onFailed(UploadTask task);

	}

	public static class SimpleUploadStatusListener implements
			UploadStatusListener {

		@Override
		public void onProgress(UploadTask entry, long speed) {
		}

		@Override
		public void onCancel(UploadTask entry) {
		}

		@Override
		public void onSuccess(Entry entry, UploadTask uploadTak) {
		}

		@Override
		public void onFailed(UploadTask entry) {
		}
	}

	public void addUploadQueue(UploadTask task) {
		if (queue.contains(task)) {
			return;
		}
		queue.add(task);
	}

	public void uploadFile(UploadTask tempUploadTask) {

		addUploadQueue(tempUploadTask);

		Logger.d(TAG, "queue size-->" + queue.size());

		if (queue.size() == 1) {
			if (Utils.isNetworkAvailable(sContext)) {
				startUpload();
			} else {
				Utils.showToastString(sContext, sContext
						.getString(R.string.no_network_connection_toast), 0);
				startScheduleUploadTask();
			}
		}
	}

	public boolean uploadFile(String fileName, String srcPath, String desPath,
			String from) {

		long fileSize = new File(srcPath).length();
		CloudDB tempdb = CloudDB.getInstance(sContext);

		UploadTask tempUploadTask;
		if (fileSize <= UPLOAD_FILE_POINT) {
			// 小于等于4M时采用小文件处理
			tempUploadTask = new SimpleUploadTask();
		} else {
			// 采用大文件处理
			tempUploadTask = new ComplexUploadTask();
		}

		tempUploadTask.filename = fileName;
		tempUploadTask.srcPath = srcPath;
		tempUploadTask.desPath = desPath;
		tempUploadTask.fileprogress = 0;
		tempUploadTask.state = String.valueOf(TaskInfo.TASK_WAITTING);
		tempUploadTask.fileSize = fileSize;
		tempUploadTask.from = from;

		if (tempdb.insertUploadTask(tempUploadTask)) {

			int templastId = 0;
			Cursor templastCursor = null;
			try {
				templastCursor = tempdb.selectLastRecord();
				templastId = templastCursor
						.getInt(templastCursor.getPosition());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (templastCursor != null) {
					templastCursor.close();
					templastCursor = null;
				}
			}

			tempUploadTask.taskid = String.valueOf(templastId);
			uploadFile(tempUploadTask);

			return true;
		}

		return false;
	}

	public void uploadCallback(UploadTask uploadTask, int type) {
		CloudDB db = CloudDB.getInstance(sContext);
		switch (type) {
		case UPLOAD_SUCCESS:
			uploadTask.state = String.valueOf(TaskInfo.TASK_FINISH);
			// 更新数据库，将实例状态置为3，即finish
			LocalFileInfo localInfo = new LocalFileInfo();
			localInfo.filename = uploadTask.filename;
			localInfo.bytes = String.valueOf(uploadTask.fileSize);
			localInfo.sha1 = uploadTask.sha1;
			localInfo.modified = String.valueOf(new Date().getTime());
			localInfo.source = LocalFileInfo.SOURCE_UPLOAD;
			localInfo.path = uploadTask.srcPath;

			// 记录已经上传的本地文件，以后在我的微盘列表打开时就不用下载了
			db.insertLocalFile(localInfo);
			db.deleteUploadEntry(uploadTask);
			break;
		case UPLOAD_FAILED:
			// 更新数据库，将实例状态置为4，即error
			uploadTask.state = String.valueOf(TaskInfo.TASK_ERROR);
			db.updateUploadTable(uploadTask);
			break;
		}
	}

	public static String fileNameFilter(String fileName) {
		// 过滤文件名中的这些字符 \ / : ? * " > < |
		String regEx = "[/:?*\"><|\\\\]";
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(fileName);
		return m.replaceAll("_").trim();
	}
}
