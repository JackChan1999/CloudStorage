package com.itcast.cloudstorage.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.itcast.cloudstorage.bean.DownloadEntry;
import com.itcast.cloudstorage.bean.LocalFileInfo;
import com.itcast.cloudstorage.bean.UploadTask;
import com.itcast.cloudstorage.bean.User;
import com.itcast.cloudstorage.upload.net.ComplexUploadTask;
import com.itcast.cloudstorage.upload.net.SimpleUploadTask;
import com.itcast.cloudstorage.upload.net.UploadManager;

public class CloudDB extends SQLiteOpenHelper {

	private static final String TAG = CloudDB.class.getSimpleName();

	private static Context sContext;

	public final static String DB_NAME = "clouddb";

	private final static int VERSION = 2;

	private static CloudDB instance = null;

	private SQLiteDatabase db = null;

	public final static String UID = "uid";

	// download
	private final static String DOWNLOAD_TASK_TABLE = "download_table";
	private final static String DOWNLOAD_TASK_ID = "_id";
	private final static String DOWNLOAD_TASK_PATH = "path_or_copyref";
	private final static String DOWNLOAD_TASK_NAME = "filename";
	private final static String DOWNLOAD_TASK_BYTES = "bytes";
	private final static String DOWNLOAD_TASK_SIZE = "size";
	private final static String DOWNLOAD_TASK_MD5 = "md5";
	private final static String DOWNLOAD_TASK_SHA1 = "sha1";
	private final static String DOWNLOAD_TASK_MODIFIED = "modified";
	private final static String DOWNLOAD_TASK_PROGRESS = "progress";
	private final static String DOWNLOAD_TASK_STATE = "state";
	private final static String DOWNLOAD_TASK_SOURCE = "source";
	private final static String DOWNLOAD_TASK_DATA = "data";
	private final static String DOWNLOAD_TASK_LOCAL_PATH = "local_path";

	// local file
	private final static String LOCAL_FILE_TABLE = "local_file_table";
	private final static String LOCAL_FILE_ID = "_id";
	private final static String LOCAL_FILE_PATH = "path";
	private final static String LOCAL_FILE_FILENAME = "filename";
	private final static String LOCAL_FILE_BYTES = "bytes";
	private final static String LOCAL_FILE_MD5 = "md5";
	private final static String LOCAL_FILE_SHA1 = "sha1";
	private final static String LOCAL_FILE_MODIFIED = "modified";
	private final static String LOCAL_FILE_SOURCE = "source";
	private final static String LOCAL_FILE_DATA = "data"; // 收藏标记state

	// upload columns
	private final static String UPLOAD_TABLE = "upload_table";
	public final static String UPLOAD_ID = "_id";
	public final static String TASK_UPLOAD_FILE_NAME = "filename";
	public final static String TASK_UPLOAD_FILE_PATH = "filepath";
	public final static String TASK_UPLOAD_TARGET_FOLDER_PATH = "path";

	public final static String TASK_UPLOAD_CTIME = "ctime";
	public final static String TASK_UPLOAD_TASK = "task";
	public final static String TASK_UPLOAD_PROGRESS = "fileprogress";
	public final static String TASK_UPLOAD_STATE = "state";
	public final static String TASK_UPLOAD_SIZE = "filesize";
	public final static String TASK_UPLOAD_DATA = "data";

	// upload_session table
	private final static String UPLOAD_SESSION_TABLE = "upload_session_table";
	public final static String UPLOAD_SESSION_ID = "_id";
	public final static String UPLOAD_TASK_ID = "task_id";
	public final static String UPLOAD_FILE_ID = "file_id";
	public final static String UPLOAD_FILE_OBJECT = "file_obj";
	public final static String UPLOAD_UID = "uid";

	private CloudDB(Context context) {
		super(context, DB_NAME, null, VERSION);
		sContext = context;

		try {
			db = this.getWritableDatabase();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static synchronized CloudDB getInstance(Context ctx) {
		if (instance == null) {
			return instance = new CloudDB(ctx);
		}
		return instance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String create_download_sql = "CREATE TABLE IF NOT EXISTS "
				+ DOWNLOAD_TASK_TABLE + " (" + DOWNLOAD_TASK_ID
				+ " Integer primary key autoincrement, " + DOWNLOAD_TASK_PATH
				+ " TEXT, " + DOWNLOAD_TASK_STATE + " TEXT, "
				+ DOWNLOAD_TASK_NAME + " TEXT, " + DOWNLOAD_TASK_BYTES
				+ " TEXT, " + DOWNLOAD_TASK_SIZE + " TEXT, "
				+ DOWNLOAD_TASK_MD5 + " TEXT, " + DOWNLOAD_TASK_MODIFIED
				+ " TEXT, " + DOWNLOAD_TASK_PROGRESS + " TEXT, "
				+ DOWNLOAD_TASK_SOURCE + " TEXT, " + DOWNLOAD_TASK_SHA1
				+ " TEXT, " + DOWNLOAD_TASK_DATA + " TEXT, "
				+ DOWNLOAD_TASK_LOCAL_PATH + " TEXT, " + UID + " TEXT, UNIQUE("
				+ DOWNLOAD_TASK_PATH + ", " + UID + ") )";

		String local_file_sql = "CREATE TABLE IF NOT EXISTS "
				+ LOCAL_FILE_TABLE + " (" + LOCAL_FILE_ID
				+ " Integer primary key autoincrement, " + LOCAL_FILE_PATH
				+ " TEXT, " + LOCAL_FILE_FILENAME + " TEXT, "
				+ LOCAL_FILE_BYTES + " TEXT, " + LOCAL_FILE_MD5 + " TEXT, "
				+ LOCAL_FILE_SHA1 + " TEXT, " + LOCAL_FILE_MODIFIED + " TEXT, "
				+ LOCAL_FILE_SOURCE + " TEXT, " + LOCAL_FILE_DATA + " TEXT, "
				+ UID + " TEXT, UNIQUE(" + LOCAL_FILE_PATH + ", " + UID + ") )";

		String upload_sql = "CREATE TABLE IF NOT EXISTS " + UPLOAD_TABLE + " ("
				+ UPLOAD_ID + " Integer primary key autoincrement, "
				+ TASK_UPLOAD_FILE_NAME + " TEXT, " + TASK_UPLOAD_FILE_PATH
				+ " TEXT, " + TASK_UPLOAD_TARGET_FOLDER_PATH + " TEXT, "
				+ TASK_UPLOAD_CTIME + " TEXT, " + TASK_UPLOAD_STATE + " TEXT, "
				+ TASK_UPLOAD_SIZE + " TEXT, " + TASK_UPLOAD_PROGRESS
				+ " INTEGER, " + TASK_UPLOAD_DATA + " TEXT, " + UID + " TEXT )";
		
		String upload_session_sql = "CREATE TABLE IF NOT EXISTS "
				+ UPLOAD_SESSION_TABLE + " (" + UPLOAD_SESSION_ID
				+ " Integer primary key autoincrement, " + UPLOAD_TASK_ID
				+ " Integer, " + UPLOAD_FILE_ID + " TEXT, "
				+ UPLOAD_FILE_OBJECT + " TEXT, " + UID + " TEXT )";

		db.execSQL(create_download_sql);
		db.execSQL(local_file_sql);
		db.execSQL(upload_sql);
		db.execSQL(upload_session_sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Logger.e(TAG, "onUpgrade");
		onCreate(db);
	}

	public DownloadEntry getDownloadEntry(String path) {
		DownloadEntry downloadEntry = null;

		Cursor cursor = db.query(DOWNLOAD_TASK_TABLE, null, DOWNLOAD_TASK_PATH
				+ " = ? AND " + UID + " = ?",
				new String[] { path, User.getUid(sContext) }, null, null, null);
		if (cursor.moveToNext()) {
			downloadEntry = cursorToDownloadEntry(cursor);
		}
		cursor.close();

		return downloadEntry;
	}

	private DownloadEntry cursorToDownloadEntry(Cursor cursor) {
		DownloadEntry downloadEntry = new DownloadEntry();
		downloadEntry._id = cursor.getInt(cursor
				.getColumnIndexOrThrow(CloudDB.DOWNLOAD_TASK_ID));
		downloadEntry.pathOrCopyRef = String.valueOf(cursor.getString(cursor
				.getColumnIndexOrThrow(CloudDB.DOWNLOAD_TASK_PATH)));
		downloadEntry.name = cursor.getString(cursor
				.getColumnIndexOrThrow(CloudDB.DOWNLOAD_TASK_NAME));
		downloadEntry.bytes = cursor.getLong(cursor
				.getColumnIndexOrThrow(CloudDB.DOWNLOAD_TASK_BYTES));
		downloadEntry.size = cursor.getString(cursor
				.getColumnIndexOrThrow(CloudDB.DOWNLOAD_TASK_SIZE));
		downloadEntry.md5 = cursor.getString(cursor
				.getColumnIndexOrThrow(CloudDB.DOWNLOAD_TASK_MD5));
		downloadEntry.lastModifyTime = cursor.getString(cursor
				.getColumnIndexOrThrow(CloudDB.DOWNLOAD_TASK_MODIFIED));
		downloadEntry.source = cursor.getString(cursor
				.getColumnIndex(CloudDB.DOWNLOAD_TASK_SOURCE));
		downloadEntry.fileProgress = cursor.getString(cursor
				.getColumnIndex(CloudDB.DOWNLOAD_TASK_PROGRESS));
		downloadEntry.state = cursor.getString(cursor
				.getColumnIndexOrThrow(CloudDB.DOWNLOAD_TASK_STATE));
		downloadEntry.localPath = cursor.getString(cursor
				.getColumnIndex(DOWNLOAD_TASK_LOCAL_PATH));

		Logger.d(TAG, "downloadEntry: " + downloadEntry.toString());

		return downloadEntry;
	}
	
	private UploadTask cursorToUploadEntry(Cursor cursor) {
		UploadTask uploadTask;

		long fileSize = Long
				.parseLong(cursor.getString(cursor
						.getColumnIndexOrThrow(TASK_UPLOAD_SIZE)));
		if (fileSize <= UploadManager.UPLOAD_FILE_POINT) {
			// 小于等于4M时采用小文件处理
			uploadTask = new SimpleUploadTask();
		} else {
			// 采用大文件处理
			uploadTask = new ComplexUploadTask();
		}
		uploadTask.taskid = String
				.valueOf(cursor.getInt(cursor
						.getColumnIndexOrThrow(UPLOAD_ID)));
		uploadTask.filename = cursor
				.getString(cursor
						.getColumnIndexOrThrow(TASK_UPLOAD_FILE_NAME));
		uploadTask.srcPath = cursor
				.getString(cursor
						.getColumnIndexOrThrow(TASK_UPLOAD_FILE_PATH));
		uploadTask.desPath = cursor
				.getString(cursor
						.getColumnIndexOrThrow(TASK_UPLOAD_TARGET_FOLDER_PATH));
		uploadTask.state = cursor
				.getString(cursor
						.getColumnIndexOrThrow(TASK_UPLOAD_STATE));
		uploadTask.fileSize = fileSize;

		uploadTask.fileprogress = cursor
				.getInt(cursor
						.getColumnIndexOrThrow(TASK_UPLOAD_PROGRESS));
		
		return uploadTask;
	}

	public boolean updateDownloadEntry(DownloadEntry downloadEntry) {
		String whereClause = DOWNLOAD_TASK_PATH + " = ? AND " + UID + " = ?";
		String[] whereArgs = { downloadEntry.pathOrCopyRef,
				User.getUid(sContext) };
		ContentValues c = new ContentValues();
		c.put(DOWNLOAD_TASK_STATE, downloadEntry.state);
		c.put(DOWNLOAD_TASK_NAME, downloadEntry.name);
		c.put(DOWNLOAD_TASK_PROGRESS, downloadEntry.fileProgress);
		// c.put(DOWNLOAD_TASK_LOCAL_PATH, downloadEntry.localPath);
		return db.update(DOWNLOAD_TASK_TABLE, c, whereClause, whereArgs) > 0;
	}

	public boolean insertDownloadEntry(DownloadEntry entry) {
		ContentValues c = new ContentValues();
		c.put(DOWNLOAD_TASK_PATH, entry.pathOrCopyRef);
		c.put(DOWNLOAD_TASK_NAME, entry.name);
		c.put(DOWNLOAD_TASK_BYTES, entry.bytes);
		c.put(DOWNLOAD_TASK_SIZE, entry.size);
		c.put(DOWNLOAD_TASK_MD5, entry.md5);
		c.put(DOWNLOAD_TASK_MODIFIED, entry.lastModifyTime);
		c.put(DOWNLOAD_TASK_SOURCE, entry.source);
		c.put(DOWNLOAD_TASK_PROGRESS, entry.fileProgress);
		c.put(DOWNLOAD_TASK_STATE, entry.state);
		c.put(DOWNLOAD_TASK_DATA, entry.data);
		c.put(UID, User.getUid(sContext));
		c.put(DOWNLOAD_TASK_LOCAL_PATH, entry.localPath);
		long rownum = db.insert(DOWNLOAD_TASK_TABLE, null, c);

		return rownum != -1;
	}

	public boolean insertLocalFile(LocalFileInfo localFileInfo) {
		try {
			ContentValues c = new ContentValues();
			c.put(LOCAL_FILE_PATH, localFileInfo.path);
			c.put(LOCAL_FILE_FILENAME, localFileInfo.filename);
			c.put(LOCAL_FILE_BYTES, localFileInfo.bytes);
			c.put(LOCAL_FILE_MD5, localFileInfo.md5);
			c.put(LOCAL_FILE_SHA1, localFileInfo.sha1);
			c.put(LOCAL_FILE_MODIFIED, localFileInfo.modified);
			c.put(LOCAL_FILE_SOURCE, localFileInfo.source);
			c.put(LOCAL_FILE_DATA, localFileInfo.state);
			c.put(UID, User.getUid(sContext));
			return db.insert(LOCAL_FILE_TABLE, null, c) > 0;
		} catch (Exception e) {
			// e.printStackTrace();
			return false;
		}
	}

	public boolean deleteDownloadEntry(String vDiskPath) {
		int rowCount = db.delete(DOWNLOAD_TASK_TABLE, DOWNLOAD_TASK_PATH
				+ " = ? AND " + UID + " = ?",
				new String[] { vDiskPath, User.getUid(sContext) });
		Logger.d(TAG, "deleteDownloadEntry rowCount: " + rowCount);
		return rowCount > 0;
	}
	
	public boolean deleteUploadEntry(UploadTask task) {
		int rowCount = db.delete(UPLOAD_TABLE, UPLOAD_ID
				+ " = ? AND " + UPLOAD_UID + " = ?",
				new String[] { task.taskid, User.getUid(sContext) });
		Logger.d(TAG, "deleteUploadEntry rowCount: " + rowCount);
		return rowCount > 0;
	}

	public LocalFileInfo getLocalFile(String filename, String source,
			String md5, String sha1) {
		LocalFileInfo localFileInfo = null;

		Cursor cursor = db.query(LOCAL_FILE_TABLE, null, LOCAL_FILE_FILENAME
				+ " = ? AND " + LOCAL_FILE_SOURCE + " = ? AND ("
				+ LOCAL_FILE_MD5 + " = ? OR " + LOCAL_FILE_SHA1 + " = ?) AND "
				+ UID + " = ?",
				new String[] { filename, source, md5 == null ? "" : md5,
						sha1 == null ? "" : sha1, User.getUid(sContext) },
				null, null, null);

		if (cursor != null) {
			if (cursor.getCount() > 0 && cursor.moveToNext()) {
				localFileInfo = cursorToLocalFileInfo(cursor);
			}
			cursor.close();
		}

		return localFileInfo;
	}

	private LocalFileInfo cursorToLocalFileInfo(Cursor cursor) {
		LocalFileInfo localFileInfo = new LocalFileInfo();
		localFileInfo._id = cursor.getInt(cursor
				.getColumnIndexOrThrow(CloudDB.LOCAL_FILE_ID));
		localFileInfo.path = cursor.getString(cursor
				.getColumnIndexOrThrow(CloudDB.LOCAL_FILE_PATH));
		localFileInfo.bytes = cursor.getString(cursor
				.getColumnIndexOrThrow(CloudDB.LOCAL_FILE_BYTES));
		localFileInfo.filename = cursor.getString(cursor
				.getColumnIndexOrThrow(CloudDB.LOCAL_FILE_FILENAME));
		localFileInfo.md5 = cursor.getString(cursor
				.getColumnIndexOrThrow(CloudDB.LOCAL_FILE_MD5));
		localFileInfo.sha1 = cursor.getString(cursor
				.getColumnIndexOrThrow(CloudDB.LOCAL_FILE_SHA1));
		localFileInfo.modified = cursor.getString(cursor
				.getColumnIndexOrThrow(CloudDB.LOCAL_FILE_MODIFIED));
		localFileInfo.source = cursor.getString(cursor
				.getColumnIndexOrThrow(CloudDB.LOCAL_FILE_SOURCE));
		localFileInfo.state = cursor.getString(cursor
				.getColumnIndexOrThrow(CloudDB.LOCAL_FILE_DATA));
		if (localFileInfo.state == null) {
			localFileInfo.state = "";
		}

		return localFileInfo;
	}

	public List<LocalFileInfo> getLocalFiles(String source) {
		List<LocalFileInfo> entries = new ArrayList<LocalFileInfo>();

		Cursor cursor = db.query(LOCAL_FILE_TABLE, null, LOCAL_FILE_SOURCE
				+ " = ? and " + UID + " = ?",
				new String[] { source, User.getUid(sContext) }, null, null,
				LOCAL_FILE_ID + " desc");
		while (cursor.moveToNext()) {
			entries.add(cursorToLocalFileInfo(cursor));
		}
		cursor.close();

		return entries;
	}

	public List<DownloadEntry> getAllDownloadEntries() {
		List<DownloadEntry> entries = new ArrayList<DownloadEntry>();

		Cursor cursor = db.query(DOWNLOAD_TASK_TABLE, null, UID + " = ?",
				new String[] { User.getUid(sContext) }, null, null,
				DOWNLOAD_TASK_ID + " asc");
		while (cursor.moveToNext()) {
			entries.add(cursorToDownloadEntry(cursor));
		}
		cursor.close();

		return entries;
	}
	
	public List<UploadTask> getAllUploadEntries() {
		List<UploadTask> entries = new ArrayList<UploadTask>();
		
		Cursor cursor = db.query(UPLOAD_TABLE, null, UID + " = ?",
				new String[] { User.getUid(sContext) }, null, null,
				UPLOAD_ID + " asc");
		
		while (cursor.moveToNext()) {
			entries.add(cursorToUploadEntry(cursor));
		}
		cursor.close();
		
		return entries;
	}

	public boolean deleteLocalFile(String localPath) {
		return db.delete(LOCAL_FILE_TABLE, LOCAL_FILE_PATH + " = ?",
				new String[] { localPath }) > 0;
	}

	public void updateUploadTable(UploadTask uploadTask) {

		String whereClause = UPLOAD_ID + " =? " + " AND " + UID + "=?";
		String[] whereArgs = { uploadTask.taskid, User.getUid(sContext) };
		ContentValues c = new ContentValues();
		c.put(TASK_UPLOAD_FILE_NAME, uploadTask.filename);
		c.put(TASK_UPLOAD_TARGET_FOLDER_PATH, uploadTask.desPath);

		c.put(TASK_UPLOAD_STATE, uploadTask.state);
		c.put(TASK_UPLOAD_PROGRESS, uploadTask.fileprogress);

		db.update(UPLOAD_TABLE, c, whereClause, whereArgs);
	}

	public boolean insertUploadTask(UploadTask uploadTask) {

		ContentValues c = new ContentValues();
		// c.put(TASK_UPLOAD_ID, uploadTask.fid);
		c.put(TASK_UPLOAD_FILE_NAME, uploadTask.filename);
		c.put(TASK_UPLOAD_FILE_PATH, uploadTask.srcPath);
		c.put(TASK_UPLOAD_TARGET_FOLDER_PATH, uploadTask.desPath);
		c.put(TASK_UPLOAD_CTIME, Utils.getFormateTime(new Date()));

		c.put(TASK_UPLOAD_STATE, uploadTask.state);
		c.put(TASK_UPLOAD_SIZE, uploadTask.fileSize);

		c.put(UID, User.getUid(sContext));
		long rownum = db.insert(UPLOAD_TABLE, null, c);
		if (rownum == -1)
			return false;
		return true;
	}

	public Cursor selectLastRecord() {
		String sql = "SELECT LAST_INSERT_ROWID()";
		Cursor c = db.rawQuery(sql, null);
		c.moveToFirst();
		return c;
	}

	public String readUploadFileInfo(String taskId, String fileId) {
		Cursor cursor = db.query(UPLOAD_SESSION_TABLE,
				new String[] { UPLOAD_FILE_OBJECT }, UPLOAD_TASK_ID
						+ " = ? and " + UPLOAD_FILE_ID + " = ? and "
						+ UPLOAD_UID + " = ?", new String[] { taskId, fileId,
						User.getUid(sContext) }, null, null, null);

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				String serStr = cursor.getString(0);
				cursor.close();
				return serStr;
			}

			cursor.close();
		}

		return null;
	}
	
	/**
	 * 更新上传文件分段信息
	 * 
	 * @param fileId
	 * @param serStr
	 * @return
	 */
	public boolean updateUploadFileInfo(String taskId, String fileId,
			String serStr) {
		if (readUploadFileInfo(taskId, fileId) == null) {
			ContentValues c = new ContentValues();
			c.put(UPLOAD_FILE_ID, fileId);
			c.put(UPLOAD_FILE_OBJECT, serStr);
			c.put(UPLOAD_TASK_ID, taskId);
			c.put(UPLOAD_UID, User.getUid(sContext));

			long rownum = db.insert(UPLOAD_SESSION_TABLE, null, c);
			if (rownum == -1) {
				return false;
			}

			return true;
		} else {
			ContentValues c = new ContentValues();
			c.put(UPLOAD_FILE_OBJECT, serStr);

			db.update(UPLOAD_SESSION_TABLE, c, UPLOAD_TASK_ID + " = ? and "
					+ UPLOAD_FILE_ID + " = ? and " + UPLOAD_UID + " = ?",
					new String[] { taskId, fileId, User.getUid(sContext) });
			return true;
		}
	}
	
	/**
	 * 删除上传文件分段信息
	 * 
	 * @param fileId
	 */
	public void deleteUploadFileInfo(String taskId) {
		String sql = "DELETE FROM " + UPLOAD_SESSION_TABLE + " WHERE "
				+ UPLOAD_TASK_ID + " = ? and " + UPLOAD_UID + " = ?";

		String[] args = new String[] { taskId, User.getUid(sContext) };

		db.execSQL(sql, args);
	}
	
	public boolean deleteRecordById(int id) {
		String sql = "DELETE FROM " + UPLOAD_TABLE + " WHERE " + UPLOAD_ID
				+ " = " + id + " AND " + UID + " = '" + User.getUid(sContext)
				+ "'";
		db.execSQL(sql);
		return true;
	}

}
