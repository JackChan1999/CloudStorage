package com.itcast.cloudstorage.net;

import android.os.Bundle;

public interface IDataCallBack {

	public void handleServiceResult(int requestCode, int errCode, Object data,
									Bundle session);
}