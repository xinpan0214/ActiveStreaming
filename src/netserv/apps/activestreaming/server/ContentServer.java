package netserv.apps.activestreaming.server;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.*;

import org.mortbay.jetty.*;
import org.mortbay.jetty.servlet.*;

public class ContentServer extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		String file = request.getParameter("file");
		final String client = request.getRemoteAddr();

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
						+ "http://127.0.1.1:5555/stream/" + file.trim() +"\""+ " />");
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
	

	public static void main(String[] args) throws Exception {
		// starting streaming service
		Streamer.playMedia("/media/netserv/NetServ/activestreaming/Hop.avi",
				"hop");
		
		// starting jetty server
		Server server = new Server(8080);
		Context root = new Context(server, "/", Context.SESSIONS);
		root.addServlet(new ServletHolder(new ContentServer()), "/stream/*");
		System.out.println("Server started..");
		server.start();
		server.join();
	}
}