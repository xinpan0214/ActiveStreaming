package netserv.apps.activestreaming.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class NetServStreamingNode {
	private static final int BUF_SIZE = 1024;

	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		this.doGet(request, response);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		String url = request.getParameter("url");
		final String client = request.getRemoteAddr();
		System.out.println("NetServ Node : Get request !");

		/**
		 * Check whether url file is present or not ? if present generate html
		 * with two links else return the save and send stream
		 */
		// For live Stream
		if (url == null) {
			try {
				response.getWriter().write("It Works !!");
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		}
		// Stream it from cache
		// final CacheVideo c = new CacheVideo (url);
		// final String localStr = c.getFileForURL();
		// File local = new File (localStr);
		// this.serveAndSaveURL(url, response, local);
		try {
			this.serveAndSaveURL(url, response);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void serveAndSaveURL(String url, HttpServletResponse response,
			File... cache) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		OutputStream out_file = null;
		boolean writeToFile = false;
		long start, duration = 0;

		start = System.currentTimeMillis();

		try {

			String filename = url.substring(url.lastIndexOf('/') + 1,
					url.length());
			response.setHeader("Content-Disposition", "inline; filename="
					+ filename);
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Expires", "-1");

			// Open URL and read it in
			URL urlstream = new URL(url);
			in = urlstream.openStream();
			out = response.getOutputStream();
			if (writeToFile && cache.length > 0)
				out_file = new FileOutputStream(cache[0]);
		} catch (org.mortbay.jetty.EofException e) {
			// Nothing we can do about this exception.
			// Browser closed the connection and Jetty reports EOF.
		} catch (Exception e) {
			String out1 = "While writing to browser's stream. (";
			out1 += e.toString();
			out1 += ")";
		}

		// Copy the contents of the file to the output stream
		byte[] buf = new byte[BUF_SIZE];
		int count = 0;
		int total = 0;
		System.out.println("Starting serving new client");
		while ((count = in.read(buf)) > 0) {
			try {
				out.write(buf, 0, count);
			} catch (Exception e) {
				String out1 = "While writing to browser's stream. (";
				out1 += e.toString();
				out1 += ")";

			}
			try {
				if (writeToFile)
					out_file.write(buf, 0, count);
			} catch (Exception e) {

			}
			// System.err.printf("%d: wrote %d bytes\n", ++i, count);
			total += count;
		}

		in.close();
		out.close();

		if (out_file != null) {
			out_file.close();
			duration = System.currentTimeMillis() - start;
		}
		System.out.println("Total served duration:" + duration);
	}

	/**
	 * This is the netserv node !! We need to save the stream from the server
	 * and pass it to connected client.
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// starting jetty server
		Server server = new Server(8888);
		Context root = new Context(server, "/", Context.SESSIONS);
		root.addServlet(new ServletHolder(new ContentServer()),
				"/stream-cdn/*");
		System.out.println("NetServ node started..");
		try {
			server.start();
			server.join();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
