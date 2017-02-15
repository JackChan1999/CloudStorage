package com.itcast.cloudstorage.bean;

import java.io.Serializable;
/**
 * ============================================================
 * Copyright：Google有限公司版权所有 (c) 2017
 * Author：   陈冠杰
 * Email：    815712739@qq.com
 * GitHub：   https://github.com/JackChen1999
 * 博客：     http://blog.csdn.net/axi295309066
 * 微博：     AndroidDeveloper
 * <p>
 * Project_Name：CloudStorage
 * Package_Name：com.itcast.cloudstorage
 * Version：1.0
 * time：2016/2/15 14:33
 * des ：${TODO}
 * gitVersion：$Rev$
 * updateAuthor：$Author$
 * updateDate：$Date$
 * updateDes：${TODO}
 * ============================================================
 **/
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
