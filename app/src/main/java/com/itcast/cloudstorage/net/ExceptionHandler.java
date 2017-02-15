package com.itcast.cloudstorage.net;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.itcast.cloudstorage.R;
import com.itcast.cloudstorage.R.string;
import com.itcast.cloudstorage.bean.Event;
import com.vdisk.net.exception.VDiskServerException;

public class ExceptionHandler {
	public static final int UNLINKED_ERROR = 1005;
	public static final int FILE_TOO_BIG_ERROR = 1100;
	public static final int PARTIAL_FILE_ERROR = 1101;
	public static final int OTHER_ERROR = 1102;
	public static final int FILE_NOT_FOUND = 1103;
	public static final int DOWNLOAD_FILE_ALREADY_EXIST = 1104;

	// 网络请求相关，客户端错误码
	public static final int VdiskConnectionFailureErrorType = 1;
	public static final int VdiskRequestTimedOutErrorType = 2;
	public static final int VdiskAuthenticationErrorType = 3;
	public static final int VdiskRequestCancelledErrorType = 4;
	public static final int VdiskUnableToCreateRequestErrorType = 5;
	public static final int VdiskInternalErrorWhileBuildingRequestType = 6;
	public static final int VdiskInternalErrorWhileApplyingCredentialsType = 7;
	public static final int VdiskFileManagementError = 8; // 下载文件时候的文件管理方面的错误，文件不存在、文件删除失败、文件属性获取失败、文件移动失败等
	public static final int VdiskTooMuchRedirectionErrorType = 9;
	public static final int VdiskUnhandledExceptionError = 10; // 未处理的异常错误
	public static final int VdiskCompressionError = 11; // http请求压缩相关

	// 业务逻辑相关，客户端错误码
	public static final int kVdiskErrorNone = 0;
	public static final int kVdiskErrorGenericError = 1000; // 粗略的错误
	public static final int kVdiskErrorFileNotFound = 1001; // 本地某个文件不存在
	public static final int kVdiskErrorInsufficientDiskSpace = 1002; // 本地空间不足
	public static final int kVdiskErrorIllegalFileType = 1003;
	public static final int kVdiskErrorInvalidResponse = 1004;
	public static final int kVdiskErrorSessionError = 1005;
	public static final int kVdiskErrorFileContentLengthNotMatch = 1006; // 下载文件的大小和http响应头Content-Length不一致
	public static final int kVdiskErrorGetFileAttributesFailure = 1007; // 获得文件属性失败
	public static final int kVdiskErrorS3URLExpired = 1008; // S3下载链接过期
	public static final int kVdiskErrorMd5NotMatched = 1009; // MD5不匹配

	// 微博SSO相关
	public static final int kSinaWeiboSDKErrorCodeParseError = 1200; // 微博SSO返回不是json格式
	public static final int kSinaWeiboSDKErrorCodeSSOParamsError = 1202; // 返现微博SSO解析返回参数不符合规范

	/**
	 * 微盘错误码 v2
	 */

	/* 系统繁忙，请稍后再试(21331) */
	public static final int _21331_SYSTEM_BUSY = 21331;

	/* 重定向地址不匹配(21322) */
	public static final int _21322_REDIRECT_URL_WRONG = 21322;

	/* 请求不合法(21323) */
	public static final int _21323_REQUEST_ILLEGAL = 21323;

	/* 账号异常, 请先解除异常(21334) */
	public static final int _21334_ACCOUNT_ERROR = 21334;

	/* 登录超时(21325) */
	public static final int _21325_LOGIN_TIMEOUT = 21325;

	/* 登录超时, 请重新登录 */
	public static final int _21327_LOGIN_TIMEOUT = 21327;

	/* 请求无响应。 */
	public static final int _40001_SERVER_NO_RESPONSE = 40001;

	/* 路径错误。 */
	public static final int _40002_INVALIDATE_PATH = 40002;

	/* 目标路径不存在 */
	public static final int _40003_PATH_NOT_EXIST = 40003;

	/* 令牌无效。 */
	public static final int _40101_INVALIDATE_TOKEN = 40101;

	/* 令牌验证失败。 */
	public static final int _40102_TOKEN_INVALIDATION_FAILED = 40102;

	/* 令牌验证失败。 */
	public static final int _40103_TOKEN_INVALIDATION_FAILED = 40103;

	/* 禁止访问。没有足够的访问权限。 */
	public static final int _40301_ACCESS_FORBIDDEN = 40301;

	/* 在指定的路径下已有目录或文件。 */
	public static final int _40302_FILE_ALREADY_EXIST = 40302;

	/* 操作无效（如：把目录移动或复制到自己的子目录里）。 */
	public static final int _40303_INVALIDATE_OPERATION = 40303;

	/* 由于政策限制，该文件不允许分享。 */
	public static final int _40304_SHARE_NOT_ALLOWED = 40304;

	/* 不允许操作此文件 */
	public static final int _40305_USE_NOT_ALLOWED = 40305;

	/* 此操作不可重复，禁止操作。 */
	public static final int _40306_OPERATION_CANNOT_REPEATED = 40306;

	/* 事件ID已过期，禁止操作。 */
	public static final int _40307_EVENT_ID_IS_OLD = 40307;

	/* 文件正被其他操作占用，不允许对文件进行写操作。 */
	public static final int _40308_ANOTHER_OPERATION_IN_PROGRESS = 40308;

	/* 上传合并失败 */
	public static final int _40309_UPLOAD_MERGE_FAILED = 40309;

	/* 上传MD5检测失败 */
	public static final int _40310_CHECK_MD5_FAILED = 40310;

	/* Forbidden. Your IP is not permitted to access(40311). */
	public static final int _40311_IP_NOT_ALLOWED_TO_ACCESS = 40311;

	/* 文件夹不允许被分享。 */
	public static final int _40312_SHARE_FOLDER_NOT_ALLOWED = 40312;

	/* 分享操作涉及到文件或目录过多 */
	public static final int _40313_SHARE_TOO_MANY_FILES = 40313;

	/* 在指定的路径已存在同名文件 */
	public static final int _40314_SAME_FILE_ALREADY_EXIST_IN_THE_SPECIFIED_PATH = 40314;

	/* 在指定的路径已存在同名目录 */
	public static final int _40315_SAME_DIR_ALREADY_EXIST_IN_THE_SPECIFIED_PATH = 40315;
	
	/* 账号异常, 你可能没有开通微博或者账号被屏蔽. */
	public static final int _40317_ACCOUNT_IS_NOT_WEIBO_USER_OR_FORBIDDEN = 40317;

	/* 用户不存在。 */
	public static final int _40401_USER_NOT_FOUND = 40401;

	/* 上级目录不存在。 */
	public static final int _40402_PARENT_PATH_NOT_EXIST = 40402;

	/* 指定目录中找不到该文件或文件夹。 */
	public static final int _40403_NO_FILE_FOUND_IN_THE_SPECIFIED_PATH = 40403;

	/* 版本信息不存在。 */
	public static final int _40404_REVISION_NOT_EXIST = 40404;

	/* 无法找到相关文件(40405) */
	public static final int _40405_REFERENCE_FILE_NOT_FOUND = 40405;

	/* 无法找到相关文件。 */
	public static final int _40406_REFERENCE_FILE_NOT_FOUND = 40406;
	
	/* 分享的文件或目录被屏蔽。 */
	public static final int _40622_SHARED_FILE_OR_FOLDER_HAS_BEEN_FORBIDDEN = 40622;

	/* 没有可用的文件流 */
	public static final int _40407_NO_FILE_STREAM = 40407;

	/* 分类不存在 */
	public static final int _40410_NO_CATEGORY = 40410;

	/* 没有可用的在线阅读文件 */
	public static final int _40411_NO_ONLINE_READABLE_FILE = 40411;

	/* 该目录中目录数已达上限。 */
	public static final int _40601_MAXIMUM_NUMBER_OF_FOLDERS_WITHIN_ONE_FOLDER_EXCEEDED = 40601;

	/* 该目录文件数已达上限。 */
	public static final int _40602_MAXIMUM_NUMBER_OF_FILES_WITHIN_ONE_FOLDER_EXCEEDED = 40602;

	/* 选中的文件或文件夹过多，操作失败。 */
	public static final int _40603_TOO_MANY_OPERATIONS = 40603;

	/* 本操作仅针对单个文件。 */
	public static final int _40604_ONLY_ONE_FILE_SUPPORTED = 40604;

	/* 本操作仅针对单个文件夹。 */
	public static final int _40605_ONLY_ONE_FOLDER_SUPPORTED = 40605;

	/* 该目录中成员数已达上限 */
	public static final int _40606_MAX_MEMBERS_IN_FOLDER = 40606;

	/* 该目录文件数已达上限，系统自动添加新文件失败。 */
	public static final int _40607_CANNOT_ADD_MORE_FILE_IN_FOLDER = 40607;

	/* 文件过大，无法上传。 */
	public static final int _40608_FILE_TOO_BIG_TO_UPLOAD = 40608;

	/* 该文件不支持所选的输出格式 */
	public static final int _40609_FORMAT_NOT_SUPPORT = 40609;

	/* 只有分享的目录可以列出 */
	public static final int _40610_ONLY_LIST_SHARED_FOLDER = 40610;

	/* 指定的资源没有内容 */
	public static final int _40611_NO_CONTENT_FOR_THE_RESOURCE = 40611;

	/* 应用请求过多，单IP、单应用或单用户的请求频率将受限。 */
	public static final int _40612_TOO_MANY_REQUESTS = 40612;

	/* 没有足够的访问权限。 */
	public static final int _40613_OPERATION_ON_ROOT_IS_FORBIDDEN = 40613;

	/* 无操作记录。 */
	public static final int _40614_NO_RECORD_OF_ACTIONABLE = 40614;

	/* 输入格式不支持此操作 */
	public static final int _40615_NOT_SUPPORT_THE_OPERATION = 40615;

	/* 版本信息已过期 */
	public static final int _40616_VERSION_INFO_OUT_OF_DATE = 40616;

	/* 文件或目录已分享 */
	public static final int _40617_MEMBER_ALREADY_SHARED = 40617;

	/* 文件或目录总体积已达上限 */
	public static final int _40618_MEMBER_SIZE_TO_BIG = 40618;

	/* 文件或目录未分享 */
	public static final int _40619_MEMBER_NOT_SHARED = 40619;

	/* 好友选择过多 */
	public static final int _40620_MAX_FRIENDS_HAS_BEEN_SELECTED = 40620;

	/* 文件上传失败。 */
	public static final int _41001_UPLOAD_FAILED = 41001;

	/* 文件过大，无法显示缩略图。 */
	public static final int _41501_FILE_TOO_BIG_TO_SHOW_THUMBNAIL = 41501;

	/* 系统错误。 */
	public static final int _50001_SYSTEM_ERROR = 50001;

	/* 文件上传失败 */
	public static final int _50002_UPLOAD_FAILED = 50002;

	/* 文件未保存。 */
	public static final int _50003_FILE_NOT_STORED = 50003;

	/* 文件上传中断 */
	public static final int _50004_UPLOAD_INTERRUPT = 50004;

	/* 请求API验证接口失败 */
	public static final int _50401_API_VALIDATE_FAILED = 50401;

	/* 请求API回调接口失败 */
	public static final int _50402_API_CALLBACK_FAILED = 50402;

	/* 系统错误(50403) */
	public static final int _50403_SYSTEM_ERROR = 50403;

	/* 用户空间已满。 */
	public static final int _50701_VDISK_STORAGE_IS_FULL = 50701;

	/**
	 * 微博错误码
	 */
	/* 任务过多，系统繁忙 */
	public static final int _10009_SYSTEM_IS_BUSY = 10009;

	/* 任务超时 */
	public static final int _10010_JOB_EXPIRED = 10010;

	/* 非法请求 */
	public static final int _10012_ILLEGAL_REQUEST = 10012;

	/* 不合法的微博用户 */
	public static final int _10013_INVALID_WEIBO_USER = 10013;

	/* 请求长度超过限制 */
	public static final int _10018_REQUEST_BODY_LENGTH_OVER_LIMIT = 10018;

	/* 图片太大 */
	public static final int _20006_IMAGE_SIZE_TOO_LARGE = 20006;

	/* 内容为空 */
	public static final int _20008_CONTENT_IS_NULL = 20008;

	/* 发布内容过于频繁 */
	public static final int _20016_OUT_OF_LIMIT = 20016;

	/* 提交相似的信息 */
	public static final int _20017_REPEAT_CONTENT = 20017;

	/* 包含非法网址 */
	public static final int _20018_CONTAIN_ILLEGAL_WEBSITE = 20018;

	/* 提交相同的信息 */
	public static final int _20019_REPEAT_CONTENT = 20019;

	/* 包含广告信息 */
	public static final int _20020_CONTAIN_ADVERTISING = 20020;

	/* 包含非法内容 */
	public static final int _20021_CONTAIN_IS_ILLEGAL = 20021;

	/* 需要验证码 */
	public static final int _20031_TEST_AND_VERIFY = 20031;

	/* 帐号处于锁定状态 */
	public static final int _20034_ACCOUNT_IS_LOCKED = 20034;

	/* 帐号未实名认证 */
	public static final int _20035_ACCOUNT_REALNAME_IS_NOT_VERIFY = 20035;

	/* 微博 id为空 */
	public static final int _20109_WEIBO_ID_IS_NULL = 20109;

	/* 不能发布相同的微博 */
	public static final int _20111_REPEATED_WEIBO_TEXT = 20111;

	/* 不能给不是你粉丝的人发私信 */
	public static final int _20301_WHO_IS_NOT_YOUR_FOLLOWER = 20301;

	/* 不合法的私信 */
	public static final int _20302_ILLEGAL_DIRECT_MESSAGE = 20302;

	/* 不能发布相同的私信 */
	public static final int _20306_REPEATED_DIRECT_MESSAGE_TEXT = 20306;

	/* 发私信太多 */
	public static final int _20308_TOO_MUCH_DIRECT_MESSAGES_WERE_SENDED = 20308;

	/*
	 * 很抱歉，根据相关法规和政策，你暂时无法发送任何内容的私信。如需帮助请联 系 @微博客服 或者致电客服电话400 096 0960（个人） 400
	 * 098 0980（企业）
	 */
	public static final int _20311_CONTENT_IS_ILLEGAL = 20311;

	/* 屏蔽用户列表中存在此uid */
	public static final int _20403_UID_EXISTS_IN_FILTERED_LIST = 20403;

	/* uid对应用户不是登录用户的好友 */
	public static final int _20405_THE_USER_IS_NOT_THE_CURRENT_USER_FRIEND = 20405;

	/* 没有合适的uid */
	public static final int _20407_THERE_IS_NO_PROPER_UID_TO_PROCESS = 20407;

	/* 你不能关注自己 */
	public static final int _20504_CAN_NOT_FOLLOW_YOURSELF = 20504;

	/* 加关注请求超过上限 */
	public static final int _20505_SOCIAL_GRAPH_UPDATES_OUT_OF_RATE_LIMIT = 20505;

	/* 已经关注此用户 */
	public static final int _20506_ALREADY_FOLLOWED = 20506;

	/* 需要输入验证码 */
	public static final int _20507_VERIFICATION_CODE_IS_NEEDED = 20507;

	/* 根据对方的设置，你不能进行此操作 */
	public static final int _20508_ACCORDING_TO_USER_PRIVACY_SETTINGS_YOU_CAN_NOT_DO_THIS = 20508;

	/* 悄悄关注个数到达上限 */
	public static final int _20509_PRIVATE_FRIEND_COUNT_IS_OUT_OF_LIMIT = 20509;

	/* 不是悄悄关注人 */
	public static final int _20510_NOT_PRIVATE_FRIEND = 20510;

	/* 已经悄悄关注此用户 */
	public static final int _20511_ALREADY_FOLLOWED_PRIVATELY = 20511;

	/* 你已经把此用户加入黑名单，加关注前请先解除 */
	public static final int _20512_DELETE_THE_USER_FROM_YOU_BLACKLIST_BEFORE_YOU_FOLLOW_THE_USER = 20512;

	/* 你的关注人数已达上限 */
	public static final int _20513_FRIEND_COUNT_IS_OUT_OF_LIMIT = 20513;

	/* hi超人，你今天已经关注很多喽，接下来的时间想想如何让大家都来关注你吧！如有问题，请联系新浪客服：400 690 0000 */
	public static final int _20521_CONCERNED_A_LOT_OF_PEOPLE = 20521;
	public static final int _20524_CONCERNED_A_LOT_OF_PEOPLE = 20524;

	/* 还未关注此用户 */
	public static final int _20522_NOT_FOLLOWED = 20522;

	/* 还不是粉丝 */
	public static final int _20523_NOT_FOLLOWERS = 20523;

	public static final int _21301_AUTH_FAILED = 21301;

	/* 已是关注用户，不能发送关注邀请! */
	public static final int _22305_ALREADY_FOLLOW = 22305;

	public static Event getErrEvent(Context ctx, VDiskServerException e,
			Event event) {
		if (e.body != null) {
			String errbody = e.body.error;
			if (errbody != null) {
				String[] split = errbody.split(":");
				if (split.length == 2) {
					int errCode = 0;
					try {
						errCode = Integer.parseInt(split[0].trim());
					} catch (NumberFormatException e1) {
					}
					// String errMsg = split[1].trim();
					event.errCode = errCode;
					// event.errMsg = getErrMsgByErrCode(errCode, errMsg, ctx);
					return event;
				}
			}
		}

		event.errCode = e.error;

		return event;
	}

	public static String getErrMsgByErrCode(int errCode, String defaultValue,
			Context ctx) {
		String msg = "";
		switch (errCode) {
		case _10009_SYSTEM_IS_BUSY:
			msg = ctx.getResources().getString(R.string._10009_system_is_busy);
			break;
		case _10010_JOB_EXPIRED:
			msg = ctx.getResources().getString(R.string._10010_job_expired);
			break;
		case _10012_ILLEGAL_REQUEST:
			msg = ctx.getResources().getString(R.string._10012_illegal_request);
			break;
		case _10013_INVALID_WEIBO_USER:
			msg = ctx.getResources().getString(
					R.string._10013_invalid_weibo_user);
			break;
		case _10018_REQUEST_BODY_LENGTH_OVER_LIMIT:
			msg = ctx.getResources().getString(
					R.string._10018_request_body_length_over_limit);
			break;
		case _20006_IMAGE_SIZE_TOO_LARGE:
			msg = ctx.getResources().getString(
					R.string._20006_image_size_too_large);
			break;
		case _20008_CONTENT_IS_NULL:
			msg = ctx.getResources().getString(R.string._20008_content_is_null);
			break;
		case _20016_OUT_OF_LIMIT:
			msg = ctx.getResources().getString(R.string._20016_out_of_limit);
			break;
		case _20017_REPEAT_CONTENT:
			msg = ctx.getResources().getString(R.string._20017_repeat_content);
			break;
		case _20018_CONTAIN_ILLEGAL_WEBSITE:
			msg = ctx.getResources().getString(
					R.string._20018_contain_illegal_website);
			break;
		case _20019_REPEAT_CONTENT:
			msg = ctx.getResources().getString(R.string._20019_repeat_content);
			break;
		case _20020_CONTAIN_ADVERTISING:
			msg = ctx.getResources().getString(
					R.string._20020_contain_advertising);
			break;
		case _20021_CONTAIN_IS_ILLEGAL:
			msg = ctx.getResources().getString(
					R.string._20021_contain_is_illegal);
			break;
		case _20031_TEST_AND_VERIFY:
			msg = ctx.getResources().getString(R.string._20031_test_and_verify);
			break;
		case _20034_ACCOUNT_IS_LOCKED:
			msg = ctx.getResources().getString(
					R.string._20034_account_is_locked);
			break;
		case _20035_ACCOUNT_REALNAME_IS_NOT_VERIFY:
			msg = ctx.getResources().getString(
					R.string._20035_account_realname_is_not_verify);
			break;
		case _20109_WEIBO_ID_IS_NULL:
			msg = ctx.getResources()
					.getString(R.string._20109_weibo_id_is_null);
			break;
		case _20111_REPEATED_WEIBO_TEXT:
			msg = ctx.getResources().getString(
					R.string._20111_repeated_weibo_text);
			break;
		case _20301_WHO_IS_NOT_YOUR_FOLLOWER:
			msg = ctx.getResources().getString(
					R.string._20301_who_is_not_your_follower);
			break;
		case _20302_ILLEGAL_DIRECT_MESSAGE:
			msg = ctx.getResources().getString(
					R.string._20302_illegal_direct_message);
			break;
		case _20306_REPEATED_DIRECT_MESSAGE_TEXT:
			msg = ctx.getResources().getString(
					R.string._20306_repeated_direct_message_text);
			break;
		case _20308_TOO_MUCH_DIRECT_MESSAGES_WERE_SENDED:
			msg = ctx.getResources().getString(
					R.string._20308_too_much_direct_messages_were_sended);
			break;
		case _20311_CONTENT_IS_ILLEGAL:
			msg = ctx.getResources().getString(
					R.string._20311_content_is_illegal);
			break;
		case _20403_UID_EXISTS_IN_FILTERED_LIST:
			msg = ctx.getResources().getString(
					R.string._20403_uid_exists_in_filtered_list);
			break;
		case _20405_THE_USER_IS_NOT_THE_CURRENT_USER_FRIEND:
			msg = ctx.getResources().getString(
					R.string._20405_the_user_is_not_the_current_user_friend);
			break;
		case _20407_THERE_IS_NO_PROPER_UID_TO_PROCESS:
			msg = ctx.getResources().getString(
					R.string._20407_there_is_no_proper_uid_to_process);
			break;
		case _20504_CAN_NOT_FOLLOW_YOURSELF:
			msg = ctx.getResources().getString(
					R.string._20504_can_not_follow_yourself);
			break;
		case _20505_SOCIAL_GRAPH_UPDATES_OUT_OF_RATE_LIMIT:
			msg = ctx.getResources().getString(
					R.string._20505_social_graph_updates_out_of_rate_limit);
			break;
		case _20506_ALREADY_FOLLOWED:
			msg = ctx.getResources()
					.getString(R.string._20506_already_followed);
			break;
		case _20507_VERIFICATION_CODE_IS_NEEDED:
			msg = ctx.getResources().getString(
					R.string._20507_verification_code_is_needed);
			break;
		case _20508_ACCORDING_TO_USER_PRIVACY_SETTINGS_YOU_CAN_NOT_DO_THIS:
			msg = ctx
					.getResources()
					.getString(
							R.string._20508_according_to_user_privacy_settings_you_can_not_do_this);
			break;
		case _20509_PRIVATE_FRIEND_COUNT_IS_OUT_OF_LIMIT:
			msg = ctx.getResources().getString(
					R.string._20509_private_friend_count_is_out_of_limit);
			break;
		case _20510_NOT_PRIVATE_FRIEND:
			msg = ctx.getResources().getString(
					R.string._20510_not_private_friend);
			break;
		case _20511_ALREADY_FOLLOWED_PRIVATELY:
			msg = ctx.getResources().getString(
					R.string._20511_already_followed_privately);
			break;
		case _20512_DELETE_THE_USER_FROM_YOU_BLACKLIST_BEFORE_YOU_FOLLOW_THE_USER:
			msg = ctx
					.getResources()
					.getString(
							R.string._20512_delete_the_user_from_you_blacklist_before_you_follow_the_user);
			break;
		case _20513_FRIEND_COUNT_IS_OUT_OF_LIMIT:
			msg = ctx.getResources().getString(
					R.string._20513_friend_count_is_out_of_limit);
			break;
		case _20521_CONCERNED_A_LOT_OF_PEOPLE:
		case _20524_CONCERNED_A_LOT_OF_PEOPLE:
			msg = ctx.getResources().getString(
					R.string._20521_or_20524_concerned_a_lot_of_people);
			break;
		case _20522_NOT_FOLLOWED:
			msg = ctx.getResources().getString(R.string._20522_not_followed);
			break;
		case _20523_NOT_FOLLOWERS:
			msg = ctx.getResources().getString(R.string._20523_not_followers);
			break;
		case _21301_AUTH_FAILED:
			msg = ctx.getResources().getString(R.string._21301_auth_failed);
			break;
		case _22305_ALREADY_FOLLOW:
			msg = ctx.getResources().getString(R.string._22305_already_follow);
			break;
		case _21322_REDIRECT_URL_WRONG:
			msg = ctx.getResources().getString(
					R.string._21322_redirect_url_wrong);
			break;
		case _21323_REQUEST_ILLEGAL:
			msg = ctx.getResources().getString(R.string._21323_request_illegal);
			break;
		case _21325_LOGIN_TIMEOUT:
			msg = ctx.getResources().getString(R.string._21325_login_timeout);
			break;
		case _21327_LOGIN_TIMEOUT:
			msg = ctx.getResources().getString(R.string._21327_login_timeout);
			break;
		case _21331_SYSTEM_BUSY:
			msg = ctx.getResources().getString(R.string._21331_system_busy);
			break;
		case _21334_ACCOUNT_ERROR:
			msg = ctx.getResources().getString(R.string._21334_account_error);
			break;
		case _40001_SERVER_NO_RESPONSE:
			msg = ctx.getResources().getString(
					R.string._40001_server_no_response);
			break;
		case _40002_INVALIDATE_PATH:
			msg = ctx.getResources().getString(R.string._40002_invalidate_path);
			break;
		case _40003_PATH_NOT_EXIST:
			msg = ctx.getResources().getString(R.string._40003_path_not_exist);
			break;
		case _40101_INVALIDATE_TOKEN:
			msg = ctx.getResources()
					.getString(R.string._40101_invalidate_token);
			break;
		case _40102_TOKEN_INVALIDATION_FAILED:
			msg = ctx.getResources().getString(
					R.string._40102_token_invalidation_failed);
			break;
		case _40103_TOKEN_INVALIDATION_FAILED:
			msg = ctx.getResources().getString(
					R.string._40103_token_invalidation_failed);
			break;
		case _40301_ACCESS_FORBIDDEN:
			msg = ctx.getResources()
					.getString(R.string._40301_access_forbidden);
			break;
		case _40302_FILE_ALREADY_EXIST:
			msg = ctx.getResources().getString(
					R.string._40302_file_already_exist);
			break;
		case _40303_INVALIDATE_OPERATION:
			msg = ctx.getResources().getString(
					R.string._40303_invalidate_operation);
			break;
		case _40304_SHARE_NOT_ALLOWED:
			msg = ctx.getResources().getString(
					R.string._40304_share_not_allowed);
			break;
		case _40305_USE_NOT_ALLOWED:
			msg = ctx.getResources().getString(R.string._40305_use_not_allowed);
			break;
		case _40306_OPERATION_CANNOT_REPEATED:
			msg = ctx.getResources().getString(
					R.string._40306_operation_cannot_repeated);
			break;
		case _40307_EVENT_ID_IS_OLD:
			msg = ctx.getResources().getString(R.string._40307_event_id_is_old);
			break;
		case _40308_ANOTHER_OPERATION_IN_PROGRESS:
			msg = ctx.getResources().getString(
					R.string._40308_another_operation_in_progress);
			break;
		case _40309_UPLOAD_MERGE_FAILED:
			msg = ctx.getResources().getString(
					R.string._40309_upload_merge_failed);
			break;
		case _40310_CHECK_MD5_FAILED:
			msg = ctx.getResources()
					.getString(R.string._40310_check_md5_failed);
			break;
		case _40311_IP_NOT_ALLOWED_TO_ACCESS:
			msg = ctx.getResources().getString(
					R.string._40311_ip_not_allowed_to_access);
			break;
		case _40312_SHARE_FOLDER_NOT_ALLOWED:
			msg = ctx.getResources().getString(
					R.string._40312_share_folder_not_allowed);
			break;
		case _40313_SHARE_TOO_MANY_FILES:
			msg = ctx.getResources().getString(
					R.string._40313_share_too_many_files);
			break;
		case _40314_SAME_FILE_ALREADY_EXIST_IN_THE_SPECIFIED_PATH:
			msg = ctx
					.getResources()
					.getString(
							R.string._40314_same_file_already_exist_in_the_specified_path);
			break;
		case _40315_SAME_DIR_ALREADY_EXIST_IN_THE_SPECIFIED_PATH:
			msg = ctx
					.getResources()
					.getString(
							R.string._40315_same_dir_already_exist_in_the_specified_path);
			break;
		case _40317_ACCOUNT_IS_NOT_WEIBO_USER_OR_FORBIDDEN:
			msg = ctx
			.getResources()
			.getString(
					R.string._40317_account_is_not_weibo_user_or_forbidden);
			break;
		case _40401_USER_NOT_FOUND:
			msg = ctx.getResources().getString(R.string._40401_user_not_found);
			break;
		case _40402_PARENT_PATH_NOT_EXIST:
			msg = ctx.getResources().getString(
					R.string._40402_parent_path_not_exist);
			break;
		case _40403_NO_FILE_FOUND_IN_THE_SPECIFIED_PATH:
			msg = ctx.getResources().getString(
					R.string._40403_no_file_found_in_the_specified_path);
			break;
		case _40404_REVISION_NOT_EXIST:
			msg = ctx.getResources().getString(
					R.string._40404_revision_not_exist);
			break;
		case _40405_REFERENCE_FILE_NOT_FOUND:
			msg = ctx.getResources().getString(
					R.string._40405_reference_file_not_found);
			break;
		case _40406_REFERENCE_FILE_NOT_FOUND:
			msg = ctx.getResources().getString(
					R.string._40406_reference_file_not_found);
			break;
		case _40407_NO_FILE_STREAM:
			msg = ctx.getResources().getString(R.string._40407_no_file_stream);
			break;
		case _40410_NO_CATEGORY:
			msg = ctx.getResources().getString(R.string._40410_no_category);
			break;
		case _40411_NO_ONLINE_READABLE_FILE:
			msg = ctx.getResources().getString(
					R.string._40411_no_online_readable_file);
			break;
		case _40601_MAXIMUM_NUMBER_OF_FOLDERS_WITHIN_ONE_FOLDER_EXCEEDED:
			msg = ctx
					.getResources()
					.getString(
							R.string._40601_maximum_number_of_folders_within_one_folder_exceeded);
			break;
		case _40602_MAXIMUM_NUMBER_OF_FILES_WITHIN_ONE_FOLDER_EXCEEDED:
			msg = ctx
					.getResources()
					.getString(
							R.string._40602_maximum_number_of_files_within_one_folder_exceeded);
			break;
		case _40603_TOO_MANY_OPERATIONS:
			msg = ctx.getResources().getString(
					R.string._40603_too_many_operations);
			break;
		case _40604_ONLY_ONE_FILE_SUPPORTED:
			msg = ctx.getResources().getString(
					R.string._40604_only_one_file_supported);
			break;
		case _40605_ONLY_ONE_FOLDER_SUPPORTED:
			msg = ctx.getResources().getString(
					R.string._40605_only_one_folder_supported);
			break;
		case _40606_MAX_MEMBERS_IN_FOLDER:
			msg = ctx.getResources().getString(
					R.string._40606_max_members_in_folder);
			break;
		case _40607_CANNOT_ADD_MORE_FILE_IN_FOLDER:
			msg = ctx.getResources().getString(
					R.string._40607_cannot_add_more_file_in_folder);
			break;
		case _40608_FILE_TOO_BIG_TO_UPLOAD:
			msg = ctx.getResources().getString(
					R.string._40608_file_too_big_to_upload);
			break;
		case _40609_FORMAT_NOT_SUPPORT:
			msg = ctx.getResources().getString(
					R.string._40609_format_not_support);
			break;
		case _40610_ONLY_LIST_SHARED_FOLDER:
			msg = ctx.getResources().getString(
					R.string._40610_only_list_shared_folder);
			break;
		case _40611_NO_CONTENT_FOR_THE_RESOURCE:
			msg = ctx.getResources().getString(
					R.string._40611_no_content_for_the_resource);
			break;
		case _40612_TOO_MANY_REQUESTS:
			msg = ctx.getResources().getString(
					R.string._40612_too_many_requests);
			break;
		case _40613_OPERATION_ON_ROOT_IS_FORBIDDEN:
			msg = ctx.getResources().getString(
					R.string._40613_operation_on_root_is_forbidden);
			break;
		case _40614_NO_RECORD_OF_ACTIONABLE:
			msg = ctx.getResources().getString(
					R.string._40614_no_record_of_actionable);
			break;
		case _40615_NOT_SUPPORT_THE_OPERATION:
			msg = ctx.getResources().getString(
					R.string._40615_not_support_the_operation);
			break;
		case _40616_VERSION_INFO_OUT_OF_DATE:
			msg = ctx.getResources().getString(
					R.string._40616_version_info_out_of_date);
			break;
		case _40617_MEMBER_ALREADY_SHARED:
			msg = ctx.getResources().getString(
					R.string._40617_member_already_shared);
			break;
		case _40618_MEMBER_SIZE_TO_BIG:
			msg = ctx.getResources().getString(
					R.string._40618_member_size_to_big);
			break;
		case _40619_MEMBER_NOT_SHARED:
			msg = ctx.getResources().getString(
					R.string._40619_member_not_shared);
			break;
		case _40620_MAX_FRIENDS_HAS_BEEN_SELECTED:
			msg = ctx.getResources().getString(
					R.string._40620_max_friends_has_been_selected);
			break;
		case _40622_SHARED_FILE_OR_FOLDER_HAS_BEEN_FORBIDDEN:
			msg = ctx.getResources().getString(
					R.string._40622_shared_file_or_folder_has_been_forbidden);
			break;
		case _41001_UPLOAD_FAILED:
			msg = ctx.getResources().getString(R.string._41001_upload_failed);
			break;
		case _41501_FILE_TOO_BIG_TO_SHOW_THUMBNAIL:
			msg = ctx.getResources().getString(
					R.string._41501_file_too_big_to_show_thumbnail);
			break;
		case _50001_SYSTEM_ERROR:
			msg = ctx.getResources().getString(R.string._50001_system_error);
			break;
		case _50002_UPLOAD_FAILED:
			msg = ctx.getResources().getString(R.string._50002_upload_failed);
			break;
		case _50003_FILE_NOT_STORED:
			msg = ctx.getResources().getString(R.string._50003_file_not_stored);
			break;
		case _50004_UPLOAD_INTERRUPT:
			msg = ctx.getResources()
					.getString(R.string._50004_upload_interrupt);
			break;
		case _50401_API_VALIDATE_FAILED:
			msg = ctx.getResources().getString(
					R.string._50401_api_validate_failed);
			break;
		case _50402_API_CALLBACK_FAILED:
			msg = ctx.getResources().getString(
					R.string._50402_api_callback_failed);
			break;
		case _50403_SYSTEM_ERROR:
			msg = ctx.getResources().getString(R.string._50403_system_error);
			break;
		case _50701_VDISK_STORAGE_IS_FULL:
			msg = ctx.getResources().getString(
					R.string._50701_vdisk_storage_is_full);
			break;
		case VdiskConnectionFailureErrorType:
			msg = ctx.getResources().getString(
					R.string.network_connection_error);
			break;
		case VdiskRequestCancelledErrorType:
			msg = ctx.getResources().getString(R.string.request_canceled);
			break;
		case kVdiskErrorInvalidResponse:
			msg = ctx.getResources().getString(R.string.data_parser_error);
			break;
		case kVdiskErrorInsufficientDiskSpace:
			msg = ctx.getResources().getString(R.string.local_storage_is_full);
			break;
		case UNLINKED_ERROR:
			msg = ctx.getResources().getString(R.string.token_out_of_date);
			break;
		case FILE_TOO_BIG_ERROR:
			msg = ctx.getResources().getString(R.string.file_too_big);
			break;
		case PARTIAL_FILE_ERROR:
			msg = ctx.getResources().getString(R.string.transfer_not_complete);
			break;
		case FILE_NOT_FOUND:
			msg = ctx.getResources().getString(R.string.file_not_found);
			break;
		case DOWNLOAD_FILE_ALREADY_EXIST:
			msg = ctx.getResources().getString(
					R.string.download_file_already_exist);
			break;
		case OTHER_ERROR:
			msg = ctx.getResources().getString(R.string.unknow_error);
			break;
		default:
			if (!TextUtils.isEmpty(defaultValue)) {
				msg = defaultValue;
			} else {
				msg = ctx.getResources().getString(R.string.unknow_error);
			}
			break;
		}
		return msg;
	}

	public static String getErrMsgByErrCode(int errCode, Context ctx) {
		return getErrMsgByErrCode(errCode, null, ctx);
	}

	public static void toastErrMessage(final Context ctx, int errCode) {
		String msg = getErrMsgByErrCode(errCode, ctx);
		if (TextUtils.isEmpty(msg)) {
			return;
		}
		Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
	}
}
