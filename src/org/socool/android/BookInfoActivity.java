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
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.*;

import org.socool.socoolreader.yhyxcs.R;

import org.socool.android.util.UIUtil;
import org.socool.screader.Paths;
import org.socool.screader.screader.FBReaderApp;
import org.socool.screader.bookmodel.BookChapter;
import org.socool.screader.library.*;
import org.socool.zlibrary.text.ZLTextPosition;

import com.umeng.analytics.MobclickAgent;
//import com.waps.AppConnect;

public class BookInfoActivity extends Activity {
	public static final String CURRENT_BOOK_PATH_KEY = "CurrentBookPath";
	ExpandableListView m_chapter;
	BookChapterJuanAdapter m_adapter;
	TextView m_bookIntro;
	TextView m_bookProgress;
	Button m_btnChapter;
	Button m_btnIntro;
	Button m_btnOpen;
	
	BookChapter m_bookChapter;
	Book m_currentBook;
	
	static final int BOOK_INFO_CHAPTER = 0;
	static final int BOOK_INFO_INTRO = 1;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		// 友盟
		try {
			MobclickAgent.onError(this);
			
			// 万普
//			AppConnect.getInstance(BookInfoActivity.this);
//			FBReaderApp.Instance().initOfferWall(this);
		} catch (Exception e) {
			e.printStackTrace();
			MobclickAgent.reportError(this, "init umeng error");
		}

		final String currentBookPath = "book/yhyxcs";//getIntent().getStringExtra(CURRENT_BOOK_PATH_KEY);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.book_info);
		
		m_btnOpen = (Button)findViewById(R.id.book_info_button_open);
		final ImageButton btnApp = (ImageButton)findViewById(R.id.book_info_button_app);
		final Button btnImport = (Button)findViewById(R.id.book_info_button_import);
		
		m_currentBook = Book.getByPath(currentBookPath);	
		BookState state = m_currentBook.getStoredPosition();
		
		View.OnClickListener listener = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (v == m_btnOpen) {
					startActivity(
							new Intent(getApplicationContext(), SCReaderActivity.class)
								.setAction(Intent.ACTION_VIEW)
								.putExtra(SCReaderActivity.BOOK_PATH_KEY, currentBookPath)
								.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
						);
				} else if (v == btnApp) {
					FBReaderApp.Instance().showFetureApp(BookInfoActivity.this);
				} else if (v == btnImport) {
					MobclickAgent.onEvent(BookInfoActivity.this, "import", "onclickbtn");
					final int currentPoints = FBReaderApp.Instance().getOfferPoints(BookInfoActivity.this);
					if (currentPoints < FBReaderApp.IMPORT_BOOK_POINT) {
						String text = String.format("只要消耗 %1d积分（当前积分%2d），您就可以把内置电子书导出到SD卡中，使用您最喜欢的阅读器进行阅读。\n您可以通过下载推荐应用的方式免费获取积分", FBReaderApp.IMPORT_BOOK_POINT, currentPoints);
						Dialog dialog = new AlertDialog.Builder(BookInfoActivity.this).setTitle("积分不足").setMessage(text)
								.setPositiveButton("确定",
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int whichButton) {
												dialog.cancel();
											}
										}).setNegativeButton("推荐应用",
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												FBReaderApp.Instance().showOfferWall(BookInfoActivity.this);
												dialog.cancel();
											}
										}).create();// 创建按钮
						dialog.show();
					} else {
//						String text = "导出电子书";
						String text = String.format("导出电子书将会消耗%1d积分（当前积分%2d）", FBReaderApp.IMPORT_BOOK_POINT, currentPoints);
						LayoutInflater inflater = LayoutInflater.from(BookInfoActivity.this);
				        final View view = inflater.inflate(R.layout.alert_dialog_edit, null);
				        final EditText edit = (EditText)view.findViewById(R.id.alert_dialog_edit);
				        edit.setText(Paths.BooksDirectoryOption().getValue());
				        final EditText editFile = (EditText)view.findViewById(R.id.alert_dialog_edit_file);
				        editFile.setText(m_currentBook.myTitle + ".txt");

						Dialog dialog = new AlertDialog.Builder(BookInfoActivity.this).setTitle(text).setView(view)
								.setPositiveButton("确定",
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int whichButton) {
												String pathDir = edit.getText().toString();
												String destFile = editFile.getText().toString();
												if (pathDir.length() <= 1) {
													UIUtil.showMessageText(BookInfoActivity.this, "路径错误");
													return;
												}
												
												if (destFile.length() < 1) {
													UIUtil.showMessageText(BookInfoActivity.this, "文件名错误");
													return;
												}
								
												FBReaderApp.Instance().importBook(BookInfoActivity.this,
														currentBookPath, pathDir, destFile);
												dialog.cancel();
											}
										}).setNegativeButton("取消",
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												dialog.cancel();
											}
										}).create();// 创建按钮
						dialog.show();
					}			
				}
			}
		};
		
		m_btnOpen.setOnClickListener(listener);
		btnApp.setOnClickListener(listener);
		btnImport.setOnClickListener(listener);
		
		m_bookChapter = FBReaderApp.Instance().loadChapter(currentBookPath);
		if (state != null) {
			startActivity(new Intent(getApplicationContext(), SCReaderActivity.class)
						.setAction(Intent.ACTION_VIEW)
						.putExtra(SCReaderActivity.BOOK_PATH_KEY, currentBookPath)
						.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		}
		
		m_chapter = (ExpandableListView)findViewById(R.id.book_info_chapterjuan);
		m_bookIntro = (TextView)findViewById(R.id.book_info_intro_text);
		m_bookProgress = (TextView)findViewById(R.id.book_info_progress);
		
		TextView bookInfoTitle = (TextView)findViewById(R.id.book_info_title);
		TextView bookInfoAuthor = (TextView)findViewById(R.id.book_info_author);
		
		bookInfoTitle.setText("书名：" + m_currentBook.myTitle);
		bookInfoAuthor.setText("作者：" + m_currentBook.m_bookAuthor);
		
		MobclickAgent.onEvent(this, "openBook", m_currentBook.myTitle);
		
		m_bookIntro.setText(m_currentBook.m_bookIntro + '\n' + m_currentBook.m_bookAuthorIntro);
		m_bookIntro.setMovementMethod(ScrollingMovementMethod.getInstance());
		
		m_adapter = new BookChapterJuanAdapter(this, m_bookChapter, -1, currentBookPath);
		m_chapter.setAdapter(m_adapter);
		m_chapter.setOnChildClickListener(m_adapter);
		
		m_btnChapter = (Button)findViewById(R.id.book_info_btn_chapter);
		m_btnIntro = (Button)findViewById(R.id.book_info_btn_intro);
		
		m_btnChapter.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				gotoPage(BOOK_INFO_CHAPTER);
			}
		});
		m_btnIntro.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				gotoPage(BOOK_INFO_INTRO);
			}
		});
		
		gotoPage(BOOK_INFO_CHAPTER);
	}
	
	
	
	public void gotoPage(int page)
	{
		if (page == BOOK_INFO_CHAPTER) {
			m_btnChapter.setSelected(true);
			m_btnIntro.setSelected(false);
			m_chapter.setVisibility(View.VISIBLE);
			m_bookIntro.setVisibility(View.INVISIBLE);
		} else if (page == BOOK_INFO_INTRO) {
			m_btnChapter.setSelected(false);
			m_btnIntro.setSelected(true);
			m_chapter.setVisibility(View.INVISIBLE);
			m_bookIntro.setVisibility(View.VISIBLE);
		}
	}

	public void onDestroy()
	{
		FBReaderApp.Instance().releaseOfferWall(this);
		super.onDestroy();
	}
	
	public void onResume() {
	    super.onResume();
	    
	    BookState state = m_currentBook.getStoredPosition();
	    int chapterIndex = -1;
	    if (state != null) {
	    	final int paragraphIndex = state.m_textPosition.getParagraphIndex();
			chapterIndex = m_bookChapter.getChapterIndexByParagraph(paragraphIndex);
			m_btnOpen.setText("继续阅读");
			m_bookProgress.setText(String.format("阅读进度：%1$.2f%%", state.m_lastReadPercent / 100.0));
	    } else {
	    	m_btnOpen.setText("开始阅读");
			m_bookProgress.setText("阅读进度：0%");
	    }
	    
	    if (m_adapter.m_currentChapter != chapterIndex) {
	    	m_adapter.setCurrentChapter(chapterIndex);
	    	m_adapter.notifyDataSetChanged();
	    }

	    MobclickAgent.onResume(this);
	}
	public void onPause() {
	    super.onPause();
	    MobclickAgent.onPause(this);
	}
}
