package com.itcast.cloudstorage.bean;
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
public class LocalFileInfo {

	public static final String SOURCE_DOWNLOAD = "download";
	public static final String SOURCE_UPLOAD = "upload";

	public int _id;
	public String path;
	public String filename;
	public String bytes;
	public String md5;
	public String sha1;
	public String modified;
	public String source;
	public String state = "";

	public boolean isChecked;
	

	public static LocalFileInfo valueOf(DownloadEntry entry) {
		LocalFileInfo info = new LocalFileInfo();
		info.path = entry.localPath;
		info.filename = entry.name;
		info.bytes = String.valueOf(entry.bytes);
		info.md5 = entry.md5;
		info.sha1 = entry.sha1;
		info.modified = entry.lastModifyTime;
		info.source = SOURCE_DOWNLOAD;

		return info;
	}
}
