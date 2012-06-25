/*
 * Copyright (C) 2009-2012 Geometer Plus <contact@geometerplus.com>
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

package org.socool.screader.library;

import org.socool.zlibrary.filesystem.ZLResource;

public class FirstLevelTree extends LibraryTree {
	private final String myId;
	private final ZLResource myResource;

	FirstLevelTree(RootTree root, int position, String id) {
		super(root, position);
		myId = id;
		myResource = ZLResource.resource("library").getResource(myId);
	}

	public FirstLevelTree(RootTree root, String id) {
		super(root);
		myId = id;
		myResource = ZLResource.resource("library").getResource(myId);
	}

	@Override
	public String getName() {
		return myResource.getValue();
	}

	@Override
	public String getTreeTitle() {
		return getSummary();
	}

	@Override
	public String getSummary() {
		return myResource.getResource("summary").getValue();
	}

	@Override
	protected String getStringId() {
		return myId;
	}

	@Override
	public boolean isSelectable() {
		return false;
	}
}
