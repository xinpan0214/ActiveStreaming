package netserv.apps.activestreaming.module;

import java.io.File;

import netserv.apps.activestreaming.module.ActiveStreamMap.CacheVideo;

public class Test {
	ActiveStreamMap singleton = ActiveStreamMap.getInstance();
	private static final int BUF_SIZE = 1024;
	String url = "http://192.168.15.4:8080";

	public class Reader implements Runnable {
		public void run() {
			CacheVideo cacheVideo = singleton.addURL(url);
			File cacheFile = cacheVideo.getFileForURL(url);
			while (true) {
				if(cacheFile != null)
					System.out.println(cacheFile.length());
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Test test = new Test();
		Reader r = test.new Reader();
		Thread t = new Thread(r);
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
