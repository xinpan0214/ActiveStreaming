package netserv.apps.activestreaming;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

	public CacheVideo getVideo(String url) {
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
}