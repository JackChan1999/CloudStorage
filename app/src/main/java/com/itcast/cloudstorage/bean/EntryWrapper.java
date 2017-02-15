package com.itcast.cloudstorage.bean;

import com.vdisk.net.VDiskAPI.Entry;
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
public class EntryWrapper{
	public Entry entry;
	public boolean isChecked = false;
	
	public EntryWrapper() {};
	
	public EntryWrapper(Entry entry) {
		this.entry = entry;
	};
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof EntryWrapper) {
			EntryWrapper fileListElt = (EntryWrapper) o;
			return entry.path.equals(fileListElt.entry.path);
		}

		return false;
	}
}
