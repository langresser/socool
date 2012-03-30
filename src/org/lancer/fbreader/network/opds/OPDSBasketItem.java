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

package org.lancer.fbreader.network.opds;

import java.util.List;


import org.lancer.fbreader.network.BasketItem;
import org.lancer.fbreader.network.tree.NetworkItemsLoader;
import org.lancer.fbreader.network.urlInfo.*;
import org.lancer.zlibrary.core.network.ZLNetworkException;
import org.lancer.zlibrary.core.util.ZLMiscUtil;

class OPDSBasketItem extends BasketItem {
	OPDSBasketItem(OPDSNetworkLink link) {
		super(link);
	}

	@Override
	public void loadChildren(NetworkItemsLoader loader) throws ZLNetworkException {
		final List<String> ids = bookIds();
		if (ids.isEmpty()) {
			return;
		}

		if (isFullyLoaded()) {
			for (String id : ids) {
				loader.onNewItem(getBook(id));
			}
			loader.getTree().confirmAllItems();
			return;
		}

		final OPDSNetworkLink opdsLink = (OPDSNetworkLink)Link;
		String url = opdsLink.getUrl(UrlInfo.Type.ListBooks);
		if (url == null) {
			return;
		}
		url = url.replace("{ids}", ZLMiscUtil.listToString(ids, ","));

		final OPDSCatalogItem.State state = opdsLink.createOperationData(loader);
		doLoadChildren(state, opdsLink.createNetworkData(url, state));
	}
}
