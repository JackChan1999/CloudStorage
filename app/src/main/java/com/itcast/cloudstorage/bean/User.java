package com.itcast.cloudstorage.bean;

import android.content.Context;
import android.text.TextUtils;

import com.itcast.cloudstorage.global.Constants;
import com.vdisk.android.VDiskAuthSession;
import com.vdisk.net.session.AppKeyPair;
import com.vdisk.net.session.Session.AccessType;
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
public class User {

	private static String uid;

	public static boolean isUserLogined(Context context) {
		AppKeyPair appKeyPair = new AppKeyPair(Constants.CONSUMER_KEY,
				Constants.CONSUMER_SECRET);
		VDiskAuthSession session = VDiskAuthSession.getInstance(context,
				appKeyPair, AccessType.APP_FOLDER);

		return session.isLinked();
	}

	public static String getUid(Context context) {
		if (TextUtils.isEmpty(uid) && context != null) {
			AppKeyPair appKeyPair = new AppKeyPair(Constants.CONSUMER_KEY,
					Constants.CONSUMER_SECRET);
			VDiskAuthSession session = VDiskAuthSession.getInstance(context,
					appKeyPair, AccessType.APP_FOLDER);
			uid = session.getAccessToken().mUid;
		}

		return uid;
	}

}
