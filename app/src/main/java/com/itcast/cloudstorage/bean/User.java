package com.itcast.cloudstorage.bean;

import android.content.Context;
import android.text.TextUtils;

import com.itcast.cloudstorage.global.Constants;
import com.vdisk.android.VDiskAuthSession;
import com.vdisk.net.session.AppKeyPair;
import com.vdisk.net.session.Session.AccessType;

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
