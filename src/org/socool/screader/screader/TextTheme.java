package org.socool.screader.screader;

import org.socool.zlibrary.util.ZLColor;

public class TextTheme {
	public String m_path;				// 主题文件夹路径，作为id
	public String m_title = null;				// 显示的名字
	public String m_imagePath = null;			// 背景图片位置
	public String m_thumbPath = null;			// 缩略图片位置(可选)
	public ZLColor m_textColor = null;			// 文字颜色
	public ZLColor m_bgColor = null;			// 背景颜色
	public ZLColor m_selectTextColor = null;	// 选择的文本颜色
	public ZLColor m_selectBgColor = null;		// 选择的背景颜色
	public boolean m_isNightMode = false;		// 这个主题是否是夜间模式
}
