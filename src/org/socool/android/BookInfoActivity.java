/*
 * Copyright (C) 2010-2012 Geometer Plus <contact@geometerplus.com>
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.*;

import org.socool.socoolreader.mcnxs.R;

import org.socool.android.util.UIUtil;
import org.socool.screader.Paths;
import org.socool.screader.screader.FBReaderApp;
import org.socool.screader.library.*;
import org.socool.zlibrary.text.ZLTextPosition;

import com.umeng.analytics.MobclickAgent;

public class BookInfoActivity extends Activity {
	public static final String CURRENT_BOOK_PATH_KEY = "CurrentBookPath";
	private String m_currentBookPath;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		// ����
		MobclickAgent.updateOnlineConfig(this);
		MobclickAgent.onError(this);
		
		FBReaderApp.Instance().initOfferWall(this);

		m_currentBookPath = getIntent().getStringExtra(CURRENT_BOOK_PATH_KEY);
		if (m_currentBookPath == null) {
			m_currentBookPath = "book/mcnxs";
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.book_info);
		
		final Button btnOpen = (Button)findViewById(R.id.book_info_button_open);
		final Button btnApp = (Button)findViewById(R.id.book_info_button_app);
		final Button btnImport = (Button)findViewById(R.id.book_info_button_import);
		
		View.OnClickListener listener = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (v == btnOpen) {
					startActivity(
							new Intent(getApplicationContext(), SCReaderActivity.class)
								.setAction(Intent.ACTION_VIEW)
								.putExtra(SCReaderActivity.BOOK_PATH_KEY, m_currentBookPath)
								.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
						);
				} else if (v == btnApp) {
					FBReaderApp.Instance().showOfferWall(BookInfoActivity.this);
				} else if (v == btnImport) {
					MobclickAgent.onEvent(BookInfoActivity.this, "import", "onclickbtn");
					final int currentPoints = FBReaderApp.Instance().getOfferPoints(BookInfoActivity.this);
					if (currentPoints < FBReaderApp.IMPORT_BOOK_POINT) {
						String text = String.format("ֻҪ���� %1d���֣���ǰ����%2d�������Ϳ��԰����õ����鵼����SD���У�ʹ������ϲ�����Ķ��������Ķ���\n������ͨ�������Ƽ�Ӧ�õķ�ʽ��ѻ�ȡ����", FBReaderApp.IMPORT_BOOK_POINT, currentPoints);
						Dialog dialog = new AlertDialog.Builder(BookInfoActivity.this).setTitle("���ֲ���").setMessage(text)
								.setPositiveButton("ȷ��",
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int whichButton) {
												dialog.cancel();
											}
										}).setNegativeButton("�Ƽ�Ӧ��",
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												FBReaderApp.Instance().showOfferWall(BookInfoActivity.this);
												dialog.cancel();
											}
										}).create();// ������ť
						dialog.show();
					} else {
						String text = "����������";
//						String text = String.format("���������齫������%1d���֣���ǰ����%2d��", FBReaderApp.IMPORT_BOOK_POINT, currentPoints);
						LayoutInflater inflater = LayoutInflater.from(BookInfoActivity.this);
				        final View view = inflater.inflate(R.layout.alert_dialog_edit, null);
				        final EditText edit = (EditText)view.findViewById(R.id.alert_dialog_edit);
				        edit.setText(Paths.BooksDirectoryOption().getValue());
				        final EditText editFile = (EditText)view.findViewById(R.id.alert_dialog_edit_file);
				        editFile.setText("������Щ�¶�.txt");
				        
						Dialog dialog = new AlertDialog.Builder(BookInfoActivity.this).setTitle(text).setView(view)
								.setPositiveButton("ȷ��",
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int whichButton) {
												String pathDir = edit.getText().toString();
												String destFile = editFile.getText().toString();
												if (pathDir.length() <= 1) {
													UIUtil.showMessageText(BookInfoActivity.this, "·������");
													return;
												}
												
												if (destFile.length() < 1) {
													UIUtil.showMessageText(BookInfoActivity.this, "�ļ�������");
													return;
												}
								
												FBReaderApp.Instance().importBook(BookInfoActivity.this,
														m_currentBookPath, pathDir, destFile);
												dialog.cancel();
											}
										}).setNegativeButton("ȡ��",
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												dialog.cancel();
											}
										}).create();// ������ť
						dialog.show();
					}			
				}
				
			}
		};
		
		btnOpen.setOnClickListener(listener);
		btnApp.setOnClickListener(listener);
		btnImport.setOnClickListener(listener);
		
		final Book book = Book.getByPath(m_currentBookPath);
		ZLTextPosition position = book.getStoredPosition();
		if (position != null) {
			startActivity(
					new Intent(getApplicationContext(), SCReaderActivity.class)
						.setAction(Intent.ACTION_VIEW)
						.putExtra(SCReaderActivity.BOOK_PATH_KEY, m_currentBookPath)
						.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
				);
		}
	}
	
	public void onResume() {
	    super.onResume();
	    MobclickAgent.onResume(this);
	}
	public void onPause() {
	    super.onPause();
	    MobclickAgent.onPause(this);
	}
}
