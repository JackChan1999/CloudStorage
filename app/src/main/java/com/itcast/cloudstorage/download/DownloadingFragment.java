package com.itcast.cloudstorage.download;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.itcast.cloudstorage.R;
import com.itcast.cloudstorage.bean.DownloadEntry;
import com.itcast.cloudstorage.bean.TaskInfo;
import com.itcast.cloudstorage.net.CloudEngine;
import com.itcast.cloudstorage.net.ExceptionHandler;
import com.itcast.cloudstorage.net.IDataCallBack;
import com.itcast.cloudstorage.utils.CloudDB;
import com.itcast.cloudstorage.utils.Logger;
import com.itcast.cloudstorage.utils.Utils;

public class DownloadingFragment extends DownloadBaseFragment implements
		IDataCallBack {
	private static DownloadingFragment sInstance;

	private static final int REQUEST_GET_DOWNLOAD_LIST = 0;
	private static final int NOTIFY_ID = 0;

	private ListView mList;
	private ArrayList<DownloadEntry> mFileList = new ArrayList<DownloadEntry>();
	private DownloadingAdapter mAdapter;
	
	private TextView mEmptyView;

	DownloadManager.DownloadStatusListener mDownloadStatusListener = new DownloadManager.SimpleDownloadStatusListener() {

		@Override
		public void onCreate(DownloadEntry entry, boolean exists) {
			iniitDownloadingList();
		}

		@Override
		public void onBatchCreate(List<DownloadEntry> entries) {

		}

		@Override
		public void onStart(DownloadEntry entry) {
			iniitDownloadingList();
		}

		@Override
		public void onProgress(DownloadEntry entry, float speed) {
			int index = mFileList.indexOf(entry);
			if (index >= 0) {
				mFileList.get(index).fileProgress = entry.fileProgress;
			}

			mAdapter.notifyDataSetChanged();
		}

		@Override
		public void onCancel(DownloadEntry entry) {
			iniitDownloadingList();
		}

		@Override
		public void onPause(DownloadEntry entry) {

		}

		public void onFinish(DownloadEntry entry) {

			iniitDownloadingList();

			DownloadingFragment.this.getActivity().sendBroadcast(
					new Intent(DownloadManager.ACTION_DOWNLOAD_FINISH));
		}

		public void onFailed(DownloadEntry entry) {
			iniitDownloadingList();
		}
	};

	public static DownloadingFragment newInstance() {
		if (sInstance == null) {
			sInstance = new DownloadingFragment();
		}
		return sInstance;
	}

	private void iniitDownloadingList() {
		CloudEngine.getInstance(getActivity()).getDownloadList(this,
				REQUEST_GET_DOWNLOAD_LIST, null);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mContentView = inflater.inflate(R.layout.downloading_fragment, null);

		mList = (ListView) mContentView.findViewById(R.id.lv_downloading);
		mAdapter = new DownloadingAdapter();
		mList.setAdapter(mAdapter);

		mList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				showChooseDialog(position);
			}

		});

		mList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				return false;
			}
		});
		
		mEmptyView = new TextView(getActivity());
		mEmptyView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		mEmptyView.setGravity(Gravity.CENTER);
		mEmptyView.setTextColor(getResources().getColor(R.color.black));
		mEmptyView.setText(R.string.empty_upload_all);
		
		((ViewGroup) mList.getParent()).addView(mEmptyView);

		DownloadManager.getInstance().addDownloadStatusListener(
				mDownloadStatusListener);

		return mContentView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		CloudEngine.getInstance(getActivity()).getDownloadList(this,
				REQUEST_GET_DOWNLOAD_LIST, null);
	}

	@Override
	public void onDestroy() {
		DownloadManager.getInstance().removeDownloadStatusListener(
				mDownloadStatusListener);
		super.onDestroy();
	}

	private void showChooseDialog(final int position) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(
				getActivity());
		builder.setTitle("选择");

		ArrayList<String> menus = new ArrayList<String>();
		menus.add("取消");
		builder.setItems(menus.toArray(new CharSequence[0]),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Logger.d("Download", "item " + which + " choosed!");
						switch (which) {
						case 0: {
							// 取消
							DownloadEntry workingDownloadEntry = null;
							try {
								CloudDB db = CloudDB.getInstance(getActivity());

								DownloadEntry downloadEntry = mFileList
										.get(position);

								db.deleteDownloadEntry(downloadEntry.pathOrCopyRef);
								if (Integer.parseInt(downloadEntry.state) == TaskInfo.TASK_WORKING) {
									workingDownloadEntry = downloadEntry;
								} else {
									DownloadManager.getInstance()
											.getDownloadQueue()
											.remove(downloadEntry);
								}

								if (workingDownloadEntry != null) {
									workingDownloadEntry.cancel(false);
									NotificationManager notificationManager = (NotificationManager) getActivity()
											.getSystemService(
													Context.NOTIFICATION_SERVICE);
									if (notificationManager != null) {
										notificationManager.cancel(NOTIFY_ID);
									}
								}

								File localFile = new File(
										downloadEntry.localPath);
								localFile.delete();

								mFileList.remove(position);
								
								if (mFileList.isEmpty()) {
									mList.setEmptyView(mEmptyView);
								}
								
								mAdapter.notifyDataSetChanged();

							} catch (Exception e) {
								e.printStackTrace();
								Utils.showToastString(
										getActivity(),
										getString(R.string.transfer_cancel_failed),
										0);
							}
						}
							break;

						default:
							break;
						}
					}

				});
		final AlertDialog alert = builder.create();
		alert.setCanceledOnTouchOutside(true);
		alert.getWindow().setGravity(Gravity.CENTER);
		alert.show();
	}

	class DownloadingAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mFileList.size();
		}

		@Override
		public Object getItem(int position) {
			return mFileList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = DownloadingFragment.this.getActivity()
						.getLayoutInflater()
						.inflate(R.layout.local_task_item, null);

				holder = new ViewHolder();
				holder.ivIcon = (ImageView) convertView
						.findViewById(R.id.iv_file_icon);
				holder.tvName = (TextView) convertView
						.findViewById(R.id.local_task_name);
				holder.pbProgress = (ProgressBar) convertView
						.findViewById(R.id.localtaskProgress);
				holder.tvPercent = (TextView) convertView
						.findViewById(R.id.local_task_percent);
				holder.tvState = (TextView) convertView
						.findViewById(R.id.tv_task_state);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			DownloadEntry downloadEntry = mFileList.get(position);

			holder.pbProgress.setVisibility(View.GONE);
			if (downloadEntry.state != null) {
				if (Integer.parseInt(downloadEntry.state) == TaskInfo.TASK_WORKING) {
					holder.tvState.setVisibility(View.GONE);
					holder.pbProgress.setVisibility(View.VISIBLE);
					holder.tvPercent.setVisibility(View.VISIBLE);
					if (!TextUtils.isEmpty(downloadEntry.fileProgress)) {
						if (downloadEntry.fileProgress.equals("0")) {
							holder.pbProgress.setIndeterminate(true);
						} else {
							holder.pbProgress.setIndeterminate(false);
						}
						holder.pbProgress.setProgress(Integer
								.parseInt(downloadEntry.fileProgress));
						holder.tvPercent.setText(downloadEntry.fileProgress
								+ "%");
					} else {
						holder.pbProgress.setIndeterminate(true);
						holder.pbProgress.setProgress(0);
						holder.tvPercent.setText("0%");
					}
				} else if (Integer.parseInt(downloadEntry.state) == TaskInfo.TASK_WAITTING) {
					holder.pbProgress.setVisibility(View.GONE);
					holder.tvPercent.setVisibility(View.GONE);
					holder.tvState.setVisibility(View.VISIBLE);
					holder.tvState.setText("等待下载...");
				} else if (Integer.parseInt(downloadEntry.state) == TaskInfo.TASK_ERROR) {
					holder.tvPercent.setVisibility(View.GONE);
					holder.tvState.setVisibility(View.VISIBLE);
					holder.tvState.setText("下载失败");
				}

				holder.tvName.setText(downloadEntry.name);

				Object[] mimeType = Utils.getMIMEType(downloadEntry.name);
				holder.ivIcon.setImageResource((Integer) mimeType[1]);
			}

			return convertView;
		}

	}

	class ViewHolder {
		TextView tvName;
		TextView tvSize;
		TextView tvPercent;
		TextView tvState;

		ImageView ivIcon;

		ProgressBar pbProgress;

	}

	@Override
	public void handleServiceResult(int requestCode, int errCode, Object data,
			Bundle session) {
		switch (requestCode) {
		case REQUEST_GET_DOWNLOAD_LIST:
			if (errCode == 0) {
				List<DownloadEntry> entries = (List<DownloadEntry>) data;

				Logger.d("Download", "downloading list =" + entries.size());

				mFileList.clear();
				mFileList.addAll(entries);
				
				if(mFileList.isEmpty()) {
					mList.setEmptyView(mEmptyView);
				}

				mAdapter.notifyDataSetChanged();

			} else {
				ExceptionHandler.toastErrMessage(getActivity(), errCode);
			}
			break;
		}
	}
}
