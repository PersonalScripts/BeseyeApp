package com.app.beseye.pairing;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.CameraViewActivity;
import com.app.beseye.R;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.ota.BeseyeCamSWVersionMgr;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_GROUP;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_STATUS;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_VER_CHECK_STATUS;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.OnCamUpdateStatusChangedListener;
import com.app.beseye.ota.CamSwUpdateRecord;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;

public class SoundPairingNamingActivity extends BeseyeBaseActivity implements OnCamUpdateStatusChangedListener{	
	private EditText mEtCamName;
	private Button mBtnDone;
	private String mStrCamNameCandidate = null;
	private boolean mbGetOTAVerCheckResult = false;
	private boolean mbIsLatestOTAVersion = true;

	private boolean mbSetCamNameDone = false;

    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreCamVerCheck = true;
		getSupportActionBar().hide();
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			if(null != mCam_obj){
				//Log.i(TAG, "CameraViewActivity::updateAttrByIntent(), mCam_obj:"+mCam_obj.toString());
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
				mStrVCamName = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME);
			}
		} catch (JSONException e1) {
			Log.e(TAG, "SoundPairingNamingActivity, failed to parse, e1:"+e1.toString());
		}
		
		mBtnDone = (Button)findViewById(R.id.button_done);
		if(null != mBtnDone){
			mBtnDone.setFocusable(false);
			mBtnDone.setOnClickListener(this);
			mBtnDone.setEnabled((null != mStrVCamName && 0 < mStrVCamName.length()));
		}
		
		mEtCamName = (EditText)findViewById(R.id.editText_name_camera);
		if(null != mEtCamName){
			if(null != mStrVCamName){
				mEtCamName.requestFocus();
				mEtCamName.setText(mStrVCamName);
				mEtCamName.setSelection(0, mStrVCamName.length());
			}
			
			mEtCamName.setOnEditorActionListener(mOnEditorActionListener);
			mEtCamName.addTextChangedListener(new TextWatcher(){
				@Override
				public void afterTextChanged(Editable editable) {
					mBtnDone.setEnabled(editable.length() > 0);
				}

				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}

				@Override
				public void onTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {
				
				}
			});		
		}
		
		BeseyeCamSWVersionMgr.getInstance().registerOnCamUpdateStatusChangedListener(this);
		BeseyeCamSWVersionMgr.getInstance().AddUpdateVCam(mCam_obj, CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PERONSAL);
		BeseyeCamSWVersionMgr.getInstance().checkCamOTAVer(CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PERONSAL, this.mStrVCamID, false);
	}
	
	@Override
	public void onDestroy() {
		BeseyeCamSWVersionMgr.getInstance().unregisterOnCamUpdateStatusChangedListener(this);
		super.onDestroy();
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_done:{
				checkCamName();
				break;
			}
			default:
				Log.w(TAG, "onClick(), not handle view id:"+view.getId());
		}
	}
	
	@Override
	protected void onSessionComplete() {
		super.onSessionComplete();
		if(null != mEtCamName){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					mEtCamName.requestFocus();
					BeseyeUtils.showSoftKeyboard(SoundPairingNamingActivity.this, mEtCamName);
				}}, 1000);
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_signup_paring_name_cam_page;
	}
 
	private void checkCamName(){
		if(0 < mEtCamName.getText().length()){
			mStrCamNameCandidate = mEtCamName.getText().toString();
			Log.i(TAG, "onEditorAction(), mStrVCamName:["+mStrVCamName+"]");
			BeseyeUtils.hideSoftKeyboard(SoundPairingNamingActivity.this, mEtCamName);
			monitorAsyncTask(new BeseyeAccountTask.SetCamAttrTask(SoundPairingNamingActivity.this), false, mStrVCamID, mStrCamNameCandidate);
		}else 
			Toast.makeText(SoundPairingNamingActivity.this, R.string.toast_pairing_enter_cam_name, Toast.LENGTH_SHORT).show();
	}
    
    TextView.OnEditorActionListener mOnEditorActionListener = new TextView.OnEditorActionListener(){
		@Override
		public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
			//Log.i(TAG, "onEditorAction(), actionId:["+actionId+"]");
			if (view.equals(mEtCamName) && actionId == EditorInfo.IME_ACTION_DONE) { 
				checkCamName();
				return true;
			}			
			return false;
		}
	};
	
	
	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, int iErrType, String strTitle,String strMsg) {	
		Log.e(TAG, "onErrorReport(), "+task.getClass().getSimpleName()+", iErrType="+iErrType);	
		if(task instanceof BeseyeAccountTask.SetCamAttrTask){
			onShowDialog(null, DIALOG_ID_WARNING, getString(R.string.dialog_title_warning), BeseyeUtils.appendErrorCode(this, R.string.msg_set_cam_name_fail, iErrType));
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}
	

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(DEBUG)
			Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.SetCamAttrTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString());
					//monitorAsyncTask(new BeseyeAccountTask.SetCamAttrTask(this), true, vcam_id, "My Test Cam");
					if(null != mCam_obj){
						BeseyeJSONUtil.setJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME, mStrCamNameCandidate);
					}
				}
				
				mbSetCamNameDone = true;
				selectNextPage();
			}else{
				Log.i(TAG, "onPostExecute(), "+result.toString());
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	private void selectNextPage(){
		if(mbSetCamNameDone && mbGetOTAVerCheckResult){
			Bundle b = new Bundle();
			b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
			b.putBoolean(CameraViewActivity.KEY_PAIRING_DONE, true);
			b.putBoolean(CameraViewActivity.KEY_PAIRING_OTA_NEED_UPDATE, !mbIsLatestOTAVersion);

			launchDelegateActivity(CameraListActivity.class.getName(), b);
			finish();
		}else if(!mbGetOTAVerCheckResult){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					BeseyeCamSWVersionMgr.getInstance().checkCamOTAVer(CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PERONSAL, mStrVCamID, !mbSetCamNameDone);
				}}, 1000L);
		}
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			Toast.makeText(this, R.string.toast_pairing_enter_cam_name, Toast.LENGTH_SHORT).show();
			return true;
		}else
			return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onCamUpdateStatusChanged(String strVcamId, CAM_UPDATE_STATUS curStatus, CAM_UPDATE_STATUS prevStatus, CamSwUpdateRecord objUpdateRec) {
	
	}

	@Override
	public void onCamUpdateProgress(String strVcamId, int iPercetage) {
		
	}
	
	@Override
	public void onCamUpdateVerChkStatusChanged(String strVcamId, CAM_UPDATE_VER_CHECK_STATUS curStatus, CAM_UPDATE_VER_CHECK_STATUS prevStatus, CamSwUpdateRecord objUpdateRec){
		if(mStrVCamID.equals(strVcamId)){
			if(objUpdateRec.getVerCheckStatus().equals(CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_UPDATED) ||
			   objUpdateRec.getVerCheckStatus().equals(CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_OUT_OF_DATE)){
				mbGetOTAVerCheckResult = true;
				mbIsLatestOTAVersion = objUpdateRec.getVerCheckStatus().equals(CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_UPDATED);
			}
			selectNextPage();
		}
	}
	
}

