package com.itcast.cloudstorage.utils;

import java.math.BigDecimal;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.itcast.cloudstorage.R;
import com.itcast.cloudstorage.bean.LocalFileDirInfo;

public class Utils {
	public static String getFormateTime(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(date);
	}

	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	public static boolean isMountSdCard(Context ctx) {
		return Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
	}

	public static void showToast(Context ctx, int msg, int duration) {
		Toast mToast = Toast.makeText(ctx, msg, duration);
		mToast.show();
	}

	public static void showToastString(Context ctx, String msg, int duration) {
		Toast mToast = Toast.makeText(ctx, msg, duration);
		mToast.show();
	}

	public static boolean isNetworkAvailable(Context ctx) {
		Context context = ctx;
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			// Error
		} else {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static String formatSize(float size) {
		long kb = 1024;
		long mb = (kb * 1024);
		long gb = (mb * 1024);
		if (size < kb) {
			return String.format("%d B", (int) size);
		} else if (size < mb) {
			return String.format("%.2f KB", size / kb); // 保留两位小数
		} else if (size < gb) {
			return String.format("%.2f MB", size / mb);
		} else {
			return String.format("%.2f GB", size / gb);
		}
	}
	
	public static List<LocalFileDirInfo> orderFileListByName(
			ArrayList<LocalFileDirInfo> orgList) {
		LocalFileDirInfo[] arr = orgList.toArray(new LocalFileDirInfo[orgList
				.size()]);
		Arrays.sort(arr, new NameComparator<LocalFileDirInfo>());
		return Arrays.asList(arr);
	}
	
	public static class NameComparator<T> implements Comparator<Object> {

		@Override
		public int compare(Object object1, Object object2) {
			if (object1 instanceof LocalFileDirInfo
					&& object2 instanceof LocalFileDirInfo) {
				String name1 = ((LocalFileDirInfo) object1).name;
				String name2 = ((LocalFileDirInfo) object2).name;
				Comparator cmp = Collator.getInstance(java.util.Locale.CHINA);
				return cmp.compare(name1, name2);
			
			}
			return Integer.MIN_VALUE;
		}
	}
	
	public static String formateFileSize(double filesize) {
		double kiloByte = filesize / 1024;
		if (kiloByte < 1) {
			return filesize + " B";
		}
		double megaByte = kiloByte / 1024;
		if (megaByte < 1) {
			BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
			return result1.setScale(2, BigDecimal.ROUND_HALF_UP)
					.toPlainString() + " KB";
		}
		double gigaByte = megaByte / 1024;
		if (gigaByte < 1) {
			BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
			return result2.setScale(2, BigDecimal.ROUND_HALF_UP)
					.toPlainString() + " MB";
		}
		double teraBytes = gigaByte / 1024;
		if (teraBytes < 1) {
			BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
			return result3.setScale(2, BigDecimal.ROUND_HALF_UP)
					.toPlainString() + " GB";
		}
		double petaBytes = teraBytes / 1024;
		if (petaBytes < 1) {
			BigDecimal result4 = new BigDecimal(Double.toString(teraBytes));
			return result4.setScale(2, BigDecimal.ROUND_HALF_UP)
					.toPlainString() + " TB";
		}

		double exaBytes = petaBytes / 1024;
		if (exaBytes < 1) {
			BigDecimal result5 = new BigDecimal(Double.toString(petaBytes));
			return result5.setScale(2, BigDecimal.ROUND_HALF_UP)
					.toPlainString() + " PB";
		}

		double zettaBytes = exaBytes / 1024;
		if (zettaBytes < 1) {
			BigDecimal result6 = new BigDecimal(Double.toString(exaBytes));
			return result6.setScale(2, BigDecimal.ROUND_HALF_UP)
					.toPlainString() + " EB";
		}
		double yottaBytes = zettaBytes / 1024;
		if (yottaBytes < 1) {
			BigDecimal result7 = new BigDecimal(Double.toString(zettaBytes));
			return result7.setScale(2, BigDecimal.ROUND_HALF_UP)
					.toPlainString() + " ZB";
		}
		BigDecimal result8 = new BigDecimal(yottaBytes);
		return result8.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()
				+ " YB";
	}
	
	public static Object[] getMIMEType(String fileName) {

		String type = "";
		// String fName = f.getName();
		String end = fileName.substring(fileName.lastIndexOf(".") + 1,
				fileName.length()).toLowerCase();

		Object[] res = new Object[2];

		if (end.equals("bmp") || end.equals("gif") || end.equals("jpg")
				|| end.equals("jpeg") || end.equals("png") || end.equals("tif")
				|| end.equals("ico") || end.equals("dwg") || end.equals("webp")) {
			// picture
			type = "image/*";
			res[1] = R.drawable.picture_icon;

		} else if (end.equals("chm")) {
			type = "application/x-chm";
			res[1] = R.drawable.chm_file_icon;
		} else if (end.equals("exe")) {
			type = "application/x-exe";
			res[1] = R.drawable.exe_icon;
		} else if (end.equals("psd")) {

			type = "image/*";
			res[1] = R.drawable.psd_picture_icon;

		} else if (end.equals("ai")) {

			type = "image/*";
			res[1] = R.drawable.ai_icon;

		} else if (end.equals("bz2")) {

			type = "application/x-bzip2";
			res[1] = R.drawable.compressed_icon;

		} else if (end.equals("gz")) {

			type = "application/x-gzip";
			res[1] = R.drawable.compressed_icon;

		} else if (end.equals("zip")) {

			type = "application/x-zip";
			res[1] = R.drawable.compressed_icon;

		} else if (end.equals("rar")) {

			type = "application/x-rar-compressed";
			res[1] = R.drawable.compressed_icon;

		} else if (end.equals("jar")) {

			type = "application/java-archive";
			res[1] = R.drawable.compressed_icon;

		} else if (end.equals("tar")) {

			type = "application/x-tar";
			res[1] = R.drawable.compressed_icon;

		} else if (end.equals("7z")) {

			type = "application/x-7z-compressed";
			res[1] = R.drawable.compressed_icon_7;

		} else if (end.equals("avi") || end.equals("mp4") || end.equals("mov")
				|| end.equals("flv") || end.equals("3gp") || end.equals("m4v")
				|| end.equals("wmv") || end.equals("rm") || end.equals("rmvb")
				|| end.equals("mkv") || end.equals("ts") || end.equals("webm")
				|| end.equals("f4v")) {
			// video

			type = "video/*";
			res[1] = R.drawable.video_icon;

		} else if (end.equals("swf")) {
			// swf

			type = "swf/*";
			res[1] = R.drawable.swf_icon;
		} else if (end.equals("fla")) {
			type = "fla/*";
			res[1] = R.drawable.fla_icon;
		} else if (end.equals("mid") || end.equals("midi") || end.equals("mp3")
				|| end.equals("wav") || end.equals("wma") || end.equals("amr")
				|| end.equals("ogg") || end.equals("m4a") || end.equals("aac")) {
			// audio
			type = "audio/*";
			res[1] = R.drawable.audio_icon;

		} else if (end.equals("css") || end.equals("txt") || end.equals("cpp")
				|| end.equals("el") || end.equals("py") || end.equals("xml")
				|| end.equals("json") || end.equals("js") || end.equals("pl")) {
			// text
			type = "text/*";
			res[1] = R.drawable.txt_icon;

		} else if (end.equals("csv")) {
			type = "text/csv";
			res[1] = R.drawable.csv_icon;
		} else if (end.equals("php")) {
			type = "text/php";
			res[1] = R.drawable.php_icon;
		} else if (end.equals("c")) {
			type = "text/java";
			res[1] = R.drawable.c_icon;
		} else if (end.equals("java")) {
			type = "text/java";
			res[1] = R.drawable.java_icon;
		} else if (end.equals("html") || end.equals("htm")) {
			type = "text/html";
			res[1] = R.drawable.html_icon;
		} else if (end.equals("rtf")) {

			type = "text/rtf";
			res[1] = R.drawable.rtf_icon;

		} else if (end.equals("pdf")) {
			// pdf
			type = "application/pdf";
			res[1] = R.drawable.pdf_icon;

		} else if (end.equals("pptx")) {
			// Office
			type = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
			res[1] = R.drawable.ppt_icon;

		} else if (end.equals("xlsx")) {
			// Office
			type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
			res[1] = R.drawable.xls_icon;

		} else if (end.equals("docx")) {
			// Office
			type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
			res[1] = R.drawable.word_icon;

		} else if (end.equals("xls") || end.equals("xlt") || end.equals("xltx")
				|| end.equals("xltm") || end.equals("xlsm")) {
			// Office
			type = "application/vnd.ms-excel";
			res[1] = R.drawable.xls_icon;

		} else if (end.equals("doc") || end.equals("dot") || end.equals("docm")
				|| end.equals("dotm") || end.equals("dotx")) {
			// Office
			type = "application/msword";
			res[1] = R.drawable.word_icon;

		} else if (end.equals("ppt") || end.equals("pps") || end.equals("pot")
				|| end.equals("potx") || end.equals("pptm")
				|| end.equals("potm")) {
			// Office
			type = "application/vnd.ms-powerpoint";
			res[1] = R.drawable.ppt_icon;

		} else if (end.equals("apk")) {
			// apk
			type = "application/vnd.android.package-archive";
			res[1] = R.drawable.apk_file_icon;
		} else if (end.equals("epub")) {
			type = "application/epub";
			res[1] = R.drawable.epub_icon;
		} else if (end.equals("ipa")) {
			type = "*/*";
			res[1] = R.drawable.ipa_icon;
		} else if (end.equals("xap")) {
			type = "*/*";
			res[1] = R.drawable.xap_icon;
		} else {
			type = "*/*";
			res[1] = R.drawable.unknow_file_icon;
		}

		res[0] = type;

		return res;
	}
	
	public static String removePathLastSlice(String path) {
		if (path == null) {
			return null;
		}

		if (path.endsWith("/") && !path.equals("/")) {
			return path.substring(0, path.length() - 1);
		}

		return path;
	}
	
	public static void showChooseDialog(Context ctx, int title, String msg,
			View contentView, DialogInterface.OnClickListener listener) {

		final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setCancelable(true);
		if (title != 0) {
			builder.setTitle(title);
		}
		if (msg != null)
			builder.setMessage(msg);
		if (contentView != null)
			builder.setView(contentView);
		if (title != 0) {
			builder.setPositiveButton(R.string.ok, listener);
			builder.setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

						}
					});
		}
		final AlertDialog alert = builder.create();
		alert.setCanceledOnTouchOutside(true);
		alert.show();
	}
}
