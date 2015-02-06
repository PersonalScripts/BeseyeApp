package com.app.beseye.httptask;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeSharedPreferenceUtil.getPrefIntValue;
import static com.app.beseye.util.BeseyeSharedPreferenceUtil.getPrefLongValue;
import static com.app.beseye.util.BeseyeSharedPreferenceUtil.getPrefStringValue;
import static com.app.beseye.util.BeseyeSharedPreferenceUtil.getSecuredSharedPreferences;
import static com.app.beseye.util.BeseyeSharedPreferenceUtil.getSharedPreferences;
import static com.app.beseye.util.BeseyeSharedPreferenceUtil.setPrefIntValue;
import static com.app.beseye.util.BeseyeSharedPreferenceUtil.setPrefLongValue;
import static com.app.beseye.util.BeseyeSharedPreferenceUtil.setPrefStringValue;

import java.lang.ref.WeakReference;
import java.net.URLEncoder;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.app.beseye.BeseyeNewsActivity.BeseyeNewsHistoryMgr;

//SessionMgr is responsible for storing back-end URL, token, user Userid in storage/memory
public class SessionMgr {
	static public enum SERVER_MODE{
		MODE_DEV,
		MODE_DEV2,
		MODE_STAGING,
		MODE_STAGING_TOKYO,
		MODE_PRODUCTION,
		MODE_TYPES_CNT;
		
		static public SERVER_MODE translateToMode(int iMode){
			SERVER_MODE mode = MODE_DEV;
			switch(iMode){
				case 1:{
					mode = MODE_DEV2;
					break;
				} 
				case 2:{
					mode = MODE_STAGING;
					break;
				} 
				case 3:{
					mode = MODE_STAGING_TOKYO;
					break;
				} 
				case 4:{
					mode = MODE_PRODUCTION;
					break;
				} 
			}
			return mode;
		}
	}
	
	static public final SERVER_MODE DEFAULT_SERVER_MODE = SERVER_MODE.MODE_STAGING;
	
	static private final String ACCOUNT_URL_FORMAT = "%s/be_acc/v1/";
	
	static private final String[] ACC_VPC_BE_URL = {"https://tokyo-p1-dev-api-1.beseye.com/acc",
													"https://acc-dev.beseye.com", 
													"https://oregon-p1-stage-api-1.beseye.com/acc",
													"https://tokyo-acc-stage.beseye.com",
													"https://acc-stage.beseye.com"}; 
	
	static private final String[] ACCOUNT_BE_URL = {"https://tokyo-p1-dev-api-1.beseye.com/acc",
													"https://acc-dev.beseye.com", 
													"https://oregon-p1-stage-api-%d.beseye.com/acc",
													"https://tokyo-acc-stage.beseye.com",
													"https://acc-stage.beseye.com"}; 
	
	static private final String[] MM_BE_URL = { "https://tokyo-p1-dev-api-1.beseye.com/mm/",
												"https://mm-dev.beseye.com/",
												"https://oregon-p1-stage-api-%d.beseye.com/mm/",
												"https://tokyo-mm-stage.beseye.com/",
												"https://mm-stage.beseye.com/"}; 

	static private final String[] CAM_BE_URL = { "https://tokyo-p1-dev-api-1.beseye.com/cam/",
												 "https://ns-dev.beseye.com/",
												 "https://oregon-p1-stage-api-%d.beseye.com/cam/",
												 "https://tokyo-ns-stage.beseye.com/",
												 "https://ns-stage.beseye.com/"}; 
	
	static private final String[] NS_BE_URL = { "https://tokyo-p1-dev-api-1.beseye.com/ns/",
												"https://ns-dev.beseye.com/",
												"https://oregon-p1-stage-api-%d.beseye.com/ns/",
												"https://tokyo-ns-stage.beseye.com/",
												"https://ns-stage.beseye.com/"}; 
	
	static private final String[] CAM_WS_BE_URL = { "https://tokyo-p1-dev-api-1.beseye.com/ws/",
													"https://ns-dev.beseye.com/",
													"https://oregon-p1-stage-api-%d.beseye.com/ws/",
													"https://tokyo-ns-stage.beseye.com/",
													"https://ns-stage.beseye.com/"}; 
	
	static private final String[] WS_BE_URL = { "https://tokyo-p1-dev-ws-1.beseye.com/",
												"https://ws-dev.beseye.com/", 
												"https://oregon-p1-stage-ws-%d.beseye.com/",
												"https://tokyo-ws-stage.beseye.com/",
												"https://ws-stage.beseye.com/"}; 
	
	static private final String[] WSA_BE_URL = { "https://tokyo-p1-dev-wsa-1.beseye.com/",
												 "https://wsa-dev.beseye.com/", 
												 "https://oregon-p1-stage-wsa-%d.beseye.com/",
												 "https://tokyo-wsa-stage.beseye.com/",
												 "https://wsa-stage.beseye.com/"}; 
	
	static private final String[] NEWS_BE_URL = { "https://tokyo-p1-dev-api-1.beseye.com/news/",
												  "https://news-dev.beseye.com/", 
												  "https://oregon-p1-stage-api-%d.beseye.com/news/",
												  "https://tokyo-news-stage.beseye.com/",
												  "https://news-stage.beseye.com/"}; 
	
	
	
	static private final String SESSION_PREF 				= "beseye_ses";
	static private final String SESSION_TOKEN 				= "beseye_token";
	static private final String SESSION_DOMAIN 				= "beseye_domain";
	static private final String SESSION_USERID				= "beseye_userid";
	static private final String SESSION_ACCOUNT				= "beseye_account";
	static private final String SESSION_ACC_CERTIFICATED	= "beseye_certificated";
	static private final String SESSION_OWNER_INFO			= "beseye_owner_data";
	static private final String SESSION_OWNER_VPC_NUM		= "beseye_owner_vpc_no";
	
	static private final String SESSION_UPDATE_TS			= "beseye_cam_update_ts";
	static private final String SESSION_UPDATE_CAMS			= "beseye_cam_update_list";
	
	static private final String SESSION_SERVER_MODE	    	= "beseye_server_mode";
	
	static private final String SESSION_PAIR_TOKEN	    	= "beseye_pair_token";
	
	static private final String SESSION_NEWS_HISTORY	    = "beseye_news_history";
	static private final String SESSION_NEWS_LAST_MAX	    = "beseye_news_last_max";
	static private final String SESSION_NEWS_IND_SHOW	    = "beseye_news_show_ind";
	
	static private SessionMgr sSessionMgr;
	
	
	static public void createInstance(Context context){
		if(null == sSessionMgr)
			sSessionMgr = new SessionMgr(context);
	}
	
	static public SessionMgr getInstance(){
		return sSessionMgr;
	}
	
	private SharedPreferences mPref, mSecuredPref;
	//private String mStrHostUrl, mStrMMHostUrl;
	private SessionData mSessionData;
	private boolean mbShowPirvateCam  = false;
	//private ChannelInfo mOwnerChannelInfo;
	
	private SessionMgr(Context context){
		if(null != context){
			mPref = getSharedPreferences(context, SESSION_PREF);
			mSecuredPref = getSecuredSharedPreferences(context, SESSION_PREF);
			mSessionData = new SessionData();
			if(null != mSessionData){
				mSessionData.setUserid(getPrefStringValue(mPref, SESSION_USERID));
				mSessionData.setAccount(getPrefStringValue(mPref, SESSION_ACCOUNT));
				mSessionData.setDomain(getPrefStringValue(mPref, SESSION_DOMAIN));
				mSessionData.setAuthToken(getPrefStringValue(mPref, SESSION_TOKEN));
				mSessionData.setIsCertificated(0 <getPrefIntValue(mPref, SESSION_ACC_CERTIFICATED));
				mSessionData.setServerMode(SERVER_MODE.translateToMode(getPrefIntValue(mPref, SESSION_SERVER_MODE, DEFAULT_SERVER_MODE.ordinal())));
				mSessionData.setOwnerInfo(getPrefStringValue(mPref, SESSION_OWNER_INFO));
				mSessionData.setPairToken(getPrefStringValue(mPref, SESSION_PAIR_TOKEN));
				mSessionData.setCamUpdateTimestamp(getPrefLongValue(mPref, SESSION_UPDATE_TS));
				mSessionData.setCamUpdateList(getPrefStringValue(mPref, SESSION_UPDATE_CAMS));
				mSessionData.setVPCNumber(getPrefIntValue(mPref, SESSION_OWNER_VPC_NUM, 1));
			}
		}
	}
	
	public void cleanSession(){
		Log.e(TAG, "cleanSession()");
		Thread.dumpStack();
		//clearSharedPreferences(mPref);
		//clearSharedPreferences(mSecuredPref);
		setAuthToken("");
		setUserid("");
		setAccount("");
		setOwnerInfo("");
		setIsCertificated(false);
		setVPCNumber(1);
		setNewsHistory("");
		BeseyeNewsHistoryMgr.deinit();
		//setOwnerChannelInfo(null);
		mbShowPirvateCam = false;
		notifySessionUpdate();
	}
	
	public void cleanHostUrls(){
		setAccountBEHostUrl("");
		setVPCAccountBEHostUrl("");
		setMMBEHostUrl("");
		setCamBEHostUrl("");
		setNSBEHostUrl("");
		setCamWSBEHostUrl("");
		setWSHostUrl("");
		setWSAHostUrl("");
		setNewsHostUrl("");
	}
	
	public void setShowPirvateCam(boolean bFlag){
		mbShowPirvateCam = bFlag;
	}
	
	public boolean getShowPirvateCam(){
		return mbShowPirvateCam;
	}
	
	public String getNewsHistory(){
		return getPrefStringValue(mPref, SESSION_NEWS_HISTORY);
	}
	
	public void setNewsHistory(String strHistory){
		setPrefStringValue(mPref, SESSION_NEWS_HISTORY, strHistory);
	}
	
	public int getNewsLastMax(){
		return getPrefIntValue(mPref, SESSION_NEWS_LAST_MAX, -1);
	}
	
	public void setNewsLastMax(int iValue){
		setPrefIntValue(mPref, SESSION_NEWS_LAST_MAX, iValue);
	}
	
	public int getNewsShowInd(){
		return getPrefIntValue(mPref, SESSION_NEWS_IND_SHOW, -1);
	}
	
	public void setNewsShowInd(int iValue){
		setPrefIntValue(mPref, SESSION_NEWS_IND_SHOW, iValue);
	}
	
	public void setBEHostUrl(SERVER_MODE mode){
		setAccountBEHostUrl(mode);
		setVPCAccountBEHostUrl(mode);
		setMMBEHostUrl(mode);
		setCamBEHostUrl(mode);
		setNSBEHostUrl(mode);
		setCamWSBEHostUrl(mode);
		setWSBEHostUrl(mode);
		setWSABEHostUrl(mode);
		setNewsBEHostUrl(mode);
	}
	
	public int getVPCNumber(){
		return mSessionData.getVPCNumber();
	}
	
	public void setVPCNumber(int iVPCno){
		Log.e(TAG, "setVPCNumber(), iVPCno:"+iVPCno);

		mSessionData.setVPCNumber(iVPCno);
		setPrefIntValue(mPref, SESSION_OWNER_VPC_NUM, mSessionData.getVPCNumber());
		setBEHostUrl(getServerMode());
		notifySessionUpdate();
	}
	
	public String getAccountBEHostUrl(){
		return mSessionData.getAccountBEHostUrl();
	}
	
	public void setAccountBEHostUrl(SERVER_MODE mode){
		setAccountBEHostUrl(String.format(ACCOUNT_URL_FORMAT, String.format(ACCOUNT_BE_URL[mode.ordinal()], getVPCNumber())));
	}
	
	synchronized public void setAccountBEHostUrl(String strURL){
		mSessionData.setAccountBEHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getVPCAccountBEHostUrl(){
		return mSessionData.getVPCAccountBEHostUrl();
	}
	
	public void setVPCAccountBEHostUrl(SERVER_MODE mode){
		setVPCAccountBEHostUrl(String.format(ACCOUNT_URL_FORMAT, ACC_VPC_BE_URL[mode.ordinal()]));
	}
	
	synchronized public void setVPCAccountBEHostUrl(String strURL){
		mSessionData.setVPCAccountBEHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getMMBEHostUrl(){
		return mSessionData.getMMBEHostUrl();
	}
	
	public void setMMBEHostUrl(SERVER_MODE mode){
		setMMBEHostUrl(String.format(MM_BE_URL[mode.ordinal()], getVPCNumber()));
	}
	
	synchronized public void setMMBEHostUrl(String strURL){
		mSessionData.setMMBEHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getCamBEHostUrl(){
		return mSessionData.getCAMBEHostUrl();
	}
	
	public void setCamBEHostUrl(SERVER_MODE mode){
		setCamBEHostUrl(String.format(CAM_BE_URL[mode.ordinal()], getVPCNumber()));
	}
	
	synchronized public void setCamBEHostUrl(String strURL){
		mSessionData.setCAMBEHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getNSBEHostUrl(){
		return mSessionData.getNSBEHostUrl();
	}
	
	public void setNSBEHostUrl(SERVER_MODE mode){
		setNSBEHostUrl(String.format(NS_BE_URL[mode.ordinal()], getVPCNumber()));
	}
	
	synchronized public void setNSBEHostUrl(String strURL){
		mSessionData.setNSBEHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getCamWSBEHostUrl(){
		return mSessionData.getCamWSHostUrl();
	}
	
	public void setCamWSBEHostUrl(SERVER_MODE mode){
		setCamWSBEHostUrl(String.format(CAM_WS_BE_URL[mode.ordinal()], getVPCNumber()));
	}
	
	synchronized public void setCamWSBEHostUrl(String strURL){
		mSessionData.setCamWSHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getWSHostUrl(){
		return mSessionData.getWSHostUrl();
	}
	
	synchronized public void setWSHostUrl(String strURL){
		mSessionData.setWSHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public void setWSBEHostUrl(SERVER_MODE mode){
		setWSHostUrl(String.format(WS_BE_URL[mode.ordinal()], getVPCNumber()));
	}
	
	public String getWSAHostUrl(){
		return mSessionData.getWSAHostUrl();
	}
	
	synchronized public void setWSAHostUrl(String strURL){
		mSessionData.setWSAHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public void setWSABEHostUrl(SERVER_MODE mode){
		setWSAHostUrl(String.format(WSA_BE_URL[mode.ordinal()], getVPCNumber()));
	}
	
	public String getNewsHostUrl(){
		return mSessionData.getNewsHostUrl();
	}
	
	synchronized public void setNewsHostUrl(String strURL){
		mSessionData.setNewsHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public void setNewsBEHostUrl(SERVER_MODE mode){
		setNewsHostUrl(String.format(NEWS_BE_URL[mode.ordinal()], getVPCNumber()));
	}
	
	public String getUserid(){
		return mSessionData.getUserid();
	}
	
	synchronized public void setUserid(String strId){
		setPrefStringValue(mPref, SESSION_USERID, strId);
		mSessionData.setUserid(strId);
		notifySessionUpdate();
	}
	
	public String getAccount(){
		return mSessionData.getAccount();
	}
	
	synchronized public void setAccount(String strAccount){
		setPrefStringValue(mPref, SESSION_ACCOUNT, strAccount);
		mSessionData.setAccount(strAccount);
		notifySessionUpdate();
	}
	
	public boolean isUseridValid(){
		String id = getUserid();
		return (null != id) && (0 < id.length());
	}
	
	public String getDomain(){
		return mSessionData.getDomain();
	}
	
	synchronized public void setDomain(String strDomain){
		setPrefStringValue(mPref, SESSION_DOMAIN, strDomain);
		mSessionData.setDomain(strDomain);
		notifySessionUpdate();
	}
	
	public boolean isTokenValid(){
		String token = getAuthToken();
		return (null != token) && (0 < token.length());
	}
	
	public String getAuthToken(){
		return mSessionData.getAuthToken();
	}
	
	synchronized public void setAuthToken(String strToken){
		if(DEBUG)
			Log.e(TAG, "setAuthToken(), old session = "+mSessionData.getAuthToken()+", strToken = "+strToken);
		
		setPrefStringValue(mPref, SESSION_TOKEN, strToken);
		mSessionData.setAuthToken(strToken);
		notifySessionUpdate();
	}
	
	public String getPairToken(){
		return mSessionData.getPairToken();
	}
	
	synchronized public void setPairToken(String strToken){
		setPrefStringValue(mPref, SESSION_PAIR_TOKEN, strToken);
		mSessionData.setPairToken(strToken);
		notifySessionUpdate();
	}
	
	public boolean getIsCertificated(){
		return mSessionData.getIsCertificated();
	}
	
	public void setIsCertificated(boolean bIsCertificated){
		setPrefIntValue(mPref, SESSION_ACC_CERTIFICATED, bIsCertificated?1:0);
		mSessionData.setIsCertificated(bIsCertificated);
		notifySessionUpdate();
	}
	
	public SERVER_MODE getServerMode(){
		return mSessionData.getServerMode();
	}
	
	public void setServerMode(SERVER_MODE serverMode){
		setPrefIntValue(mPref, SESSION_SERVER_MODE, serverMode.ordinal());
		mSessionData.setServerMode(serverMode);
		notifySessionUpdate();
		//BeseyeHttpTask.checkHostAddr();
	}
	
	public String getOwnerInfo(){
		return mSessionData.getOwnerInfo();
	}
	
	synchronized public void setOwnerInfo(String strOwnerInfo){
		setPrefStringValue(mPref, SESSION_OWNER_INFO, strOwnerInfo);
		mSessionData.setOwnerInfo(strOwnerInfo);
		notifySessionUpdate();
	}
	
	public long getCamUpdateTimestamp(){
		return mSessionData.getCamUpdateTimestamp();
	}
	
	synchronized public void setCamUpdateTimestamp(long lCamUpdateTs){
		setPrefLongValue(mPref, SESSION_UPDATE_TS, lCamUpdateTs);
		mSessionData.setCamUpdateTimestamp(lCamUpdateTs);
		notifySessionUpdate();
	}
	
	public String getCamUpdateList(){
		return mSessionData.getCamUpdateList();
	}
	
	synchronized public void setCamUpdateList(String strCamUpdateList){
		setPrefStringValue(mPref, SESSION_UPDATE_CAMS, strCamUpdateList);
		mSessionData.setCamUpdateList(strCamUpdateList);
		notifySessionUpdate();
	}
	
	public SessionData getSessionData(){
		return mSessionData;
	}
	
	public void setSessionData(SessionData data){
		mSessionData = data;
	}
	
	private WeakReference<ISessionUpdateCallback> mSessionUpdateCallback;
	public void registerSessionUpdateCallback(ISessionUpdateCallback cb){
		mSessionUpdateCallback = new WeakReference<ISessionUpdateCallback>(cb);
	}
	
	static public interface ISessionUpdateCallback{
		public void onSessionUpdate(SessionData data);
	}
	
	private void notifySessionUpdate(){
		if(null != mSessionUpdateCallback){
			ISessionUpdateCallback cb  = mSessionUpdateCallback.get();
			if(null != cb)
				cb.onSessionUpdate(mSessionData);
		}
	}
	
	public static class SessionData implements Parcelable{
		private String mStrHostUrl, mStrVPCHostUrl, mStrMMHostUrl, mStrCamHostUrl, mStrNSHostUrl, mStrCamWsHostUrl, mStrWSHostUrl, mStrWSAHostUrl, mStrNewsHostUrl, mStrUserid, mStrAccount, mStrDomain, mStrToken, mStrOwnerInfo, mStrPairToken;
		private boolean mbIsCertificated;
		private SERVER_MODE mServerMode;
		private long mlCamUpdateTs;
		private String mStrCamUpdateList;
		private int miVPCno;
		
		public SessionData() {
			mStrHostUrl = "";
			mStrVPCHostUrl = "";
			mStrMMHostUrl = "";
			mStrCamHostUrl = "";
			mStrNSHostUrl = "";
			mStrCamWsHostUrl = "";
			mStrWSHostUrl = "";
			mStrWSAHostUrl = "";
			mStrNewsHostUrl = "";
			mStrUserid = "";
			mStrAccount = "";
			mStrDomain = "";
			mStrToken = "";
			mStrOwnerInfo = "";
			mStrPairToken = "";
			mbIsCertificated = false;
			mServerMode = DEFAULT_SERVER_MODE;
			
			mlCamUpdateTs = 0;
			mStrCamUpdateList = "";
			
			miVPCno = 1;
		}
		
		public String getAccountBEHostUrl(){
			return mStrHostUrl;
		}
		
		synchronized public void setAccountBEHostUrl(String strURL){
			mStrHostUrl = strURL;
		}
		
		public String getVPCAccountBEHostUrl(){
			return mStrVPCHostUrl;
		}
		
		synchronized public void setVPCAccountBEHostUrl(String strURL){
			mStrVPCHostUrl = strURL;
		}
		
		public String getMMBEHostUrl(){
			return mStrMMHostUrl;
		}
		
		synchronized public void setMMBEHostUrl(String strURL){
			mStrMMHostUrl = strURL;
		}
		
		public String getCAMBEHostUrl(){
			return mStrCamHostUrl;
		}
		
		synchronized public void setCAMBEHostUrl(String strURL){
			mStrCamHostUrl = strURL;
		}
		
		public String getNSBEHostUrl(){
			return mStrNSHostUrl;
		}
		
		synchronized public void setNSBEHostUrl(String strURL){
			mStrNSHostUrl = strURL;
		}
		
		public String getCamWSHostUrl(){
			return mStrCamWsHostUrl;
		}
		
		synchronized public void setCamWSHostUrl(String strURL){
			mStrCamWsHostUrl = strURL;
		}

		public String getWSHostUrl(){
			return mStrWSHostUrl;
		}
		
		synchronized public void setWSHostUrl(String strURL){
			mStrWSHostUrl = strURL;
		}
		
		public String getWSAHostUrl(){
			return mStrWSAHostUrl;
		}
		
		synchronized public void setWSAHostUrl(String strURL){
			mStrWSAHostUrl = strURL;
		}
		
		public String getNewsHostUrl(){
			return mStrNewsHostUrl;
		}
		
		synchronized public void setNewsHostUrl(String strURL){
			mStrNewsHostUrl = strURL;
		}

		public String getUserid(){
			return mStrUserid;
		}
		
		synchronized public void setUserid(String strId){
			mStrUserid = strId;
		}
		
		public String getAccount(){
			return mStrAccount;
		}
		
		synchronized public void setAccount(String strAccount){
			mStrAccount = strAccount;
		}
		
		public boolean isUseridValid(){
			String id = getUserid();
			return (null != id) && (0 < id.length());
		}
		
		public String getDomain(){
			return mStrDomain;
		}
		
		synchronized public void setDomain(String strDomain){
			mStrDomain = strDomain;
		}
		
		public boolean isTokenValid(){
			String token = getAuthToken();
			return (null != token) && (0 < token.length());
		}
		
		public String getAuthToken(){
			return mStrToken;
		}
		
		synchronized public void setAuthToken(String strToken){
			mStrToken = strToken;
		}
		
		public String getPairToken(){
			return mStrPairToken;
		}
		
		synchronized public void setPairToken(String strToken){
			mStrPairToken = strToken;
		}
		
		public boolean getIsCertificated(){
			return mbIsCertificated;
		}
		
		public void setIsCertificated(boolean bIsCertificated){
			mbIsCertificated = bIsCertificated;
		}
		
		public SERVER_MODE getServerMode(){
			return mServerMode;
		}
		
		public void setServerMode(SERVER_MODE mode){
			mServerMode = mode;
		}

		@Override
		public int describeContents() {
			return 0;
		}
		
		public String getOwnerInfo(){
			return mStrOwnerInfo;
		}
		
		synchronized public void setOwnerInfo(String strOwnerInfo){
			mStrOwnerInfo = strOwnerInfo;
		}
		
		public long getCamUpdateTimestamp(){
			return mlCamUpdateTs;
		}
		
		synchronized public void setCamUpdateTimestamp(long lCamUpdateTs){
			mlCamUpdateTs = lCamUpdateTs;
		}
		
		public String getCamUpdateList(){
			return mStrCamUpdateList;
		}
		
		synchronized public void setCamUpdateList(String strCamUpdateList){
			mStrCamUpdateList = strCamUpdateList;
		}
		
		public int getVPCNumber(){
			return miVPCno;
		}
		
		synchronized public void setVPCNumber(int iVPCno){
			miVPCno = (0 >= iVPCno)?1:iVPCno;
		}
	
		@Override
		public void writeToParcel(Parcel dest, int flags) {
	 
			// We just need to write each field into the
			// parcel. When we read from parcel, they
			// will come back in the same order
			dest.writeString(mStrHostUrl);
			dest.writeString(mStrVPCHostUrl);
			dest.writeString(mStrMMHostUrl);
			dest.writeString(mStrCamHostUrl);
			dest.writeString(mStrNSHostUrl);
			dest.writeString(mStrCamWsHostUrl);
			dest.writeString(mStrWSHostUrl);
			dest.writeString(mStrWSAHostUrl);
			dest.writeString(mStrNewsHostUrl);
			
			dest.writeString(mStrUserid);
			dest.writeString(mStrAccount);
			dest.writeString(mStrDomain);
			dest.writeString(mStrToken);
			dest.writeString(mStrOwnerInfo);
			dest.writeString(mStrPairToken);
			
			dest.writeInt(mbIsCertificated?1:0);
			dest.writeInt(mServerMode.ordinal());
			
			dest.writeLong(mlCamUpdateTs);
			dest.writeString(mStrCamUpdateList);
			
			dest.writeInt(miVPCno);
		}
		
		private SessionData(Parcel in) {
			readFromParcel(in);
	    }
	 
		/**
		 *
		 * Called from the constructor to create this
		 * object from a parcel.
		 *
		 * @param in parcel from which to re-create object
		 */
		private void readFromParcel(Parcel in) {
	 
			// We just need to read back each
			// field in the order that it was
			// written to the parcel
			mStrHostUrl = in.readString();
			mStrVPCHostUrl = in.readString();
			mStrMMHostUrl = in.readString();
			mStrCamHostUrl = in.readString();
			mStrNSHostUrl = in.readString();
			mStrCamWsHostUrl = in.readString();
			mStrWSHostUrl = in.readString();
			mStrWSAHostUrl = in.readString();
			mStrNewsHostUrl = in.readString();
			
			mStrUserid = in.readString();
			mStrAccount = in.readString();
			mStrDomain = in.readString();
			mStrToken = in.readString();
			mStrOwnerInfo = in.readString();
			mStrPairToken = in.readString();
			
			mbIsCertificated = in.readInt()>0?true:false;
			mServerMode = SERVER_MODE.translateToMode(in.readInt());
			
			mlCamUpdateTs = in.readLong();
			mStrCamUpdateList = in.readString();
			
			miVPCno = in.readInt();
		}
		
		public static final Parcelable.Creator<SessionData> CREATOR = new Parcelable.Creator<SessionData>() {
	        public SessionData createFromParcel(Parcel in) {
	            return new SessionData(in);
	        }

	        public SessionData[] newArray(int size) {
	            return new SessionData[size];
	        }
	    };
	}
	
	//Utils function
	static public String appendSession(String url){
		String strUrl = url;
		if(SessionMgr.getInstance().isTokenValid()){
         	Bundle paramters = new Bundle();
            paramters.putString("session", SessionMgr.getInstance().getAuthToken());
             
         	int iIndex = -1;
         	if(-1 < (iIndex = url.lastIndexOf("?"))){
         		if(iIndex == url.length()-1){
         			strUrl = url+encodeUrl(paramters);
         		}else{
         			strUrl = url+"&"+encodeUrl(paramters);
         		}
         	}else{
         		strUrl = url+"?"+encodeUrl(paramters);
         	}
        } 
		return strUrl;
	}
	
	static public String encodeUrl(Bundle parameters) {
        if (parameters == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String key : parameters.keySet()) {
            Object parameter = parameters.get(key);
            if (!(parameter instanceof String)) {
                continue;
            }

            if (first) first = false; else sb.append("&");
            sb.append(URLEncoder.encode(key) + "=" +
                      URLEncoder.encode(parameters.getString(key)));
        }
        return sb.toString();
    }
}
