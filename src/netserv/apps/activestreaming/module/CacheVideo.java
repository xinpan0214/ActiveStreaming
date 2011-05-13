package netserv.apps.activestreaming;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import netserv.apps.activestreaming.ActiveStreamNode.VideoWriter;


/**
 * Each video URL instantiates this class object, This class stores all the
 * information related to a URL.
 */

public class CacheVideo {
	/* cache video file states */
	public static final int NOT_PRESENT = 0;
	public static final int INITIAL = 1;
	public static final int LIVE = 2;
	public static final int LOCAL = 3;
	public static final int STORAGE_THRESHOLD = 2;
	public static final String LOCALROOT = "./cached-video";

	public InputStream originInputStream;
	public VideoWriter writerInstance;

	// video buffer
	public byte[] videoBuffer;
	public Long writerFrame = new Long(0);
	public boolean onceFilled;

	// total active connections for this video url
	public Integer activeConn = new Integer(0);

	private URL videoURL = null;
	private int state;
	private File cacheFile;
	private boolean storeCache;
	private long totalByteSaved = 0;

	CacheVideo(String url) {
		videoBuffer = new byte[ActiveStreamNode.FRAMES
				* ActiveStreamNode.FRAME_SIZE];
		try {
			videoURL = new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		cacheFile = initializeURL(url);
		this.state = INITIAL;
	}

	/**
	 * Ideally we should check whether the URL is streaming. We assuming the
	 * stream is present.
	 */
	private File initializeURL(String url) {

		String host = videoURL.getHost();
		String path = videoURL.getPath();
		path = path.replaceAll("~", "");
		String cache = LOCALROOT + "/" + host + path;
		cacheFile = new File(cache);
		cacheFile.getParentFile().mkdirs();
		return cacheFile;
	}

	public boolean isStoreCache() {
		return storeCache;
	}

	public void setStoreCache(boolean storeCache) {
		this.storeCache = storeCache;
	}

	public URL getVideoURL() {
		return videoURL;
	}

	public void setVideoURL(URL videoURL) {
		this.videoURL = videoURL;
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

	public void incrementTotalBytes(long count) {
		this.totalByteSaved += count;
	}
}