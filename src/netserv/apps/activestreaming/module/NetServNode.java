package netserv.apps.activestreaming.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
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
	 * Because Jetty servlet ThreadPool, each request is processed in a
	 * different thread
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		final String url = request.getParameter("url");
		final String mode = request.getParameter("mode"); // live or vod

		if (url == null)
			return;

		CacheVideo cacheVideo = singleton.addURL(url);
		File cacheFile = cacheVideo.getFileForURL();

		System.out.println();
		Util.print("Request for " + url + " received from "
				+ request.getRemoteAddr());

		try {
			if (singleton.getState(url) == CacheVideo.INITIAL) {
				Util.print("Setting up a new stream");
				cacheVideo.setState(CacheVideo.LIVE);
				cacheVideo.connectedClients.add(response);
				this.serveURL(url, cacheFile, true);
			} else if (singleton.getState(url) == CacheVideo.LIVE) {
				// Live Broadcast is happening, add the client to list
				Util.print("adding new client into the live broadcast list..");
				cacheVideo.connectedClients.add(response);
				this.serveURL(url, cacheFile, false);
			} else if (singleton.getState(url) == CacheVideo.LIVE
					&& (mode != null) && mode.equalsIgnoreCase("vod")) {
				Util.print("Live streaming still happening.. but serving missed content "
						+ cacheFile.getName());
				serveFromInputStream(cacheFile, response);
			} else if (singleton.getState(url) == CacheVideo.VOD) {
				// Live Broadcast has finished .. serve the recording
				Util.print("Local cache: sending " + cacheFile.getName());
				serveFromInputStream(cacheFile, response);
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

	public void serveURL(String url, File cacheFile, boolean saveStream)
			throws IOException {
		InputStream in = null;
		OutputStream out_file = null;
		long start, duration = 0;
		CacheVideo cv = singleton.getVideo(url);

		start = System.currentTimeMillis();
		try {

			String filename = cacheFile.getName();

			// Copy the contents of the file to the output stream
			byte[] buf = new byte[BUF_SIZE];
			int count = 0;
			/**
			 * All the streaming logic is below, we need to make this as solid
			 * as possible.
			 * 
			 * @aditya - look at File Channels
			 */
			// first time
			if (saveStream) {
				Util.print("Serving the url stream & saving in local cache");
				URL urlstream = new URL(url);
				in = urlstream.openStream();
				out_file = new FileOutputStream(cacheFile);
			}
			for (HttpServletResponse res : cv.connectedClients) {
				res.setHeader("Content-Disposition", "inline; filename="
						+ filename);
				res.setHeader("Cache-Control", "no-cache");
				res.setHeader("Expires", "-1");
			}

			while ((count = in.read(buf)) > 0) {

				for (HttpServletResponse res : cv.connectedClients) {
					res.getOutputStream().write(buf, 0, count);
				}

				if (saveStream) {
					out_file.write(buf, 0, count);
					cv.incrementTotalBytes(count);
				}
			}
		} catch (org.mortbay.jetty.EofException e) {
			Util.error("Jetty.EOF : Browser connection closed !");
		}

		try {
			if (in != null)
				in.close();
			for (HttpServletResponse res : cv.connectedClients) {
				if (res.getOutputStream() != null) {
					res.getOutputStream().flush();
					res.getOutputStream().close();
				}

			}
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
			HttpServletResponse response) {
		try {
			FileInputStream in = new FileInputStream(localFile);
			// Get the MIME type of the image
			String mimeType = "video/mp4";

			// Set content type
			response.setContentType(mimeType);
			response.setHeader("Content-Disposition", "inline; filename="
					+ localFile.getName());
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Expires", "-1");
			// Set content size
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
