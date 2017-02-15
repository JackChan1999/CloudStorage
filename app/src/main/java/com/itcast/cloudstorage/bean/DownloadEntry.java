package com.itcast.cloudstorage.bean;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.itcast.cloudstorage.download.DownloadManager;
import com.itcast.cloudstorage.utils.Logger;
import com.vdisk.net.VDiskAPI.Entry;

public class DownloadEntry implements Serializable {

	private static final long serialVersionUID = -6225257641770509579L;

	private static final String TAG = DownloadEntry.class.getSimpleName();

	public static final String SOURCE_HOME = "0";
	public static final String SOURCE_SHARE = "1";
	public static final String SOURCE_DIRECT_SHARE = "2";
	public static final String SOURCE_APK = "3";
	public static final String SOURCE_FRIENDSHARE = "4";

	public int _id;
	public String pathOrCopyRef;
	public String name;
	public String size;
	public long bytes;
	public String md5;
	public String sha1;
	public String lastModifyTime;
	public String data;
	public String state = String.valueOf(TaskInfo.TASK_WAITTING);
	public String source;
	public String fileProgress;
	public String localPath;

	public boolean isCancel;
	public transient OutputStream targetFileStream;

	public boolean isPause = false;

	public boolean isChecked;

	@Override
	public int hashCode() {
		return pathOrCopyRef.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}

		if (pathOrCopyRef == null) {
			return false;
		}

		if (o instanceof DownloadEntry) {
			DownloadEntry other = (DownloadEntry) o;
			return pathOrCopyRef.equals(other.pathOrCopyRef);
		} else {
			return false;
		}
	}

	public boolean cancel(boolean pause) {
		this.isCancel = true;
		this.isPause = pause;

		if (targetFileStream == null) {
			List<DownloadEntry> queue = DownloadManager.getInstance().getDownloadQueue();
			if (queue != null) {
				int index = queue.indexOf(this);
				if (index >= 0) {
					DownloadEntry entry = queue.get(index);
					entry.isCancel = true;
					entry.isPause = pause;

					OutputStream stream = entry.targetFileStream;
					if (stream != null) {
						try {
							stream.close();
							return true;
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		} else {
			try {
				targetFileStream.close();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return false;
	}
	
	public static DownloadEntry valueOf(Entry entry) {
		DownloadEntry downloadEntry = new DownloadEntry();
		downloadEntry.pathOrCopyRef = entry.path;
		downloadEntry.name = entry.fileName();
		downloadEntry.size = entry.size;
		downloadEntry.bytes = entry.bytes;
		downloadEntry.sha1 = entry.sha1;
		downloadEntry.md5 = entry.md5;
		// downloadEntry.lastModifyTime = entry.modified;
		downloadEntry.lastModifyTime = String.valueOf(new Date().getTime());
		downloadEntry.fileProgress = "0";
		downloadEntry.isCancel = false;
		downloadEntry.source = SOURCE_HOME;

		Logger.d(TAG, "valueOf entry: " + downloadEntry.toString());

		return downloadEntry;
	}

	@Override
	public String toString() { 
		return String.format(
				"[path:%s,name:%s,size:%s,bytes:%d,source:%s,state:%s,isCancel:%b,isPause:%b,progress:%s,md5:%s]",
				pathOrCopyRef, name, size, bytes, source, state, isCancel, isPause, fileProgress, md5);
	}
}
