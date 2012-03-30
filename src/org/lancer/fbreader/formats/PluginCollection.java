package org.lancer.fbreader.formats;

import java.util.*;


import org.lancer.fbreader.filetype.*;
import org.lancer.fbreader.formats.fb2.FB2Plugin;
import org.lancer.fbreader.formats.oeb.OEBPlugin;
import org.lancer.fbreader.formats.pdb.MobipocketPlugin;
import org.lancer.zlibrary.core.filesystem.ZLFile;

public class PluginCollection {
	static {
		System.loadLibrary("NativeFormats-v2");
	}

	private static PluginCollection ourInstance;

	private final Map<FormatPlugin.Type,List<FormatPlugin>> myPlugins =
		new HashMap<FormatPlugin.Type,List<FormatPlugin>>();

	public static PluginCollection Instance() {
		if (ourInstance == null) {
			ourInstance = new PluginCollection();

			// This code can not be moved to constructor because nativePlugins() is a native method
			for (NativeFormatPlugin p : ourInstance.nativePlugins()) {
				ourInstance.addPlugin(p);
				System.err.println("native plugin: " + p);
			}
		}
		return ourInstance;
	}

	public static void deleteInstance() {
		if (ourInstance != null) {
			ourInstance = null;
		}
	}

	private PluginCollection() {
		addPlugin(new FB2Plugin());
		addPlugin(new MobipocketPlugin());
		addPlugin(new OEBPlugin());
	}

	private void addPlugin(FormatPlugin plugin) {
		final FormatPlugin.Type type = plugin.type();
		List<FormatPlugin> list = myPlugins.get(type);
		if (list == null) {
			list = new ArrayList<FormatPlugin>();
			myPlugins.put(type, list);
		}
		list.add(plugin);
	}

	public FormatPlugin getPlugin(ZLFile file) {
		return getPlugin(file, FormatPlugin.Type.ANY);
	}

	public FormatPlugin getPlugin(ZLFile file, FormatPlugin.Type formatType) {
		final FileType fileType = FileTypeCollection.Instance.typeForFile(file);
		return getPlugin(fileType, formatType);
	}

	public FormatPlugin getPlugin(FileType fileType, FormatPlugin.Type formatType) {
		if (fileType == null) {
			return null;
		}

		if (formatType == FormatPlugin.Type.ANY) {
			FormatPlugin p = getPlugin(fileType, FormatPlugin.Type.JAVA);
			if (p == null) {
				p = getPlugin(fileType, FormatPlugin.Type.NATIVE);
			}
			return p;
		} else {
			final List<FormatPlugin> list = myPlugins.get(formatType);
			if (list == null) {
				return null;
			}
			for (FormatPlugin p : list) {
				if (fileType.Id.equalsIgnoreCase(p.supportedFileType())) {
					return p;
				}
			}
			return null;
		}
	}

	private native NativeFormatPlugin[] nativePlugins();
	private native void free();

	protected void finalize() throws Throwable {
		free();
		super.finalize();
	}
}
