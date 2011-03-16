package netserv.apps.activestreaming.module;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Serialization of this object !
 * 
 * @author aman
 * 
 */
public class ActiveStreamMap {

	private Map<String, CacheVideo> urlMap;

	private ActiveStreamMap() {
		urlMap = Collections.synchronizedMap(new HashMap<String, CacheVideo>());
	}

	/**
	 * SingletonHolder is loaded on the first execution of
	 * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
	 * not before.
	 */
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
		public static final int VOD = 3;
		public static final String LOCALROOT = "./cached-video";

		private URL videourl = null;
		private int currentFilePointer = 0;
		private int state;
		private File cacheFile;
		private long totalByteSaved = 0;

		/**
		 * only the parent can instantiate this class
		 * 
		 * @param url
		 */
		private CacheVideo(String url) {
			try {
				videourl = new URL(url);
			} catch (Exception e) {
				Util.print("Problem casting URL - " + url);
				Util.print(e.toString());
			}
			cacheFile = getFileForURL();
			state = INITIAL;
		}

		/**
		 * Ideally we should check whether the url is streaming We assuming thee
		 * stream is present
		 * 
		 * @return
		 */
		public File getFileForURL() {

			String host = videourl.getHost();
			String path = videourl.getPath();
			path = path.replaceAll("~", "");
			String cache = LOCALROOT + "/" + host + path;
			Util.print("loading local file :" + cache);
			return new File(cache);
		}

		public int getCurrentFilePointer() {
			return currentFilePointer;
		}

		public void setCurrentFilePointer(int currentFilePointer) {
			this.currentFilePointer = currentFilePointer;
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
			this.totalByteSaved += this.totalByteSaved + count; 
		}
	}
}