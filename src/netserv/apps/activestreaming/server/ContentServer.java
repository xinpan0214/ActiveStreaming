package netserv.apps.activestreaming.server;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;


import org.mortbay.jetty.*;
import org.mortbay.jetty.servlet.*;

public class ContentServer extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static String SERVER_IP = "10.0.1.6";
	private static String STREAM_SERVER_IP = "10.0.1.6";
	private static final int CONTENT_SERVER_PORT = 8088;
	private static final int NETSERV_NODE_PORT = 8888;
	private static final int STREAM_SERVER_PORT = 8080;
	public static final String LOCALROOT = "./video-library";

	private static final Logger log = Logger.getLogger(ContentServer.class
			.getName());

	// NetServ Configuration
	static final String SERVER_PROPERTIES = "./server.properties";

	// These static variables are also being set in the server.properties file
	public static String CONTENT_HOST = "http://netserv-server/content/";
	public static String WEBROOT = "./";
	public static String NSIS_TRIGGER = "./netserv-trigger";
	public static String GEOIP_FILE = "./GeoLiteCity.dat";
	public static int MODULE_LIFETIME = 240;
	public static double THRESHOLD_DISTANCE = 100.0; // kilometers ?

	ContentSingleton singleton = ContentSingleton.getInstance();

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		String file = request.getParameter("file");
		String mode = request.getParameter("mode");
		final String client = request.getRemoteAddr();

		log.info("Request for " + file + " from " + request.getRemoteAddr());
		if (file == null) {
			try {
				response.getWriter().write("File not found !");
				return;
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
		String netServNode = getNetServNode(client);
		if (netServNode == null) {
			// First request
			Thread signalThread = new Thread(new Runnable() {
				public void run() {
					String node = null;
					// 5 times: sleep 2 second and probe the install
					for (int i = 0; i < 5; i++) {
						sendNetServSetup(client);
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							log.severe("signalThread : Error while sleeping: "
									+ e.toString());
						}
						node = sendNetServProbe(client);
						if (node != null) {
							break;
						}
					}
					try {
						Thread.sleep(MODULE_LIFETIME * 1000 - 5000);
					} catch (InterruptedException e) {
						log.severe("Error while sleeping: " + e.toString());
					}

					// if (netServNode != null)
					// sendNetServTeardown(netServNode);
				}
			});
			signalThread.start();

			if (mode != null && mode.equalsIgnoreCase("live")) {
				log.info("Sending directly to streaming server .. mode=Live");
				sendVLCVideo(file, response, true, true);
			} else {
				sendVLCVideo(file, response, false, true);
			}
		} else {
			// NetServ node is present
			if (mode != null && mode.equalsIgnoreCase("live")) {
				log.info("Sending to NetServ Node .. mode=Live");
				sendVLCVideo(file, response, true, false);
			} else {
				sendVLCVideo(file, response, false, false);
			}
		}
	}

	/**
	 * Returns the nearest NetServ Node for a client.
	 * 
	 * @param client
	 * @return
	 */
	private String getNetServNode(String client) {
		// Get all NetServ nodes
		Set<String> nodes = singleton.getNodes();

		String netServNode = null;
		double globalDistance = THRESHOLD_DISTANCE;
		try {
			Iterator<String> it = nodes.iterator();
			while (it.hasNext()) {
				String node = (String) it.next();
				double distance = singleton.calc_distance(netServNode, client);
				if (distance < globalDistance) {
					globalDistance = distance;
					netServNode = node;
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			log.severe("getNetServNode : Error calculating NetServ Node distance.");
		}

		if (globalDistance < THRESHOLD_DISTANCE) {
			log.info("getNetServNode : Found NetServ node " + netServNode
					+ " [" + client + ", " + globalDistance + "]");
			return netServNode + ":" + NETSERV_NODE_PORT;
		} else {
			return null;
		}
	}

	/**
	 * Send NetServ setup signal
	 * 
	 * @param client
	 *            - IP address of the client node
	 * @return Boolean value determining whether operation was successful or not
	 */
	private boolean sendNetServSetup(String client) {
		String s;
		String command = NSIS_TRIGGER
				+ " "
				+ client
				+ " -s -user jae -id NetServ.apps.ActiveStreaming_1.0.0 -url http://netserv-server/modules/activestreaming.jar -ttl "
				+ MODULE_LIFETIME;

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

			while ((s = stdInput.readLine()) != null) {
				// output will look like: 2 0 0
				if (s.equals("2 0 0"))
					return true;
				else
					return false;
			}
		} catch (IOException e) {
			log.severe("sendNetServSetup : Error running trigger setup \n"
					+ e.toString());
		}
		return false;
	}

	/**
	 * Send NetServ probe
	 */
	private String sendNetServProbe(String client) {
		String command = NSIS_TRIGGER
				+ " "
				+ client
				+ " -p -user jae -id NetServ.apps.ActiveStreaming_1.0.0 -probe 2";
		BufferedReader stdInput;
		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			stdInput = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			// output will look like:
			// 1.2.3.4 ACTIVE (for working nodes)
			// 1.2.3.4 NOT PRESENT (for non-working nodes)
			String s;
			while ((s = stdInput.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(s);
				String ipAddr = null;
				String status = null;
				ipAddr = st.nextToken();
				if (ipAddr != null) {
					status = st.nextToken();
				}
				if (status.equals("ACTIVE")) {
					if (singleton.addNode(ipAddr))
						log.info("sendNetServProbe : Adding NetServ node "
								+ ipAddr);
					return ipAddr;
				}
			}
		} catch (IOException e) {
			log.severe("sendNetServProbe : NSIS trigger not present");
			log.severe("sendNetServProbe : Error adding NetServ node"
					+ e.toString());
		}
		return null;
	}

	/**
	 * Removes the NetServ Node mapping for given IP address.
	 * 
	 * @param ipAddr
	 * @return
	 */

	private boolean sendNetServTeardown(String ipAddr) {
		String command = NSIS_TRIGGER + " " + ipAddr
				+ " -r -user jae -id NetServ.apps.ActiveStreaming_1.0.0";
		try {
			Runtime.getRuntime().exec(command);
			singleton.removeNode(ipAddr);
			log.info("sendNetServTeardown : Removing NetServ node " + ipAddr);
			return true;
		} catch (IOException e) {
			log.info("sendNetServTeardown : Error removing node list: "
					+ e.toString());
		}
		return false;
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
			boolean isLive, boolean isDirect) {
		String url = null;
		if (isDirect) {
			url = this.directURL(file);
		} else {
			if (isLive) {
				url = this.netServNodeURL(file);
				url += "&mode=live";
			} else {
				url = this.netServNodeURL(file);
				url += "&mode=vod";
			}
		}
		try {
			PrintWriter pr = response.getWriter();
			pr.write("<html>");
			pr.write("<head><title>NetServ Active Streaming</title></head>");
			pr.write("<body>");
			pr.write("<div id=\"content\" align=\"center\">");
			pr.write("<h2>NetServ Active Streaming</h2>");

			pr.write("<div align=\"center\"><a href=\"http://"
					+ ContentServer.SERVER_IP + ":"
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
			pr.write("<source src=\"" + this.netServNodeURL(file) + "\""
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

	public String netServNodeURL(String file) {
		String netserv = SERVER_IP + ":" + NETSERV_NODE_PORT;
		String newurl = "";
		newurl = "http://" + netserv + "/stream-cdn/?url=" + directURL(file);
		return newurl;
	}

	public String directURL(String file) {
		String newurl = "";
		newurl = "http://" + STREAM_SERVER_IP + ":" + STREAM_SERVER_PORT;
		return newurl;
	}

	public static void main(String[] args) throws Exception {
		// starting streaming service
		// VideoStreamer.playMedia(LOCALROOT + "/kungfu.ogv", "smurfs");

		// starting jetty server
		Server server = new Server(ContentServer.CONTENT_SERVER_PORT);
		Context root = new Context(server, "/", Context.SESSIONS);
		root.addServlet(new ServletHolder(new ContentServer()), "/stream/*");
		log.info("Content Server started..");
		server.start();
		server.join();
	}
}