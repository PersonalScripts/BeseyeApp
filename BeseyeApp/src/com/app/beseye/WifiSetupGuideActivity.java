package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.TAG;

import com.app.beseye.WifiControlBaseActivity.WIFI_SETTING_STATE;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.WifiAPInfo;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class WifiSetupGuideActivity extends WifiControlBaseActivity {
	protected View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	protected ImageView mIvBack;
	protected TextView mTxtNavTitle;
	private Button mBtnChooseWifiAP, mBtnUseConnected;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_signup_nav, null);
		if(null != mVwNavBar){
			mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_left_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
			}
			
			mTxtNavTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != mTxtNavTitle){
				mTxtNavTitle.setText(R.string.signup_title_cam_wifi_settings);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		mBtnChooseWifiAP = (Button)this.findViewById(R.id.button_choose_network);
		if(null != mBtnChooseWifiAP){
			mBtnChooseWifiAP.setOnClickListener(this);
			mBtnChooseWifiAP.setEnabled(false);
		}
		
		mBtnUseConnected = (Button)this.findViewById(R.id.button_wifi_yes);
		if(null != mBtnUseConnected){
			mBtnUseConnected.setOnClickListener(this);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateBtnByScanResult();
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.iv_nav_left_btn:{
				finish();
				break;
			}
			case R.id.button_choose_network:{
				launchActivityForResultByClassName(WifiListActivity.class.getName(), null, REQ_CODE_PICK_WIFI);
				break;
			}
			case R.id.button_wifi_yes:{
				showMyDialog(DIALOG_ID_WIFI_AP_INFO);				
				break;
			}
			default:
				super.onClick(view);
		}		
	}
	
	static public final int REQ_CODE_PICK_WIFI = 0x1001;
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(REQ_CODE_PICK_WIFI == requestCode){
			if(RESULT_OK == resultCode){
				finish();
			}else{
				setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
			}
		}else
			super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_signup_wifi_setting;
	}
	
	protected void updateUIByWifiStatus(int iWifiState){
		super.updateUIByWifiStatus(iWifiState);
		updateBtnByScanResult();
	}
	
	protected void onWiFiScanComplete(){
		updateBtnByScanResult();
	}

	private void updateBtnByScanResult(){
		boolean bWifiEnabled = NetworkMgr.getInstance().isWifiEnabled();
		BeseyeUtils.setEnabled(mBtnChooseWifiAP, bWifiEnabled);
		
		if(!bWifiEnabled || null == mlstScanResult || 0 == mlstScanResult.size()){
			Log.i(TAG, "updateBtnByScanResult(), no Wifi ap was scanned");	
			BeseyeUtils.setEnabled(mBtnUseConnected, false);
		}else{
			Log.i(TAG, "Scan List:"+mlstScanResult.toString());	
			mChosenWifiAPInfo = null;
			for(WifiAPInfo info : mlstScanResult){
				if(null != info && info.bActiveConn){
					mChosenWifiAPInfo = info;
					Log.i(TAG, "updateBtnByScanResult(), get connected ap:["+mChosenWifiAPInfo.toString()+"]");	
					break;
				}
			}
			BeseyeUtils.setEnabled(mBtnUseConnected, null != mChosenWifiAPInfo);
		}
	}
}