package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.List;

import org.json.JSONObject;

import com.app.beseye.error.BeseyeError;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;
import com.app.beseye.util.BeseyeAccountFilter;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.DeviceUuidFactory;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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

public class SignupActivity extends BeseyeAccountBaseActivity {
	private EditText mEtUserName, mEtPassword;
	private TextView mTvTermOfService, mTvPrivacyPolicy;
	private Button mBtnSignUp;
	private String mstrSN;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		
		if(null != mTxtNavTitle){
			mTxtNavTitle.setText(R.string.signup_title_create_account);
		}
		
		mEtUserName = (EditText)findViewById(R.id.editText_username);
		if(null != mEtUserName){
			mEtUserName.addTextChangedListener(mTextWatcher);
			if(SessionMgr.getInstance().getServerMode().ordinal() <= SERVER_MODE.MODE_DEV.ordinal()){
				if(DEBUG){
					//if(BeseyeConfig.COMPUTEX_PAIRING){
					File snFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_sn");
					mstrSN = "0000";
					if(null != snFile && snFile.exists()){
						try {
							BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(snFile)));
							try {
								mstrSN = (null != reader)?reader.readLine():null;
							} catch (IOException e) {
								e.printStackTrace();
							}
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						}
					}
					try{
						mstrSN = String.format("%04d", Integer.parseInt(mstrSN)+1);	 
					}catch(java.lang.NumberFormatException ex){
						mstrSN = "0000";
					}
					mEtUserName.setText("beseye"+DeviceUuidFactory.getDeviceUuid().toString().substring(1,4)+mstrSN+"@beseye.com");
				}else{
					mEtUserName.setText(TEST_ACC);
				}
			}
			
			//}
		}
		
		mEtPassword = (EditText)findViewById(R.id.editText_password);
		if(null != mEtPassword){
			mEtPassword.addTextChangedListener(mTextWatcher);
			mEtPassword.setOnEditorActionListener(mOnEditorActionListener);
			if(DEBUG && SessionMgr.getInstance().getServerMode().ordinal() <= SERVER_MODE.MODE_DEV.ordinal())
				mEtPassword.setText("beseye1234");
		}
		
		mTvTermOfService = (TextView)findViewById(R.id.tv_bottom_description_terms);
		if(null != mTvTermOfService){
			mTvTermOfService.setOnClickListener(this);
		}
		
		mTvPrivacyPolicy = (TextView)findViewById(R.id.tv_bottom_description_policy);
		if(null != mTvPrivacyPolicy){
			mTvPrivacyPolicy.setOnClickListener(this);
		}
		
		mBtnSignUp = (Button)findViewById(R.id.button_continue);
		if(null != mBtnSignUp){
			mBtnSignUp.setOnClickListener(this);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		checkEditTextStates();
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_signup_create_account;
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.tv_bottom_description_terms:{
				//Toast.makeText(this, "TOS click", Toast.LENGTH_SHORT).show();
				break;
			}
			case R.id.tv_bottom_description_policy:{
				//Toast.makeText(this, "Privacy Policy click", Toast.LENGTH_SHORT).show();
				break;
			}
			case R.id.button_continue:{
				//Toast.makeText(this, "Login click", Toast.LENGTH_SHORT).show();
				checkLoginInfo();
				break;
			}
			default:
				super.onClick(view);
		}
	}
	
	private TextWatcher mTextWatcher = new TextWatcher(){

		@Override
		public void afterTextChanged(Editable arg0) {
			checkEditTextStates();
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
	};
	
	
	private void checkEditTextStates(){
		if(null != mBtnSignUp){
			mBtnSignUp.setEnabled(BeseyeUtils.haveText(mEtUserName) && BeseyeUtils.haveText(mEtPassword));
		}
	}
	
	private void checkLoginInfo(){
		if(null != mEtUserName){
			mEtUserName.setText((new BeseyeAccountFilter()).filter(mEtUserName.getText(), 0, mEtUserName.length(), null, 0, 0));
			
			String strAccount = mEtUserName.getText().toString();
			if(!BeseyeUtils.validEmail(strAccount)){
				onShowDialog(null, DIALOG_ID_WARNING, getString(R.string.dialog_title_warning), getString(R.string.msg_invalid_account_format));
				return;
			}
			
			String strPw 		= (null != mEtPassword)?mEtPassword.getText().toString():null;
			//if(null == strPw || 6 > strPw.length() || 20 < strPw.length()){
			if(!BeseyeUtils.validPassword(strPw)){
				onShowDialog(null, DIALOG_ID_WARNING, getString(R.string.dialog_title_warning), getString(R.string.msg_pw_length_error));
				return;
			}
			
			monitorAsyncTask(new BeseyeAccountTask.RegisterTask(this), true, mEtUserName.getText().toString(), mEtPassword.getText().toString());
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle,String strMsg) {	
		if(task instanceof BeseyeAccountTask.RegisterTask){
			//launchActivityByClassName(WifiSetupGuideActivity.class.getName());
			//finish();
			int iErrMsgId = R.string.msg_signup_error;
			if(BeseyeError.E_BE_ACC_USER_ALREADY_EXIST == iErrType){
				iErrMsgId = R.string.msg_signup_err_email_used;
			}
			onShowDialog(null, DIALOG_ID_WARNING, getString(R.string.dialog_title_warning), getString(iErrMsgId));
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}

	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.RegisterTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					JSONObject obj = result.get(0);
					if(null != obj){
						JSONObject objSes = BeseyeJSONUtil.getJSONObject(obj, BeseyeJSONUtil.ACC_SES);
						if(null != objSes){
							SessionMgr.getInstance().setAuthToken(BeseyeJSONUtil.getJSONString(objSes, BeseyeJSONUtil.ACC_SES_TOKEN));
						}
						
						JSONObject objUser = BeseyeJSONUtil.getJSONObject(obj, BeseyeJSONUtil.ACC_USER);
						if(null != objUser){
							SessionMgr.getInstance().setUserid(BeseyeJSONUtil.getJSONString(objUser, BeseyeJSONUtil.ACC_ID));
							SessionMgr.getInstance().setAccount(BeseyeJSONUtil.getJSONString(objUser, BeseyeJSONUtil.ACC_EMAIL));
							SessionMgr.getInstance().setIsCertificated(BeseyeJSONUtil.getJSONBoolean(objUser, BeseyeJSONUtil.ACC_ACTIVATED));
						}
						
						if(null != mstrSN){
							Writer writer = null;
							try {
								File snFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_sn");
								snFile.delete();
								if(null != snFile){
									writer = new BufferedWriter(new FileWriter(snFile));
									if(null != writer){
										writer.write(mstrSN);
										writer.close();
									}
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
					Bundle b = new Bundle();
					//b.putBoolean(OpeningPage.KEY_IGNORE_ACTIVATED_FLAG, true);
					launchActivityByClassName(PairingWatchOutActivity.class.getName(), b);
					//launchDelegateActivity(WifiSetupGuideActivity.class.getName(), b);
					//setResult(RESULT_OK);
					//finish();
					
					if(null != mEtPassword){
						mEtPassword.setText("");
					}
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	TextView.OnEditorActionListener mOnEditorActionListener = new TextView.OnEditorActionListener(){

		@Override
		public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_DONE) { 
				if(view.equals(mEtPassword)){
					checkLoginInfo();
					BeseyeUtils.hideSoftKeyboard(SignupActivity.this, mEtPassword);
					
					return true;
				}
			}			
			return false;
		}};
}
