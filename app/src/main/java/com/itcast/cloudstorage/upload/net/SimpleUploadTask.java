package com.itcast.cloudstorage.upload.net;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import com.itcast.cloudstorage.bean.UploadTask;
import com.vdisk.net.exception.VDiskPartialFileException;

public class SimpleUploadTask extends UploadTask {

	private static final long serialVersionUID = -683481723062773315L;
	private SimpleUploadAsyncTask task;

	@Override
	public void startUpload(Context ctx, Notification uploadNotification,
			NotificationManager uploadNotificationManager) {
		if (!isCanceled()) {
			task = new SimpleUploadAsyncTask(ctx);
			task.execute(new Object[] { this, uploadNotification,
					uploadNotificationManager });
		}
	}

	@Override
	public void cancel() {
		isCancel = true;

		if (task != null) {
			task.cancel();
		}
	}

	public boolean isCanceled() {
		return isCancel;
	}

	public void assertCanceled() throws VDiskPartialFileException {
		if (isCancel) {
			throw new VDiskPartialFileException(-1);
		}
	}

	@Override
	public UploadTask valueOf(UploadTask entry) {
		SimpleUploadTask task = new SimpleUploadTask();
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
