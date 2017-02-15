package com.itcast.cloudstorage;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.itcast.cloudstorage.global.Constants;
import com.itcast.cloudstorage.utils.Logger;
import com.umeng.analytics.MobclickAgent;
import com.vdisk.android.VDiskAuthSession;
import com.vdisk.android.VDiskDialogListener;
import com.vdisk.net.exception.VDiskDialogError;
import com.vdisk.net.exception.VDiskException;
import com.vdisk.net.session.AccessToken;
import com.vdisk.net.session.AppKeyPair;
import com.vdisk.net.session.Session.AccessType;

public class OAuthActivity extends SherlockActivity implements
		VDiskDialogListener {
	Button btn_oauth;
	VDiskAuthSession session;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AppKeyPair appKeyPair = new AppKeyPair(Constants.CONSUMER_KEY,
				Constants.CONSUMER_SECRET);
		session = VDiskAuthSession.getInstance(this, appKeyPair,
				AccessType.VDISK);

		setContentView(R.layout.auth_main);

		btn_oauth = (Button) this.findViewById(R.id.btnOAuth);
		btn_oauth.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				session.setRedirectUrl(Constants.REDIRECT_URL);
				session.authorize(OAuthActivity.this, OAuthActivity.this);
			}
		});

		// 如果已经登录，直接跳转到测试页面
		// If you are already logged in, jump to the test page directly.
		if (session.isLinked()) {

			AccessToken mToken = session.getAccessToken();

			Logger.d("Login", "token-->" + mToken.mAccessToken
					+ "; expires time-->" + mToken.mExpiresIn + "; uid-->"
					+ mToken.mUid + "; refresh toke-->" + mToken.mRefreshToken);
			startActivity(new Intent(this, MainActivity.class));
			finish();
		}
	}

	/**
	 * 认证结束后的回调方法
	 * 
	 * Callback method after authentication.
	 */
	@Override
	public void onComplete(Bundle values) {

		if (values != null) {
			AccessToken mToken = (AccessToken) values
					.getSerializable(VDiskAuthSession.OAUTH2_TOKEN);

			Logger.d("Login", "token-->" + mToken.mAccessToken
					+ "; expires time-->" + mToken.mExpiresIn + "; uid-->"
					+ mToken.mUid + "; refresh toke-->" + mToken.mRefreshToken);
			session.finishAuthorize(mToken);
		}

		startActivity(new Intent(this, MainActivity.class));
		finish();
	}

	/**
	 * 认证出错的回调方法
	 * 
	 * Callback method for authentication errors.
	 */
	@Override
	public void onError(VDiskDialogError error) {
		Toast.makeText(getApplicationContext(),
				"Auth error : " + error.getMessage(), Toast.LENGTH_LONG).show();
	}

	/**
	 * 认证异常的回调方法
	 * 
	 * Callback method for authentication exceptions.
	 */
	@Override
	public void onVDiskException(VDiskException exception) {
		// TODO Auto-generated method stub
		Toast.makeText(getApplicationContext(),
				"Auth exception : " + exception.getMessage(), Toast.LENGTH_LONG)
				.show();
	}

	/**
	 * 认证被取消的回调方法
	 * 
	 * Callback method as authentication is canceled.
	 */
	@Override
	public void onCancel() {
		Toast.makeText(getApplicationContext(), "Auth cancel",
				Toast.LENGTH_LONG).show();
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
