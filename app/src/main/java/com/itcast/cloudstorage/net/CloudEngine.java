package com.itcast.cloudstorage.net;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.itcast.cloudstorage.bean.DownloadEntry;
import com.itcast.cloudstorage.bean.EntryWrapper;
import com.itcast.cloudstorage.bean.Event;
import com.itcast.cloudstorage.bean.LocalFileDirInfo;
import com.itcast.cloudstorage.bean.LocalFileInfo;
import com.itcast.cloudstorage.bean.UploadTask;
import com.itcast.cloudstorage.global.Constants;
import com.itcast.cloudstorage.utils.CloudAsyncTask;
import com.itcast.cloudstorage.utils.CloudDB;
import com.itcast.cloudstorage.utils.Logger;
import com.itcast.cloudstorage.utils.Utils;
import com.vdisk.android.VDiskAuthSession;
import com.vdisk.net.VDiskAPI;
import com.vdisk.net.VDiskAPI.Entry;
import com.vdisk.net.exception.VDiskException;
import com.vdisk.net.exception.VDiskFileNotFoundException;
import com.vdisk.net.exception.VDiskFileSizeException;
import com.vdisk.net.exception.VDiskIOException;
import com.vdisk.net.exception.VDiskLocalStorageFullException;
import com.vdisk.net.exception.VDiskParseException;
import com.vdisk.net.exception.VDiskPartialFileException;
import com.vdisk.net.exception.VDiskServerException;
import com.vdisk.net.exception.VDiskUnlinkedException;
import com.vdisk.net.session.AppKeyPair;
import com.vdisk.net.session.Session.AccessType;

public class CloudEngine {

	private static final String TAG = "CloudEngine";

	public VDiskAPI<VDiskAuthSession> mApi;
	private VDiskAuthSession sSession;
	private Context mContext = null;
	private static CloudEngine sInstance;

	public static synchronized CloudEngine getInstance(Context context) {
		// mApi = getApi(context);
		if (sInstance == null) {
			sInstance = new CloudEngine(context);
		}
		return sInstance;
	}

	private CloudEngine(Context context) {
		mApi = getApi(context);
		mContext = context;
	}

	public VDiskAPI<VDiskAuthSession> getApi(Context context) {
		if (mApi != null) {
			return mApi;
		}

		AppKeyPair appKeyPair = new AppKeyPair(Constants.CONSUMER_KEY,
				Constants.CONSUMER_SECRET);
		sSession = VDiskAuthSession.getInstance(context, appKeyPair,
				AccessType.VDISK);
		mApi = new VDiskAPI<VDiskAuthSession>(sSession);
		return mApi;
	}

	abstract class BaseAsyncTask extends CloudAsyncTask<Void, Void, Event> {

		IDataCallBack handler;
		Bundle session;
		Event event = new Event();

		public BaseAsyncTask(IDataCallBack handler, int requestCode,
				Bundle session) {
			this.handler = handler;
			this.session = session;
			event.requestCode = requestCode;
		}

		@Override
		protected void onPostExecute(Event event) {
			handler.handleServiceResult(event.requestCode, event.errCode,
					event.data, session);
		}
	}

	public void getFileList(IDataCallBack handler, int requestCode,
			String path, Bundle session) {
		new GetFileTask(handler, requestCode, path, session).execute();
	}

	public class GetFileTask extends BaseAsyncTask {

		String path;

		public GetFileTask(IDataCallBack handler, int requestCode, String path,
				Bundle session) {
			super(handler, requestCode, session);
			this.path = path;
		}

		@Override
		protected Event doInBackground(Void... params) {
			try {
				Entry metadata = mApi.metadata(path, null, true, false);
				event.data = metadata.contents;
			} catch (VDiskException e) {
				updateEvent(mContext, e, event);
			}

			return event;
		}
	}

	/**
	 * 获取本地文件
	 * 
	 * @param handler
	 * @param requestCode
	 * @param source
	 *            来源
	 * @param session
	 */
	public void getLocalFiles(IDataCallBack handler, int requestCode,
			String source, Bundle session) {
		new GetLocalFilesTask(handler, requestCode, source, session).execute();
	}

	private class GetLocalFilesTask extends BaseAsyncTask {

		String source;

		public GetLocalFilesTask(IDataCallBack handler, int requestCode,
				String source, Bundle session) {
			super(handler, requestCode, session);
			this.source = source;
		}

		@Override
		protected Event doInBackground(Void... params) {
			List<LocalFileInfo> localFiles = CloudDB.getInstance(mContext)
					.getLocalFiles(source);
			// if (localFiles.size() == 0) {
			// localFiles = loadSdDonwloadedFileAndInsertDb(true);
			// }
			for (int i = localFiles.size() - 1; i >= 0; i--) {
				String path = localFiles.get(i).path;
				if (TextUtils.isEmpty(path) || !(new File(path).exists())) {
					localFiles.remove(i);
				}
			}

			event.data = localFiles;

			return event;
		}
	}

	public void getDownloadList(IDataCallBack handler, int requestCode,
			Bundle session) {
		new InitDownloadListTask(handler, requestCode, session).execute();
	}

	private class InitDownloadListTask extends BaseAsyncTask {

		public InitDownloadListTask(IDataCallBack handler, int requestCode,
				Bundle session) {
			super(handler, requestCode, session);
		}

		@Override
		protected Event doInBackground(Void... params) {
			try {
				// 查询数据库,初始化上传列表
				CloudDB db = CloudDB.getInstance(mContext);
				List<DownloadEntry> entries = new ArrayList<DownloadEntry>();
				entries.addAll(db.getAllDownloadEntries());

				event.data = entries;
			} catch (Exception e) {
				event.errCode = -1;
			}
			return event;
		}
	}

	public class ExploerLoadSdFileTask extends
			CloudAsyncTask<Void, Void, Event> {
		IDataCallBack handler;
		Event event = new Event();
		File file;
		Bundle session;

		ArrayList<LocalFileDirInfo> list = new ArrayList<LocalFileDirInfo>();

		public ExploerLoadSdFileTask(IDataCallBack handler, int requestCode,
				File file, Bundle session) {
			this.handler = handler;
			event.requestCode = requestCode;
			this.file = file;
			this.session = session;
		}

		@Override
		protected Event doInBackground(Void... params) {

			FileFilter fileFilter = new FileFilter() {
				@Override
				public boolean accept(File pathname) {

					if (!pathname.canRead()) {
						return false;
					}

					return true;
				}
			};

			ArrayList<LocalFileDirInfo> dirList = new ArrayList<LocalFileDirInfo>();
			ArrayList<LocalFileDirInfo> fileList = new ArrayList<LocalFileDirInfo>();
			File[] files = file.listFiles(fileFilter);
			for (File f : files) {
				LocalFileDirInfo info = new LocalFileDirInfo();
				info.file = f;
				info.name = f.getName();
				info.ctime = Utils.getFormateTime(new Date(f.lastModified()));
				if (f.isDirectory()) {
					info.isFile = false;
					File[] childFiles = f.listFiles(fileFilter);
					if (childFiles == null) {
						continue;
					} else {
						info.fileNum = childFiles.length;
					}
					dirList.add(info);
				} else {
					info.isFile = true;
					info.filesize = Utils.formateFileSize(f.length());
					fileList.add(info);
				}
			}
			list.addAll(Utils.orderFileListByName(dirList));
			list.addAll(Utils.orderFileListByName(fileList));
			event.data = list;
			return event;
		}

		@Override
		protected void onPostExecute(Event result) {
			handler.handleServiceResult(event.requestCode, event.errCode,
					event.data, session);
		}
	}

	/**
	 * 获取上传列表
	 * 
	 * @param handler
	 * @param requestCode
	 * @param session
	 */
	public void getUploadList(IDataCallBack handler, int requestCode,
			Bundle session) {
		new InitUploadListTask(handler, requestCode, session).execute();
	}

	private class InitUploadListTask extends BaseAsyncTask {

		public InitUploadListTask(IDataCallBack handler, int requestCode,
				Bundle session) {
			super(handler, requestCode, session);
		}

		@Override
		protected Event doInBackground(Void... params) {
			try {
				// 查询数据库,初始化上传列表
				CloudDB db = CloudDB.getInstance(mContext);
				List<UploadTask> entries = new ArrayList<UploadTask>();
				entries.addAll(db.getAllUploadEntries());

				event.data = entries;
			} catch (Exception e) {
				event.errCode = -1;
			}
			return event;
		}
	}

	/**
	 * 新建文件夹
	 * 
	 * @param handler
	 * @param requestCode
	 * @param parentDirPath
	 *            当前路径
	 * @param dirName
	 *            文件夹名称
	 * @param sSession
	 */
	public void makeDir(IDataCallBack handler, int requestCode,
			String parentDirPath, String dirName, Bundle session) {
		new MakeDirTask(handler, requestCode, parentDirPath, dirName, session)
				.execute();
	}

	private class MakeDirTask extends BaseAsyncTask {

		String parentDirPath;
		String dirName;

		public MakeDirTask(IDataCallBack handler, int requestCode,
				String parentDirPath, String dirName, Bundle session) {
			super(handler, requestCode, session);
			this.parentDirPath = parentDirPath;
			this.dirName = dirName;
		}

		@Override
		protected Event doInBackground(Void... params) {
			try {
				String folder;
				if (parentDirPath.equals("/")) {
					folder = "/" + dirName;
				} else {
					folder = parentDirPath + "/" + dirName;
				}

				Entry entry = mApi.createFolder(folder);
				event.data = entry;
			} catch (VDiskException e) {
				e.printStackTrace();
				event = updateEvent(mContext, e, event);
			}

			return event;
		}

	}

	/**
	 * 删除文件或文件夹
	 * 
	 * @param handler
	 * @param requestCode
	 * @param path
	 *            文件路径
	 * @param sSession
	 */
	public void delete(IDataCallBack handler, int requestCode, String path,
			String parentPath, Bundle session) {
		new DeleteTask(handler, requestCode, path, parentPath, session)
				.execute();
	}

	private class DeleteTask extends BaseAsyncTask {

		String path;
		String parentPath;

		public DeleteTask(IDataCallBack handler, int requestCode, String path,
				String parentPath, Bundle session) {
			super(handler, requestCode, session);
			this.path = path;
		}

		@Override
		protected Event doInBackground(Void... params) {
			try {
				Entry entry = mApi.delete(path);
				event.data = entry;
			} catch (VDiskException e) {
				e.printStackTrace();
				event = updateEvent(mContext, e, event);
			}

			return event;
		}

	}

	/**
	 * 批量删除文件和文件夹
	 * 
	 * @param handler
	 * @param requestCode
	 * @param sourceFolderPath
	 *            删除文件所在路径
	 * @param fileNames
	 *            删除文件名列表
	 * @param dirPaths
	 *            删除文件夹路径列表
	 * @param session
	 */
	public BatchDeleteVDiskTask batchDelete(IDataCallBack handler,
			int requestCode, String sourceFolderPath, List<String> paths,
			Bundle session) {
		BatchDeleteVDiskTask task = new BatchDeleteVDiskTask(handler,
				requestCode, sourceFolderPath, paths, session);
		task.execute();
		return task;
	}

	public static class BatchDeleteResult {

		public List<Entry> deletedEntries;
		public List<String> errPaths;
		public boolean isCanceled;

		public BatchDeleteResult(List<Entry> deletedEntries,
				List<String> errPaths, boolean isCanceled) {
			this.deletedEntries = deletedEntries;
			this.errPaths = errPaths;
			this.isCanceled = isCanceled;
		}

	}

	public class BatchDeleteVDiskTask extends BaseAsyncTask {
		String sourceFolderPath;
		List<String> filePaths;
		boolean isCanceled = false;

		public BatchDeleteVDiskTask(IDataCallBack handler, int requestCode,
				String sourceFolderPath, List<String> filePaths, Bundle session) {
			super(handler, requestCode, session);
			this.sourceFolderPath = sourceFolderPath;
			this.filePaths = filePaths;
		}

		public void cancel() {
			isCanceled = true;
		}

		@Override
		protected Event doInBackground(Void... params) {
			List<String> errDirs = new ArrayList<String>();
			List<Entry> deletedEntries = new ArrayList<Entry>();

			for (String path : filePaths) {
				if (!isCanceled) {
					try {
						deletedEntries.add(mApi.delete(path));
					} catch (VDiskException e) {
						errDirs.add(path);
						e.printStackTrace();
					}
				}
			}

			event.data = new BatchDeleteResult(deletedEntries, errDirs,
					isCanceled);

			return event;
		}
	}

	public class InitVDiskDirListTask extends CloudAsyncTask<Void, Void, Event> {

		IDataCallBack handler;
		Event event = new Event();
		Bundle session;
		String path;

		public InitVDiskDirListTask(IDataCallBack handler, int requestCode,
				String path, Bundle session) {
			this.handler = handler;
			event.requestCode = requestCode;
			this.path = path;
			this.session = session;
		}

		@Override
		protected Event doInBackground(Void... params) {

			try {
				Logger.d(TAG, "metedata Path-->" + path);
				Entry root = mApi.metadata(path, null, true, false);
				ArrayList<EntryWrapper> list = new ArrayList<EntryWrapper>();
				List<Entry> entries = root.contents;

				for (Entry entry : entries) {
					if (entry.isDir) {
						EntryWrapper elt = new EntryWrapper(entry);
						list.add(elt);
					}
				}
				event.data = list;

				Logger.d(TAG, "metedata Path size-->" + list.size());
			} catch (VDiskException e) {
				e.printStackTrace();
				event = updateEvent(mContext, e, event);
			}

			return event;
		}

		@Override
		protected void onPostExecute(Event event) {
			handler.handleServiceResult(event.requestCode, event.errCode,
					event.data, session);
		}

	}

	/**
	 * 移动文件或文件夹
	 * 
	 * @param handler
	 * @param requestCode
	 * @param copyRef
	 *            文件路径
	 * @param sSession
	 */
	public void move(IDataCallBack handler, int requestCode, String sourcePath,
			String targetPath, Bundle session) {
		new MoveTask(handler, requestCode, sourcePath, targetPath, session)
				.execute();
	}

	private class MoveTask extends BaseAsyncTask {

		String sourcePath;
		String targetPath;

		public MoveTask(IDataCallBack handler, int requestCode,
				String sourcePath, String targetPath, Bundle session) {
			super(handler, requestCode, session);
			this.sourcePath = sourcePath;
			this.targetPath = targetPath;
		}

		@Override
		protected Event doInBackground(Void... params) {
			try {
				Entry entry = mApi.move(sourcePath, targetPath);
				entry.path = sourcePath;
				event.data = entry;
			} catch (VDiskException e) {
				e.printStackTrace();
				event = updateEvent(mContext, e, event);
			}

			return event;
		}
	}

	/**
	 * 批量移动接口
	 * 
	 * @author Kevin
	 * 
	 */
	public class BatchMoveTask extends BaseAsyncTask {
		IDataCallBack handler;

		ArrayList<String> fileList;
		String sourceFolder;
		String targetFolder;
		boolean isCanceled = false;

		public BatchMoveTask(IDataCallBack handler, int requestCode,
				Bundle session, ArrayList<String> fileList,
				String sourceFolder, String targetFolder) {
			super(handler, requestCode, session);
			this.fileList = fileList;
			this.sourceFolder = sourceFolder;
			this.targetFolder = targetFolder;
		}

		public void cancelBatchMove() {
			isCanceled = true;
		}

		@Override
		protected Event doInBackground(Void... params) {
			ArrayList<String> failResult = new ArrayList<String>();
			ArrayList<Entry> successEntries = new ArrayList<Entry>();

			// 队列移动文件夹
			// 生成队列，对每个文件夹分别移动，如果中途出错，记录下来，继续移动
			if (fileList != null && !fileList.isEmpty()) {
				for (String folderName : fileList) {
					Logger.d(TAG, "dirList.size-->" + fileList.size());
					if (!isCanceled) {
						String fromPath = sourceFolder + "/" + folderName;
						String toPath = targetFolder + "/" + folderName;

						try {
							successEntries.add(mApi.move(fromPath, toPath));
						} catch (VDiskException e) {
							e.printStackTrace();
							failResult.add(folderName);
						}
					}
				}
			}

			ArrayList<Entry> sourceEntries = new ArrayList<Entry>(
					successEntries);

			for (Entry entry : sourceEntries) {
				entry.path = sourceFolder + "/" + entry.fileName();
				entry.path = entry.path.replace("//", "/");
				Logger.d(TAG, "Entry.path:" + entry.path);
			}

			event.data = new Object[] { failResult, sourceEntries };

			return event;
		}
	}
	
	/**
	 * 文件、文件夹重命名
	 * 
	 * @param handler
	 * @param requestCode
	 * @param parentPath
	 *            文件路径
	 * @param oldName
	 *            原来的名字
	 * @param newName
	 *            新的名字
	 * @param isDir
	 * @param sSession
	 */
	public void rename(IDataCallBack handler, int requestCode,
			String parentPath, String oldName, String newName, Bundle session) {
		new RenameTask(handler, requestCode, parentPath, oldName, newName,
				session).execute();
	}

	private class RenameTask extends BaseAsyncTask {

		String parentPath;
		String oldName;
		String newName;

		public RenameTask(IDataCallBack handler, int requestCode,
				String parentPath, String oldName, String newName,
				Bundle session) {
			super(handler, requestCode, session);
			this.parentPath = parentPath;
			this.oldName = oldName;
			this.newName = newName;
		}

		@Override
		protected Event doInBackground(Void... params) {
			try {
				parentPath = Utils.removePathLastSlice(parentPath);
				String oldPath = parentPath
						+ (parentPath.equals("/") ? "" : "/") + oldName;
				String newPath = parentPath
						+ (parentPath.equals("/") ? "" : "/") + newName;
				Logger.d(TAG, "oldPath: " + oldPath + ", newPath: " + newPath);
				Entry entry = mApi.move(oldPath, newPath);
				event.data = entry;
			} catch (VDiskException e) {
				e.printStackTrace();
				event = updateEvent(mContext, e, event);
			}

			return event;
		}

	}

	public static Event updateEvent(Context ctx, VDiskException e, Event event) {
		if (event == null) {
			event = new Event();
		}
		if (e instanceof VDiskServerException) {
			return ExceptionHandler.getErrEvent(ctx, (VDiskServerException) e,
					event);
		} else if (e instanceof VDiskIOException) {
			event.errCode = ExceptionHandler.VdiskConnectionFailureErrorType;
		} else if (e instanceof VDiskParseException) {
			event.errCode = ExceptionHandler.kVdiskErrorInvalidResponse;
		} else if (e instanceof VDiskLocalStorageFullException) {
			event.errCode = ExceptionHandler.kVdiskErrorInsufficientDiskSpace;
		} else if (e instanceof VDiskUnlinkedException) {
			event.errCode = ExceptionHandler.UNLINKED_ERROR;
		} else if (e instanceof VDiskFileSizeException) {
			event.errCode = ExceptionHandler.FILE_TOO_BIG_ERROR;
		} else if (e instanceof VDiskPartialFileException) {
			event.errCode = ExceptionHandler.PARTIAL_FILE_ERROR;
		} else if (e instanceof VDiskFileNotFoundException) {
			event.errCode = ExceptionHandler.FILE_NOT_FOUND;
		} else {
			event.errCode = ExceptionHandler.OTHER_ERROR;
		}
		return event;
	}

}
