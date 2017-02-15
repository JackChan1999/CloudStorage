package com.itcast.cloudstorage.upload;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Stack;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.itcast.cloudstorage.R;
import com.itcast.cloudstorage.bean.LocalFileDirInfo;
import com.itcast.cloudstorage.bean.UploadTask;
import com.itcast.cloudstorage.net.CloudEngine;
import com.itcast.cloudstorage.net.CloudEngine.ExploerLoadSdFileTask;
import com.itcast.cloudstorage.net.IDataCallBack;
import com.itcast.cloudstorage.upload.net.UploadManager;
import com.itcast.cloudstorage.utils.CloudAsyncTask;
import com.itcast.cloudstorage.utils.Logger;
import com.itcast.cloudstorage.utils.Utils;

public class UploadFileExploerActivity extends SherlockActivity implements
		OnClickListener, IDataCallBack {

	private static final String TAG = UploadFileExploerActivity.class
			.getSimpleName();

	private static final int REQUEST_LOAD_SD_FILE = 2;

	private Button mButton_select;
	private Button mButton_upload;

	private boolean mSelected;

	public File root;

	private ViewHolder holder;
	public FileExploerAdapter exploerAdapter;
	public ListView listView;

	private ProgressDialog pd;
	public File[] fileList;
	public File curFile;
	private Stack<Integer> history = new Stack<Integer>();

	public static final int SERVER_ERR = 500;
	public UploadTask tempUploadTask;

	private static final int UPLOAD_MSG = 0x0;
	private static final int START_PROGRESS = 0x1;
	private static final int END_PROGRESS = 0x2;
	
	private int selectSize;
	private boolean isCanceled;

	private Stack<Recorder> mStack = new Stack<Recorder>();
	private ExploerLoadSdFileTask mExploerLoadSdFileTask;
	public ArrayList<LocalFileDirInfo> mFileData;// 当前目录下文件数，不包括文件夹

	private class Recorder {
		/* 当前文件夹名 */
		@SuppressWarnings("unused")
		String dirPath = "";
		/* 当前文件夹下的LIST */
		ArrayList<LocalFileDirInfo> list;
	}

	private String mDesPath = "/";

	private TextView mEmptyView;
	private TextView mTvDesPath;
	
	private Dialog progressDialog;
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case UPLOAD_MSG:
				if (pd != null && pd.isShowing()) {
					pd.dismiss();
				}
				finish();
				break;
			case START_PROGRESS:
				if (progressDialog != null && !progressDialog.isShowing()) {
					View v = getLayoutInflater().inflate(
							R.layout.custom_progress, null);
					progressDialog.setContentView(v);
					progressDialog.show();
				}

				break;
			case END_PROGRESS:
				if (progressDialog != null)
					progressDialog.cancel();
				break;
			}
		}
	};

	public ArrayList<LocalFileDirInfo> list;
	public ArrayList<LocalFileDirInfo> selectedData = new ArrayList<LocalFileDirInfo>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		getSupportActionBar().setDisplayShowHomeEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(
				getString(com.itcast.cloudstorage.R.string.local_file_label));

		setContentView(R.layout.local_file_exploer);
		mButton_select = (Button) findViewById(R.id.btn_select);
		mButton_select.setOnClickListener(this);
		mButton_upload = (Button) findViewById(R.id.btn_upload);
		mButton_upload.setOnClickListener(this);
		mTvDesPath = (TextView) findViewById(R.id.tv_dir);
		if (!Utils.isMountSdCard(this)) {
			Utils.showToast(this, R.string.please_insert_sdcard,
					Toast.LENGTH_SHORT);

			super.onCreate(savedInstanceState);
			return;
		}

		history.clear();
		history.push(0);
		root = Environment.getExternalStorageDirectory();
		curFile = root;

		list = new ArrayList<LocalFileDirInfo>();

		listView = (ListView) findViewById(R.id.list);
		listView.setItemsCanFocus(false);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		exploerAdapter = new FileExploerAdapter(UploadFileExploerActivity.this);
		listView.setAdapter(exploerAdapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				LocalFileDirInfo info = list.get(position);
				if (!info.isFile) {
					initCurrentFolderState();
					history.push(0);
					list.clear();
					refresh();
					curFile = info.file;
					Recorder r = new Recorder();
					r.dirPath = curFile.getPath();
					mStack.push(r);
					startLoadTask();
				} else {
					// is file
					holder = (ViewHolder) view.getTag();
					holder.ch_upload.toggle();
					info.isSelected = true;
					exploerAdapter.isSelected.put(position,
							holder.ch_upload.isChecked());

					if (holder.ch_upload.isChecked()) {
						selectedData.add(list.get(position));
					} else {
						info.isSelected = false;
						selectedData.remove(list.get(position));
						if (selectedData.size() == 0) {
							 UploadManager.getInstance(
							 UploadFileExploerActivity.this)
							 .clearUploadQueue();
						}
					}

					changeButtonState();
					if (!selectedData.isEmpty()) {
						mSelected = true;
					} else {
						mSelected = false;
					}
				}
			}
		});

		mEmptyView = new TextView(this);
		mEmptyView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		mEmptyView.setGravity(Gravity.CENTER);
		mEmptyView.setTextColor(getResources().getColor(R.color.black));
		mEmptyView.setText(R.string.empty_upload_all);
		mEmptyView.setVisibility(View.GONE);
		
		progressDialog = new Dialog(this, R.style.custom_dialog_style);
		View v = getLayoutInflater().inflate(R.layout.custom_progress, null);
		progressDialog.setContentView(v);
		progressDialog.setCancelable(true);

		((ViewGroup) listView.getParent()).addView(mEmptyView);

		initStackInfo();

		startLoadTask();
		
		changeButtonState();
		
		String extra = getIntent().getStringExtra("currentPath");
		if(!TextUtils.isEmpty(extra)) {
			mDesPath = extra;
			Logger.d(TAG, "des path-->" + mDesPath);
		}
		
		mTvDesPath.setText(mDesPath);

		super.onCreate(savedInstanceState);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Logger.d(TAG, "onclick back btn");
			if (!Utils.isMountSdCard(this)) {
				Logger.d(TAG, "onclick back btn before finish");
				finish();
				Logger.d(TAG, "onclick back btn after finish");
				return true;
			}

			initCurrentFolderState();

			if (history.size() > 0)
				history.pop();
			if (isRootDirectory(curFile)) {
				finish();
			} else {
				mStack.pop();
				curFile = curFile.getParentFile();
				list.clear();
				list.addAll(mStack.lastElement().list);
				if (exploerAdapter != null) {
					exploerAdapter.init();
					for (int i = 0; i < exploerAdapter.getFileCount(); i++) {
						this.exploerAdapter.getFileItem(i).isSelected = false;
						selectedData.remove(mFileData.get(i));
					}
				}
				refresh();
				// afterServiceConnected();
			}
			break;
		}

		return true;
	}

	private void startLoadTask() {
		mHandler.sendEmptyMessage(START_PROGRESS);
		
		if (mExploerLoadSdFileTask != null
				&& mExploerLoadSdFileTask.getStatus() == CloudAsyncTask.Status.RUNNING) {
			mExploerLoadSdFileTask.cancel(true);
		}
		mExploerLoadSdFileTask = CloudEngine.getInstance(this).new ExploerLoadSdFileTask(
				this, REQUEST_LOAD_SD_FILE, curFile, null);
		mExploerLoadSdFileTask.execute();
	}

	private void initStackInfo() {
		Recorder r = new Recorder();
		r.dirPath = Environment.getExternalStorageDirectory().getPath();
		r.list = new ArrayList<LocalFileDirInfo>();
		mStack.push(r);
	}

	@Override
	protected void onDestroy() {
		if (selectedData != null && !selectedData.isEmpty()) {
			selectedData.clear();
		}
		super.onDestroy();
	}

	private void refresh() {
		exploerAdapter.notifyDataSetChanged();
		int curPos;
		try {
			curPos = history.lastElement();
			if (curPos < 0 || curPos > listView.getBottom()) {
				history.set(history.size() - 1, 0);
			}
			listView.setSelection(history.lastElement());
		} catch (NoSuchElementException e) {
			e.printStackTrace();
		}
	}

	private boolean isRootDirectory(File f) {
		if (f.getPath().equals(root.getPath()))
			return true;
		return false;
	}

	class FileExploerAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private HashMap<Integer, Boolean> isSelected;

		public FileExploerAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		public void init() {
			mFileData = new ArrayList<LocalFileDirInfo>();
			int size = list.size();
			for (int i = 0; i < size; i++) {
				if (list.get(i).isFile) {
					mFileData.add(list.get(i));
				}
			}
			isSelected = new HashMap<Integer, Boolean>();
			int mSize = mFileData.size();
			for (int i = 0; i < mSize; i++) {
				isSelected.put(i, false);
			}
		}

		@Override
		public int getCount() {
			if (list == null || list.size() == 0) {
				return 0;
			} else {
				return list.size();
			}
		}

		// 当前目录下文件数，不包括文件夹
		public int getFileCount() {
			if (mFileData == null || mFileData.size() == 0) {
				return 0;
			} else {
				return mFileData.size();
			}
		}

		@Override
		public LocalFileDirInfo getItem(int position) {
			return list.get(position);
		}

		public LocalFileDirInfo getFileItem(int position) {
			return mFileData.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (convertView == null) {

				convertView = mInflater.inflate(
						R.layout.file_exploer_list_item, null);
				holder = new ViewHolder();
				holder.image = (ImageView) convertView
						.findViewById(R.id.file_icon);
				holder.text = (TextView) convertView
						.findViewById(R.id.fileDirName);
				holder.textSize = (TextView) convertView
						.findViewById(R.id.fileSize);
				holder.modifyText = (TextView) convertView
						.findViewById(R.id.lastModifyTime);
				holder.btn_enter = (Button) convertView
						.findViewById(R.id.enterBtn);
				holder.ch_upload = (CheckBox) convertView
						.findViewById(R.id.uploadCheckBox);
				convertView.setTag(holder);

			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.ch_upload.setChecked(list.get(position).isSelected);
			holder.update(UploadFileExploerActivity.this, list.get(position));
			return convertView;
		}
	}

	private class ViewHolder {

		public ImageView image;
		public TextView text;
		private TextView textSize;
		public TextView modifyText;
		public Button btn_enter;
		public CheckBox ch_upload;
		public String size;

		public void update(Context ctx, final LocalFileDirInfo info) {
			text.setText(info.name);
			btn_enter.setVisibility(View.GONE);
			ch_upload.setVisibility(View.GONE);
			if (info.isFile) {
				Object[] arr = Utils.getMIMEType(info.name);
				image.setImageResource((Integer) arr[1]);

				size = info.filesize;
				ch_upload.setVisibility(View.VISIBLE);
			} else {
				btn_enter.setVisibility(View.VISIBLE);
				image.setImageResource(R.drawable.directory_icon);
				size = String.format(ctx.getString(R.string.home_list_dir_num),
						info.fileNum);
			}
			textSize.setText(size);
			modifyText.setText(info.ctime);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {

			if (!Utils.isMountSdCard(this)) {
				finish();
				return true;
			}

			initCurrentFolderState();

			if (history.size() > 0)
				history.pop();
			if (isRootDirectory(curFile)) {
				finish();
			} else {
				/*
				 * curFile = curFile.getParentFile(); afterServiceConnected();
				 */
				mStack.pop();
				curFile = curFile.getParentFile();
				list.clear();
				list.addAll(mStack.lastElement().list);
				if (exploerAdapter != null) {
					exploerAdapter.init();
					for (int i = 0; i < exploerAdapter.getFileCount(); i++) {
						this.exploerAdapter.getFileItem(i).isSelected = false;
						selectedData.remove(mFileData.get(i));
					}
				}
				refresh();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void initCurrentFolderState() {

		if (mExploerLoadSdFileTask != null
				&& mExploerLoadSdFileTask.getStatus() == CloudAsyncTask.Status.RUNNING) {
			mExploerLoadSdFileTask.cancel(true);
		}

		if (!selectedData.isEmpty()) {
			selectedData.clear();
		}
		mSelected = false;
		
		mButton_upload.setText(getString(R.string.upload_bottom_label) + "(" + 0 + ")");
		mButton_select.setText(getResources().getString(
				R.string.all_select_button));
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_select: {
			changeSelectedFile();
			changeButtonState();
			break;
		}
		case R.id.btn_upload: {
			if (Utils.isNetworkAvailable(this)) {
				pd = new ProgressDialog(UploadFileExploerActivity.this);
				pd.setMessage(getString(R.string.init_upload_queue));
				pd.setCancelable(true);
				pd.setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						isCanceled = true;
						finish();
					}
				});
				pd.setIndeterminate(true);
				pd.show();
				if (pd != null && pd.isShowing()) {
					selectSize = selectedData.size();
					if (selectSize != 0) {
						new Thread(new Runnable() {

							@Override
							public void run() {
								for (int i = 0; i < selectSize; i++) {
									if (isCanceled) {
										break;
									}
									try {
										LocalFileDirInfo localFileDirInfo = selectedData
												.get(i);
										UploadManager.getInstance(
												UploadFileExploerActivity.this)
												.uploadFile(
														localFileDirInfo.name,
														localFileDirInfo.file
																.getPath(),
														mDesPath, "others");
									} catch (Exception e) {
										e.printStackTrace();
									}
								}

								mHandler.sendEmptyMessage(UPLOAD_MSG);
							}
						}).start();
					} else {
						pd.dismiss();
						Utils.showToast(UploadFileExploerActivity.this,
								R.string.no_upload_file_toast, 0);
					}
				}
			} else {
				Utils.showToast(this, R.string.no_network_connection_toast, 0);
			}
			break;
		}
		
		default:
			break;
		}
	}

	private void changeSelectedFile() {
		if (!selectedData.isEmpty()) {
			selectedData.clear();
		}
		if (!mSelected) {
			mSelected = true;
			if (exploerAdapter != null) {
				for (int i = 0; i < exploerAdapter.getFileCount(); i++) {
					this.exploerAdapter.getFileItem(i).isSelected = true;
					selectedData.add(mFileData.get(i));
				}
				this.exploerAdapter.notifyDataSetChanged();
			}
		} else {
			mSelected = false;
			if (exploerAdapter != null) {
				for (int i = 0; i < exploerAdapter.getFileCount(); i++) {
					this.exploerAdapter.getFileItem(i).isSelected = false;
					selectedData.remove(mFileData.get(i));
				}
				this.exploerAdapter.notifyDataSetChanged();
			}
		}
	}

	private void changeButtonState() {
		if (!selectedData.isEmpty()) {
			mButton_upload.setText(getString(R.string.upload_bottom_label) + "("
					+ selectedData.size() + ")");
			mButton_select.setText(getResources().getString(R.string.cancel));

		} else {
			mButton_upload.setText(getString(R.string.upload_bottom_label) + "(" + 0 + ")");
			mButton_select.setText(getResources().getString(
					R.string.all_select_button));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleServiceResult(int requestCode, int errCode, Object data,
			Bundle sessio) {
		mHandler.sendEmptyMessage(END_PROGRESS);
		switch (requestCode) {
		case REQUEST_LOAD_SD_FILE:
			if (errCode == 0) {
				if (list != null && !list.isEmpty()) {
					list.clear();
				}
				list = (ArrayList<LocalFileDirInfo>) data;
				mStack.lastElement().list = new ArrayList<LocalFileDirInfo>(
						list);
				this.exploerAdapter.init();

				if (list.isEmpty()) {
					listView.setEmptyView(mEmptyView);
				}

				refresh();
			}
			break;

		default:
			break;
		}
	}
}
