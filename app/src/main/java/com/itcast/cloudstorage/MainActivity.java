package com.itcast.cloudstorage;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.itcast.cloudstorage.bean.DownloadEntry;
import com.itcast.cloudstorage.bean.EntryWrapper;
import com.itcast.cloudstorage.bean.LocalFileInfo;
import com.itcast.cloudstorage.bean.UploadTask;
import com.itcast.cloudstorage.download.DownloadAsyncTask;
import com.itcast.cloudstorage.download.DownloadManager;
import com.itcast.cloudstorage.download.DownloadTabActivity;
import com.itcast.cloudstorage.global.Constants;
import com.itcast.cloudstorage.net.CloudEngine;
import com.itcast.cloudstorage.net.CloudEngine.BatchDeleteResult;
import com.itcast.cloudstorage.net.ExceptionHandler;
import com.itcast.cloudstorage.net.IDataCallBack;
import com.itcast.cloudstorage.upload.UploadFileExploerActivity;
import com.itcast.cloudstorage.upload.UploadTabActivity;
import com.itcast.cloudstorage.upload.net.UploadManager;
import com.itcast.cloudstorage.utils.CloudAsyncTask;
import com.itcast.cloudstorage.utils.CloudDB;
import com.itcast.cloudstorage.utils.Logger;
import com.itcast.cloudstorage.utils.Utils;
import com.umeng.analytics.MobclickAgent;
import com.vdisk.android.VDiskAuthSession;
import com.vdisk.net.RESTUtility;
import com.vdisk.net.VDiskAPI.Entry;
import com.vdisk.net.session.AppKeyPair;
import com.vdisk.net.session.Session.AccessType;

public class MainActivity extends SherlockActivity implements IDataCallBack, OnClickListener {

	private static final int			REQUEST_CODE_GET_FILE_LIST	= 0;
	private static final int			REQUEST_CREATE_DIRECTORY	= 1;
	protected static final int			REQUEST_DEL_FILE			= 2;
	protected static final int			REQUEST_BATCH_DELETE		= 3;
	protected static final int			REQUEST_MOVE				= 106;
	public static final int				BATCH_MOVE_REQUEST_CODE		= 33;
	protected static final int			REQUEST_RENAME				= 101;

	private static final String			TAG							= "MainActivity";

	private PullToRefreshListView		mHomeList					= null;
	private View						mRootView					= null;
	private View						mViewTag					= null;

	private ListView					mActualHomeList				= null;
	private CloudListAdapter			mListAdapter				= null;

	protected ArrayList<EntryWrapper>	mCurrentDataItems			= new ArrayList<EntryWrapper>();

	private String						mCurrentPath				= "/";

	private boolean						isEditMode					= false;

	// private ActionModeCallback mActionMode;
	private MenuItem					mMenuMoreItem;
	private MenuItem					mMenuUploadItem;
	private MenuItem					mMenuDownloadItem;
	private MenuItem					mMenuSelectItem;

	private TextView					mEmptyView;
	private ProgressDialog				mProgressDialog;

	// 批量移动
	private ArrayList<String>			mDirPathList;
	private ArrayList<String>			mFileList;

	UploadManager.UploadStatusListener	mUploadStatusListener		= new UploadManager.SimpleUploadStatusListener() {

																		@Override
																		public void onSuccess(Entry entry,
																				UploadTask uploadTask) {
																			Logger.d(TAG,
																					"UploadManager onSuccess entry: "
																							+ entry.fileName());
																			Utils.showToastString(MainActivity.this,
																					entry.fileName() + "上传成功",
																					Toast.LENGTH_SHORT);
																		}
																	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getSupportActionBar().setDisplayShowHomeEnabled(false);

		mHomeList = (PullToRefreshListView) findViewById(R.id.list);
		mRootView = findViewById(R.id.rl_root);
		mViewTag = findViewById(R.id.view_tag);

		mHomeList.setOnRefreshListener(new OnRefreshListener() {
			@Override
			public void onRefresh() {
				// TODO
				initList();
			}

		});

		mActualHomeList = mHomeList.getRefreshableView();
		mActualHomeList.setFastScrollEnabled(true);

		mActualHomeList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				position = position - mActualHomeList.getHeaderViewsCount();
				Log.d(TAG, "item click-->" + position);
				if (isEditMode) {
					mListAdapter.toggleChecked(position, view);
				} else {
					EntryWrapper entry = mCurrentDataItems.get(position);
					Log.d(TAG, "FileEntry is dir?" + entry.entry.isDir + "; FileEntry name:" + entry.entry.fileName());
					if (entry.entry.isDir) {
						Log.d(TAG, "enter folder");
						enterFolder(entry);
					} else {
						prepareDownload(entry);
					}
				}
			}

		});

		mActualHomeList.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				if (position > 0) {
					Log.d(TAG, "item long click-->" + position);
					startActionMode();
					mListAdapter.toggleChecked(position - 1, view);
				}
				return true;
			}
		});

		mListAdapter = new CloudListAdapter();
		mActualHomeList.setAdapter(mListAdapter);

		mEmptyView = new TextView(this);
		mEmptyView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mEmptyView.setGravity(Gravity.CENTER);
		mEmptyView.setTextColor(getResources().getColor(R.color.black));
		mEmptyView.setText(R.string.empty_upload_all);

		mProgressDialog = new ProgressDialog(this);

		initList();

		UploadManager.getInstance(this).regiserObserver(mUploadStatusListener);
	}

	private void prepareDownload(EntryWrapper entry) {
		File localFile = DownloadManager.getLocalFile(this, entry.entry.fileName(), entry.entry.md5, entry.entry.sha1,
				entry.entry.bytes);
		Logger.d(TAG, "entry is file and exists: " + (localFile != null));
		if (localFile != null && localFile.exists()) {
			DownloadManager.openFile(localFile, this);
		} else {
			DownloadEntry downloadEntry = DownloadEntry.valueOf(entry.entry);
			DownloadManager.getInstance().initDownloadFile(CloudDB.getInstance(this), this, downloadEntry);
		}
	}

	private void initList() {
		mHomeList.setRefreshing();

		CloudEngine.getInstance(this).getFileList(this, REQUEST_CODE_GET_FILE_LIST, mCurrentPath, null);
	}

	private void enterFolder(EntryWrapper entry) {
		mCurrentPath = entry.entry.path;
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(mCurrentPath.substring(mCurrentPath.lastIndexOf("/") + 1, mCurrentPath.length()));

		mCurrentDataItems.clear();
		mListAdapter.notifyDataSetChanged();

		initList();
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main, menu);

		mMenuDownloadItem = menu.findItem(R.id.menu_download);
		mMenuUploadItem = menu.findItem(R.id.menu_upload);
		mMenuSelectItem = menu.findItem(R.id.menu_select);

		SubMenu moreSubMenu = menu.addSubMenu("更多操作");
		moreSubMenu.add(0, Menu.FIRST, 0, "新建文件夹");
		moreSubMenu.add(0, Menu.FIRST + 1, 0, "上传");
		moreSubMenu.add(0, Menu.FIRST + 2, 0, "注销");

		mMenuMoreItem = moreSubMenu.getItem();
		mMenuMoreItem.setIcon(R.drawable.icon_action_home_category);
		mMenuMoreItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (isEditMode) {
				// mActionMode.onDestroyActionMode(null);
				stopActionMode();
			} else {
				homeBackAction();
			}
			break;
		case R.id.menu_upload:
			startActivity(new Intent(this, UploadTabActivity.class));
			break;
		case R.id.menu_download:
			startActivity(new Intent(this, DownloadTabActivity.class));
			break;
		case R.id.menu_select:
			if (mListAdapter.isAllChecked()) {
				mListAdapter.removeSelection();
			} else {
				mListAdapter.checkAll();
			}
			break;
		case Menu.FIRST:
			makeDir();
			hidePopup();
			break;
		case Menu.FIRST + 1:
			// 上传
			Intent intent = new Intent(this, UploadFileExploerActivity.class);
			intent.putExtra("currentPath", mCurrentPath);
			startActivity(intent);
			break;
		case Menu.FIRST + 2:
			AppKeyPair appKeyPair = new AppKeyPair(Constants.CONSUMER_KEY, Constants.CONSUMER_SECRET);
			VDiskAuthSession session = VDiskAuthSession.getInstance(this, appKeyPair, AccessType.APP_FOLDER);
			session.unlink();
			startActivity(new Intent(this, OAuthActivity.class));
			finish();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (isEditMode) {
				// mActionMode.onDestroyActionMode(null);
				stopActionMode();
			} else {
				homeBackAction();
			}

			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void homeBackAction() {
		if (!isRootDirectory()) {
			back();
		} else {
			finish();
		}
	}

	private void back() {
		mCurrentPath = mCurrentPath.substring(0, mCurrentPath.lastIndexOf("/"));

		if (TextUtils.isEmpty(mCurrentPath)) {
			mCurrentPath = "/";
		}

		Log.d(TAG, "mCurrentPath-->" + mCurrentPath);
		if (isRootDirectory()) {
			ActionBar actionBar = getSupportActionBar();
			actionBar.setDisplayHomeAsUpEnabled(false);
			actionBar.setTitle(getString(R.string.app_name));
		} else {
			getSupportActionBar().setTitle(
					mCurrentPath.substring(mCurrentPath.lastIndexOf("/") + 1, mCurrentPath.length()));
		}

		initList();
	}

	private boolean isRootDirectory() {
		if (mCurrentPath.equals("/"))
			return true;
		return false;
	}

	class CloudListAdapter extends BaseAdapter implements OnClickListener {

		PopupWindow		mPopupWindow;
		private Integer	mPosition;

		@Override
		public int getCount() {
			return mCurrentDataItems.size();
		}

		@Override
		public Object getItem(int position) {
			return mCurrentDataItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = MainActivity.this.getLayoutInflater().inflate(R.layout.file_item, null);

				holder = new ViewHolder();
				holder.tvName = (TextView) convertView.findViewById(R.id.tv_name);
				holder.tvTime = (TextView) convertView.findViewById(R.id.tv_time);
				holder.tvSize = (TextView) convertView.findViewById(R.id.tv_size);
				holder.ivIcon = (ImageView) convertView.findViewById(R.id.iv_icon);
				holder.ivOption = (ImageView) convertView.findViewById(R.id.iv_option);
				holder.cbCheck = (CheckBox) convertView.findViewById(R.id.cb_checkbox);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			EntryWrapper entry = mCurrentDataItems.get(position);

			holder.tvName.setText(entry.entry.fileName());
			holder.tvTime.setText(Utils.getFormateTime(RESTUtility.parseDate(entry.entry.modified)));

			if (entry.entry.isDir) {
				holder.tvSize.setVisibility(View.GONE);
				holder.ivIcon.setImageResource(R.drawable.directory_icon);
			} else {
				holder.tvSize.setVisibility(View.VISIBLE);
				holder.tvSize.setText(entry.entry.size);

				Object[] mimeType = Utils.getMIMEType(entry.entry.fileName());
				holder.ivIcon.setImageResource((Integer) mimeType[1]);
			}

			if (isEditMode) {
				holder.cbCheck.setVisibility(View.VISIBLE);
				holder.cbCheck.setChecked(entry.isChecked);
				holder.ivOption.setVisibility(View.GONE);
			} else {
				holder.cbCheck.setVisibility(View.GONE);
				holder.ivOption.setVisibility(View.VISIBLE);

				holder.ivOption.setTag(position);
				holder.ivOption.setOnClickListener(mEditListener);
			}

			return convertView;
		}

		OnClickListener	mEditListener	= new OnClickListener() {

											@Override
											public void onClick(View v) {
												mPosition = (Integer) v.getTag();
												showPopup(v);
											}
										};

		private void showPopup(View v) {
			View listItemView = (View) v.getParent();
			if (mPopupWindow == null) {
				View view = MainActivity.this.getLayoutInflater().inflate(R.layout.file_item_pop, null, true);

				view.findViewById(R.id.ll_move).setOnClickListener(this);
				view.findViewById(R.id.ll_delete).setOnClickListener(this);
				view.findViewById(R.id.ll_rename).setOnClickListener(this);

				mPopupWindow = new PopupWindow(view, LayoutParams.MATCH_PARENT, listItemView.getHeight(), true);
				mPopupWindow.setOutsideTouchable(true);
				mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
			}

			boolean checkPopupUpOrDown = checkPopupUpOrDown(listItemView);

			if (!checkPopupUpOrDown) {
				mPopupWindow.showAsDropDown(v, 0, Utils.dip2px(MainActivity.this, 19));
			} else {
				mPopupWindow.showAsDropDown(v, 0, -listItemView.getHeight() - Utils.dip2px(MainActivity.this, 44));
			}
		}

		private boolean checkPopupUpOrDown(View listItemView) {
			int[] pos = new int[2];
			listItemView.getLocationOnScreen(pos);
			int offsetY = pos[1] + listItemView.getHeight();

			WindowManager wm = (WindowManager) MainActivity.this.getSystemService(Context.WINDOW_SERVICE);
			int screenHeight = wm.getDefaultDisplay().getHeight();

			if (screenHeight - offsetY < listItemView.getHeight() + 2) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public void onClick(View v) {
			if (mPopupWindow != null) {
				mPopupWindow.dismiss();
			}

			switch (v.getId()) {
			case R.id.ll_delete:
				Log.d(TAG, "item delete!-->" + mPosition);

				delete(mCurrentDataItems.get(mPosition), mPosition);
				break;
			case R.id.ll_move:
				Log.d(TAG, "item move!");
				moveFileOrFolder(mCurrentDataItems.get(mPosition));
				break;
			case R.id.ll_rename:
				Log.d(TAG, "item rename!");
				renameFileOrFolder(mCurrentDataItems.get(mPosition), mPosition);
				break;

			default:
				break;
			}
		}

		private int	mCheckedCount	= 0;

		public void removeSelection() {
			for (EntryWrapper entry : mCurrentDataItems) {
				entry.isChecked = false;
			}

			mCheckedCount = 0;
			notifyDataSetChanged();

			updateActionMenuSelectAllTitle(true);
			updateCheckedCountShow();
		}

		public void toggleChecked(int position, View view) {
			EntryWrapper entry = (EntryWrapper) getItem(position);
			entry.isChecked = !entry.isChecked;

			Logger.d(TAG, "entry is checked-->" + entry.isChecked);

			CheckBox checkBox = (CheckBox) view.findViewById(R.id.cb_checkbox);
			checkBox.setChecked(entry.isChecked);

			Logger.d(TAG, "checkBox.isChecked()-->" + checkBox.isChecked());
			Logger.d(TAG, "mCheckedCount1-->" + mCheckedCount);

			if (checkBox.isChecked()) {
				mCheckedCount++;
			} else {
				mCheckedCount--;
			}

			Logger.d(TAG, "mCheckedCount2-->" + mCheckedCount);

			if (mCheckedCount == mCurrentDataItems.size()) {
				updateActionMenuSelectAllTitle(false);
			} else {
				updateActionMenuSelectAllTitle(true);
			}

			updateCheckedCountShow();
		}

		private void updateActionMenuSelectAllTitle(boolean showSelectionAll) {
			mMenuSelectItem.setTitle(showSelectionAll ? "全选" : "取消");
		}

		private void updateCheckedCountShow() {
			MainActivity.this.getSupportActionBar().setTitle(String.format("已选定%d个", mCheckedCount));
		}

		public void checkAll() {
			for (EntryWrapper entry : mCurrentDataItems) {
				entry.isChecked = true;
			}

			mCheckedCount = getCount();
			notifyDataSetChanged();

			updateActionMenuSelectAllTitle(false);

			updateCheckedCountShow();
		}

		public boolean isAllChecked() {
			return mCheckedCount == getCount();
		}
	}

	class ViewHolder {
		TextView	tvName;
		TextView	tvTime;
		TextView	tvSize;

		ImageView	ivIcon;
		ImageView	ivOption;

		CheckBox	cbCheck;
	}

	@Override
	public void handleServiceResult(int requestCode, int errCode, Object data, Bundle session) {
		switch (requestCode) {
		case REQUEST_CODE_GET_FILE_LIST:
			if (errCode == 0) {
				mCurrentDataItems.clear();

				ArrayList<Entry> list = (ArrayList<Entry>) data;

				for (Entry entry : list) {
					EntryWrapper wrapper = new EntryWrapper();
					wrapper.entry = entry;
					mCurrentDataItems.add(wrapper);
				}

				if (mCurrentDataItems.isEmpty()) {
					mActualHomeList.setEmptyView(mEmptyView);
				}

				mListAdapter.notifyDataSetChanged();
				mHomeList.onRefreshComplete();
			} else {
				ExceptionHandler.toastErrMessage(this, errCode);
			}
			break;

		case REQUEST_CREATE_DIRECTORY:
			if (errCode == 0) {
				initList();
			} else {
				ExceptionHandler.toastErrMessage(this, errCode);
			}
			mProgressDialog.dismiss();
			break;
		case REQUEST_DEL_FILE:
			if (errCode == 0) {
				String parentPath = session.getString("parentPath");
				String delPath = session.getString("delPath");

				int position = session.getInt("position");
				try {
					mCurrentDataItems.remove(position);
				} catch (IndexOutOfBoundsException e) {
					e.printStackTrace();
				}

				mListAdapter.notifyDataSetChanged();
			} else {
				ExceptionHandler.toastErrMessage(this, errCode);
			}
			mProgressDialog.dismiss();
			break;

		case REQUEST_BATCH_DELETE: {
			if (errCode == 0 && data != null) {
				String parentPath = session.getString("parentPath");
				int deleteCount = session.getInt("deleteCount");
				Logger.d(TAG, "parentPath: " + parentPath);
				Logger.d(TAG, "deleteCount: " + deleteCount);

				BatchDeleteResult result = (BatchDeleteResult) data;

				if (result.deletedEntries.size() >= deleteCount) {
					Utils.showToast(MainActivity.this, R.string.batch_delete_toast, Toast.LENGTH_SHORT);
				} else if (result.deletedEntries.isEmpty()) {
					Utils.showToast(MainActivity.this, R.string.batch_delete_all_failed_toast, Toast.LENGTH_SHORT);
				} else {
					Utils.showToast(MainActivity.this, R.string.batch_delete_failed_toast, Toast.LENGTH_SHORT);
				}

				try {
					ArrayList<EntryWrapper> fileListElts = new ArrayList<EntryWrapper>();
					Logger.d(TAG, "deletedEntries size-->" + result.deletedEntries.size());

					for (Entry entry : result.deletedEntries) {
						fileListElts.add(new EntryWrapper(entry));
					}

					mCurrentDataItems.removeAll(fileListElts);
				} catch (IndexOutOfBoundsException e) {
					e.printStackTrace();
				}

				mListAdapter.notifyDataSetChanged();
			}

			mProgressDialog.dismiss();
		}
			break;

		case REQUEST_MOVE:
			if (errCode == 0) {
				Entry entry = (Entry) data;
				EntryWrapper elt = new EntryWrapper(entry);
				Logger.d(TAG, "move result path:" + elt.entry.path);

				mCurrentDataItems.remove(elt);

				mListAdapter.notifyDataSetChanged();

				Utils.showToast(this, R.string.batch_move_success, Toast.LENGTH_SHORT);
			} else {
				ExceptionHandler.toastErrMessage(this, errCode);
			}

			mProgressDialog.dismiss();
			break;
		case BATCH_MOVE_REQUEST_CODE: {
			Logger.d(TAG, "BATCH_MOVE_REQUEST_CODE");
			if (data != null) {
				Object[] obj = (Object[]) data;
				ArrayList<Entry> sourceEntries = (ArrayList<Entry>) obj[1];

				int moveCount = session.getInt("moveCount");

				if (sourceEntries.size() >= moveCount) {
					Utils.showToast(this, R.string.batch_move_success, Toast.LENGTH_SHORT);
				} else if (sourceEntries.size() < moveCount && !sourceEntries.isEmpty()) {
					Utils.showToast(this, R.string.some_failed_when_moving, Toast.LENGTH_SHORT);
				} else {
					Utils.showToast(this, R.string.batch_move_failed, Toast.LENGTH_SHORT);
				}
				try {
					List<EntryWrapper> fileListElts = new ArrayList<EntryWrapper>();
					for (Entry entry : sourceEntries) {
						fileListElts.add(new EntryWrapper(entry));
					}
					mCurrentDataItems.removeAll(fileListElts);
				} catch (IndexOutOfBoundsException e) {
					e.printStackTrace();
				}
			} else {
				ExceptionHandler.toastErrMessage(this, errCode);
			}

			mProgressDialog.dismiss();

			hidePopup();
		}
			break;

		case REQUEST_RENAME:
			if (errCode == 0) {
				String parentPath = session.getString("parentPath");
				Logger.d(TAG, "parentPath: " + parentPath);

				Entry entry = (Entry) data;
				int postion = session.getInt("position");
				// String newname = session.getString("newname");
				EntryWrapper elt = mCurrentDataItems.get(postion);
				elt.entry = entry;

				mListAdapter.notifyDataSetChanged();
			} else {
				ExceptionHandler.toastErrMessage(this, errCode);
			}

			mProgressDialog.dismiss();

			break;

		default:
			break;
		}

	}

	PopupWindow	mEditPopupWindow;

	private void startActionMode() {
		// if (mActionMode != null) {
		// return;
		// }
		//
		// mActionMode = new ActionModeCallback();
		// mActionMode.onCreateActionMode(null, null);

		if (!isEditMode) {
			getSupportActionBar().setTitle(String.format("已选定%d个", 0));
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);

			isEditMode = true;
			mListAdapter.notifyDataSetChanged();

			mMenuDownloadItem.setVisible(false);
			mMenuMoreItem.setVisible(false);
			mMenuUploadItem.setVisible(false);
			mMenuSelectItem.setVisible(true);

			mViewTag.setVisibility(View.VISIBLE);

			if (mEditPopupWindow == null) {
				View editCabView = getLayoutInflater().inflate(R.layout.bottom_edit_pop, null);
				mEditPopupWindow = new PopupWindow(editCabView, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				editCabView.findViewById(R.id.DeleteBtn).setOnClickListener(this);
				editCabView.findViewById(R.id.movebtn).setOnClickListener(this);
				editCabView.findViewById(R.id.btn_download_all).setOnClickListener(this);
			}

			mEditPopupWindow.showAtLocation(mRootView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);

		}
	}

	private void stopActionMode() {
		if (isEditMode) {
			// mActionMode = null;
			isEditMode = false;

			mListAdapter.removeSelection();

			mMenuDownloadItem.setVisible(true);
			mMenuMoreItem.setVisible(true);
			mMenuUploadItem.setVisible(true);
			mMenuSelectItem.setVisible(false);

			mViewTag.setVisibility(View.GONE);

			if (mEditPopupWindow != null && mEditPopupWindow.isShowing()) {
				mEditPopupWindow.dismiss();
			}

			if (isRootDirectory()) {
				ActionBar actionBar = getSupportActionBar();
				actionBar.setDisplayHomeAsUpEnabled(false);
				actionBar.setTitle(getString(R.string.app_name));
			} else {
				getSupportActionBar().setTitle(
						mCurrentPath.substring(mCurrentPath.lastIndexOf("/") + 1, mCurrentPath.length()));
			}

		}
	}

	private void hidePopup() {
		if (isEditMode) {
			// mActionMode.onDestroyActionMode(null);
			stopActionMode();
		}
	}

	private class ActionModeCallback implements ActionMode.Callback, OnClickListener {

		private PopupWindow	mEditPopupWindow;

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			return true;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, com.actionbarsherlock.view.Menu menu) {
			getSupportActionBar().setTitle(String.format("已选定%d个", 0));
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);

			isEditMode = true;
			mListAdapter.notifyDataSetChanged();

			mMenuDownloadItem.setVisible(false);
			mMenuMoreItem.setVisible(false);
			mMenuUploadItem.setVisible(false);
			mMenuSelectItem.setVisible(true);

			mViewTag.setVisibility(View.VISIBLE);

			if (mEditPopupWindow == null) {
				View editCabView = getLayoutInflater().inflate(R.layout.bottom_edit_pop, null);
				mEditPopupWindow = new PopupWindow(editCabView, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				editCabView.findViewById(R.id.DeleteBtn).setOnClickListener(this);
				editCabView.findViewById(R.id.movebtn).setOnClickListener(this);
				editCabView.findViewById(R.id.btn_download_all).setOnClickListener(this);
			}

			mEditPopupWindow.showAtLocation(mRootView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);

			return true;
		}

		@Override
		public void onClick(View v) {
			final ArrayList<EntryWrapper> selectedData = new ArrayList<EntryWrapper>();
			for (EntryWrapper entry : mCurrentDataItems) {
				if (entry.isChecked) {
					Log.d(TAG, "entry selected-->" + entry.entry.fileName());
					selectedData.add(entry);
				}
			}

			switch (v.getId()) {
			case R.id.DeleteBtn:
				if (Utils.isNetworkAvailable(MainActivity.this)) {
					if (!selectedData.isEmpty()) {
						final ArrayList<String> pathList = new ArrayList<String>();
						int size = selectedData.size();
						for (int i = 0; i < size; i++) {
							EntryWrapper fileListElt = selectedData.get(i);
							pathList.add(fileListElt.entry.path);
						}

						Utils.showChooseDialog(MainActivity.this, R.string.delete_msg, null, null,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										mProgressDialog.show();

										Bundle session = new Bundle();
										session.putString("parentPath", mCurrentPath);
										session.putInt("deleteCount", pathList.size());
										CloudEngine.getInstance(MainActivity.this).batchDelete(MainActivity.this,
												REQUEST_BATCH_DELETE, mCurrentPath, pathList, session);
									}
								});

						hidePopup();
					} else {
						Utils.showToast(MainActivity.this, R.string.toast_no_file_selected, 0);
					}
				} else {
					Utils.showToast(MainActivity.this, R.string.no_network_connection_toast, 0);
				}
				break;
			case R.id.movebtn:
				if (!selectedData.isEmpty()) {
					mFileList = new ArrayList<String>();
					mDirPathList = new ArrayList<String>();
					int size = selectedData.size();
					Logger.d(TAG, TAG + "size:" + selectedData.size());
					if (size != 0) {
						for (int i = 0; i < size; i++) {
							EntryWrapper fileListElt = selectedData.get(i);
							if (fileListElt.entry.isDir) {
								// 文件夹
								mDirPathList.add(fileListElt.entry.path);
							}

							mFileList.add(fileListElt.entry.fileName());
						}

						Logger.d(TAG, "mFileList-->" + mFileList);
						Logger.d(TAG, "mDirPathList-->" + mDirPathList);

						batchMove();
					}
				} else {
					Utils.showToast(MainActivity.this, R.string.toast_no_file_selected, 0);
				}

				break;
			case R.id.btn_download_all: {

				if (!Utils.isNetworkAvailable(MainActivity.this)) {
					Utils.showToast(MainActivity.this, R.string.no_network_connection_toast, 0);
					hidePopup();
					return;
				}

				for (EntryWrapper entryWrapper : selectedData) {
					if (entryWrapper.entry.isDir) {
						Toast.makeText(MainActivity.this, "不能下载文件夹!", Toast.LENGTH_SHORT).show();
						return;
					}
				}

				new CloudAsyncTask<Void, Void, List<DownloadEntry>>() {
					ProgressDialog	mLoading;

					@Override
					protected void onPreExecute() {
						super.onPreExecute();

						mLoading = ProgressDialog.show(MainActivity.this, "",
								getString(R.string.batch_download_loading), true, false);
					}

					@Override
					protected void onPostExecute(List<DownloadEntry> result) {
						super.onPostExecute(result);

						List<DownloadEntry> entries = result;
						Logger.d(TAG, "download all onPostExecute task: " + entries.size());

						String toast;
						if (entries.size() == 0) {
							toast = getString(R.string.no_selected_file);
						} else {
							toast = String.format(getString(R.string.add_some_task), entries.size());

							for (final DownloadManager.DownloadStatusListener l : DownloadManager.getInstance()
									.getDownloadStatusListeners()) {
								l.onBatchCreate(entries);
							}

							LinkedList<DownloadEntry> q = DownloadManager.getInstance().getDownloadQueue();
							boolean startDownload = q.isEmpty();
							q.addAll(entries);

							Logger.d(TAG, "startDownload: " + startDownload + "mList.size: " + entries.size());
							if (startDownload && entries.size() > 0) {
								new DownloadAsyncTask(MainActivity.this, entries.get(0),
										CloudDB.getInstance(MainActivity.this)).execute();
							}
						}
						Utils.showToastString(MainActivity.this, toast, 0);

						try {
							mLoading.dismiss();
						} catch (Exception e) {
							e.printStackTrace();
						}

						hidePopup();
					}

					@Override
					protected List<DownloadEntry> doInBackground(Void... params) {
						List<DownloadEntry> entries = new ArrayList<DownloadEntry>();

						Logger.d(TAG, "mSelectedData: " + selectedData.size());
						if (!selectedData.isEmpty()) {
							for (int i = 0; i < selectedData.size(); i++) {
								EntryWrapper fileListElt = selectedData.get(i);
								Logger.d(TAG, "alldownload fileListElt: " + fileListElt.entry.fileName());
								if (fileListElt.entry.isDir) {
									continue;
								}
								// 检测已下载
								LocalFileInfo localFileInfo = CloudDB.getInstance(MainActivity.this).getLocalFile(
										fileListElt.entry.fileName(), LocalFileInfo.SOURCE_DOWNLOAD,
										fileListElt.entry.md5, fileListElt.entry.sha1);
								Logger.d(TAG, "alldownload fileListElt localFileInfo: " + localFileInfo);
								if (localFileInfo == null || localFileInfo.path == null
										|| !new File(localFileInfo.path).exists()) {
									DownloadEntry entry = DownloadEntry.valueOf(fileListElt.entry);

									DownloadEntry dbEntry = CloudDB.getInstance(MainActivity.this).getDownloadEntry(
											entry.pathOrCopyRef);
									if (dbEntry == null) {
										entry.localPath = DownloadManager.createDownloadTempFile(MainActivity.this,
												entry.name);
										entries.add(entry);
										boolean success = CloudDB.getInstance(MainActivity.this).insertDownloadEntry(
												entry);
										Logger.d(TAG, "btn_download_all background insert entry success: " + success);
									}
								}
							}
						}

						return entries;
					}
				}.execute();

			}

				break;

			default:
				break;
			}
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, com.actionbarsherlock.view.Menu menu) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// mActionMode = null;
			isEditMode = false;

			mListAdapter.removeSelection();

			mMenuDownloadItem.setVisible(true);
			mMenuMoreItem.setVisible(true);
			mMenuUploadItem.setVisible(true);
			mMenuSelectItem.setVisible(false);

			mViewTag.setVisibility(View.GONE);

			if (mEditPopupWindow != null && mEditPopupWindow.isShowing()) {
				mEditPopupWindow.dismiss();
			}

			if (isRootDirectory()) {
				ActionBar actionBar = getSupportActionBar();
				actionBar.setDisplayHomeAsUpEnabled(false);
				actionBar.setTitle(getString(R.string.app_name));
			} else {
				getSupportActionBar().setTitle(
						mCurrentPath.substring(mCurrentPath.lastIndexOf("/") + 1, mCurrentPath.length()));
			}

		}
	}

	@Override
	protected void onDestroy() {
		UploadManager.getInstance(this).unregiserObserver(mUploadStatusListener);
		super.onDestroy();
	}

	protected void makeDir() {
		LayoutInflater inflater = LayoutInflater.from(this);
		final View layout = inflater.inflate(R.layout.rename_layout, null);
		final EditText edt = (EditText) layout.findViewById(R.id.renameEdit);
		ImageView ivIcon = (ImageView) layout.findViewById(R.id.iv_folder_icon);
		ivIcon.setVisibility(View.VISIBLE);

		final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				if (Utils.isNetworkAvailable(MainActivity.this)) {
					String newName = edt.getText().toString().trim();
					if (TextUtils.isEmpty(newName)) {
						Utils.showToastString(MainActivity.this, getString(R.string.create_dir_name_null), 0);
						return;
					}

					if (newName.contains("/") || newName.contains(":") || newName.contains("*")
							|| newName.contains("?") || newName.contains("<") || newName.contains(">")
							|| newName.contains("\\") || newName.contains("|") || newName.contains("\"")) {
						String formatStr = String.format(getString(R.string.create_dir_name_not_contain), "\\/:*?"
								+ "\"" + "<>|");
						Utils.showToastString(MainActivity.this, formatStr, 0);
						return;
					}

					mProgressDialog.show();

					Bundle session = new Bundle();
					session.putString("parentPath", mCurrentPath);
					CloudEngine.getInstance(MainActivity.this).makeDir(MainActivity.this, REQUEST_CREATE_DIRECTORY,
							mCurrentPath, newName, session);
				} else {
					Utils.showToastString(MainActivity.this, getString(R.string.no_network_connection_toast), 0);
				}
			}
		};

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);
		builder.setTitle(R.string.menu_mk_dir);
		builder.setView(layout);

		builder.setPositiveButton(R.string.ok, listener);
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

			}
		});

		final AlertDialog alert = builder.create();
		alert.setCanceledOnTouchOutside(true);
		alert.show();
	}

	public void delete(final EntryWrapper info, final int position) {

		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				mProgressDialog.show();
				if (Utils.isNetworkAvailable(MainActivity.this)) {
					String filePath = info.entry.path;
					Bundle session = new Bundle();
					session.putInt("position", position);
					session.putString("delPath", info.entry.path);
					session.putString("parentPath", info.entry.parentPath());
					CloudEngine.getInstance(MainActivity.this).delete(MainActivity.this, REQUEST_DEL_FILE, filePath,
							info.entry.parentPath(), session);
				} else {
					Utils.showToastString(MainActivity.this, getString(R.string.no_network_connection_toast), 0);
					mProgressDialog.dismiss();
				}
			}
		};

		Utils.showChooseDialog(MainActivity.this, info.entry.isDir ? R.string.confirm_del_directory
				: R.string.confirm_del_file, null, null, listener);
	}

	public void moveFileOrFolder(EntryWrapper elt) {

		Intent intent = new Intent(this, CloudDirActivity.class);
		intent.putExtra("filePath", elt.entry.path);
		startActivityForResult(intent, REQUEST_MOVE);
	}

	private void batchMove() {
		Intent intent = new Intent(this, CloudDirActivity.class);
		int size = mDirPathList.size();
		if (size != 0) {
			Bundle extras = new Bundle();
			extras.putStringArrayList("batch_move_dir_list", mDirPathList);
			intent.putExtras(extras);
		}

		startActivityForResult(intent, BATCH_MOVE_REQUEST_CODE);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Logger.d(TAG, "onActivityResult" + "_requestCode:" + requestCode + "_resultCode:" + resultCode);
		if (resultCode == Activity.RESULT_OK) {

			switch (requestCode) {
			case REQUEST_MOVE:
				Logger.d(TAG, "onActivityResult");
				String toPath = null;
				String sourcePath = null;

				Bundle extras = data.getExtras();
				if (extras != null) {
					toPath = extras.getString("path");
					sourcePath = extras.getString("filePath");
				}

				if (toPath != null && sourcePath != null) {
					mProgressDialog.show();

					if (toPath.equals("/")) {
						toPath = "";
					}

					toPath = toPath + sourcePath.substring(sourcePath.lastIndexOf("/"), sourcePath.length());

					Bundle session = new Bundle();
					session.putString("targetPath", toPath);
					session.putString("sourcePath", sourcePath);

					Logger.d(TAG, "move topath-->" + toPath);

					CloudEngine.getInstance(this).move(this, REQUEST_MOVE, sourcePath, toPath, session);
				}
				break;
			case BATCH_MOVE_REQUEST_CODE:
				Logger.d(TAG, "requestCode:" + requestCode);
				String mToPath = null;

				Bundle extra = data.getExtras();
				if (extra != null) {
					mToPath = extra.getString("path");
					mProgressDialog.show();
				}

				if (mToPath != null) {
					Bundle session = new Bundle();
					session.putString("parentPath", mCurrentPath);
					session.putInt("moveCount", mFileList.size());

					CloudEngine.getInstance(this).new BatchMoveTask(this, BATCH_MOVE_REQUEST_CODE, session, mFileList,
							mCurrentPath, mToPath).execute();
				}

				break;
			default:
				break;

			}

			super.onActivityResult(requestCode, resultCode, data);
		}

	};

	public void renameFileOrFolder(EntryWrapper i, int pos) {
		LayoutInflater inflater = LayoutInflater.from(this);
		final View layout = inflater.inflate(R.layout.rename_layout, null);
		final EditText edt = (EditText) layout.findViewById(R.id.renameEdit);
		ImageView ivIcon = (ImageView) layout.findViewById(R.id.iv_folder_icon);
		ivIcon.setVisibility(View.GONE);
		final EntryWrapper info = i;
		final int position = pos;
		final String fileEndsWith = info.entry.fileName().contains(".") ? info.entry.fileName().substring(
				info.entry.fileName().lastIndexOf("."), info.entry.fileName().length()) : "";

		if (!info.entry.isDir) {

			if (info.entry.fileName().contains(".")) {
				String fileName = info.entry.fileName().substring(0, info.entry.fileName().lastIndexOf("."));
				edt.setText(fileName);
			} else {
				edt.setText(info.entry.fileName());
			}
		} else {
			edt.setText(info.entry.fileName());
		}

		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String newName = edt.getText().toString();
				if (TextUtils.isEmpty(newName)) {
					Utils.showToastString(MainActivity.this, getString(R.string.rename_is_null), 0);
					return;
				}

				newName = newName.trim();
				if (newName.contains("/") || newName.contains(":") || newName.contains("*") || newName.contains("?")
						|| newName.contains("<") || newName.contains(">") || newName.contains("\\")
						|| newName.contains("|") || newName.contains("\"")) {
					String formatStr = String.format(getString(R.string.create_dir_name_not_contain), "\\/:*?" + "\""
							+ "<>|");
					Utils.showToastString(MainActivity.this, formatStr, 0);
					return;
				}

				if (!info.entry.isDir) {
					if (info.entry.fileName().contains(".")) {
						String fileName = info.entry.fileName().substring(0, info.entry.fileName().lastIndexOf("."));
						if (newName.equals(fileName)) {
							Utils.showToastString(MainActivity.this, getString(R.string.filename_same), 0);
							return;
						}
					} else {
						if (newName.equals(info.entry.fileName())) {
							Utils.showToastString(MainActivity.this, getString(R.string.filename_same), 0);
							return;
						}
					}
				} else {
					if (newName.equals(info.entry.fileName())) {
						Utils.showToastString(MainActivity.this, getString(R.string.filename_same), 0);
						return;
					}
				}

				mProgressDialog.show();

				Bundle session = new Bundle();
				String newname;
				if (info.entry.isDir) {
					newname = newName;
				} else {
					newname = newName + fileEndsWith;
				}
				session.putInt("position", position);
				session.putString("newname", newname);
				session.putString("parentPath", info.entry.parentPath());
				CloudEngine.getInstance(MainActivity.this).rename(MainActivity.this, REQUEST_RENAME,
						info.entry.parentPath(), info.entry.fileName(), newname, session);
			}
		};

		Utils.showChooseDialog(this, R.string.file_dir_rename, null, layout, listener);
	}

	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

	@Override
	public void onClick(View v) {
		final ArrayList<EntryWrapper> selectedData = new ArrayList<EntryWrapper>();
		for (EntryWrapper entry : mCurrentDataItems) {
			if (entry.isChecked) {
				Log.d(TAG, "entry selected-->" + entry.entry.fileName());
				selectedData.add(entry);
			}
		}

		switch (v.getId()) {
		case R.id.DeleteBtn:
			if (Utils.isNetworkAvailable(MainActivity.this)) {
				if (!selectedData.isEmpty()) {
					final ArrayList<String> pathList = new ArrayList<String>();
					int size = selectedData.size();
					for (int i = 0; i < size; i++) {
						EntryWrapper fileListElt = selectedData.get(i);
						pathList.add(fileListElt.entry.path);
					}

					Utils.showChooseDialog(MainActivity.this, R.string.delete_msg, null, null,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									mProgressDialog.show();

									Bundle session = new Bundle();
									session.putString("parentPath", mCurrentPath);
									session.putInt("deleteCount", pathList.size());
									CloudEngine.getInstance(MainActivity.this).batchDelete(MainActivity.this,
											REQUEST_BATCH_DELETE, mCurrentPath, pathList, session);
								}
							});

					hidePopup();
				} else {
					Utils.showToast(MainActivity.this, R.string.toast_no_file_selected, 0);
				}
			} else {
				Utils.showToast(MainActivity.this, R.string.no_network_connection_toast, 0);
			}
			break;
		case R.id.movebtn:
			if (!selectedData.isEmpty()) {
				mFileList = new ArrayList<String>();
				mDirPathList = new ArrayList<String>();
				int size = selectedData.size();
				Logger.d(TAG, TAG + "size:" + selectedData.size());
				if (size != 0) {
					for (int i = 0; i < size; i++) {
						EntryWrapper fileListElt = selectedData.get(i);
						if (fileListElt.entry.isDir) {
							// 文件夹
							mDirPathList.add(fileListElt.entry.path);
						}

						mFileList.add(fileListElt.entry.fileName());
					}

					Logger.d(TAG, "mFileList-->" + mFileList);
					Logger.d(TAG, "mDirPathList-->" + mDirPathList);

					batchMove();
				}
			} else {
				Utils.showToast(MainActivity.this, R.string.toast_no_file_selected, 0);
			}

			break;
		case R.id.btn_download_all: {

			if (!Utils.isNetworkAvailable(MainActivity.this)) {
				Utils.showToast(MainActivity.this, R.string.no_network_connection_toast, 0);
				hidePopup();
				return;
			}

			for (EntryWrapper entryWrapper : selectedData) {
				if (entryWrapper.entry.isDir) {
					Toast.makeText(MainActivity.this, "不能下载文件夹!", Toast.LENGTH_SHORT).show();
					return;
				}
			}

			new CloudAsyncTask<Void, Void, List<DownloadEntry>>() {
				ProgressDialog	mLoading;

				@Override
				protected void onPreExecute() {
					super.onPreExecute();

					mLoading = ProgressDialog.show(MainActivity.this, "", getString(R.string.batch_download_loading),
							true, false);
				}

				@Override
				protected void onPostExecute(List<DownloadEntry> result) {
					super.onPostExecute(result);

					List<DownloadEntry> entries = result;
					Logger.d(TAG, "download all onPostExecute task: " + entries.size());

					String toast;
					if (entries.size() == 0) {
						toast = getString(R.string.no_selected_file);
					} else {
						toast = String.format(getString(R.string.add_some_task), entries.size());

						for (final DownloadManager.DownloadStatusListener l : DownloadManager.getInstance()
								.getDownloadStatusListeners()) {
							l.onBatchCreate(entries);
						}

						LinkedList<DownloadEntry> q = DownloadManager.getInstance().getDownloadQueue();
						boolean startDownload = q.isEmpty();
						q.addAll(entries);

						Logger.d(TAG, "startDownload: " + startDownload + "mList.size: " + entries.size());
						if (startDownload && entries.size() > 0) {
							new DownloadAsyncTask(MainActivity.this, entries.get(0),
									CloudDB.getInstance(MainActivity.this)).execute();
						}
					}
					Utils.showToastString(MainActivity.this, toast, 0);

					try {
						mLoading.dismiss();
					} catch (Exception e) {
						e.printStackTrace();
					}

					hidePopup();
				}

				@Override
				protected List<DownloadEntry> doInBackground(Void... params) {
					List<DownloadEntry> entries = new ArrayList<DownloadEntry>();

					Logger.d(TAG, "mSelectedData: " + selectedData.size());
					if (!selectedData.isEmpty()) {
						for (int i = 0; i < selectedData.size(); i++) {
							EntryWrapper fileListElt = selectedData.get(i);
							Logger.d(TAG, "alldownload fileListElt: " + fileListElt.entry.fileName());
							if (fileListElt.entry.isDir) {
								continue;
							}
							// 检测已下载
							LocalFileInfo localFileInfo = CloudDB.getInstance(MainActivity.this).getLocalFile(
									fileListElt.entry.fileName(), LocalFileInfo.SOURCE_DOWNLOAD, fileListElt.entry.md5,
									fileListElt.entry.sha1);
							Logger.d(TAG, "alldownload fileListElt localFileInfo: " + localFileInfo);
							if (localFileInfo == null || localFileInfo.path == null
									|| !new File(localFileInfo.path).exists()) {
								DownloadEntry entry = DownloadEntry.valueOf(fileListElt.entry);

								DownloadEntry dbEntry = CloudDB.getInstance(MainActivity.this).getDownloadEntry(
										entry.pathOrCopyRef);
								if (dbEntry == null) {
									entry.localPath = DownloadManager.createDownloadTempFile(MainActivity.this,
											entry.name);
									entries.add(entry);
									boolean success = CloudDB.getInstance(MainActivity.this).insertDownloadEntry(entry);
									Logger.d(TAG, "btn_download_all background insert entry success: " + success);
								}
							}
						}
					}

					return entries;
				}
			}.execute();

		}

			break;

		default:
			break;
		}
	}
}
