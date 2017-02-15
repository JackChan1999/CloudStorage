package com.itcast.cloudstorage.bean;

import com.vdisk.net.VDiskAPI.Entry;

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
