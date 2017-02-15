package com.itcast.cloudstorage.upload;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
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
import com.itcast.cloudstorage.bean.TaskInfo;
import com.itcast.cloudstorage.bean.UploadTask;
import com.itcast.cloudstorage.net.CloudEngine;
import com.itcast.cloudstorage.net.ExceptionHandler;
import com.itcast.cloudstorage.net.IDataCallBack;
import com.itcast.cloudstorage.upload.net.UploadManager;
import com.itcast.cloudstorage.utils.CloudDB;
import com.itcast.cloudstorage.utils.Logger;
import com.itcast.cloudstorage.utils.Utils;
import com.vdisk.net.VDiskAPI.Entry;

public class UploadingFragment extends UploadBaseFragment implements
		IDataCallBack {
	private static UploadingFragment sInstance;

	private static final int REQUEST_GET_UPLOAD_LIST = 0;

	private ListView mList;
	private ArrayList<UploadTask> mFileList = new ArrayList<UploadTask>();
	private UploadingAdapter mAdapter;
	
	private TextView mEmptyView;

	UploadManager.UploadStatusListener mUploadStatusListener = new UploadManager.SimpleUploadStatusListener() {
		public void onCancel(UploadTask entry) {
			iniitUploadingList();
		};

		public void onFailed(UploadTask entry) {
			iniitUploadingList();
		};

		public void onSuccess(Entry entry, UploadTask uploadTak) {
			iniitUploadingList();
		};

		public void onProgress(UploadTask entry, long speed) {
			int index = mFileList.indexOf(entry);
			if (index >= 0) {
				mFileList.get(index).fileprogress = entry.fileprogress;
			}

			mAdapter.notifyDataSetChanged();
		};
	};

	public static UploadingFragment newInstance() {
		if (sInstance == null) {
			sInstance = new UploadingFragment();
		}
		return sInstance;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mContentView = inflater.inflate(R.layout.uploading_fragment, null);

		mList = (ListView) mContentView.findViewById(R.id.lv_uploading);
		mAdapter = new UploadingAdapter();
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

		UploadManager.getInstance(getActivity()).regiserObserver(
				mUploadStatusListener);
		return mContentView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		iniitUploadingList();
	}

	private void iniitUploadingList() {
		CloudEngine.getInstance(getActivity()).getUploadList(this,
				REQUEST_GET_UPLOAD_LIST, null);
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
						switch (which) {
						case 0: {

							UploadTask info = mFileList.get(position);

							Logger.d("Upload", "file " + info.filename
									+ " canceled!");
							int state = Integer.parseInt(info.state);
							boolean isWorking = false;
							if (state == TaskInfo.TASK_WORKING
									|| state == TaskInfo.TASK_BATCH_SIGN
									|| state == TaskInfo.TASK_CREATE_MD5S
									|| state == TaskInfo.TASK_CREATE_SHA1
									|| state == TaskInfo.TASK_GET_HOST) {
								isWorking = true;
							}

							Logger.d("Upload", "file " + info.filename
									+ " canceled! isworking-->" + isWorking);

							ArrayList<UploadTask> queue = UploadManager
									.getInstance(
											UploadingFragment.this
													.getActivity())
									.getUploadQueue();

							CloudDB db = CloudDB
									.getInstance(UploadingFragment.this
											.getActivity());

							if (!isWorking) {
								if (queue != null && !queue.isEmpty()) {
									queue.remove(info);
								}
							} else {
								if (queue != null && !queue.isEmpty()) {
									UploadTask uploader = queue.get(0);
									uploader.cancel();
								}
							}

							NotificationManager notificationManager = (NotificationManager) UploadingFragment.this
									.getActivity().getSystemService(
											Context.NOTIFICATION_SERVICE);
							if (notificationManager != null) {
								notificationManager
										.cancel(UploadManager.UPLOAD_NOTIFY_ID);
							}

							db.deleteRecordById(Integer.parseInt(info.taskid));
							db.deleteUploadFileInfo(info.taskid);

							iniitUploadingList();
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

	@Override
	public void onDestroy() {
		UploadManager.getInstance(getActivity()).unregiserObserver(
				mUploadStatusListener);
		super.onDestroy();
	}

	class UploadingAdapter extends BaseAdapter {

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
				convertView = UploadingFragment.this.getActivity()
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

			UploadTask uploadTask = mFileList.get(position);

			holder.pbProgress.setVisibility(View.GONE);
			if (uploadTask.state != null) {
				int stateCode = Integer.parseInt(uploadTask.state);

				switch (stateCode) {
				case TaskInfo.TASK_WORKING:
					holder.tvState.setVisibility(View.INVISIBLE);
					holder.pbProgress.setVisibility(View.VISIBLE);
					holder.tvPercent.setVisibility(View.VISIBLE);
					holder.pbProgress.setProgress(uploadTask.fileprogress);
					holder.tvPercent.setText(uploadTask.fileprogress + "%");
					break;
				case TaskInfo.TASK_WAITTING:
					holder.pbProgress.setVisibility(View.INVISIBLE);
					holder.tvState.setVisibility(View.VISIBLE);
					holder.tvState.setText(R.string.transfer_state_waiting);
					holder.tvPercent.setVisibility(View.GONE);
					break;
				case TaskInfo.TASK_ERROR:
					holder.pbProgress.setVisibility(View.INVISIBLE);
					holder.tvState.setVisibility(View.VISIBLE);
					holder.tvState.setText(R.string.transfer_state_failed);
					holder.tvPercent.setVisibility(View.GONE);
					break;
				case TaskInfo.TASK_GET_HOST:
					holder.pbProgress.setVisibility(View.INVISIBLE);
					holder.tvState.setVisibility(View.VISIBLE);
					holder.tvState
							.setText(R.string.transfer_state_getting_host);
					holder.tvPercent.setVisibility(View.GONE);
					break;
				case TaskInfo.TASK_CREATE_SHA1:
					holder.pbProgress.setVisibility(View.INVISIBLE);
					holder.tvState.setVisibility(View.VISIBLE);
					holder.tvState.setText(R.string.transfer_state_making_sha1);
					holder.tvPercent.setVisibility(View.GONE);
					break;
				case TaskInfo.TASK_BATCH_SIGN:
					holder.pbProgress.setVisibility(View.INVISIBLE);
					holder.tvState.setVisibility(View.VISIBLE);
					holder.tvState.setText(R.string.transfer_state_batch_sign);
					holder.tvPercent.setVisibility(View.GONE);
					break;
				case TaskInfo.TASK_CREATE_MD5S:
					holder.pbProgress.setVisibility(View.INVISIBLE);
					holder.tvState.setVisibility(View.VISIBLE);
					holder.tvState.setText(R.string.transfer_state_making_md5s);
					holder.tvPercent.setVisibility(View.GONE);
					break;

				default:
					break;
				}

				holder.tvName.setText(uploadTask.filename);

				Object[] mimeType = Utils.getMIMEType(uploadTask.filename);
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
		case REQUEST_GET_UPLOAD_LIST:
			if (errCode == 0) {
				List<UploadTask> entries = (List<UploadTask>) data;

				Logger.d("Upload", "Uploading list =" + entries.size());

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
