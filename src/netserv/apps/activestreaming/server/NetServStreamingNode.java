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

	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		String file = request.getParameter("url");
		final String client = request.getRemoteAddr();

		/**
		 * Check whether url file is present or not ?
		 * if present generate html with two links
		 * else
		 * return the save and send stream
		 */
		// For live Stream
		if (file == null) {
			try {
				response.getWriter().write("It Works !!");
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		}

		// stream selector in a thread
		URL url = null;
		if (file != null && Streamer.Files.contains(file.trim())) {
			// file is streaming
			System.out.println("Found file in the directory !");
			try {
				PrintWriter pr = response.getWriter();
				pr.write("<html>");
				pr.write("<head><title>NetServ Active Streaming</title></head>");
				pr.write("<body>");
				pr.write("<h1>NetServ Active Streaming</h1>");

				pr.write("<embed type=\"application/x-vlc-plugin\" name="
						+ file
						+ " autoplay=\"yes\" loop=\"no\" width=\"1280\" height=\"500\" target=\""
						+ "http://127.0.1.1:5555/stream/" + file.trim() + "\""
						+ " />");
				pr.write("<br />");

				pr.write("</body>");
				pr.write("</html>");
				pr.flush();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		} else {
			// no file found
			System.out.println("file is not present !!");
		}

	}

	public void serveAndSaveURL(String url, HttpServletResponse response,
			File cache) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		OutputStream out_file = null;
		boolean writeToFile = true;
		long start, duration;

		start = System.currentTimeMillis();

		try {
			// Get the MIME type of the image
			String mimeType = "video/mp4";

			// Set content type
			response.setContentType(mimeType);

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
			if (writeToFile)
				out_file = new FileOutputStream(cache);
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

		while ((count = in.read(buf)) > 0) {
			try {
				out.write(buf, 0, count);
			} catch (org.mortbay.jetty.EofException e) {
				// Nothing we can do about this exception.
				// Browser closed the connection and Jetty reports EOF.

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

		try {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
			if (out_file != null) {
				out_file.close();
				duration = System.currentTimeMillis() - start;
			}
		} catch (IOException e) {

		}
	}

	/**
	 * This is the netserv node !!
	 * We need to save the stream from the server
	 * and pass it to connected client.
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// starting jetty server
		Server server = new Server(8080);
		Context root = new Context(server, "/", Context.SESSIONS);
		root.addServlet(new ServletHolder(new ContentServer()),
				"/streaming-cdn/*");
		System.out.println("Streaming CDN NetServ node started..");
		try {
			server.start();
			server.join();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
