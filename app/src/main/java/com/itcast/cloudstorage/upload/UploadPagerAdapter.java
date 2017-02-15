package com.itcast.cloudstorage.upload;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class UploadPagerAdapter extends FragmentPagerAdapter {

	private Context mContext;
	public UploadPagerAdapter(FragmentManager fm, Context context) {
		super(fm);
		this.mContext = context;
	}

	@Override
	public UploadBaseFragment getItem(int arg0) {
		switch (arg0) {
		case 0:
			return UploadingFragment.newInstance();
		case 1:
			return UploadedFragment.newInstance();
		}
		return null;
	}

	@Override
	public int getCount() {
		return 2;
	}
	
	@Override
	public CharSequence getPageTitle(int position) {
		switch (position) {
		case 0:
			return "上传中";
		case 1:
			return "已上传";
		default:
			break;
		}
		return super.getPageTitle(position);
	}

}
