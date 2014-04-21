package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;

import java.util.ArrayList;
import java.util.List;

import com.app.beseye.pairing.SoundPairingActivity;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.OnNetworkChangeCallback;
import com.app.beseye.util.NetworkMgr.OnWifiScanResultAvailableCallback;
import com.app.beseye.util.NetworkMgr.OnWifiStatusChangeCallback;
import com.app.beseye.util.NetworkMgr.WifiAPInfo;

import android.app.Dialog;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public abstract class WifiControlBaseActivity extends BeseyeBaseActivity 
							  				  implements OnWifiScanResultAvailableCallback, 
							  				  			 OnNetworkChangeCallback, 
							  				  			 OnWifiStatusChangeCallback{
	
	protected WIFI_SETTING_STATE mWifiSettingState = WIFI_SETTING_STATE.STATE_UNINIT; 
	protected List<WifiAPInfo> mlstScanResult;
	protected TextView mtxtKeyIndex;
	protected String mWifiApPassword = null;
	protected WifiAPInfo mChosenWifiAPInfo;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "WifiControlBaseActivity::onCreate()");
		super.onCreate(savedInstanceState);
		mlstScanResult = new ArrayList<WifiAPInfo>();
		setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
	}
	
    @Override
	protected void onResume() {
    	Log.d(TAG, "WifiControlBaseActivity::onResume()");
		super.onResume();
		NetworkMgr.getInstance().registerNetworkChangeCallback(this);
		NetworkMgr.getInstance().registerWifiStatusChangeCallback(this);
		
		if(getWifiSettingState().ordinal() <= WIFI_SETTING_STATE.STATE_WIFI_SCAN_DONE.ordinal()){
			setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
		}
	}
    
	@Override
	protected void onPause() {
		Log.d(TAG, "WifiControlBaseActivity::onPause()");
		if(inWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_SCANNING))
			cancelScanWifi();
		NetworkMgr.getInstance().unregisterNetworkChangeCallback(this);
		NetworkMgr.getInstance().unregisterWifiStatusChangeCallback(this);
		super.onPause();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Log.d(TAG, "WifiControlBaseActivity::onCreateDialog()");
		Dialog dialog;
		switch(id){
			case DIALOG_ID_WIFI_AP_INFO:{
				dialog = new Dialog(this);
				dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
				dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(createWifiAPInfoView(false, 0));
				
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
					dialog.setOnCancelListener(new OnCancelListener(){
						@Override
						public void onCancel(DialogInterface arg0) {
							removeMyDialog(DIALOG_ID_WIFI_AP_INFO);
						}});
					dialog.setOnDismissListener(new OnDismissListener(){
						@Override
						public void onDismiss(DialogInterface arg0) {
							mWifiApPassword = null;
						}});
				}
		    	break;
			}
			case DIALOG_ID_WIFI_TURN_ON_FAILED:{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setIcon(android.R.drawable.ic_dialog_alert);
            	builder.setTitle(getString(R.string.dialog_title_warning));
            	builder.setMessage(getString(R.string.dialog_wifi_fail_on));
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	dialog.dismiss();
				    	setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_TURNING_ON);
				    }
				});
				
				builder.setOnCancelListener(new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						//finish();
						dialog.dismiss();
						setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_TURNING_ON);
					}});
				dialog = builder.create();
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
				}
            	break;
            }
			case DIALOG_ID_WIFI_SCAN_FAILED:{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setIcon(android.R.drawable.ic_dialog_alert);
            	builder.setTitle(getString(R.string.dialog_title_warning));
            	builder.setMessage(getString(R.string.dialog_wifi_fail_scan));
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	dialog.dismiss();
				    	setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_ON);
				    }
				});
				builder.setOnCancelListener(new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						//finish();
						dialog.dismiss();
						setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_ON);
					}});
				dialog = builder.create();
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
				}
            	break;
            }

			default:
				dialog = super.onCreateDialog(id);
		}
		return dialog;
	}
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle bundle) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_WARNING:{
				dialog = super.onCreateDialog(id, bundle);
				if(null != dialog){
					dialog.setOnDismissListener(new OnDismissListener(){
						@Override
						public void onDismiss(DialogInterface arg0) {
							clearScanResult();
							setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
						}});
				}
				
				break;
			}
			default:
				dialog = super.onCreateDialog(id, bundle);
		}
		
		return dialog;
	}
	
	protected void clearScanResult(){
		if(null != mlstScanResult)
			mlstScanResult.clear();
	}

	@Override
	public void onWifiScanResultAvailable() {
		//Toast.makeText(this, "onWifiScanResultAvailable()", Toast.LENGTH_SHORT).show();
		if(inWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_SCANNING)){
			setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_SCAN_DONE);
		}
	}

	@Override
	public void onWifiStateChanged(int iWifiState, int iPrevWifiState) {
		Log.i(TAG, "onWifiStateChanged(), iWifiState from "+iPrevWifiState+" to "+iWifiState);
		if(iWifiState == WifiManager.WIFI_STATE_ENABLED){
			if(inWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_TURNING_ON))
				setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_ON);
		}
		
		updateUIByWifiStatus(iWifiState);
	}
	
	protected void updateUIByWifiStatus(int iWifiState){}
	
	@Override
	public void onWifiNetworkStateChanged(DetailedState iWifiNetworkState, DetailedState iPrevWifiNetworkState) {
		loadWifiAPList();
//		if(iWifiNetworkState == DetailedState.CONNECTED){
//			if(inWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_AP_SETTING))
//				setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_AP_SET_DONE);
//		}
	}

	@Override
	public void onConnectivityChanged(boolean onConnectivityChanged) {
		Log.i(TAG, "onConnectivityChanged(), onConnectivityChanged "+onConnectivityChanged);
	}
	
	protected void loadWifiAPList(){
		Log.i(TAG, "loadWifiAPList()");
		if(null != mlstScanResult){
			NetworkMgr.getInstance().filterWifiAPInfo(mlstScanResult, NetworkMgr.getInstance().getWifiScanList());
			onWiFiScanComplete();
		}
	}
	
	protected void onWiFiScanComplete(){}
	
	protected boolean scanWifi(boolean bForceShowDialog){
		boolean bRet = false;
		if(bRet = NetworkMgr.getInstance().scanWifiList(this)){
			setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_SCANNING);
			if(bForceShowDialog || (null != mlstScanResult && 0 == mlstScanResult.size()))
				showMyDialog(DIALOG_ID_WIFI_SCANNING);
		}else{
			showMyDialog(DIALOG_ID_WIFI_SCAN_FAILED);
		}
		
		return bRet;
	}
	
	protected boolean cancelScanWifi(){
		return NetworkMgr.getInstance().cancelScanWifiList(this);
	}
	
	protected enum WIFI_SETTING_STATE{
		STATE_UNINIT,
		STATE_INIT,
		STATE_WIFI_TURNING_ON,
		STATE_WIFI_ON,
		STATE_WIFI_SCANNING,
		STATE_WIFI_SCAN_DONE,
		STATE_WIFI_AP_PICKING,
		STATE_WIFI_AP_SETTING,
		STATE_WIFI_AP_SET_DONE
	}

	protected WIFI_SETTING_STATE getWifiSettingState(){
		return mWifiSettingState;
	}
	
	protected boolean inWifiSettingState(WIFI_SETTING_STATE state){
		return mWifiSettingState == state;
	}
	
	protected void setWifiSettingState(WIFI_SETTING_STATE state){
		Log.i(TAG, "WifiControlBaseActivity::setWifiSettingState(), state:"+state);
		WIFI_SETTING_STATE prevState = mWifiSettingState;
		mWifiSettingState = state;
		switch(mWifiSettingState){
			case STATE_INIT:{
				if(NetworkMgr.getInstance().getWifiStatus() != WifiManager.WIFI_STATE_ENABLED){
					Log.d(TAG, "WifiControlBaseActivity::setWifiSettingState(), wifi is not enabled");
					if(NetworkMgr.getInstance().turnOnWifi()){
						//mLoadWifiListRunnable = new LoadWifiListRunnable(this);
						setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_TURNING_ON);
						showMyDialog(DIALOG_ID_TURN_ON_WIFI);
					}else{
						showMyDialog(DIALOG_ID_WIFI_TURN_ON_FAILED);
					}
				}else{
					setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_ON);
				}
				break;
			}
			case STATE_WIFI_TURNING_ON:{
//				if(null != mLoadWifiListRunnable){
//					removeMyDialog(DIALOG_ID_TURN_ON_WIFI);
//					mLoadWifiListRunnable.run();
//					mLoadWifiListRunnable = null;
//				}
				break;
			}
			case STATE_WIFI_ON:{
				if(NetworkMgr.getInstance().getWifiStatus() == WifiManager.WIFI_STATE_ENABLED){
					removeMyDialog(DIALOG_ID_TURN_ON_WIFI);
					scanWifi(false);
				}else{
					Log.i(TAG, "WifiControlBaseActivity::setWifiSettingState(), can't scan due to wifi off");
					setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
				}
				break;
			}
			case STATE_WIFI_SCANNING:{
				break;
			}
			case STATE_WIFI_SCAN_DONE:{
				removeMyDialog(DIALOG_ID_WIFI_SCANNING);
				loadWifiAPList();
//				if(REDDOT_DEMO){
//					mChosenWifiAPInfo = null;
//					for(WifiAPInfo info : mlstScanResult){
//						if(null != info && RELAY_AP_SSID.equals(BeseyeUtils.removeDoubleQuote(info.SSID))){
//							mChosenWifiAPInfo = info;
//							mChosenWifiAPInfo.password = RELAY_AP_PW;
//					    	if(null == mWifiAPSetupDelegator){
//					    		mWifiAPSetupDelegator = new WifiAPSetupDelegator(mChosenWifiAPInfo, WifiControlBaseActivity.this);
//					    	}else{
//					    		mWifiAPSetupDelegator.updateTargetAPInfo(mChosenWifiAPInfo);
//					    	}
//					    	setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_AP_SETTING);
//					    	break;
//						}
//					}
//					
//					if(null == mChosenWifiAPInfo){
//						Bundle b = new Bundle();
//						b.putString(KEY_WARNING_TEXT, String.format(getResources().getString(R.string.dialog_wifi_fail_to_connect), RELAY_AP_SSID));
//						showMyDialog(DIALOG_ID_WARNING, b);
//					}
//				}
				break;
			}
			default:
				Log.i(TAG, "setWifiSettingState(), invalid state "+state);
		}
	}
	
	protected void turnOnWifi(){
		if(NetworkMgr.getInstance().turnOnWifi()){
			//mLoadWifiListRunnable = new LoadWifiListRunnable(this);
			setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_TURNING_ON);
			showMyDialog(DIALOG_ID_TURN_ON_WIFI);
		}else{
			showMyDialog(DIALOG_ID_WIFI_TURN_ON_FAILED);
		}
	}
	
	protected View createWifiAPInfoView(final boolean bPasswordOnly, int iWrongPWId){
		View vgApInfo = null;
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			vgApInfo = (View)inflater.inflate(R.layout.wifi_ap_info_dialog, null);
			if(null != vgApInfo){
				TextView txtSSID = (TextView)vgApInfo.findViewById(R.id.txt_ap_name);
				if(null != txtSSID){
					txtSSID.setText(mChosenWifiAPInfo.SSID);
				}
				
				ViewGroup vgAPBasicInfo = (ViewGroup)vgApInfo.findViewById(R.id.vg_ap_basic_info_holder);
				if(bPasswordOnly){
					vgAPBasicInfo.setVisibility(View.GONE);
					RelativeLayout vgWrongPW = (RelativeLayout)vgApInfo.findViewById(R.id.vg_incorrect_password_holder);
					if(null != vgWrongPW){
						vgWrongPW.setVisibility(View.VISIBLE);
						TextView txtWrongPW = (TextView)vgWrongPW.findViewById(R.id.txt_incorrect_password);
						if(null != txtWrongPW){
							txtWrongPW.setText(iWrongPWId);
						}
					}
				}else{
					TextView txtSignal = (TextView)vgAPBasicInfo.findViewById(R.id.txt_signal_value);
					if(null != txtSignal){
						txtSignal.setText(NetworkMgr.getInstance().getSignalStrengthTermId(mChosenWifiAPInfo.signalLevel));
					}
					
					TextView txtSecurity = (TextView)vgAPBasicInfo.findViewById(R.id.txt_security_value);
					if(null != txtSecurity){
						txtSecurity.setText((WifiAPInfo.AUTHNICATION_NONE.equals(mChosenWifiAPInfo.cipher))?getResources().getString(R.string.dialog_wifi_ap_security_none):mChosenWifiAPInfo.cipher);
					}
				}
				
				Button btnCancel= (Button)vgApInfo.findViewById(R.id.btn_cancel);
				if(null != btnCancel){
					btnCancel.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							if(bPasswordOnly){
								removeMyDialog(DIALOG_ID_WIFI_AP_INCORRECT_PW);
							}else{
								removeMyDialog(DIALOG_ID_WIFI_AP_INFO);
							}
						}});
				}
				
				mtxtKeyIndex = (TextView)vgApInfo.findViewById(R.id.txt_keyindex_value);
				if(null != mtxtKeyIndex){
					WifiConfiguration config = NetworkMgr.getInstance().getWifiConfigurationBySSID(mChosenWifiAPInfo.SSID);
					if(null != config){
						mChosenWifiAPInfo.wepkeyIdx = config.wepTxKeyIndex;
					}else{
						
					}
					mtxtKeyIndex.setText(String.valueOf(mChosenWifiAPInfo.wepkeyIdx+1));
					mtxtKeyIndex.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							showMyDialog(DIALOG_ID_WIFI_AP_KEYINDEX);
						}});
				}
				
				ImageView ivSpinner = (ImageView)vgApInfo.findViewById(R.id.iv_spinner);
				if(null != ivSpinner){
					ivSpinner.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							showMyDialog(DIALOG_ID_WIFI_AP_KEYINDEX);
						}});
				}

				final Button btnConnect = (Button)vgApInfo.findViewById(R.id.btn_connect);
				if(null != btnConnect){
					btnConnect.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							if(bPasswordOnly){
								removeMyDialog(DIALOG_ID_WIFI_AP_INCORRECT_PW);
							}else{
								removeMyDialog(DIALOG_ID_WIFI_AP_INFO);
							}
							
							mChosenWifiAPInfo.password = mWifiApPassword;
					    	mChosenWifiAPInfo.wepkeyIdx = Integer.parseInt(String.valueOf(mtxtKeyIndex.getText())) -1;
					    	
							Intent intent = new Intent();
							intent.setClass(WifiControlBaseActivity.this, SoundPairingActivity.class);
							intent.putExtra(SoundPairingActivity.KEY_WIFI_INFO, mChosenWifiAPInfo);
							startActivity(intent);
							setResult(RESULT_OK);
//					    	if(null == mWifiAPSetupDelegator){
//					    		mWifiAPSetupDelegator = new WifiAPSetupDelegator(mChosenWifiAPInfo, WifiListActivity.this);
//					    	}else{
//					    		mWifiAPSetupDelegator.updateTargetAPInfo(mChosenWifiAPInfo);
//					    	}
//					    	setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_AP_SETTING);
						}});
				}

				
				RelativeLayout vgPassord = (RelativeLayout)vgApInfo.findViewById(R.id.vg_password_holder);
				if(null != vgPassord){
					if(WifiAPInfo.AUTHNICATION_NONE.equals(mChosenWifiAPInfo.cipher)){
						mWifiApPassword = "";
						vgPassord.setVisibility(View.GONE);
					}else{
						final int iMinPasswordLength = mChosenWifiAPInfo.cipher.contains(WifiAPInfo.AUTHNICATION_WPA)?8:13;
						final EditText etPassword = (EditText)vgPassord.findViewById(R.id.et_password_value);
						if(null != etPassword){
							if(DEBUG){
								etPassword.setText(mChosenWifiAPInfo.cipher.contains(WifiAPInfo.AUTHNICATION_WPA)?"0630BesEye":"0630BesEye123");
							}
							mWifiApPassword = etPassword.getText().toString();
							etPassword.addTextChangedListener(new TextWatcher(){
								@Override
								public void afterTextChanged(Editable editable) {
									btnConnect.setEnabled(editable.length() >= iMinPasswordLength);
									mWifiApPassword = etPassword.getText().toString();
									//password.matches("[0-9A-Fa-f]*")
								}

								@Override
								public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}

								@Override
								public void onTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {
									
								}});
						}
						
						CheckBox cbShowPW = (CheckBox)vgPassord.findViewById(R.id.cb_show_password);
						if(null != cbShowPW){
							cbShowPW.setOnCheckedChangeListener(new OnCheckedChangeListener(){
								@Override
								public void onCheckedChanged(CompoundButton view,boolean bChecked) {
									if(null != etPassword){
										etPassword.setInputType(InputType.TYPE_CLASS_TEXT|(bChecked?InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:InputType.TYPE_TEXT_VARIATION_PASSWORD));
									}
								}});
						}
					}
				}
				
				RelativeLayout vgKeyIndex = (RelativeLayout)vgApInfo.findViewById(R.id.vg_keyindex_holder);
				if(null != vgKeyIndex){
					vgKeyIndex.setVisibility(mChosenWifiAPInfo.cipher.contains(WifiAPInfo.AUTHNICATION_WEP)?View.VISIBLE:View.GONE);
				}
			}
		}
		return vgApInfo;
	}
}