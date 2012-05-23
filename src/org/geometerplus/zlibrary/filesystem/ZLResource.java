package org.geometerplus.zlibrary.filesystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.zlibrary.xml.ZLStringMap;
import org.geometerplus.zlibrary.xml.ZLXMLReaderAdapter;

public class ZLResource {
	public final String Name;
	
	public static ZLResource resource(String key) {
		ZLResource.buildTree();
		if (ZLResource.ourRoot == null) {
			return null;
		}
		return ZLResource.ourRoot.getResource(key);
	}

	protected ZLResource(String name) {
		Name = name;
	}

	private static interface Condition {
		abstract boolean accepts(int number);
	}

	private static class ValueCondition implements Condition {
		private final int myValue;

		ValueCondition(int value) {
			myValue = value;
		}

		@Override
		public boolean accepts(int number) {
			return myValue == number;
		}
	}

	private static class RangeCondition implements Condition {
		private final int myMin;
		private final int myMax;

		RangeCondition(int min, int max) {
			myMin = min;
			myMax = max;
		}

		@Override
		public boolean accepts(int number) {
			return myMin <= number && number <= myMax;
		}
	}
	
	private static class ModRangeCondition implements Condition {
		private final int myMin;
		private final int myMax;
		private final int myBase;

		ModRangeCondition(int min, int max, int base) {
			myMin = min;
			myMax = max;
			myBase = base;
		}

		@Override
		public boolean accepts(int number) {
			number = number % myBase;
			return myMin <= number && number <= myMax;
		}
	}

	private static class ModCondition implements Condition {
		private final int myMod;
		private final int myBase;

		ModCondition(int mod, int base) {
			myMod = mod;
			myBase = base;
		}

		@Override
		public boolean accepts(int number) {
			return number % myBase == myMod;
		}
	}

	static private Condition parseCondition(String description) {
		final String[] parts = description.split(" ");
		try {
			if ("range".equals(parts[0])) {
				return new RangeCondition(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
			} else if ("mod".equals(parts[0])) {
				return new ModCondition(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
			} else if ("modrange".equals(parts[0])) {
				return new ModRangeCondition(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
			} else if ("value".equals(parts[0])) {
				return new ValueCondition(Integer.parseInt(parts[1]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	static volatile ZLResource ourRoot;
	private static final Object ourLock = new Object();

    private static long ourTimeStamp = 0;
    private static String ourLanguage = null;
    private static String ourCountry = null;

	private boolean myHasValue;
	private	String myValue;
	private HashMap<String,ZLResource> myChildren;
	private LinkedHashMap<Condition,String> myConditionalValues;
	
	static void buildTree() {
		synchronized (ourLock) {
			if (ourRoot == null) {
				ourRoot = new ZLResource("", null);
				ourLanguage = "en";
				ourCountry = "UK";
				loadData();
			}
		}
	}

    private static void updateLanguage() {
        final long timeStamp = System.currentTimeMillis();
        if (timeStamp > ourTimeStamp + 1000) {
            synchronized (ourLock) {
                if (timeStamp > ourTimeStamp + 1000) {
					ourTimeStamp = timeStamp;
        			final String language = Locale.getDefault().getLanguage();
        			final String country = Locale.getDefault().getCountry();
					if ((language != null && !language.equals(ourLanguage)) ||
						(country != null && !country.equals(ourCountry))) {
						ourLanguage = language;
						ourCountry = country;
						loadData();
					}
				}
			}
		}
    }
	
	private static void loadData(ResourceTreeReader reader, String fileName) {
		reader.readDocument(ourRoot, FBReaderApp.Instance().createResourceFile("resources/zlibrary/" + fileName));
		reader.readDocument(ourRoot, FBReaderApp.Instance().createResourceFile("resources/application/" + fileName));
	}

	private static void loadData() {
		ResourceTreeReader reader = new ResourceTreeReader();
		loadData(reader, ourLanguage + ".xml");
		loadData(reader, ourLanguage + "_" + ourCountry + ".xml");
	}

	private	ZLResource(String name, String value) {
		this(name);
		setValue(value);
	}
	
	private void setValue(String value) {
		myHasValue = value != null;
		myValue = value;
	}
	
	public boolean hasValue() {
		return myHasValue;
	}
	
	public String getValue() {
		updateLanguage();
		return myHasValue ? myValue : null;
	}

	public String getValue(int number) {
		updateLanguage();
		if (myConditionalValues != null) {
			for (Map.Entry<Condition,String> entry: myConditionalValues.entrySet()) {
				if (entry.getKey().accepts(number)) {
					return entry.getValue();
				}
			}
		}
		return myHasValue ? myValue : null;
	}

	public ZLResource getResource(String key) {
		final HashMap<String,ZLResource> children = myChildren;
		if (children != null) {
			ZLResource child = children.get(key);
			if (child != null) {
				return child;
			}
		}
		return null;
	}
		
	private static class ResourceTreeReader extends ZLXMLReaderAdapter {
		private static final String NODE = "node"; 
		private final ArrayList<ZLResource> myStack = new ArrayList<ZLResource>();
		
		public void readDocument(ZLResource root, ZLFile file) {
			myStack.clear();
			myStack.add(root);
			readQuietly(file);
		}

		public boolean dontCacheAttributeValues() {
			return true;
		}

		public boolean startElementHandler(String tag, ZLStringMap attributes) {
			final ArrayList<ZLResource> stack = myStack;
			if (!stack.isEmpty() && (NODE.equals(tag))) {
				final String name = attributes.getValue("name");
				final String condition = attributes.getValue("condition");
				final String value = attributes.getValue("value");
				final ZLResource peek = stack.get(stack.size() - 1);
				if (name != null) {
					ZLResource node;
					HashMap<String,ZLResource> children = peek.myChildren;
					if (children == null) {
						node = null;
						children = new HashMap<String,ZLResource>();
						peek.myChildren = children;
					} else {
						node = children.get(name);
					}
					if (node == null) {
						node = new ZLResource(name, value);
						children.put(name, node);
					} else {
						if (value != null) {
							node.setValue(value);
							node.myConditionalValues = null;
						}
					}
					stack.add(node);
				} else if (condition != null && value != null) {
					final Condition compiled = parseCondition(condition);
					if (compiled != null) {
						if (peek.myConditionalValues == null) {
							peek.myConditionalValues = new LinkedHashMap<Condition,String>();
						}
						peek.myConditionalValues.put(compiled, value);
					}
					stack.add(peek);
				}
			}
			return false;
		}

		public boolean endElementHandler(String tag) {
			final ArrayList<ZLResource> stack = myStack;
			if (!stack.isEmpty() && (NODE.equals(tag))) {
				stack.remove(stack.size() - 1);
			}
			return false;
		}
	}
}
