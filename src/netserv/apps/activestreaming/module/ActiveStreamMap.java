package netserv.apps.activestreaming.module;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import netserv.apps.activestreaming.module.NetServNode.Writer;

public class ActiveStreamMap {

	private Map<String, CacheVideo> urlMap;

	private ActiveStreamMap() {
		urlMap = Collections.synchronizedMap(new HashMap<String, CacheVideo>());
	}

	private static class SingletonHolder {
		public static final ActiveStreamMap INSTANCE = new ActiveStreamMap();
	}

	public static ActiveStreamMap getInstance() {
		return SingletonHolder.INSTANCE;
	}

	public CacheVideo getVideo(String url){
		return urlMap.get(url);
	}
	
	public void setState(String url, int state) {
		CacheVideo cv = urlMap.get(url);
		if (cv != null)
			cv.setState(state);
	}

	public int getState(String url) {
		CacheVideo cv = urlMap.get(url);
		if (cv != null)
			return cv.getState();
		else
			return 0;
	}

	public CacheVideo addURL(String url) {
		CacheVideo cv = urlMap.get(url);
		if (cv == null) {
			cv = new CacheVideo(url);
			urlMap.put(url, cv);
		}
		return cv;
	}

	public void removeURL(String n) {
		urlMap.remove(n);
	}

	public class CacheVideo {
		/** cache video file states */
		public static final int NOT_PRESENT = 0;
		public static final int INITIAL = 1;
		public static final int LIVE = 2;
		public static final int LOCAL = 3;
		public static final int STORAGE_THRESHOLD = 0;
		public static final String LOCALROOT = "./cached-video";
		
		public InputStream originInputStream;
		public Writer writerInstance;
		// total active connections for this video url
		public int activeConn; 
		
		private URL videourl = null;
		private int state;
		private File cacheFile;
		private long totalByteSaved = 0;

		private CacheVideo(String url) {
			cacheFile = getFileForURL(url);
			this.state = INITIAL;
		}

		/**
		 * Ideally we should check whether the url is streaming We assuming thee
		 * stream is present
		 */
		public File getFileForURL(String url) {
			try {
				videourl = new URL(url);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			String host = videourl.getHost();
			String path = videourl.getPath();
			path = path.replaceAll("~", "");
			String cache = LOCALROOT + "/" + host +path;
			File c = new File(cache);
			c.getParentFile().mkdirs();
			return c;
		}

		public int getState() {
			return state;
		}

		public void setState(int currentState) {
			this.state = currentState;
		}

		public File getCacheFile() {
			return cacheFile;
		}

		public void setCacheFile(File cacheFile) {
			this.cacheFile = cacheFile;
		}

		public long getTotalByteSaved() {
			return totalByteSaved;
		}

		public void setTotalByteSaved(long totalByteSaved) {
			this.totalByteSaved = totalByteSaved;
		}
		public void incrementTotalBytes(long count){
			this.totalByteSaved += count; 
		}
	}
}