package com.itcast.cloudstorage.download;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import com.itcast.cloudstorage.R;
import com.itcast.cloudstorage.bean.DownloadEntry;
import com.itcast.cloudstorage.bean.LocalFileInfo;
import com.itcast.cloudstorage.bean.TaskInfo;
import com.itcast.cloudstorage.net.CloudEngine;
import com.itcast.cloudstorage.utils.CloudDB;
import com.itcast.cloudstorage.utils.Logger;
import com.itcast.cloudstorage.utils.Utils;
import com.vdisk.android.VDiskAuthSession;
import com.vdisk.net.VDiskAPI;

public class DownloadManager {

	private static final String TAG = DownloadManager.class.getSimpleName();
	
	public static final String ACTION_DOWNLOAD_FINISH = "com.itcast.cloudstorage.ACTION_DOWNLOAD_FINISH";

	private static DownloadManager instance = null;

	public static DownloadManager getInstance() {
		if (instance == null) {
			instance = new DownloadManager();
		}
		return instance;
	}

	public interface DownloadStatusListener {
		void onCreate(DownloadEntry entry, boolean exists);

		void onStart(DownloadEntry entry);

		void onProgress(DownloadEntry entry, float speed);

		void onCancel(DownloadEntry entry);

		void onFinish(DownloadEntry entry);

		void onFailed(DownloadEntry entry);

		void onPause(DownloadEntry entry);

		void onBatchCreate(List<DownloadEntry> tasks);
	}

	public static class SimpleDownloadStatusListener implements
			DownloadStatusListener {
		@Override
		public void onCreate(DownloadEntry entry, boolean exists) {
		}

		@Override
		public void onStart(DownloadEntry entry) {
		}

		@Override
		public void onProgress(DownloadEntry entry, float speed) {
		}

		@Override
		public void onCancel(DownloadEntry entry) {
		}

		@Override
		public void onFinish(DownloadEntry entry) {
		}

		@Override
		public void onFailed(DownloadEntry entry) {
		}

		@Override
		public void onPause(DownloadEntry entry) {

		}

		@Override
		public void onBatchCreate(List<DownloadEntry> tasks) {
		}
	}

	private ArrayList<DownloadStatusListener> mDownloadStatusListeners = new ArrayList<DownloadStatusListener>();

	public void addDownloadStatusListener(
			DownloadStatusListener l) {
		mDownloadStatusListeners.add(l);
	}

	public void removeDownloadStatusListener(
			DownloadStatusListener l) {
		mDownloadStatusListeners.remove(l);
	}

	public ArrayList<DownloadStatusListener> getDownloadStatusListeners() {
		return mDownloadStatusListeners;
	}

	private LinkedList<DownloadEntry> mQueue = new LinkedList<DownloadEntry>();

	public LinkedList<DownloadEntry> getDownloadQueue() {
		return mQueue;
	}

	public void initDownloadFile(CloudDB vDiskDB, Context ctx,
			DownloadEntry entry) {
		if (!Utils.isMountSdCard(ctx)) {
			Toast.makeText(ctx, "请插入sdcard", Toast.LENGTH_SHORT).show();
			return;
		}

		DownloadEntry dbEntry = vDiskDB.getDownloadEntry(entry.pathOrCopyRef);
		Logger.d(TAG, "initDownloadFile dbEntry: " + dbEntry);
		if (dbEntry != null) {
			if (String.valueOf(TaskInfo.TASK_ERROR).equals(dbEntry.state)
					|| String.valueOf(TaskInfo.TASK_DOWNLOAD_PAUSE).equals(
							dbEntry.state)) {
				dbEntry.state = String.valueOf(TaskInfo.TASK_WAITTING);
				CloudDB db = CloudDB.getInstance(ctx);
				db.updateDownloadEntry(dbEntry);

				beginDownloadFile(vDiskDB, ctx, dbEntry, true);
			} else {
				String toast = String.format(
						ctx.getString(R.string.task_in_queue), dbEntry.name);
				Utils.showToastString(ctx, toast, 0);
			}
		} else {
			Logger.d(TAG, "insertDownloadFile: " + entry.pathOrCopyRef);
			entry.localPath = createDownloadTempFile(ctx, entry.name);
			if (vDiskDB.insertDownloadEntry(entry)) {
				beginDownloadFile(vDiskDB, ctx, entry, false);
			}
		}
	}

	public static String createDownloadTempFile(Context context, String name) {
		String defaultLocation = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/微盘/";

		VDiskAPI<VDiskAuthSession> api = CloudEngine.getInstance(context)
				.getApi(context);
		String oriFile = new File(defaultLocation, name).getPath();
		Logger.d(TAG, "createDownloadTempFile file 0: " + oriFile);
		File downloadTempFile = api.createDownloadDirFile(oriFile);
		Logger.d(TAG,
				"createDownloadTempFile file 1: " + downloadTempFile.getPath());
		String newPath = getNewPath(downloadTempFile.getPath());
		Logger.d(TAG, "createDownloadTempFile file 2: " + newPath);
		try {
			new File(newPath).createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return newPath;
	}

	public static String getNewPath(String oldPath) {
		if (oldPath.endsWith(VDiskAPI.DOWNLOAD_TEMP_FILE_SUFFIX)) {
			return getNewPath(oldPath.substring(0, oldPath.length()
					- VDiskAPI.DOWNLOAD_TEMP_FILE_SUFFIX.length()))
					+ VDiskAPI.DOWNLOAD_TEMP_FILE_SUFFIX;
		}

		File testFile = new File(oldPath);
		if (!testFile.exists()
				&& !new File(testFile.getPath()
						+ VDiskAPI.DOWNLOAD_TEMP_FILE_SUFFIX).exists()) {
			return oldPath;
		}

		String absoPath = testFile.getAbsolutePath();
		int lastSlice = absoPath.lastIndexOf('/');
		int lastDot = absoPath.lastIndexOf('.');
		Logger.d(TAG, "lastSlick: " + lastSlice + ", lastDot: " + lastDot);
		String left = absoPath.substring(0, lastSlice);
		String right = absoPath.substring(lastDot, absoPath.length());
		String center = absoPath.substring(lastSlice, lastDot);
		Pattern pattern = Pattern.compile(".* \\((\\d+)\\)\\..*");
		Matcher matcher = pattern.matcher(absoPath);
		String newPath = null;
		Logger.d("FILE PATH", "matches: " + matcher.matches());
		if (matcher.matches() && matcher.groupCount() > 0) {
			try {
				int time = Integer.valueOf(matcher.group(1));
				Logger.d("FILE PATH", "time: " + time);
				newPath = left
						+ center.replaceAll(" \\(\\d+\\)",
								String.format(" (%d)", time + 1)) + right;
			} catch (Exception e) {
				// eat
			}
		} else {
			newPath = left + center + " (1)" + right;
		}

		return getNewPath(newPath);
	}

	private void beginDownloadFile(CloudDB vDiskDB, Context ctx,
			final DownloadEntry downloadEntry, boolean exists) {
		Logger.d(TAG, "beginDownloadFile: " + downloadEntry);

		if (!DownloadManager.getInstance().getDownloadQueue()
				.contains(downloadEntry)) {
			DownloadManager.getInstance().getDownloadQueue()
					.addLast(downloadEntry);

			Logger.d(TAG, "add queue: " + downloadEntry);

			for (final DownloadStatusListener l : getDownloadStatusListeners()) {
				l.onCreate(downloadEntry, exists);
			}

			Logger.d(TAG, "beginDownloadFile queue size: "
					+ DownloadManager.getInstance().getDownloadQueue().size());
			if (DownloadManager.getInstance().getDownloadQueue().size() == 1) {
				new DownloadAsyncTask(ctx, downloadEntry, vDiskDB).execute();
			} else {
				String toast = String.format(ctx.getString(R.string.add_task),
						downloadEntry.name);
				Utils.showToastString(ctx, toast, 0);
			}
		} else {
			String toast = String.format(ctx.getString(R.string.task_in_queue),
					downloadEntry.name);
			Utils.showToastString(ctx, toast, 0);
		}
	}

	public static void openFile(File file, Activity activity) {
		try {
			Intent intent = new Intent("android.intent.action.VIEW");
			intent.addCategory("android.intent.category.DEFAULT");
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra("fromVdisk", true);
			String types = (String) Utils.getMIMEType(file.getName())[0];
			intent.setDataAndType(Uri.fromFile(file), types);
			activity.startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
			Utils.showToastString(activity,
					activity.getString(R.string.not_support_file_format), 0);
		}
	}

	public static File getLocalFile(Context context, String name, String md5,
			String sha1, long fileLength) {
		Logger.d(TAG, "getLocalFile: " + name + ", md5: " + md5 + ", sha1: "
				+ sha1);

		// 检测已下载
		LocalFileInfo localFileInfo = CloudDB.getInstance(context)
				.getLocalFile(name, LocalFileInfo.SOURCE_DOWNLOAD, md5, sha1);
		if (localFileInfo != null) {
			return new File(localFileInfo.path);
		}

		// 检测从本地上传文件的记录
		localFileInfo = CloudDB.getInstance(context).getLocalFile(name,
				LocalFileInfo.SOURCE_UPLOAD, md5, sha1);
		if (localFileInfo != null) {
			return new File(localFileInfo.path);
		}

		return null;
	}
}
