package com.itcast.cloudstorage.upload.net;

import com.itcast.cloudstorage.bean.UploadTask;

public interface UploadProgressListener {

	public long progressInterval = 500;

	public void onProgress(UploadTask task, long speed);

	public void onCancel(UploadTask task);

	public void onSuccess(UploadTask task);

	public void onFailed(UploadTask task);

}
