package com.app.beseye.httptask;

import static com.app.beseye.util.BeseyeJSONUtil.*;
import static com.app.beseye.util.BeseyeConfig.*;

import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.app.beseye.R;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.DeviceUuidFactory;

public class BeseyeAccountTask {
	
	public static final String URL_LOGIN			="user/sign_in";
	public static final String URL_LOGOUT			="user/sign_out";
	public static final String URL_ACCOUNT_CHECK	="user/validate";
	
	public static final String URL_REGISTER			="user/sign_up";
	public static final String URL_PAIRING			="user/vcam/pairing";
	public static final String URL_SET_ATTR			="user/vcam/set_attr";
	public static final String URL_GET_VCAM_LST		="user/vcam/query";
	public static final String URL_GET_INFO			="user/info";
	
	
	public static final String URL_CAM_ATTACH		="vcam/attach";
	public static final String URL_CAM_DEATTACH		="user/vcam/detach";
	public static final String URL_CAM_VALIDATE		="vcam/validate";
	public static final String URL_CAM_VALIDATE_BEE	="vcam/bee_validate";
	
	static public class LoginHttpTask extends BeseyeHttpTask {	 
		public LoginHttpTask(OnHttpTaskCallback cb) {
			super(cb);
			setDialogResId(0, R.string.dialog_msg_login);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(ACC_EMAIL, strParams[0]);
				obj.put(ACC_PASSWORD, strParams[1]);
				obj.put(ACC_REMEM_ME, true);
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_CLIENT_UDID, BeseyeUtils.getAndroidUUid());
				//objClient.put(ACC_CLIENT_UA, "Android");
				//objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_CLIENT, objClient);
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_LOGIN, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	static public class LogoutHttpTask extends BeseyeHttpTask {	 	
		public LogoutHttpTask(OnHttpTaskCallback cb) {
			super(cb);
			setDialogResId(0, R.string.dialog_msg_logiout);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_CLIENT_UDID, BeseyeUtils.getAndroidUUid());
				//objClient.put(ACC_CLIENT_UA, "Android");
				//objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_CLIENT, objClient);
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_LOGOUT, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	static public class CheckAccountTask extends BeseyeHttpTask {	 	
		public CheckAccountTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				//obj.put(ACC_SES_TOKEN, strParams[0]);
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_CLIENT_UDID, BeseyeUtils.getAndroidUUid());
				//objClient.put(ACC_CLIENT_UA, "Android");
				//objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_CLIENT, objClient);
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_ACCOUNT_CHECK, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class RegisterTask extends BeseyeHttpTask {	 	
		public RegisterTask(OnHttpTaskCallback cb) {
			super(cb);
			setDialogResId(0, R.string.dialog_msg_signup);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(ACC_EMAIL, strParams[0]);
				obj.put(ACC_PASSWORD, strParams[1]);
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_CLIENT_UDID, BeseyeUtils.getAndroidUUid());
				//objClient.put(ACC_CLIENT_UA, "Android");
				//objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_CLIENT, objClient);
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_REGISTER, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;		
		}
	}
	
	static public class StartCamPairingTask extends BeseyeHttpTask {	 	
		public StartCamPairingTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(ACC_PAIRING_TYPE, Integer.parseInt(strParams[0]));
				obj.put(ACC_PAIRING_COUNT, 1);
				obj.put(ACC_PAIRING_AP_MAC, strParams[1].replaceAll(":", ""));
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_CLIENT_UDID, BeseyeUtils.getAndroidUUid());
				//objClient.put(ACC_CLIENT_UA, "Android");
				//objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_CLIENT, objClient);
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_PAIRING, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class SetCamAttrTask extends BeseyeHttpTask {	 	
		public SetCamAttrTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(ACC_VCAM_ID, strParams[0]);
				JSONObject attrObj = new JSONObject();
				attrObj.put(ACC_NAME, strParams[1]);
				obj.put(ACC_VCAM_ATTR, attrObj);
				
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_CLIENT_UDID, BeseyeUtils.getAndroidUUid());
				//objClient.put(ACC_CLIENT_UA, "Android");
				//objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_CLIENT, objClient);
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_SET_ATTR, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class GetUserInfoTask extends BeseyeHttpTask {	 	
		public GetUserInfoTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {				
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_CLIENT_UDID, BeseyeUtils.getAndroidUUid());
				//objClient.put(ACC_CLIENT_UA, "Android");
				//objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_CLIENT, objClient);
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_GET_INFO, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class GetVCamListTask extends BeseyeHttpTask {	 	
		public GetVCamListTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {				
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_CLIENT_UDID, BeseyeUtils.getAndroidUUid());
				//objClient.put(ACC_CLIENT_UA, "Android");
				//objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_CLIENT, objClient);
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_GET_VCAM_LST, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class CamDeattchTask extends BeseyeHttpTask {	 	
		public CamDeattchTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(ACC_VCAM_ID, "9b2eb34759be4320a0c70c1fc483f888");
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_CLIENT_UDID, BeseyeUtils.getAndroidUUid());
				obj.put(ACC_CLIENT, objClient);
				Log.e(TAG, "obj:"+obj.toString());
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_CAM_ATTACH, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	//Test cam behaviors
	
	static public class CamAttchTask extends BeseyeHttpTask {	 	
		public CamAttchTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(ACC_PAIRING_TOKEN, strParams[0]);
				obj.put(ACC_PAIRING_AP_MAC, strParams[1].replaceAll(":", ""));
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_CLIENT_UDID, TMP_VCAM_ID);
				objClient.put(ACC_CLIENT_UA, "BeseyeCam");
				//objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_VCAM_CLIENT, objClient);
				Log.e(TAG, "obj:"+obj.toString());
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_CAM_ATTACH, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class CamBeeValidateTask extends BeseyeHttpTask {	 	
		public CamBeeValidateTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(ACC_PAIRING_TOKEN, strParams[0]);
				obj.put(ACC_PAIRING_AP_MAC, strParams[1].replaceAll(":", ""));
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_VCAM_ID, strParams[2]);
				objClient.put(ACC_CLIENT_UDID, TMP_VCAM_ID);
				objClient.put(ACC_CLIENT_UA, "BeseyeCam");
				//objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_VCAM_CLIENT, objClient);
				Log.e(TAG, "obj:"+obj.toString());
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_CAM_VALIDATE_BEE, obj.toString(), strParams[3]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class CamValidateTask extends BeseyeHttpTask {	 	
		public CamValidateTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_VCAM_ID, strParams[0]);
				objClient.put(ACC_CLIENT_UDID, TMP_VCAM_ID);
				objClient.put(ACC_CLIENT_UA, "BeseyeCam");
				//objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_VCAM_CLIENT, objClient);
				Log.e(TAG, "obj:"+obj.toString());
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_CAM_VALIDATE, obj.toString(), strParams[1]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
}
