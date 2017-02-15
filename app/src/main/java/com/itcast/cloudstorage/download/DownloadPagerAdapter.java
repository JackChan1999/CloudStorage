package com.itcast.cloudstorage.download;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
/**
 * ============================================================
 * Copyright：Google有限公司版权所有 (c) 2017
 * Author：   陈冠杰
 * Email：    815712739@qq.com
 * GitHub：   https://github.com/JackChen1999
 * 博客：     http://blog.csdn.net/axi295309066
 * 微博：     AndroidDeveloper
 * <p>
 * Project_Name：CloudStorage
 * Package_Name：com.itcast.cloudstorage
 * Version：1.0
 * time：2016/2/15 14:33
 * des ：${TODO}
 * gitVersion：$Rev$
 * updateAuthor：$Author$
 * updateDate：$Date$
 * updateDes：${TODO}
 * ============================================================
 **/
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
