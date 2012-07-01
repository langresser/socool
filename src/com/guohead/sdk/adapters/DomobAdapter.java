/*
 * Copyright (c) 2011, GuoheAd Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'GuoheAd Inc.' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.guohead.sdk.adapters;

//import cn.domob.android.ads.DomobAdManager;
import cn.domob.android.ads.DomobAdView;

import com.guohead.sdk.BaseAdapter;
import com.guohead.sdk.GHView;
import com.guohead.sdk.utils.Logger;
import com.guohead.sdk.utils.Utils;

public class DomobAdapter extends BaseAdapter implements
		cn.domob.android.ads.DomobAdListener {

	private DomobAdView mAdView;

	public DomobAdapter(GHView view, String params) {
		super(view, params, "Domob");
	}

	@Override
	public void loadAd() {
		super.loadAd();
		String adSize = null;
		if(keys.length<3){
			adSize = DomobAdView.INLINE_SIZE_320X50;
		}else{
			if(keys[2].equals(DomobAdView.INLINE_SIZE_320X50)){
				adSize = DomobAdView.INLINE_SIZE_320X50;
			}else if(keys[2].equals(DomobAdView.INLINE_SIZE_300X250)){
				adSize = DomobAdView.INLINE_SIZE_300X250;
			}else {
				adSize = DomobAdView.INLINE_SIZE_320X50;
			}
		}
		
		mAdView = new DomobAdView(mGHView.getActivity(),keys[0],adSize);
		mAdView.setRefreshable(false);
		mAdView.setId(Utils.TYPE_DOMOB);
		mAdView.setOnAdListener(this);
		addView(mAdView);
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

	@Override
	public void onClick() {
	}

	@Override
	public void onFailedToReceiveFreshAd(DomobAdView arg0) {
		failedReceiveAd(mAdView);
	}

	@Override
	public void onReceivedFreshAd(DomobAdView arg0) {
		Logger.i("domob   onReceivedFreshAd......");
		receiveAd(mAdView);
	}

	@Override
	public void onLandingPageClose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLandingPageOpening() {
		// TODO Auto-generated method stub
		
	}
}
