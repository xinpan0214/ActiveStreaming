package netserv.apps.activestreaming.module;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;

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
	 * mode - 1. PREPARE 2. LIVE 3. PASSIVE 4. VOD 5. STOP_DOWNLOAD
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
		File cacheFile = cacheVideo.getFileForURL(url);

		try {
			if (singleton.getState(url) == CacheVideo.INITIAL
					&& mode.equalsIgnoreCase("prepare")) {
				Util.print("Request received for preparing stream" + url
						+ " from " + request.getRemoteAddr());
				Thread writerThread = new Thread(new Runnable() {
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
								cv.incrementTotalBytes(count);
							}
						} catch (IOException e) {
							Util.print("Problem occured in writer Thread !!");
							e.printStackTrace();
						}
					}
				});
				writerThread.start();
				cacheVideo.writerThread = writerThread;
				cacheVideo.setState(CacheVideo.LIVE);
				response.getWriter().print(
						"NetServ Node is downloading the stream now.. ");
				response.getWriter().close();
			} else if ((singleton.getState(url) == CacheVideo.LIVE)
					&& mode.equalsIgnoreCase("live")) {
				// Live Broadcast is happening, add the client to list
				Util.print("adding new client into the live broadcast list..");
				this.serveURL(url, cacheFile, response, false);
			} else if (singleton.getState(url) == CacheVideo.LIVE
					&& mode.equalsIgnoreCase("passive")) {
				Util.print("Live streaming still happening.. but serving from local cache !"
						+ cacheFile.getName());
				serveFromInputStream(cacheFile, response, false);
			} else if (singleton.getState(url) == CacheVideo.VOD) {
				// Live Broadcast has finished .. serve the recording
				Util.print("Video on Demand: sending from local cache"
						+ cacheFile.getName());
				serveFromInputStream(cacheFile, response, true);
			}
		} catch (IOException e) {
			Util.error("Cannot save and send :(");
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param url
	 * @param cacheFile
	 * @param saveStream
	 * @throws IOException
	 */

	public void serveURL(String url, File cacheFile, HttpServletResponse res,
			boolean saveStream) throws IOException {
		InputStream in = null;
		OutputStream out_file = null;
		long start, duration = 0;
		CacheVideo cv = singleton.getVideo(url);

		start = System.currentTimeMillis();
		try {

			String filename = cacheFile.getName();
			//res.setContentType("video/mp4");
			res.setHeader("Content-Disposition", "inline; filename=" + filename);
			res.setHeader("Cache-Control", "no-cache");
			res.setHeader("Expires", "-1");

			byte[] buf = new byte[BUF_SIZE];
			int count = 0;
			Util.print("serving directly from origin stream");
			while ((count = cv.originInputStream.read(buf)) > 0) {
				res.getOutputStream().write(buf, 0, count);
			}

		} catch (org.mortbay.jetty.EofException e) {
			Util.error("Jetty.EOF : Browser connection closed !");
		}

		try {
			if (in != null)
				in.close();
			if (out_file != null) {
				out_file.close();
				duration = System.currentTimeMillis() - start;
				cv.setState(CacheVideo.VOD);
			}
		} catch (IOException e) {
			Util.print("Error closing IO streams (" + e.toString() + ")");
		}

		System.out.println("Total served duration:" + duration);
	}

	/**
	 * Serves the stream from local repository
	 * 
	 * @param localFile
	 * @param response
	 */
	public void serveFromInputStream(File localFile,
			HttpServletResponse response, boolean vod) {
		try {
			System.out.println("Serving from local file cache..");
			FileInputStream in = new FileInputStream(localFile);
			// Get the MIME type of the image
			String mimeType = "video/MP2T";

			// Set content type
			response.setContentType(mimeType);
			response.setHeader("Content-Disposition", "inline; filename="
					+ localFile.getName());
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Expires", "-1");
			// Set content size
			if (vod)
				response.setContentLength((int) localFile.length());

			// Open the file and output streams
			OutputStream out = response.getOutputStream();

			// Copy the contents of the file to the output stream
			byte[] buf = new byte[BUF_SIZE];
			int count = 0;
			while ((count = in.read(buf)) >= 0) {
				out.write(buf, 0, count);
			}
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
