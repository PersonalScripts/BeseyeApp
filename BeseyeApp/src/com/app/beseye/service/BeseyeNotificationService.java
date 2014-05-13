package com.app.beseye.service;
import static com.app.beseye.util.BeseyeConfig.*;
import static com.app.beseye.util.BeseyeJSONUtil.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.GCMIntentService;
import com.app.beseye.R;
import com.app.beseye.httptask.BeseyeHttpTask;
import com.app.beseye.httptask.BeseyePushServiceTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SessionData;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeSharedPreferenceUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.OnNetworkChangeCallback;
import com.app.beseye.websockets.WebsocketsMgr;
import com.app.beseye.websockets.WebsocketsMgr.OnWSChannelStateChangeListener;
import com.google.android.gcm.GCMRegistrar;

import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class BeseyeNotificationService extends Service implements com.app.beseye.httptask.BeseyeHttpTask.OnHttpTaskCallback,
																  OnWSChannelStateChangeListener,
																  OnNetworkChangeCallback{
	
	/** For showing and hiding our notification. */
	private NotificationManager mNotificationManager;
	private static final int NOTIFICATION_TYPE_BASE = 0x0;
	private static final int NOTIFICATION_TYPE_INFO = NOTIFICATION_TYPE_BASE+1;
	private static final int NOTIFICATION_TYPE_MSG  = NOTIFICATION_TYPE_INFO;//NOTIFICATION_TYPE_BASE+2;
	
	
	/** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 1;
    
    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT 			= 2;
    public static final int MSG_CHECK_NOTIFY_NUM 			= 3;
    public static final int MSG_SET_NOTIFY_NUM 				= 4;
    public static final int MSG_QUERY_NOTIFY_NUM 			= 5;
    public static final int MSG_SHOW_NOTIFICATION 			= 6;
    public static final int MSG_CHECK_CACHE_STATE 			= 7;
    public static final int MSG_CHECK_UNREAD_MSG_NUM		= 8;
    public static final int MSG_SET_UNREAD_MSG_NUM 			= 9;
    public static final int MSG_APP_TO_FOREGROUND 			= 10;
    public static final int MSG_APP_TO_BACKGROUND 			= 11;
    
    public static final int MSG_POST_CHECK_NOTIFY_NUM 		= 14;
    public static final int MSG_POST_CHECK_UNREAD_MSG_NUM 	= 15;
    
    public static final int MSG_BEGIN_TO_PULL_MSG 			= 16;
    public static final int MSG_STOP_TO_PULL_MSG 			= 17;
    
    public static final int MSG_UPDATE_SESSION_DATA 		= 18;
    public static final int MSG_UPDATE_PREF_DATA 			= 19;
    
    public static final int MSG_GSM_REGISTER 				= 20;
    public static final int MSG_GSM_UNREGISTER 				= 21;
    public static final int MSG_GSM_MSG 					= 22;
    public static final int MSG_GSM_ERR 					= 23;
    public static final int MSG_CHECK_DIALOG 			    = 24;
    
    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
//        	if(BeseyeConfig.DEBUG)
//        		Log.i(TAG, "BG service detects "+msg.toString());
        	
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
//                case MSG_CHECK_NOTIFY_NUM:{
//                	if(!SessionMgr.getInstance().isUseridValid() || !SessionMgr.getInstance().getIsCertificated()){
//                		//sendMessageDelayed(Message.obtain(null,MSG_CHECK_NOTIFY_NUM,0,0), 30*1000L);
//                		return;
//                	}
//                	
//                	if(null == mNotificationInfoTask || mNotificationInfoTask.getStatus().equals(AsyncTask.Status.FINISHED)){
//                		mNotificationInfoTask = new iKalaNotificationTask.LoadNotificationInfoTask(BeseyeNotificationService.this);
//                        if(null != mNotificationInfoTask){
//                        	mNotificationInfoTask.execute(SessionMgr.getInstance().getMdid());
//                        }
//                        mNotifyInfo = null;
//                	}
//                    break;
//                }
//                case MSG_CHECK_UNREAD_MSG_NUM:{
//                	if(!SessionMgr.getInstance().isUseridValid() || !SessionMgr.getInstance().getIsCertificated()){
//                		//sendMessageDelayed(Message.obtain(null,MSG_CHECK_UNREAD_MSG_NUM,0,0), 30*1000L);
//                		return;
//                	}
//                	
//                	if(null == mMsgInfoTask || mMsgInfoTask.getStatus().equals(AsyncTask.Status.FINISHED)){
//                		mMsgInfoTask = new iKalaMsgTask.LoadMsgListInfoTask(BeseyeNotificationService.this);
//                        if(null != mMsgInfoTask){
//                        	mMsgInfoTask.execute(SessionMgr.getInstance().getMdid());
//                        }
//                	}
//                    break;
//                }
//                case MSG_QUERY_NOTIFY_NUM:
//                case MSG_SET_NOTIFY_NUM:{
//                	if(null == mNotifyInfo){
//                		sendMessage(Message.obtain(null,MSG_CHECK_NOTIFY_NUM,0,0));
//                		break;
//                	}
//                	
//              	  	int iUnreadNot = getUnreadNotificationNum();
//                	if(Configuration.DEBUG)
//                		Log.i(TAG, "MSG_QUERY_NOTIFY_NUM, iUnreadNot "+iUnreadNot);
//                	for (int i=mClients.size()-1; i>=0; i--) {
//	                      try {
//	                          mClients.get(i).send(Message.obtain(null,
//	                        		  MSG_SET_NOTIFY_NUM, iUnreadNot, 0));
//	                      } catch (RemoteException e) {
//	                          // The client is dead.  Remove it from the list;
//	                          // we are going through the list from back to front
//	                          // so this is safe to do inside the loop.
//	                          mClients.remove(i);
//	                      }
//	                  }
//                	break;
//                }
//                
//                case MSG_SET_UNREAD_MSG_NUM:{
//                	/*if(shouldPullMsg())*/{
//                		int iUnreadMsg = getUnreadMsgNum();
//                		if(Configuration.DEBUG)
//                    		Log.i(TAG, "MSG_SET_UNREAD_MSG_NUM, iUnreadMsg "+iUnreadMsg);
//	                	for (int i=mClients.size()-1; i>=0; i--) {
//		                      try {
//		                          mClients.get(i).send(Message.obtain(null,
//		                        		  MSG_SET_UNREAD_MSG_NUM, iUnreadMsg, 0));
//		                      } catch (RemoteException e) {
//		                          mClients.remove(i);
//		                      }
//		                }
//                	}
//                	break;
//                }
//                case MSG_SHOW_NOTIFICATION:{
//                	if(iKalaFeatureTable.ENABLED_PUSH_SERVICE)
//                		showFirstUnreadNotification();
//                	break;
//                }
//                case MSG_CHECK_CACHE_STATE:{
//                	if(-1 != msg.arg1){
//                		if(iKalaStorageAgent.doCheckCacheSize(BeseyeNotificationService.this.getApplicationContext())){
//                			sendMessageDelayed(Message.obtain(null,MSG_CHECK_CACHE_STATE,0,0), getTimeToCheck());
//                		}else{
//                			sendMessageDelayed(Message.obtain(null,MSG_CHECK_CACHE_STATE,0,0), 1000*60*60);//if false, check it 1 hr later
//                		}
//                	}
//                	break;
//                }
                case MSG_APP_TO_FOREGROUND:{
                	if(mbAppInBackground){
                		Log.i(TAG, "BG service detects MSG_APP_TO_FOREGROUND()");
                		mbAppInBackground = false;
                	}
                	
//                	if(shouldPullMsg())
//            			sendMessageDelayed(Message.obtain(null,MSG_CHECK_UNREAD_MSG_NUM,0,0), 1*1000L);
                	mbAppInBackground = false;
                	
                	checkUserLoginState();
                	break;
                }
                case MSG_APP_TO_BACKGROUND:{
                	if(!mbAppInBackground){
                		Log.i(TAG, "BG service detects MSG_APP_TO_BACKGROUND()");
//                		if(null != mMsgInfoTask){
//                			mMsgInfoTask.cancel(true);
//                		}
                	}
                	mbAppInBackground = true;
                	checkUserLoginState();
                	break;
                }
                case MSG_POST_CHECK_NOTIFY_NUM:{
//                	sendMessageDelayed(Message.obtain(null,MSG_CHECK_NOTIFY_NUM,0,0), 30*1000L);
                	break;
                }
                
                case MSG_POST_CHECK_UNREAD_MSG_NUM:{
//                	if(shouldPullMsg())
//                		sendMessageDelayed(Message.obtain(null,MSG_CHECK_UNREAD_MSG_NUM,0,0), 1*1000L);
                	break;
                }
                case MSG_BEGIN_TO_PULL_MSG:{
                	if(!mbNeedToPullMsg){
                		//Log.i(TAG, "BG service detects MSG_BEGIN_TO_PULL_MSG()");
                		mbNeedToPullMsg = true;
//                		if(shouldPullMsg())
//                			sendMessageDelayed(Message.obtain(null,MSG_CHECK_UNREAD_MSG_NUM,0,0), 1*1000L);
                	}
                	mbNeedToPullMsg = true;
                	break;
                }
                case MSG_STOP_TO_PULL_MSG:{
                	/*if(mbNeedToPullMsg)*/{
                		//Log.i(TAG, "BG service detects MSG_STOP_TO_PULL_MSG()");
//                		if(null != mMsgInfoTask){
//                			mMsgInfoTask.cancel(true);
//                		}
                	}
                	mbNeedToPullMsg = false;
                	break;
                }
                case MSG_UPDATE_SESSION_DATA:{
                	final Bundle bundle = msg.getData();
                	bundle.setClassLoader(getClassLoader());
                	boolean bLoginBefore = SessionMgr.getInstance().isUseridValid();
                	
                	SessionData sessionData = (SessionData) bundle.getParcelable("SessionData");
                	if(null != sessionData){
                		SessionMgr.getInstance().setSessionData(sessionData);
                		//If logout
                		if(bLoginBefore && (!SessionMgr.getInstance().isUseridValid() || null == SessionMgr.getInstance().getAccount() || SessionMgr.getInstance().getAccount().length() == 0)){
                			Log.i(TAG, "BG service detects MSG_UPDATE_SESSION_DATA() and reset");
                			mNotifyInfo = null;
                			mMsgInfo = null;
                			sendMessage(Message.obtain(null,MSG_SET_NOTIFY_NUM,0,0));
                			sendMessage(Message.obtain(null,MSG_SET_UNREAD_MSG_NUM,0,0));
                			if(!SessionMgr.getInstance().isUseridValid())
                				unregisterGCMServer();
                			cancelNotification();
                		}//If Login
                		else if(!bLoginBefore && SessionMgr.getInstance().isUseridValid()){
                			registerGCMServer();
//                			try {
//            		        	mMessenger.send(Message.obtain(null,MSG_CHECK_NOTIFY_NUM,0,0));
//            				} catch (RemoteException e) {
//            					e.printStackTrace();
//            				}
                		}
                		checkUserLoginState();
                	}
                	break;
                }
                case MSG_UPDATE_PREF_DATA:{
                	final Bundle bundle = msg.getData();
                	bundle.setClassLoader(getClassLoader());
//                	iKalaSettingsMgr.SettingData settingData = (iKalaSettingsMgr.SettingData) bundle.getParcelable("SettingData");
//                	iKalaSettingsMgr.getInstance().setSettingData(settingData);
                	break;
                }
                case MSG_GSM_REGISTER:{
                	mbRegisterGCM = true;
                	final Bundle bundle = msg.getData();
                	BeseyeSharedPreferenceUtil.setPrefStringValue(mPref, PUSH_SERVICE_REG_ID, bundle.getString(PUSH_SERVICE_REG_ID));
                	registerPushServer();
                	break;
                }
                case MSG_GSM_UNREGISTER:{
                	mbRegisterGCM = false;
                	unregisterPushServer();
                	break;
                }
                case MSG_GSM_MSG:{
                	final Bundle bundle = msg.getData();
                	String type = bundle.getString(PS_WORK_TYPE.toLowerCase());
//                	if(WORK_CONVERSATION.equals(type)){
//                		sendMessage(Message.obtain(null,MSG_CHECK_UNREAD_MSG_NUM,0,0));
//                	}
//                	sendMessage(Message.obtain(null,MSG_CHECK_NOTIFY_NUM,0,0));
//                	
                	String info = bundle.getString("info");
                	Log.d(TAG, "MSG_GSM_MSG, info : "+info+", type : "+type);
                	Toast.makeText(getApplicationContext(), "Got message from Beseye server, info = "+info, Toast.LENGTH_LONG ).show();
                	break;
                }
                case MSG_GSM_ERR:{
                	break;
                }
                case MSG_CHECK_DIALOG:{
                	checkAndCloseDialog();
                }
                default:
                    super.handleMessage(msg);
            }
        }
    }
    
    static private final int CACHE_CHECK_TIME = 4;//AM 4:00
    private long getTimeToCheck(){
    	long lRet = 0;
    	Date now = new Date();
    	if(null != now){
    		//Log.d(TAG, "getTimeToCheck(), now : "+now);
    		int iHour = now.getHours();
    		int iMin = now.getMinutes();
    		int iSec = now.getSeconds();
    		if(iHour < CACHE_CHECK_TIME){
    			lRet = Math.abs(((60*60*(CACHE_CHECK_TIME - iHour))-(60*iMin+iSec))*1000L);
    		}else if(iHour == CACHE_CHECK_TIME){
    			if(0 == iMin && 0 == iSec){
    				lRet = 0;
    			}else{
    				lRet = Math.abs(((60*60*24)-(60*iMin+iSec))*1000L);
    			}
    		}else{
    			lRet = Math.abs(((60*60*(24+CACHE_CHECK_TIME-iHour))-(60*iMin+iSec))*1000L);
    		}
    	}
    	Log.i(TAG, "getTimeToCheck(), lRet : "+lRet);
    	return lRet;
    }
    
//    private int getUnreadNotificationNum(){
//    	int iRet = 0;
//    	if(null != mNotifyInfo){
//    		iRet = BeseyeJSONUtil.getJSONInt(mNotifyInfo, OBJ_UNREAD);
//    		//iRet = iKalaJSONUtil.getJSONInt(mNotifyInfo, OBJ_UNREAD_SOCIAL)+iKalaJSONUtil.getJSONInt(mNotifyInfo, OBJ_UNREAD_MESSAGE)+iKalaJSONUtil.getJSONInt(mNotifyInfo, OBJ_UNREAD_NOTIFY);
//    	}
//    	return iRet;
//    }
//    
//    private int getUnreadMsgNum(){
//    	int iRet = 0;
//    	if(null != mMsgInfo){
//    		iRet = BeseyeJSONUtil.getJSONInt(mMsgInfo, OBJ_UNREAD);
//    	}
//    	return iRet;
//    }
    
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private String mLastNotifyId = null;
	private long mLastNotifyUpdateTime = 0;
//	private SessionData mSessionData = null;
//	private iKalaSettingsMgr.SettingData mSettingData = null;
	
	static private final String PUSH_SERVICE_PREF 				= "beseye_push_service";
	static private final String PUSH_SERVICE_SENDER_ID 			= "beseye_push_service_sender";
	static public  final String PUSH_SERVICE_REG_ID 			= "beseye_push_service_reg_id";
	static private final String PUSH_SERVICE_LAST_NOTIFY_ID 	= "beseye_push_last_notify_id";
	static private final String PUSH_SERVICE_LAST_NOTIFY_TIME 	= "beseye_push_last_notify_time";
	
	private SharedPreferences mPref;
	private boolean mbRegisterGCM = false;
	private boolean mbRegisterReceiver = false;
    
    @Override
    public void onCreate() {
    	if(DEBUG)
    		Log.i(TAG, "###########################  BeseyeNotificationService::onCreate(), this:"+this);
    	
//    	if(null == iKalaSettingsMgr.getInstance())
//    		iKalaSettingsMgr.createInstance(getApplicationContext());
//    	
//    	//mSettingData = iKalaSettingsMgr.getInstance().getSettingData();
//    	
    	if(null == SessionMgr.getInstance()){
    		SessionMgr.createInstance(getApplicationContext());
    	}
    	
//    	mSessionData = SessionMgr.getInstance().getSessionData();
    		
    	mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
//        try {
//        	mMessenger.send(Message.obtain(null,MSG_CHECK_NOTIFY_NUM,0,0));
//			mMessenger.send(Message.obtain(null,MSG_CHECK_CACHE_STATE,0,0));
//		} catch (RemoteException e) {
//			e.printStackTrace();
//		}
        
        mPref = BeseyeSharedPreferenceUtil.getSharedPreferences(getApplicationContext(), PUSH_SERVICE_PREF);
        
        mLastNotifyId = BeseyeSharedPreferenceUtil.getPrefStringValue(mPref, PUSH_SERVICE_LAST_NOTIFY_ID);
        mLastNotifyUpdateTime = BeseyeSharedPreferenceUtil.getPrefLongValue(mPref, PUSH_SERVICE_LAST_NOTIFY_TIME, -1L);
        
        registerGCMServer();
        WebsocketsMgr.getInstance().registerOnWSChannelStateChangeListener(this);
        checkUserLoginState();
    }
    
    private void checkUserLoginState(){
    	Log.i(TAG, "checkUserLoginState(), ["+mbAppInBackground+", "+SessionMgr.getInstance().isTokenValid()+", "+WebsocketsMgr.getInstance().isNotifyWSChannelAlive()+", "+NetworkMgr.getInstance().isNetworkConnected()+"]");
    	if(false == mbAppInBackground && SessionMgr.getInstance().isTokenValid() && false == WebsocketsMgr.getInstance().isNotifyWSChannelAlive()){
    		if(NetworkMgr.getInstance().isNetworkConnected())
    			;//WebsocketsMgr.getInstance().constructNotifyWSChannel();
    	}else{
    		WebsocketsMgr.getInstance().destroyNotifyWSChannel();
    	}
    }
    
    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
    	// We want this service to
    	// continue running until it is explicitly
    	// stopped, so return sticky.

		return START_STICKY;
	}

	private void registerGCMServer(){
    	if(/*null != SessionMgr.getInstance() && SessionMgr.getInstance().isUseridValid() &&*/ false == mbRegisterGCM){
    		registerReceiver(mHandleMessageReceiver,new IntentFilter(GCMIntentService.FORWARD_GCM_MSG_ACTION));
    		mbRegisterReceiver = true;
            // Make sure the device has the proper dependencies.
            GCMRegistrar.checkDevice(getApplicationContext());
            // Make sure the manifest was properly set - comment out this line
            // while developing the app, then uncomment it when it's ready.
            GCMRegistrar.checkManifest(getApplicationContext());
            
            
    		if(null != mPref){
    			String sSenderID = GCMIntentService.SENDER_ID;//BeseyeSharedPreferenceUtil.getPrefStringValue(mPref, PUSH_SERVICE_SENDER_ID);
    			if(DEBUG)
    				Log.i(TAG, "onCreate(), sSenderID "+sSenderID);
    			
    			if(null == sSenderID || 0 == sSenderID.length()){
    				new BeseyePushServiceTask.GetProjectIDTask(this).execute();
    			}else{
    				registerGCM(sSenderID);
    			}
    		}
    	}
    }
    
    private void unregisterGCMServer(){
    	if(mbRegisterReceiver){
    		mbRegisterReceiver = false;
    		unregisterReceiver(mHandleMessageReceiver);
    	}
    	
    	if(mbRegisterGCM){
    		mbRegisterGCM = false;
    		GCMRegistrar.unregister(getApplicationContext());
    	}
    }

	@Override
	public void onDestroy() {
		Log.i(TAG, "###########################  BeseyeNotificationService::onDestroy(), this:"+this);
		if (mRegisterPushServerTask != null) {
			mRegisterPushServerTask.cancel(true);
        }
		
		WebsocketsMgr.getInstance().unregisterOnWSChannelStateChangeListener();
		unregisterGCMServer();
        GCMRegistrar.onDestroy(getApplicationContext());
		super.onDestroy();
	}
	
	@Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

	private BeseyeHttpTask mRegisterPushServerTask, mUnRegisterPushServerTask;
    
    private void registerGCM(String strSenderId){
    	GCMIntentService.updateSenderId(strSenderId);
    	final String regId = GCMRegistrar.getRegistrationId(getApplicationContext());
    	Log.i(TAG, "registerGCM(), regId: "+regId);
        if (regId.equals("")) {
        	// Log.i(TAG, "registerGCM(), strSenderId "+strSenderId);
            // Automatically registers application on startup.
            GCMRegistrar.register(getApplicationContext(), strSenderId);
        } else {
        	BeseyeSharedPreferenceUtil.setPrefStringValue(mPref, PUSH_SERVICE_REG_ID, regId);
        	registerPushServer();
        }
    }
    
    private void registerPushServer(){
    	final String regId = GCMRegistrar.getRegistrationId(getApplicationContext());
    	if(null != regId && 0 < regId.length() /*&& SessionMgr.getInstance().isUseridValid()*/){
    		//final String userId = SessionMgr.getInstance().getMdid();
    		//BeseyeSharedPreferenceUtil.setPrefStringValue(mPref, USER_ID, userId);
    		// Device is already registered on GCM, check server.
            if (!GCMRegistrar.isRegisteredOnServer(getApplicationContext())) {
                // Try to register again, but not in the UI thread.
                // It's also necessary to cancel the thread onDestroy(),
                // hence the use of AsyncTask instead of a raw thread.
            	JSONObject obj = new JSONObject();
//            	try {
//					obj.put(USER_ID, userId);
//					obj.put(PS_REG_ID, regId);
//				} catch (JSONException e) {
//					e.printStackTrace();
//				}
            	//mRegisterPushServerTask = (BeseyeHttpTask) new BeseyePushServiceTask.AddRegisterIDTask(this).execute(obj.toString());
            	//showRegIdNotification();
            	Log.e(TAG, "registerPushServer(), regId: "+regId);
            }else{
            	mbRegisterGCM = true;
            }
    	}else{
    		Log.e(TAG, "registerPushServer(), invalid regId or mdid "+SessionMgr.getInstance().getUserid());
    	}
    }
    
    private void unregisterPushServer(){
    	final String regId = BeseyeSharedPreferenceUtil.getPrefStringValue(mPref, PUSH_SERVICE_REG_ID);
    	final String userId = BeseyeSharedPreferenceUtil.getPrefStringValue(mPref, USER_ID);
    	if(null != regId && 0 < regId.length() && null != userId && 0 < userId.length()){
    		JSONObject obj = new JSONObject();
        	try {
				obj.put(USER_ID, userId);
				obj.put(PS_REG_ID, regId);
			} catch (JSONException e) {
				e.printStackTrace();
			}
        	
        	mUnRegisterPushServerTask = (BeseyeHttpTask) new BeseyePushServiceTask.DelRegisterIDTask(this).execute(obj.toString());
        	BeseyeSharedPreferenceUtil.setPrefStringValue(mPref, PUSH_SERVICE_REG_ID, "");
        	BeseyeSharedPreferenceUtil.setPrefStringValue(mPref, USER_ID, "");
    	}else{
    		Log.e(TAG, "unregisterPushServer(), invalid regId or userId "+userId);
    	}
    }
    
    private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getExtras().getString(GCMIntentService.FORWARD_ACTION_TYPE);
            Message msg = null;
            Log.i(TAG, "onReceive(), action "+action);
            
            if(GCMIntentService.FORWARD_ACTION_TYPE_REG.equals(action)){	
            	msg = Message.obtain(null,MSG_GSM_REGISTER,0,0);
            }else if(GCMIntentService.FORWARD_ACTION_TYPE_UNREG.equals(action)){
            	msg = Message.obtain(null,MSG_GSM_UNREGISTER,0,0);
            }else if(GCMIntentService.FORWARD_ACTION_TYPE_MSG.equals(action)){
            	msg = Message.obtain(null,MSG_GSM_MSG,0,0);
            }else if(GCMIntentService.FORWARD_ACTION_TYPE_ERR.equals(action)){
            	msg = Message.obtain(null,MSG_GSM_ERR,0,0);
            }else if(GCMIntentService.FORWARD_ACTION_TYPE_CHECK_DIALOG.equals(action)){
            	msg = Message.obtain(null,MSG_CHECK_DIALOG,0,0);
            }else{
            	Log.e(TAG, "onReceive(), invalid action "+action);
            }
            
            try {
            	if(null != mMessenger && null != msg){
            		msg.setData(intent.getExtras());
            		mMessenger.send(msg);
            	}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
        }
    };
	
	private void setLastNotifyItem(JSONObject notifyObj){
		if(null != notifyObj){
			//mLastNotifyId = BeseyeJSONUtil.getJSONString(notifyObj, NOTIFY_ID);
			BeseyeSharedPreferenceUtil.setPrefStringValue(mPref, PUSH_SERVICE_LAST_NOTIFY_ID, mLastNotifyId);
			
			mLastNotifyUpdateTime = BeseyeJSONUtil.getJSONLong(notifyObj, UPDATE_TIME);
			BeseyeSharedPreferenceUtil.setPrefLongValue(mPref, PUSH_SERVICE_LAST_NOTIFY_TIME, mLastNotifyUpdateTime);
		}
	}
	
	private void showFirstUnreadNotification(){
		if(null != mArrRet && 0 < mArrRet.length()){
			JSONObject obj = null;
//			int iNotifyTypes = iKalaSettingsMgr.getInstance().getPushNotifyTypes();
//			
//			//get first unread itm
//			if(null != mArrRet && 0 < iNotifyTypes){
//	    		int iSize = mArrRet.length();
//	    		for(int iIndex = 0 ; iIndex < iSize ; iIndex++){
//	    			try {
//	    				obj = (JSONObject) mArrRet.get(iIndex);
//	    				if(null != obj && BeseyeJSONUtil.getJSONBoolean(obj, UNREAD)){
//	    					JSONObject notifyObj = obj;//iKalaJSONUtil.getJSONObject(obj, NOTIFY_DATA);
//    						if(null != notifyObj){
//    							if(null != mLastNotifyId){
//    								if(mLastNotifyUpdateTime > BeseyeJSONUtil.getJSONLong(notifyObj, UPDATE_TIME))
//    									return;
//    								
//    								if(mLastNotifyId.equals(BeseyeJSONUtil.getJSONString(notifyObj, NOTIFY_ID)))
//    									continue;
//    							}
//    							String strNotifyType = BeseyeJSONUtil.getJSONString(notifyObj, NOTIFY_TYPE);
//    							if(null != strNotifyType){
//    								JSONObject fromObj = BeseyeJSONUtil.getJSONObject(notifyObj, FROM);
//    								if(null != fromObj){
//    									if(NOTIFY_T_FRIEND_INVITE.equals(strNotifyType) && 0 < (iNotifyTypes&iKalaSettingsMgr.PushNotifType.TYPE_INVITE_CHORUS.value())){
//    										showNotification(NOTIFICATION_TYPE_INFO, 
//    														 createDelegateIntent(getApplicationContext(),createIntent(getApplicationContext(), iChannelSocialListActivity.class.getName()).putExtra("QuerySocialType", QuerySocialType.FRIEND.toString())), 
//    														 getForegroundSpanText(this, getFriendInviteNotifyString(getApplicationContext(), fromObj),BeseyeJSONUtil.getJSONString(fromObj, USER_NAME)), 
//    														 notifyObj);
//    										return;
//    									}
//    									
//    									
//    									JSONObject workObj = BeseyeJSONUtil.getJSONObject(notifyObj, WORK_DATA);
//    									String sWorktrType = null;
//    									if(null != workObj){
//    										sWorktrType = BeseyeJSONUtil.getJSONString(workObj, WORK_TYPE);
//    										if(NOTIFY_T_CREATE.equals(strNotifyType) && 0 < (iNotifyTypes&iKalaSettingsMgr.PushNotifType.TYPE_NEW_WORK_CMNT.value())){
//    											showNotification(NOTIFICATION_TYPE_INFO, 
//    															 createDelegateIntent(getApplicationContext(),createIntentToPlayer(getApplicationContext(), fromObj, workObj)), 
//    															 getForegroundSpanText(this, getCreateNotifyString(getApplicationContext(), workObj, fromObj, sWorktrType),BeseyeJSONUtil.getJSONString(fromObj, USER_NAME), BeseyeJSONUtil.getJSONString(workObj, WORK_NAME)), 
//    															 notifyObj);
//    											return;
//    										}
//    										else if(NOTIFY_T_COMMENT.equals(strNotifyType) && 0 < (iNotifyTypes&iKalaSettingsMgr.PushNotifType.TYPE_NEW_WORK_CMNT.value())){
//    											showNotification(NOTIFICATION_TYPE_MSG, 
//    															 createDelegateIntent(getApplicationContext(),createIntentToStatusMsgPage(getApplicationContext(), fromObj, workObj)), 
//    															 getForegroundSpanText(this, getCommentNotifyString(getApplicationContext(), fromObj, workObj, sWorktrType), BeseyeJSONUtil.getJSONString(fromObj, USER_NAME),BeseyeJSONUtil.getJSONString(workObj, WORK_NAME)), 
//    															 notifyObj);
//    											return;
//    										}
//    										else if(NOTIFY_T_COMMENT_REPLY.equals(strNotifyType) && 0 < (iNotifyTypes&iKalaSettingsMgr.PushNotifType.TYPE_NEW_WORK_CMNT.value())){
//    											showNotification(NOTIFICATION_TYPE_MSG, 
//    															 createDelegateIntent(getApplicationContext(),createIntentToStatusMsgPage(getApplicationContext(), fromObj, workObj)), 
//    															 getForegroundSpanText(this, getMsgReplyNotifyString(getApplicationContext(), fromObj), BeseyeJSONUtil.getJSONString(fromObj, USER_NAME)), 
//    															 notifyObj);
//    											return;
//    										}
//    										else if(NOTIFY_T_CHO_INVITE.equals(strNotifyType) && 0 < (iNotifyTypes&iKalaSettingsMgr.PushNotifType.TYPE_INVITE_CHORUS.value())){
//    											showNotification(NOTIFICATION_TYPE_INFO, null, 
//    															 iKalaNotificationUtil.getForegroundSpanText(this, getChrorusInviteNotifyString(getApplicationContext(), fromObj, workObj),  BeseyeJSONUtil.getJSONString(fromObj, USER_NAME),BeseyeJSONUtil.getJSONString(workObj, WORK_NAME)), 
//    															 notifyObj);
//    											return;
//    										}
//    										else if(NOTIFY_T_CHO_COMMIT.equals(strNotifyType) && 0 < (iNotifyTypes&iKalaSettingsMgr.PushNotifType.TYPE_INVITE_CHORUS.value())){
//    											showNotification(NOTIFICATION_TYPE_INFO, null, 
//    															 iKalaNotificationUtil.getForegroundSpanText(this, getChrorusCommitNotifyString(getApplicationContext(), fromObj, workObj),  BeseyeJSONUtil.getJSONString(fromObj, USER_NAME),BeseyeJSONUtil.getJSONString(workObj, WORK_NAME)), 
//    															 notifyObj);
//    											return;
//    										}
//    										else if(NOTIFY_T_MSG.equals(strNotifyType) && 0 < (iNotifyTypes&iKalaSettingsMgr.PushNotifType.TYPE_NEW_MSG.value())){
//        										showNotification(NOTIFICATION_TYPE_MSG, 
//        														 createDelegateIntent(getApplicationContext(),createIntentToConversationPage(getApplicationContext(), fromObj, workObj)),
//        														 getForegroundSpanText(this, getSendMsgNotifyString(getApplicationContext(), fromObj), BeseyeJSONUtil.getJSONString(fromObj, USER_NAME)), 
//        														 notifyObj);      									
//        										return;
//        									}
//    										else 
//    											Log.e(TAG, "setupItem(), invalid type "+strNotifyType);
//    									}
//    								}
//    							}
//    						}
//	    				}
//	    			} catch (JSONException e) {
//	    				e.printStackTrace();
//	    			}
//	    		}
//	    	}
		}
	}
	
	private void showNotification(int iNotifyId, Intent intent, CharSequence text, JSONObject notifyObj) {
		setLastNotifyItem(notifyObj);
//		
//		if(null == intent){
//			intent = new Intent(this, iKalaDelegateActivity.class).putExtra(iKalaDelegateActivity.ACTION_BRING_FRONT, true);
//		}
//		
//		int iNotifyMethods = iKalaSettingsMgr.getInstance().getPushNotifyMethods();
//		if(0 < (iNotifyMethods & (iKalaSettingsMgr.PushNotifMethod.METHOD_LIGHT.value()|iKalaSettingsMgr.PushNotifMethod.METHOD_VIBRATE.value()|iKalaSettingsMgr.PushNotifMethod.METHOD_SOUND.value()))){
//			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
//			if(null != contentIntent){
//				 final Notification notification = new Notification(
//					        				R.drawable.common_icon,       // the icon for the status bar
//					        				text,                        // the text to display in the ticker
//					        				/*System.currentTimeMillis()*/mLastNotifyUpdateTime); // the timestamp for the notification
//
//				 if(null != notification){
//					notification.setLatestEventInfo(
//							 this,                        // the context to use
//							 getText(R.string.app_name_live),
//							                              // the title for the notification
//							 text,                        // the details to display in the notification
//							 contentIntent);              // the contentIntent (see above)
//
//					notification.defaults = iNotifyMethods;//Notification.DEFAULT_LIGHTS;
//					notification.flags = Notification.FLAG_AUTO_CANCEL;
//					
//					mNotificationManager.notify(
//					iNotifyId, // we use a string id because it is a unique
//					// number.  we use it later to cancel the notification
//					notification);
//				 }
//			}
//		}
//		final KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
//		if((km.inKeyguardRestrictedInputMode() || /*mbAppInBackground*/false == BeseyeUtil.isiKalaForegroundProcess(this)) && 0 < (iNotifyMethods & iKalaSettingsMgr.PushNotifMethod.METHOD_POPUP.value())){
//			showPushDialog(intent, text);
//		}
    }
	
	private void showRegIdNotification(){
		Intent intent = new Intent(Intent.ACTION_SENDTO); // it's not ACTION_SEND
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_SUBJECT, "Beseye User RegId");
		intent.putExtra(Intent.EXTRA_TEXT, GCMRegistrar.getRegistrationId(getApplicationContext()));
		intent.setData(Uri.parse("mailto:")); // or just "mailto:" for blank
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this will make such that when user returns to your app, your app is displayed, instead of the email app.
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
		/*if(null != contentIntent)*/{
			 final Notification notification = new Notification(
				        				R.drawable.common_app_icon_shadow,       // the icon for the status bar
				        				"RegId got",                        // the text to display in the ticker
				        				/*System.currentTimeMillis()*/mLastNotifyUpdateTime); // the timestamp for the notification

			 if(null != notification){
				notification.setLatestEventInfo(
						 this,                        // the context to use
						 "Beseye User RegId",
						                              // the title for the notification
						 GCMRegistrar.getRegistrationId(getApplicationContext()),                        // the details to display in the notification
						 contentIntent);              // the contentIntent (see above)

				notification.defaults = Notification.DEFAULT_LIGHTS;
				notification.flags = Notification.FLAG_AUTO_CANCEL;
				
				mNotificationManager.notify(
				999, // we use a string id because it is a unique
				// number.  we use it later to cancel the notification
				notification);
			 }
		}
		testMsgGot();
	}
	
	private void testMsgGot(){
		new IncomingHandler().postDelayed(new Runnable(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Intent intent = new Intent(GCMIntentService.FORWARD_GCM_MSG_ACTION);
		        intent.putExtra(GCMIntentService.FORWARD_ACTION_TYPE, GCMIntentService.FORWARD_ACTION_TYPE_MSG);
		        intent.putExtra("info", "This is a beseye test message.");
		        sendBroadcast(intent);
			}}, 1000);
		
	}
	
	private KeyguardManager.KeyguardLock mKeyLock;
	private Dialog mPushDialog;
	private ViewGroup mVgPushDialogHolder;
	private TextView mtxtContent, mtxtUpdateTime;
	private Button mbtnView;
	
	public void showPushDialog(final Intent intent, CharSequence text){
//		checkAndCloseDialog();
//		
//		final KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);			
//		
//		final boolean bNeedToEnableKG = km.inKeyguardRestrictedInputMode();
//		Log.i(TAG, "----------------------showPushDialog(), bNeedToEnableKG = "+bNeedToEnableKG);
//		
//		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
//		PowerManager.WakeLock wl=pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, getPackageName());
//		wl.acquire();
//		if(null == mPushDialog){
//			mPushDialog = new Dialog(this);
//			//mPushDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
//			mPushDialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.transparent)));
//			mPushDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED /*|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON*/);
//			mPushDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
//			mPushDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
//		}
//		
//		if(null != mPushDialog){
//			if(null == mVgPushDialogHolder){
//				LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//				if(null != inflater){
//					mVgPushDialogHolder = (ViewGroup)inflater.inflate(R.layout.ikala_push_dialog_layout, null);
//					if(null != mVgPushDialogHolder){
//						mtxtContent = (TextView)mVgPushDialogHolder.findViewById(R.id.txtMsg);
//						mtxtUpdateTime = (TextView)mVgPushDialogHolder.findViewById(R.id.txtUpdateTime);
//						mbtnView = (Button)mVgPushDialogHolder.findViewById(R.id.btn_view);
//						Button btnClose = (Button)mVgPushDialogHolder.findViewById(R.id.btn_close);
//						if(null != btnClose){
//							btnClose.setOnClickListener(new OnClickListener(){
//								@Override
//								public void onClick(View arg0) {
//									if(null != mPushDialog){
//										mPushDialog.dismiss();
//										checkKeyGuard();
//									}
//								}});
//						}
//					}
//				}
//				mPushDialog.setContentView(mVgPushDialogHolder);
//			}
//			
//			if(null != mVgPushDialogHolder){
//				if(null != mtxtContent){
//					mtxtContent.setText(text);
//				}
//				
//				if(null != mtxtUpdateTime){
//					mtxtUpdateTime.setText(BeseyeUtil.getDateDiffString(this, new Date(mLastNotifyUpdateTime)));
//				}
//				
//				if(null != mbtnView){
//					mbtnView.setOnClickListener(new OnClickListener(){
//						@Override
//						public void onClick(View arg0) {
//							if(null != mPushDialog){
//								mPushDialog.dismiss();
//								checkKeyGuard();
//							}
//							cancelNotification();
//							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//							startActivity(intent);
//						}});
//				}
//			}
//			
//			if(bNeedToEnableKG){
//				if(null == mKeyLock){
//					mKeyLock = km.newKeyguardLock(getPackageName());
//					if(null != mKeyLock)
//						mKeyLock.disableKeyguard();
//					Log.i(TAG, "----------------------showPushDialog(), disableKeyguard");
//				}
//			}else{
//				if(null != mKeyLock){
//					mKeyLock = null;
//					Log.e(TAG, "----------------------showPushDialog(), shouldn't happen");
//				}
//			}
//			
//			mPushDialog.setCancelable(false);			
//			mPushDialog.show();
//		}
//		wl.release();
	}
	
	private void checkKeyGuard(){
		if(null != mKeyLock){
			mKeyLock.reenableKeyguard();
			Log.i(TAG, "----------------------checkKeyGuard(), reenableKeyguard");
		}
		mKeyLock = null;
	}
	
	private void checkAndCloseDialog(){
		if(null != mPushDialog && mPushDialog.isShowing()){
			mPushDialog.dismiss();
			//checkKeyGuard();
			//mPushDialog = null;
		}
	}
	
	private void cancelNotification(){
		mNotificationManager.cancel(NOTIFICATION_TYPE_INFO);
	}
	
	private BeseyeHttpTask mNotificationInfoTask, mNotificationListTask, mMsgInfoTask;
	private JSONObject mNotifyInfo, mJSONObjectRet, mMsgInfo;
	private JSONArray mArrRet;
	private boolean mbAppInBackground = true, mbNeedToPullMsg = false;
	
	private boolean shouldPullMsg(){
		return !mbAppInBackground && mbNeedToPullMsg;
	}

	@Override
	public void onShowDialog(AsyncTask task, int iDialogId, int iTitleRes, int iMsgRes) {}

	@Override
	public void onDismissDialog(AsyncTask task, int iDialogId) {}

	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle, String strMsg) {
		if(task instanceof BeseyePushServiceTask.GetProjectIDTask){
			Log.e(TAG, "onPostExecute(), GetProjectIDTask, iErrType = "+iErrType);
			GCMRegistrar.unregister(getApplicationContext());
		}else if(task instanceof BeseyePushServiceTask.AddRegisterIDTask){
			Log.e(TAG, "onPostExecute(), AddRegisterIDTask, iErrType = "+iErrType);
		}else if(task instanceof BeseyePushServiceTask.DelRegisterIDTask){
			Log.e(TAG, "onPostExecute(), DelRegisterIDTask, iErrType = "+iErrType);
		}
		Log.e(TAG, "Service::onErrorReport(), "+task.getClass().getSimpleName()+", params:"+strMsg+", iErrType: "+iErrType);
	}
	
	@Override
	public void onSessionInvalid(AsyncTask task, int iInvalidReason){
		
	}

	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		if(false == task.isCancelled()){
			if(task instanceof BeseyePushServiceTask.GetProjectIDTask){
				if(0 == iRetCode && null != result && 0 < result.size()){
					String senderId = BeseyeJSONUtil.getJSONString(result.get(0), PS_PORJ_ID);
					Log.i(TAG, "onPostExecute(), senderId "+senderId);
					if(0 < senderId.length()){
						BeseyeSharedPreferenceUtil.setPrefStringValue(mPref, PUSH_SERVICE_SENDER_ID, senderId);
						registerGCM(senderId);
					}else{
						Log.e(TAG, "onPostExecute(), GetProjectIDTask, invalid senderId ");
					}
				}
			}else if(task instanceof BeseyePushServiceTask.AddRegisterIDTask){
				if(0 == iRetCode || 2 == iRetCode && null != result && 0 < result.size()){
					Log.i(TAG, "onPostExecute(), AddRegisterIDTask OK");
				}
			}else if(task instanceof BeseyePushServiceTask.DelRegisterIDTask){
				if(0 == iRetCode && null != result && 0 < result.size()){
					Log.i(TAG, "onPostExecute(), DelRegisterIDTask OK");
				}
			}
		}
	}

	@Override
	public void onToastShow(AsyncTask task, String strMsg) {
		// TODO Auto-generated method stub
		
	}

	static final int MAX_WS_RETRY_TIME = 20;
	private int miWSDisconnectRetry = 0;
	
	@Override
	public void onChannelConnecting() {
		Log.i(TAG, "onChannelConnecting()---");
	}
	
	@Override
	public void onAuthfailed(){
		Log.w(TAG, "onAuthfailed()---");
	}

	@Override
	public void onChannelConnected() {
		Log.i(TAG, "onChannelConnected()---");
		miWSDisconnectRetry = 0;
	}

	@Override
	public void onMessageReceived(String msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onChannelClosed() {
		Log.i(TAG, "onChannelCloased()---");
		if(miWSDisconnectRetry < MAX_WS_RETRY_TIME && false == mbAppInBackground && SessionMgr.getInstance().isTokenValid() && NetworkMgr.getInstance().isNetworkConnected()){
			Log.i(TAG, "onChannelCloased(), abnormal close, retry-----");
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					WebsocketsMgr.getInstance().constructNotifyWSChannel();
				}}, (miWSDisconnectRetry++)*1000);
    		
    	}
	}

	@Override
	public void onConnectivityChanged(boolean bNetworkConnected) {
		checkUserLoginState();
	}
}
