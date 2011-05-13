package netserv.apps.activestreaming.server;

public class IPAddressLocation {
	public float latitude;
	public float longitude;

	private final static double EARTH_DIAMETER = 2 * 6378.2;
	private final static double PI = 3.14159265;
	private final static double RAD_CONVERT = PI / 180;

	public IPAddressLocation(String lat, String lon) {
		try {
			this.latitude = Float.valueOf(lat.trim()).floatValue();
			this.longitude = Float.valueOf(lon.trim()).floatValue();
		} catch (NumberFormatException e) {
			System.err.println("ActiveStreaming : Cound not initiate location - "
					+ e.getMessage());
		}
	}

	public double distance(IPAddressLocation ipAddr) {
		double delta_lat, delta_lon;
		double temp;

		float lat1 = latitude;
		float lon1 = longitude;
		float lat2 = ipAddr.latitude;
		float lon2 = ipAddr.longitude;

		// convert degrees to radians
		lat1 *= RAD_CONVERT;
		lat2 *= RAD_CONVERT;

		// find the deltas
		delta_lat = lat2 - lat1;
		delta_lon = (lon2 - lon1) * RAD_CONVERT;

		// Find the great circle distance
		temp = Math.pow(Math.sin(delta_lat / 2), 2) + Math.cos(lat1)
				* Math.cos(lat2) * Math.pow(Math.sin(delta_lon / 2), 2);
		return EARTH_DIAMETER
				* Math.atan2(Math.sqrt(temp), Math.sqrt(1 - temp));
	}

}
