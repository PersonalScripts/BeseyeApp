package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.ASSIGN_ST_PATH;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.app.beseye.adapter.CameraListAdapter;
import com.app.beseye.adapter.CameraListAdapter.CameraListItmHolder;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;
import com.app.beseye.widget.PullToRefreshBase.OnRefreshListener;
import com.app.beseye.widget.PullToRefreshListView;

public class CameraListActivity extends BeseyeBaseActivity implements OnSwitchBtnStateChangedListener{
	static public final String KEY_VCAM_ID = "KEY_VCAM_ID";
	static public final String KEY_VCAM_NAME = "KEY_VCAM_NAME";
	
	private PullToRefreshListView mMainListView;
	private CameraListAdapter mCameraListAdapter;
	private ViewGroup mVgEmptyView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.wifisetup_wifi_title_bg));
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE); 
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mMainListView = (PullToRefreshListView) findViewById(R.id.lv_camera_lst);
		
		if(null != mMainListView){
			mMainListView.setOnRefreshListener(new OnRefreshListener() {
    			@Override
    			public void onRefresh() {
    				monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(CameraListActivity.this), true);
    			}

				@Override
				public void onRefreshCancel() {

				}
    		});
			
			LayoutInflater inflater = getLayoutInflater();
			if(null != inflater){
				mVgEmptyView = (ViewGroup)inflater.inflate(R.layout.lv_empty_layout, null);
				if(null != mVgEmptyView){
					mMainListView.setEmptyView(mVgEmptyView);
					if(null != mVgEmptyView){
						TextView txt = (TextView)mVgEmptyView.findViewById(R.id.txtNoItmList);
						if(null != txt){
							txt.setText(R.string.no_camera);
						}
					}
				}
			}
			
        	mMainListView.setMode(LvExtendedMode.PULL_DOWN_TO_REFRESH);
        	
        	mCameraListAdapter = new CameraListAdapter(this, null, R.layout.layout_camera_list_itm, this, this);
        	if(null != mCameraListAdapter){
        		mMainListView.getRefreshableView().setAdapter(mCameraListAdapter);
        	}
		}
	}
	
	private void refreshList(){
		if(null != mCameraListAdapter){
			mCameraListAdapter.notifyDataSetChanged();
		}
	}
	
	protected void onSessionComplete(){
		monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(this), true);
	}
	
	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.GetVCamListTask){
				if(0 == iRetCode){
					Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
					int iVcamCnt = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.ACC_VCAM_CNT);
					if(0 < iVcamCnt){
						JSONArray VcamList = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.ACC_VCAM_LST);
						mCameraListAdapter.updateResultList(VcamList);
						refreshList();
//						if(null != VcamList){
//							try {
//								JSONObject vcam = VcamList.getJSONObject(0);
//								if(null != vcam){
//									mStrVCamID = BeseyeJSONUtil.getJSONString(vcam, BeseyeJSONUtil.ACC_ID);
//									mStrVCamName = BeseyeJSONUtil.getJSONString(vcam, BeseyeJSONUtil.ACC_NAME);
//									Log.e(TAG, "onPostExecute(), mStrVCamID:"+mStrVCamID);
//									getStreamingInfo();
//								}
//							} catch (JSONException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//						}
						if(null != mMainListView)
							mMainListView.onRefreshComplete();
					}else{
						onToastShow(task, "no Vcam attached.");
						Bundle b = new Bundle();
						b.putBoolean(OpeningPage.KEY_IGNORE_ACTIVATED_FLAG, true);
						launchDelegateActivity(WifiSetupGuideActivity.class.getName(), b);
					}
				}
			}else{
				Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());	
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}

	@Override
	public void onClick(View view) {
		if(view.getTag() instanceof CameraListItmHolder){
			JSONObject cam_obj = ((CameraListItmHolder)view.getTag()).mObjCam;
			if(null != cam_obj){
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_ID, BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_ID));
				b.putString(CameraListActivity.KEY_VCAM_NAME, BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_NAME));
				launchActivityByClassName(CameraViewActivity.class.getName(), b);
				return;
			}
		}
		super.onClick(view);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_camera_list;
	}

	@Override
	public void onSwitchBtnStateChanged(SwitchState state, View view) {
		
	}
}
