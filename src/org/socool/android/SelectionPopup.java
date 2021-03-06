/*
 * Copyright (C) 2007-2012 Geometer Plus <contact@geometerplus.com>
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

package org.socool.android;

import android.view.View;
import android.widget.RelativeLayout;

import org.socool.screader.screader.ActionCode;
import org.socool.screader.screader.FBReaderApp;
import org.socool.socoolreader.mcnxs.R;

public class SelectionPopup extends ButtonsPopupPanel {
	public final static String ID = "SelectionPopup";

	public SelectionPopup() {
		super();
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void createControlPanel(SCReaderActivity activity, RelativeLayout root) {
		if (myWindow != null && activity == myWindow.getActivity()) {
			return;
		}

		myWindow = new PopupWindow(activity, root, PopupWindow.Location.Floating, false);

        addButton(ActionCode.SELECTION_COPY_TO_CLIPBOARD, true, "����");
        addButton(ActionCode.SELECTION_BOOKMARK, true, "��ժ");
//        addButton(ActionCode.SELECTION_SHARE, true, "����");
//        addButton(ActionCode.SELECTION_SEARCH, true, "����");
//        addButton(ActionCode.SELECTION_TRANSLATE, true, "�ʵ�");
    }
    
    public void move(int selectionStartY, int selectionEndY) {
		if (myWindow == null) {
			return;
		}

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
		);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

        final int verticalPosition;
        final int screenHeight = ((View)myWindow.getParent()).getHeight();
        
        if (screenHeight - selectionEndY < screenHeight / 4) {
        	verticalPosition = RelativeLayout.CENTER_VERTICAL;
        } else {
        	verticalPosition = RelativeLayout.ALIGN_PARENT_BOTTOM;
        }

        layoutParams.addRule(verticalPosition);
        myWindow.setLayoutParams(layoutParams);
    }

	@Override
	public void run() {
		
	}
}
