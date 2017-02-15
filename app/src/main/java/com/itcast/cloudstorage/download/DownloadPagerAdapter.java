package com.itcast.cloudstorage.download;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class DownloadPagerAdapter extends FragmentPagerAdapter {

	private Context mContext;
	public DownloadPagerAdapter(FragmentManager fm, Context context) {
		super(fm);
		this.mContext = context;
	}

	@Override
	public DownloadBaseFragment getItem(int arg0) {
		switch (arg0) {
		case 0:
			return DownloadingFragment.newInstance();
		case 1:
			return DownloadedFragment.newInstance();
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
			return "下载中";
		case 1:
			return "已下载";
		default:
			break;
		}
		return super.getPageTitle(position);
	}

}
