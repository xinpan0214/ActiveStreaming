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
	private static final int SERVER_PORT = 8088;
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
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}

		if (mode == null) {
			System.out.println("Sending video options response !");
			sendVideoOptions(response, file);
		} else {
			System.out.println("Sending to NetServ Node !");
			sendToNetServNode(file, response, mode);
		}
	}

	public void sendVideoOptions(HttpServletResponse response, String file) {

		try {
			PrintWriter pr = response.getWriter();
			pr.write("<html>");
			pr.write("<head>");
			pr.write("<title>NetServ - Active Streaming</title>");
			pr.write("<style type=\"text/css\">");
			pr.write("body { font: 14px/1.3 verdana, arial, helvetica, sans-serif }");
			pr.write("h1 { font-size:1.3em }");
			pr.write("h2 { font-size:1.2em }");
			pr.write("a:link { color:#33c }");
			pr.write("a:visited { color:#339 }");
			pr.write("</style>");
			pr.write("</head>");
			pr.write("<body>");
			pr.write("<h1>NetServ Active Streaming</h1>");
			pr.write("<ul>");
			pr.write("<li><a href=\"http://" + ContentServer.SERVER_IP + ":"
					+ ContentServer.SERVER_PORT + "/stream/?file=" + file
					+ "&mode=live" + "\" target=\"ifrm\">Play Live TV</a></li>");
			pr.write("<li><a href=\"http://" + ContentServer.SERVER_IP + ":"
					+ ContentServer.SERVER_PORT + "/stream/?file=" + file
					+ "&mode=vod"
					+ "\" target=\"ifrm\">Play Video on Demand</a></li>");
			pr.write("<div id=\"container\" align=\"center\">");
			pr.write("<iframe id=\"ifrm\" name=\"ifrm\" scrolling=\"auto\" width=\"80%\" height=\"480\"+"
					+ " frameborder=\"1\">Your browser doesn't support iframes.</iframe>");
			pr.write("</div>");
			pr.write("</ul>");
			pr.write("</body>");
			pr.write("</html>");
			pr.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getHostIP() {
		String host = null;
		try {
			host = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return host;
	}

	public void sendToNetServNode(String file, HttpServletResponse response,
			String mode) {
		String url = null;
		if (mode != null && mode.equalsIgnoreCase("live")) {
			url = this.netservNodeURL(file);
		} else if (mode != null && mode.equalsIgnoreCase("vod")) {
			url = this.netservNodeURL_VOD(file);
		}
		try {
			PrintWriter pr = response.getWriter();
			pr.write("<html>");
			pr.write("<head><title>NetServ Active Streaming</title></head>");
			pr.write("<body>");
			pr.write("<embed type=\"application/x-vlc-plugin\" name="
					+ file
					+ " autoplay=\"yes\" loop=\"no\" width=\"680\" height=\"460\" target=\""
					+ url + "\"" + " />");
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
		newurl = "http://" + netserv + "/stream-cdn/?url=" + directURL(file)
				+ "&mode=live";
		return newurl;
	}

	public String netservNodeURL_VOD(String file) {
		String netserv = SERVER_IP + ":" + NETSERV_PORT;
		String newurl = "";
		newurl = "http://" + netserv + "/stream-cdn/?url=" + directURL(file)
				+ "&mode=vod";
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
		newurl = "http://" + SERVER_IP + ":" + STREAM_SERVER_PORT;
		return newurl;
	}

	public static void main(String[] args) throws Exception {
		// starting streaming service
		// Streamer.playMedia("/home/aman/workspace/ActiveStreaming/samples/smurfs.mp4",
		// "smurfs");

		// starting jetty server
		Server server = new Server(ContentServer.SERVER_PORT);
		Context root = new Context(server, "/", Context.SESSIONS);
		root.addServlet(new ServletHolder(new ContentServer()), "/stream/*");
		System.out.println("Content Server started..");
		server.start();
		server.join();
	}
}