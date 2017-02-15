package com.itcast.cloudstorage.bean;

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
