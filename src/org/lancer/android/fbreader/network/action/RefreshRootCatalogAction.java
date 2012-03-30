/*
 * Copyright (C) 2010-2012 Geometer Plus <wangjiatc@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.lancer.android.fbreader.network.action;

import android.app.Activity;



import org.lancer.socoolreader.R;

import org.lancer.android.util.UIUtil;
import org.lancer.fbreader.network.NetworkLibrary;
import org.lancer.fbreader.network.NetworkTree;
import org.lancer.zlibrary.core.network.ZLNetworkException;

public class RefreshRootCatalogAction extends RootAction {
	public RefreshRootCatalogAction(Activity activity) {
		super(activity, ActionCode.REFRESH, "refreshCatalogsList", R.drawable.ic_menu_refresh);
	}

	@Override
	public boolean isEnabled(NetworkTree tree) {
		return !NetworkLibrary.Instance().isUpdateInProgress();
	}

	@Override
	public void run(NetworkTree tree) {
		NetworkLibrary.Instance().runBackgroundUpdate(true);
	}
}
