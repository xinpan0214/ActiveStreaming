package netserv.apps.activestreaming.server;

import java.io.*;
import java.net.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import netserv.apps.activestreaming.module.Util;

import org.mortbay.jetty.*;
import org.mortbay.jetty.servlet.*;

public class ContentServer extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static String SERVER_IP = "192.168.15.7";
	private static String STREAM_SERVER_IP = "192.168.15.7";
	private static final int CONTENT_SERVER_PORT = 8088;
	private static final int NETSERV_PORT = 8888;
	private static final int STREAM_SERVER_PORT = 8080;

	/*
	 * static { try { SERVER_IP = InetAddress.getLocalHost().getHostAddress(); }
	 * catch (UnknownHostException e) { e.printStackTrace(); } }
	 */

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		String file = request.getParameter("file");
		String mode = request.getParameter("mode");

		Util.print("Request received for " + file + " from "
				+ request.getRemoteAddr());
		if (file == null) {
			try {
				response.getWriter().write("File not found !");
				return;
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
		
		if (mode != null && mode.equalsIgnoreCase("live")) {
			System.out.println("Sending to NetServ Node .. mode=Live");
			sendVLCVideo(file, response, true);
		} else {
			System.out.println("Sending to NetServ Node .. mode=VOD");
			sendVLCVideo(file, response, false);
		}
		// sendHTML5Video(file, response);
	}

	public String getHostIP() {
		String host = null;
		try {
			host = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return host;
	}

	public void sendVLCVideo(String file, HttpServletResponse response,
			boolean isLive) {
		String url = null;
		if (isLive) {
			url = this.netservNodeURL(file);
			url += "&mode=live";
		} else {
			url = this.netservNodeURL(file);
			url += "&mode=vod";
		}
		try {
			PrintWriter pr = response.getWriter();
			pr.write("<html>");
			pr.write("<head><title>NetServ Active Streaming</title></head>");
			pr.write("<body>");
			pr.write("<div id=\"content\" align=\"center\">");
			pr.write("<h2>NetServ Active Streaming</h2>");
			pr.write("<div align=\"center\"><a href=\"http://" + ContentServer.SERVER_IP + ":"
					+ ContentServer.CONTENT_SERVER_PORT + "/stream/?file="
					+ file + "&mode=live\">View Live</a> </div>");
			pr.write("<embed type=\"application/x-vlc-plugin\" name="
					+ file
					+ " autoplay=\"yes\" loop=\"no\" width=\"680\" height=\"460\" target=\""
					+ url + "\"" + " />");
			pr.write("</div>");
			pr.write("<br />");
			pr.write("</body>");
			pr.write("</html>");
			pr.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public void sendHTML5Video(String file, HttpServletResponse response) {
		try {
			PrintWriter pr = response.getWriter();
			pr.write("<html>");
			pr.write("<head><title>NetServ Active Streaming</title></head>");
			pr.write("<body>");
			pr.write("<div id=\"content\" align=\"center\">");
			pr.write("<h2>NetServ Active Streaming</h2>");
			pr.write("<div id=\"netserv-video\">");
			pr.write("<video id=\"demo-video\" controls>");
			pr.write("<source src=\"" + this.netservNodeURL(file) + "\""
					+ "type=\"video/ogg\" />");
			pr.write("</video>");
			pr.write("</div></div>");
			pr.write("<br />");
			pr.write("</body>");
			pr.write("</html>");
			pr.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * This goes to the netserv node Assumption: 1. Server knows on which ip &
	 * port, activeStream module is running Generated URL -
	 * netserv-node?url=streaming-server :
	 * 127.0.1.1:8888/stream-cdn/?url=http://127.0.1.1:5555/stream/hop
	 * 
	 * @param file
	 * @param response
	 */
	public String netservNodeURL(String file) {
		String netserv = SERVER_IP + ":" + NETSERV_PORT;
		String newurl = "";
		newurl = "http://" + netserv + "/stream-cdn/?url=" + directURL(file);
		return newurl;
	}

	/**
	 * Gets the video stream server address
	 * 
	 * @param file
	 * @return
	 */
	public String directURL(String file) {
		String newurl = "";
		/*
		 * try { newurl = "http://" + Streamer.streamServer + "/stream/" +
		 * URLEncoder.encode(file, "UTF-8"); } catch
		 * (UnsupportedEncodingException e) { e.printStackTrace(); }
		 */
		newurl = "http://" + STREAM_SERVER_IP + ":" + STREAM_SERVER_PORT;
		return newurl;
	}

	public static void main(String[] args) throws Exception {
		// starting streaming service
		// Streamer.playMedia("/home/aman/workspace/ActiveStreaming/samples/smurfs.mp4",
		// "smurfs");

		// starting jetty server
		Server server = new Server(ContentServer.CONTENT_SERVER_PORT);
		Context root = new Context(server, "/", Context.SESSIONS);
		root.addServlet(new ServletHolder(new ContentServer()), "/stream/*");
		System.out.println("Content Server started..");
		server.start();
		server.join();
	}
}