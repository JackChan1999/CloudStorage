package com.itcast.cloudstorage.upload.net;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import com.itcast.cloudstorage.bean.UploadTask;

public class ComplexUploadTask extends UploadTask {

	private static final long serialVersionUID = -69654441838538413L;
	private ComplexUploadAsyncTask task;

	@Override
	public void startUpload(Context ctx, Notification uploadNotification,
			NotificationManager uploadNotificationManager) {
		if (!isCancel) {
			task = new ComplexUploadAsyncTask(ctx, this);
			task.execute(new Object[] { uploadNotification,
					uploadNotificationManager });
		}
	}

	@Override
	public void cancel() {
		if (task != null) {
			task.cancel();
		}
	}

	@Override
	public UploadTask valueOf(UploadTask entry) {
		ComplexUploadTask task = new ComplexUploadTask();
		task.desPath = entry.desPath;
		task.filename = entry.filename;
		task.fileprogress = entry.fileprogress;
		task.fileSize = entry.fileSize;
		task.from = entry.from;
		task.isCancel = entry.isCancel;
		task.isChecked = entry.isChecked;
		task.sha1 = entry.sha1;
		task.srcPath = entry.srcPath;
		task.state = entry.state;
		task.taskid = entry.taskid;
		return task;
	}

}
