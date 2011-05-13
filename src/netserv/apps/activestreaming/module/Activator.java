package netserv.apps.activestreaming;

import java.io.File;
import java.util.logging.Logger;

import org.mortbay.jetty.*;
import org.mortbay.jetty.servlet.*;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public final class Activator implements BundleActivator {
	private static final Logger log = Logger.getLogger(Activator.class
			.getName());
	Server server = new Server(8080);

	@Override
	public void start(BundleContext bc) throws Exception {
		if (deleteDir(new File(CacheVideo.LOCALROOT)))
			log.info("ActiveStreaming : Removed pre-existing cache videos.");
		Context root = new Context(server, "/", Context.SESSIONS);
		root.addServlet(new ServletHolder(new ActiveStreamNode()), "/stream-cdn/*");
		log.info("ActiveStreaming : NetServ node started..");
		try {
			server.start();
			server.join();
		} catch (Exception e) {
			log.severe("ActiveStreaming : Error starting Node");
			e.printStackTrace();
		}
	}

	@Override
	public void stop(BundleContext bc) throws Exception {

		try {
			if (server != null)
				server.stop();
		} catch (Exception e) {
			log.severe("ActiveStreaming : Error stopping Node ");
			e.printStackTrace();
		}

		if (deleteDir(new File(CacheVideo.LOCALROOT)))
			System.out.println("ActiveStreaming : Removed cache videos and exiting.");
	}

	private static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		// The directory is now empty so delete it
		return dir.delete();
	}
}
