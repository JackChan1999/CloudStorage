package com.itcast.cloudstorage.bean;

import java.io.Serializable;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

public abstract class UploadTask implements Serializable {

	private static final long serialVersionUID = -2264332837843096142L;

	public String filename;
	public String srcPath;// 上传文件的本地路径
	public String desPath;// 云端目标文件夹路径
	public int fileprogress;
	public String state;
	public long fileSize;
	// public int type;//默认type为0,表示非媒体文件，type为1，表示为媒体文件
	public String taskid;
	public boolean isCancel = false;
	public String sha1;

	public String from;

	public boolean isChecked;

	// public int type;// 0表示为图片文件，1表示视频文件, 自动备份会用到
	
	public abstract void startUpload(final Context ctx,
			final Notification uploadNotification,
			final NotificationManager uploadNotificationManager);

	public abstract void cancel();

	@Override
	public int hashCode() {
		return (srcPath + desPath).hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}

		if (o instanceof UploadTask) {
			UploadTask other = (UploadTask) o;
			return srcPath.equals(other.srcPath)
					&& desPath.equals(other.desPath);
		} else {
			return false;
		}
	}
	
	public abstract UploadTask valueOf(UploadTask entry);
}
