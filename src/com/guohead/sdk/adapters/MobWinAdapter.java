package com.guohead.sdk.adapters;

import com.guohead.sdk.BaseAdapter;
import com.guohead.sdk.GHView;
import com.guohead.sdk.utils.Logger;
import com.guohead.sdk.utils.Utils;
import com.tencent.mobwin.AdListener;
import com.tencent.mobwin.AdView;

public class MobWinAdapter extends BaseAdapter implements  AdListener {
	private AdView mAdView;


	public MobWinAdapter(GHView view, String params) {
		super(view, params, "mobWin");
	}

	@Override
	public void loadAd() {
		// TODO Auto-generated method stub
		super.loadAd();
		mAdView = new AdView(mGHView.getContext(), keys[0],"no", "son1128zoz");
		// 带配置参数构造方法 
		// AdView adView = new AdView(this,Color.GRAY,Color.YELLOW,Color.BLACK,153);  
		mAdView.setAdListener(this); 
		mAdView.setId(Utils.TYPE_MOBWIN);
		addView(mAdView);
		mGHView.postInvalidate();
	}



	@Override
	public void onReceiveAd() {
		Logger.i("  mobWin     onRefreshAd======>");
		mAdView.setAdListener(null); 
		receiveAd(null);

	}

	@Override
	public void onReceiveFailed(int arg0) {
		Logger.i("  mobWin     onFailedToRefreshAd======>");
		mAdView.setAdListener(null); 
		failedReceiveAd(null);
	}

	@Override
	public void onAdClick() {
		// TODO Auto-generated method stub

	}


	@Override
	public void onClick() {
		// TODO Auto-generated method stub

	}




}
