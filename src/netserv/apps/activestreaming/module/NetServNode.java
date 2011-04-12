package netserv.apps.activestreaming.module;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import netserv.apps.activestreaming.module.ActiveStreamMap.CacheVideo;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class NetServNode extends HttpServlet {

	private static final long serialVersionUID = 1L;
	ActiveStreamMap singleton = ActiveStreamMap.getInstance();
	private static final int BUF_SIZE = 1024;

	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		this.doGet(request, response);
	}

	/**
	 * mode - 1. PREPARE 2. LIVE 3. VOD 4. STOP_DOWNLOAD
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		final String url = request.getParameter("url");
		final String mode = request.getParameter("mode");
		

		if (url == null && mode == null) {
			try {
				response.getWriter().print(
						"URL and mode are both required parameters.. ");
				response.getWriter().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		CacheVideo cacheVideo = singleton.addURL(url);
		long total = cacheVideo.getTotalByteSaved();
		File cacheFile = cacheVideo.getFileForURL(url);
		try {
			if (cacheVideo.activeConn < CacheVideo.STORAGE_THRESHOLD) {
				Util.print("Request received for streaming " + url + " from "
						+ request.getRemoteAddr());

				// increment the active connections
				cacheVideo.activeConn += 1;
				// serve url
				this.serveURL(url, cacheFile, response);
				cacheVideo.setState(CacheVideo.LIVE);

			} else if (cacheVideo.activeConn == CacheVideo.STORAGE_THRESHOLD) {
				Util.print("Storing the stream now.." + cacheFile.getName());

				// increment the active connections
				cacheVideo.activeConn += 1;

				// start storing the into local cache
				Writer writer = new Writer(url);
				Thread writerThread = new Thread(writer);
				writerThread.start();
				cacheVideo.writerInstance = writer;

				// serve from the URL
				this.serveURL(url, cacheFile, response);

			} else if (cacheVideo.activeConn > CacheVideo.STORAGE_THRESHOLD
					&& mode.equalsIgnoreCase("live")) {
				Util.print("Going to live mode for.." + cacheFile.getName());
				// serve from the local file
				//long total = cacheVideo.getTotalByteSaved();
				serveFromInputStream(cacheVideo, cacheFile, response, false, total);
			} else if (cacheVideo.activeConn > CacheVideo.STORAGE_THRESHOLD) {
				Util.print("Serving from local cache.." + cacheFile.getName());
				// increment the active connections
				cacheVideo.activeConn += 1;
				// serve from the local file
				serveFromInputStream(cacheVideo, cacheFile, response, false, 0);
			} else if (mode.equalsIgnoreCase("stop")) {
				cacheVideo.writerInstance.stop_write();
			}
		} catch (IOException e) {
			Util.error("Cannot save and send :(");
			e.printStackTrace();
		}
	}

	public void serveURL(String url, File cacheFile, HttpServletResponse res)
			throws IOException {
		InputStream in = null;
		try {

			String filename = cacheFile.getName();
			res.setHeader("Content-Disposition", "inline; filename=" + filename);
			res.setHeader("Cache-Control", "no-cache");
			res.setHeader("Expires", "-1");

			byte[] buf = new byte[BUF_SIZE];
			int count = 0;
			Util.print("serving directly from origin stream..");
			URL urlstream = new URL(url);
			in = urlstream.openStream();
			OutputStream out_stream = res.getOutputStream();
			while ((count = in.read(buf)) > 0) {
				out_stream.write(buf, 0, count);
			}

		} catch (org.mortbay.jetty.EofException e) {
			// reduce the active connections
			// singleton.getVideo(url).activeConn -= 1;
			Util.error("Jetty.EOF : Browser connection closed !");
		}

		try {
			if (in != null)
				in.close();
		} catch (IOException e) {
			Util.print("Error closing IO streams \n" + e.toString());
		}
	}

	/**
	 * Serves the stream from local repository
	 */
	public void serveFromInputStream(CacheVideo cv, File localFile,
			HttpServletResponse response, boolean vod, long skipBytes) {
		try {
			FileInputStream in = new FileInputStream(localFile);
			FileChannel in_channel = in.getChannel();
			
			response.setHeader("Content-Disposition", "inline; filename="
					+ localFile.getName());
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Expires", "-1");

			if (vod)
				response.setContentLength((int) localFile.length());

			OutputStream out = response.getOutputStream();

			// Copy the contents of the file to the output stream
			byte[] buf = new byte[BUF_SIZE];
			ByteBuffer buf_wrap = ByteBuffer.wrap(buf);
			
			long count = 0;
			if (skipBytes > 0) {

				/*in.skip(skipBytes - (long) 0.5 * skipBytes);
				 * 
				 */
				Util.print("Moving file current position to " + skipBytes
						+ " bytes.");
				in_channel.position(skipBytes - 3 * BUF_SIZE);
				while(in_channel.position() >= cv.getTotalByteSaved())
					Thread.yield();

				while (cv.getState() != CacheVideo.LOCAL) {
					//System.out.println("reached here1");
					//length = (int)( (cv.getTotalByteSaved() - in_channel.position()) / BUF_SIZE);
					
					count = in_channel.read(buf_wrap);
					if(count > 0){
						out.write(buf);
					}
					buf_wrap.clear();
					/*bytesread = (int)count/BUF_SIZE + 1;
					for (int i = 0; i < bytesread; i++) {
						out.write(buffer[i], 0, (int)count);
						buffer_wrap[i].clear();	
					}
					*/
				}
			} else {
				while ((count = in_channel.read(buf_wrap)) >= 0) {
					//System.out.println("reached here");
					out.write(buf, 0, (int)count);
					buf_wrap.clear();
				}
			}
			System.out.println("count is " + count);
			System.out.println("reached here");

			in_channel.close();
			in.close();
			out.flush();
			out.close();
		} catch (org.mortbay.jetty.EofException e) {
			Util.error("Jetty.EOF : Browser connection closed !");
		} catch (Exception e) {
			String out = "While writing to browser's stream. (";
			out += e.toString();
			out += ")";
			Util.print(out);
		}
	}

	protected class Writer implements Runnable {
		boolean stopped;
		String url;

		private Writer(String url) {
			this.url = url;
		}

		public void run() {
			FileOutputStream out_file = null;
			CacheVideo cv = singleton.addURL(url);
			File cacheFile = cv.getFileForURL(url);
			byte[] buf = new byte[BUF_SIZE];
			int count = 0;
			Util.print("Contacting streaming server & saving locally.. ");
			try {
				URL urlstream = new URL(url);
				cv.originInputStream = urlstream.openStream();
				out_file = new FileOutputStream(cacheFile);
				while ((count = cv.originInputStream.read(buf)) > 0) {
					out_file.write(buf, 0, count);
					out_file.flush();
					cv.incrementTotalBytes(count);
					if (stopped) {
						cv.setState(CacheVideo.LOCAL);
						break;
					}
				}
			} catch (IOException e) {
				Util.print("Problem occured in writer Thread !!");
				e.printStackTrace();
			} finally {
				// file is in local repository
				cv.setState(CacheVideo.LOCAL);
			}
		}

		synchronized void stop_write() {
			stopped = true;
			notify();
		}
	}

	/**
	 * This is the netserv node !! We need to save the stream from the server
	 * and pass it to connected client.
	 */
	public static void main(String[] args) {
		Server server = new Server(8888);
		Context root = new Context(server, "/", Context.SESSIONS);
		root.addServlet(new ServletHolder(new NetServNode()), "/stream-cdn/*");
		System.out.println("NetServ node started..");
		try {
			server.start();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
