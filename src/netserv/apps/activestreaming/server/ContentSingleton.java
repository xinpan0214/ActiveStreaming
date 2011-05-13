package netserv.apps.activestreaming.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.logging.Logger;
import com.maxmind.geoip.*;

public class ContentSingleton {
	// Stores all discovered NetServ nodes
	private Set<String> nodes;
	// <url, IP address location>
	private HashMap<String, IPAddressLocation> ipAddressHashmap;
	private static final Logger log = Logger.getLogger(ContentSingleton.class
			.getName());

	// Private constructor prevents instantiation from other classes
	private ContentSingleton() {
		nodes = Collections.synchronizedSet(new HashSet<String>());
		ipAddressHashmap = new HashMap<String, IPAddressLocation>();
	}

	/**
	 * SingletonHolder is loaded on the first execution of
	 * Singleton.getInstance() or the first access to SingletonHolder.INSTANCE,
	 * not before.
	 */
	private static class SingletonHolder {
		public static final ContentSingleton INSTANCE = new ContentSingleton();
	}

	public static ContentSingleton getInstance() {
		return SingletonHolder.INSTANCE;
	}

	public Set<String> getNodes() {
		return nodes;
	}

	public boolean addNode(String n) {
		return nodes.add(n);
	}

	public boolean removeNode(String n) {
		return nodes.remove(n);
	}

	public void readTranslationToHashMap() {
		try {
			FileReader is = new FileReader("translation_table.csv");
			BufferedReader bufRdr = new BufferedReader(is);
			String line = null;
			// read each line of text file
			while ((line = bufRdr.readLine()) != null) {
				// skip over blanks or comments
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#"))
					continue;
				String[] st = line.split(",");
				if (st.length == 3) {
					// Read in the ipAddress, Latitude, Longitude
					ipAddressHashmap.put(st[0].trim(), new IPAddressLocation(
							st[1].trim(), st[2].trim()));
				}
			}
		} catch (Exception e) {
			log.severe("ActiveStreaming : While processing translation table - "
					+ e.getMessage());
		}
	}

	public double calc_distance(String netservNode, String client) {
		IPAddressLocation l1 = ipAddressHashmap.get(netservNode);
		IPAddressLocation l2 = ipAddressHashmap.get(client);

		// Make the default distance value a large one
		double distance = 100.0;

		// If the location for l1 and l2 is not present, use GeoIP to look it up
		if ((l1 == null) && (l2 == null)) {
			try {
				LookupService cl = new LookupService("GeoLiteCity.dat",
						LookupService.GEOIP_MEMORY_CACHE);
				Location ll1 = cl.getLocation(netservNode);
				Location ll2 = cl.getLocation(client);
				distance = ll1.distance(ll2);
			} catch (Exception e) {
				log.severe("ActiveStreaming : Unable to use MaxMind library - "
						+ e.getMessage());
			}
		} else if (l1 == null) {
			log.severe("ActiveStreaming : no location info for " + netservNode);
		} else if (l2 == null) {
			log.severe("ActiveStreaming : no location info for " + client);
		} else {
			distance = l2.distance(l1);
		}
		return distance;
	}
}
