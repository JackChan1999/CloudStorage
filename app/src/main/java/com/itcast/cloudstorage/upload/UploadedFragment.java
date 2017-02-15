package com.itcast.cloudstorage.upload;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.itcast.cloudstorage.R;
import com.itcast.cloudstorage.bean.LocalFileInfo;
import com.itcast.cloudstorage.download.DownloadManager;
import com.itcast.cloudstorage.net.CloudEngine;
import com.itcast.cloudstorage.net.ExceptionHandler;
import com.itcast.cloudstorage.net.IDataCallBack;
import com.itcast.cloudstorage.upload.net.UploadManager;
import com.itcast.cloudstorage.utils.CloudDB;
import com.itcast.cloudstorage.utils.Logger;
import com.itcast.cloudstorage.utils.Utils;

public class UploadedFragment extends UploadBaseFragment implements
		IDataCallBack {
	private static UploadedFragment sInstance;

	private static final int REQUEST_GET_UPLOADED_FILE = 1;

	private ListView mList;
	private ArrayList<LocalFileInfo> mFileList = new ArrayList<LocalFileInfo>();
	private UploadedAdapter mAdapter;

	private TextView mEmptyView;

	public static UploadedFragment newInstance() {
		if (sInstance == null) {
			sInstance = new UploadedFragment();
		}
		return sInstance;
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		public void onReceive(Context context, android.content.Intent intent) {

			if (UploadManager.UPLOAD_FINISH.equals(intent.getAction())) {
				CloudEngine.getInstance(getActivity()).getLocalFiles(
						UploadedFragment.this, REQUEST_GET_UPLOADED_FILE,
						LocalFileInfo.SOURCE_UPLOAD, null);
			}

		};
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mContentView = inflater.inflate(
				com.itcast.cloudstorage.R.layout.uploaded_fragment, null);
		mList = (ListView) mContentView.findViewById(R.id.lv_uploaded);
		mAdapter = new UploadedAdapter();
		mList.setAdapter(mAdapter);

		mList.setOnItemClickListener(new OnItemClickListener() {

			@SuppressWarnings("static-access")
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				LocalFileInfo info = mFileList.get(position);
				File file = new File(info.path);
				if (!file.exists()) {
					Utils.showToast(getActivity(),
							R.string.local_file_not_exits, Toast.LENGTH_SHORT);
					return;
				}

				DownloadManager.getInstance().openFile(file,
						UploadedFragment.this.getActivity());
			}
		});

		mList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {

				showChooseDialog(position);

				return true;
			}
		});

		mEmptyView = new TextView(getActivity());
		mEmptyView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		mEmptyView.setGravity(Gravity.CENTER);
		mEmptyView.setTextColor(getResources().getColor(R.color.black));
		mEmptyView.setText(R.string.empty_upload_all);

		((ViewGroup) mList.getParent()).addView(mEmptyView);

		IntentFilter filter = new IntentFilter();
		filter.addAction(UploadManager.UPLOAD_FINISH);
		getActivity().registerReceiver(receiver, filter);

		return mContentView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		CloudEngine.getInstance(getActivity()).getLocalFiles(this,
				REQUEST_GET_UPLOADED_FILE, LocalFileInfo.SOURCE_UPLOAD, null);
	}

	@Override
	public void onDestroy() {
		getActivity().unregisterReceiver(receiver);
		super.onDestroy();
	}

	private void showChooseDialog(final int position) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(
				getActivity());
		builder.setTitle("选择");

		ArrayList<String> menus = new ArrayList<String>();
		menus.add("删除");
		builder.setItems(menus.toArray(new CharSequence[0]),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Logger.d("Download", "item " + which + " choosed!");
						switch (which) {
						case 0: {
							// 删除本地文件
							LocalFileInfo local = mFileList.get(position);

							mFileList.remove(position);

							new File(local.path).delete();
							CloudDB.getInstance(getActivity()).deleteLocalFile(
									local.path);
							
							if (mFileList.isEmpty()) {
								mList.setEmptyView(mEmptyView);
							}

							mAdapter.notifyDataSetChanged();
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

	class UploadedAdapter extends BaseAdapter {

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
				convertView = UploadedFragment.this.getActivity()
						.getLayoutInflater().inflate(R.layout.file_item, null);

				holder = new ViewHolder();
				holder.tvName = (TextView) convertView
						.findViewById(R.id.tv_name);
				holder.tvTime = (TextView) convertView
						.findViewById(R.id.tv_time);
				holder.tvSize = (TextView) convertView
						.findViewById(R.id.tv_size);
				holder.ivIcon = (ImageView) convertView
						.findViewById(R.id.iv_icon);
				holder.ivOption = (ImageView) convertView
						.findViewById(R.id.iv_option);
				holder.cbCheck = (CheckBox) convertView
						.findViewById(R.id.cb_checkbox);

				holder.ivOption.setVisibility(View.GONE);
				holder.cbCheck.setVisibility(View.GONE);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			LocalFileInfo info = mFileList.get(position);

			holder.tvName.setText(info.filename);
			holder.tvTime.setText(Utils.getFormateTime(new Date(Long
					.valueOf(info.modified))));
			Object[] mimeType = Utils.getMIMEType(info.filename);
			holder.ivIcon.setImageResource((Integer) mimeType[1]);

			holder.tvSize.setText(Utils.formatSize(Long.valueOf(info.bytes)));

			return convertView;
		}

	}

	class ViewHolder {
		TextView tvName;
		TextView tvTime;
		TextView tvSize;

		ImageView ivIcon;
		ImageView ivOption;

		CheckBox cbCheck;
	}

	@Override
	public void handleServiceResult(int requestCode, int errCode, Object data,
			Bundle session) {
		switch (requestCode) {
		case REQUEST_GET_UPLOADED_FILE:
			if (errCode == 0) {
				List<LocalFileInfo> localFiles = (List<LocalFileInfo>) data;
				mFileList.clear();
				mFileList.addAll(localFiles);

				if (mFileList.isEmpty()) {
					mList.setEmptyView(mEmptyView);
				}

				mAdapter.notifyDataSetChanged();
			} else {
				ExceptionHandler.toastErrMessage(getActivity(), errCode);
			}
			break;

		default:
			break;
		}

	}

}
