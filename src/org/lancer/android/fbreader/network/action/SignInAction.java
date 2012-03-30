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


import org.lancer.android.fbreader.network.Util;
import org.lancer.fbreader.network.NetworkTree;
import org.lancer.fbreader.network.authentication.NetworkAuthenticationManager;
import org.lancer.fbreader.network.tree.NetworkCatalogRootTree;

public class SignInAction extends Action {
	public SignInAction(Activity activity) {
		super(activity, ActionCode.SIGNIN, "signIn", -1);
	}

	@Override
	public boolean isVisible(NetworkTree tree) {
		if (!(tree instanceof NetworkCatalogRootTree)) {
			return false;
		}

		final NetworkAuthenticationManager mgr = tree.getLink().authenticationManager();
		return mgr != null && !mgr.mayBeAuthorised(false);
	}

	@Override
	public void run(NetworkTree tree) {
		Util.runAuthenticationDialog(myActivity, tree.getLink(), null);
	}
}
