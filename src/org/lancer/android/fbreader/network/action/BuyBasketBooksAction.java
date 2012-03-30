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

import java.util.*;

import android.app.Activity;


import org.lancer.android.fbreader.network.BuyBooksActivity;
import org.lancer.fbreader.network.*;
import org.lancer.fbreader.network.tree.*;
import org.lancer.fbreader.tree.FBTree;

public class BuyBasketBooksAction extends CatalogAction {
	public BuyBasketBooksAction(Activity activity) {
		super(activity, ActionCode.BASKET_BUY_ALL_BOOKS, "buyAllBooks");
	}

	@Override
	public boolean isVisible(NetworkTree tree) {
		return tree instanceof BasketCatalogTree && ((BasketCatalogTree)tree).canBeOpened();
	}

	@Override
	public boolean isEnabled(NetworkTree tree) {
		if (NetworkLibrary.Instance().getStoredLoader(tree) != null) {
			return false;
		}
		final Set<String> bookIds = new HashSet<String>();
		for (FBTree t : tree.subTrees()) {
			if (t instanceof NetworkBookTree) {
				bookIds.add(((NetworkBookTree)t).Book.Id);
			}
		}
		final BasketItem item = (BasketItem)((BasketCatalogTree)tree).Item;
		return bookIds.equals(new HashSet(item.bookIds()));
	}

	@Override
	public void run(NetworkTree tree) {
		final ArrayList<NetworkBookTree> bookTrees = new ArrayList<NetworkBookTree>();
		for (FBTree t : tree.subTrees()) {
			if (t instanceof NetworkBookTree) {
				bookTrees.add((NetworkBookTree)t);
			}
		}
		BuyBooksActivity.run(myActivity, bookTrees);
	}
}
