package com.itcast.cloudstorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.itcast.cloudstorage.bean.EntryWrapper;
import com.itcast.cloudstorage.net.CloudEngine;
import com.itcast.cloudstorage.net.ExceptionHandler;
import com.itcast.cloudstorage.net.IDataCallBack;
import com.itcast.cloudstorage.utils.Logger;
import com.itcast.cloudstorage.utils.Utils;
import com.umeng.analytics.MobclickAgent;

public class CloudDirActivity extends SherlockActivity implements
		OnClickListener, IDataCallBack {
	protected ProgressBar mProgressBar;
	private TextView mDirTextView;
	private Button mBtnVDiskDirSelect;
	private Button mBtnVDiskDirMakeDir;
	private ListView mListView = null;

	protected int mCurrentSelectPosition;

	private static final String TAG = CloudDirActivity.class.getSimpleName();

	public static final int REQUEST_DIR_LIST = 2;
	protected static final int REQUEST_CREATE_DIRECTORY = 102;

	private ProgressDialog pd;

	private HomeVDiskDirListAdapter mDiskDirListAdapter = null;
	private ArrayList<EntryWrapper> datalist = new ArrayList<EntryWrapper>();

	private Stack<Recorder> mRecHistory;
	private String mCurrentPath = "/";

	private String mSourcePath;

	private class Recorder {
		String path = "/";
		String dirName = "";
		/* 当前文件夹下的LIST */
		ArrayList<EntryWrapper> list;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.cloud_dir_choose_layout);

		getSupportActionBar().setDisplayShowHomeEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(
				getString(R.string.label_weipan_directory_select_title));

		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
		mDirTextView = (TextView) findViewById(R.id.tv_vdisk_dir);
		mBtnVDiskDirSelect = (Button) findViewById(R.id.btn_vdisk_dir_select);
		mBtnVDiskDirMakeDir = (Button) findViewById(R.id.btn_vdisk_dir_make_dir);
		mBtnVDiskDirSelect.setOnClickListener(this);
		mBtnVDiskDirMakeDir.setOnClickListener(this);
		mListView = (ListView) findViewById(R.id.list);

		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mCurrentSelectPosition = position;
				EntryWrapper ele = datalist.get(position);

				enterFolder(ele);
			}
		});

		init();

		mDiskDirListAdapter = new HomeVDiskDirListAdapter();
		mListView.setAdapter(mDiskDirListAdapter);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			break;
		default:
			break;
		}
		return false;
	}

	public void enterFolder(EntryWrapper ele) {
		mDiskDirListAdapter.notifyDataSetChanged();
		mProgressBar.setVisibility(View.VISIBLE);

		if (Utils.isNetworkAvailable(this)) {
			Recorder r = new Recorder();
			r.path = ele.entry.path;
			r.dirName = ele.entry.fileName();
			r.list = new ArrayList<EntryWrapper>();
			mRecHistory.push(r);

			mCurrentPath = r.path;
			mDirTextView.setText(r.path);
			initDirList(r.path, false);
		} else {
			Utils.showToast(this, R.string.no_network_connection_toast, 0);
		}
	}

	private class HomeVDiskDirListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mRecHistory.lastElement().list.size();
		}

		@Override
		public Object getItem(int position) {
			return mRecHistory.lastElement().list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = CloudDirActivity.this.getLayoutInflater()
						.inflate(R.layout.dir_choose_item, null);

				holder = new ViewHolder();
				holder.tvName = (TextView) convertView
						.findViewById(R.id.fileDirName);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			EntryWrapper ele = datalist.get(position);
			holder.tvName.setText(ele.entry.fileName());

			return convertView;
		}
	}

	class ViewHolder {
		TextView tvName;
	}

	@Override
	public void handleServiceResult(int requestCode, int errCode, Object data,
			Bundle session) {

		switch (requestCode) {
		case REQUEST_CREATE_DIRECTORY:
			if (errCode == 0) {
				initDirList(mRecHistory.lastElement().path, false);
				mListView.setSelection(0);
			} else {
				ExceptionHandler.toastErrMessage(this, errCode);
				mProgressBar.setVisibility(View.GONE);
			}
			break;
		case REQUEST_DIR_LIST:
			mProgressBar.setVisibility(View.GONE);
			if (errCode == 0) {
				ArrayList<EntryWrapper> tempLinkedList = mRecHistory
						.lastElement().list;
				if (tempLinkedList != null && !tempLinkedList.isEmpty()) {
					tempLinkedList.clear();
				}
				List<EntryWrapper> tempList = (List<EntryWrapper>) data;
				if (tempList != null && tempList.size() != 0) {
					ArrayList<EntryWrapper> newList = new ArrayList<EntryWrapper>();

					for (int i = 0; i < tempList.size(); i++) {
						EntryWrapper EntryWrapper = tempList.get(i);
						Bundle extras = getIntent().getExtras();
						if (extras != null) {
							ArrayList<String> dirPathList = extras
									.getStringArrayList("batch_move_dir_list");
							if (dirPathList != null
									&& dirPathList
											.contains(EntryWrapper.entry.path)) {
								continue;
							}

							String filePath = extras.getString("filePath");
							mSourcePath = filePath;

							if (filePath != null
									&& filePath.equals(EntryWrapper.entry.path)) {
								continue;
							}

							newList.add(EntryWrapper);
						} else {
							newList.add(EntryWrapper);
						}
					}

					mRecHistory.lastElement().list = newList;
					datalist = newList;
					mDiskDirListAdapter.notifyDataSetChanged();
				}
			} else {
				ExceptionHandler.toastErrMessage(this, errCode);
			}
			break;
		default:
			break;
		}

	}

	private void init() {
		mRecHistory = new Stack<Recorder>();
		Recorder rec = new Recorder();
		rec.path = "/";
		rec.list = new ArrayList<EntryWrapper>();
		mRecHistory.push(rec);

		mProgressBar.setVisibility(View.VISIBLE);
		initDirList(mCurrentPath, true);
	}

	private boolean isRootDirectory() {
		if (mRecHistory.lastElement().path.equals("/"))
			return true;
		return false;
	}

	public void back() {
		mRecHistory.pop();
		if (mProgressBar != null && mProgressBar.isShown()) {
			mProgressBar.setVisibility(View.GONE);
		}
		if (!isRootDirectory()) {
			mCurrentPath = mRecHistory.lastElement().path;
			mDirTextView.setText(mRecHistory.lastElement().path);
		} else {
			mCurrentPath = "/";
			mDirTextView.setText("/");
		}
		datalist = mRecHistory.lastElement().list;
		mDiskDirListAdapter.notifyDataSetChanged();

		Logger.d("position", "position:" + mCurrentSelectPosition);
	}

	private void initDirList(String path, boolean useCache) {
		Logger.d(TAG, path);
		CloudEngine.getInstance(this).new InitVDiskDirListTask(this,
				REQUEST_DIR_LIST, path, null).execute();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_vdisk_dir_select: {
			Intent intent = new Intent();
			intent.putExtra("path", mCurrentPath);
			intent.putExtra("filePath", mSourcePath);
			this.setResult(Activity.RESULT_OK, intent);
			Logger.d(TAG, "currentPath:" + mCurrentPath);
			this.finish();
			break;
		}
		case R.id.btn_vdisk_dir_make_dir: {
			makeDir();
			break;
		}
		}

	}

	@Override
	public void onBackPressed() {
		Logger.d(TAG, "isrootDirectory:" + isRootDirectory());

		if (!isRootDirectory()) {
			back();
			return;
		}

		super.onBackPressed();
	}

	protected void makeDir() {
		LayoutInflater inflater = LayoutInflater.from(this);
		final View layout = inflater.inflate(R.layout.rename_layout, null);
		final EditText edt = (EditText) layout.findViewById(R.id.renameEdit);
		ImageView ivIcon = (ImageView) layout.findViewById(R.id.iv_folder_icon);
		ivIcon.setVisibility(View.VISIBLE);

		final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				if (Utils.isNetworkAvailable(CloudDirActivity.this)) {
					String newName = edt.getText().toString().trim();
					if (TextUtils.isEmpty(newName)) {
						Utils.showToastString(CloudDirActivity.this,
								getString(R.string.create_dir_name_null), 0);
						return;
					}

					if (newName.contains("/") || newName.contains(":")
							|| newName.contains("*") || newName.contains("?")
							|| newName.contains("<") || newName.contains(">")
							|| newName.contains("\\") || newName.contains("|")
							|| newName.contains("\"")) {
						String formatStr = String
								.format(getString(R.string.create_dir_name_not_contain),
										"\\/:*?" + "\"" + "<>|");
						Utils.showToastString(CloudDirActivity.this, formatStr,
								0);
						return;
					}

					mProgressBar.setVisibility(View.VISIBLE);

					Bundle session = new Bundle();
					session.putString("parentPath", mCurrentPath);
					CloudEngine.getInstance(CloudDirActivity.this).makeDir(
							CloudDirActivity.this, REQUEST_CREATE_DIRECTORY,
							mCurrentPath, newName, session);
				} else {
					Utils.showToastString(CloudDirActivity.this,
							getString(R.string.no_network_connection_toast), 0);
				}
			}
		};

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);
		builder.setTitle(R.string.menu_mk_dir);
		builder.setView(layout);

		builder.setPositiveButton(R.string.ok, listener);
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

					}
				});

		final AlertDialog alert = builder.create();
		alert.setCanceledOnTouchOutside(true);
		alert.show();
	}

	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

}
