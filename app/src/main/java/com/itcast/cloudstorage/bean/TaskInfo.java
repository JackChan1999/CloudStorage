package com.itcast.cloudstorage.bean;

import java.io.Serializable;

public class TaskInfo implements Cloneable, Serializable {

	private static final long serialVersionUID = -6293864978601789997L;

	public static final int TASK_WORKING = 1;
	public static final int TASK_WAITTING = 2;
	public static final int TASK_FINISH = 3;
	public static final int TASK_ERROR = 4;
	public static final int TASK_DOWNLOAD_PAUSE = 5;
	
	//大文件分段上传状态
	public static final int TASK_GET_HOST = 5;
	public static final int TASK_CREATE_SHA1 = 6;
	public static final int TASK_BATCH_SIGN = 7;
	public static final int TASK_CREATE_MD5S = 8;
	
	public static final int TASK_UPLOAD_PAUSE = 9;

	// public static final int TASK_PAUSE = 5;
	// public static final int TASK_NEW = 6;
}
