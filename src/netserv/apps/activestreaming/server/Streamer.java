package netserv.apps.activestreaming.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;

class Streamer {
	private static HeadlessMediaPlayer mediaPlayer = null;
	private static String serverAddress;
	private static String serverPort = "5555";
	public static Set<String> Files = new HashSet<String>();
	public static final Streamer PLAYER;
	
	static {
		PLAYER = new Streamer();
		try {
			InetAddress addr = InetAddress.getLocalHost();
			byte[] host = addr.getAddress();
			Streamer.serverAddress = addr.getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * A private constructor which creates a single instance of the headless player.
	 */
	private Streamer() {
		MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory();
		mediaPlayer = mediaPlayerFactory.newMediaPlayer();
	}

	public static void playMedia(String media, String file) {
		Files.add(file);
		file = formatHttpStream(file);
		mediaPlayer.playMedia(media,file);
		System.out.println("Starting streaming.. " + media + " with options "
				+ file);
		
		// when the streaming gets over we need to remove the file
	}

	public void closePlayer() {
		mediaPlayer.release();
	}

	private static String formatHttpStream(String file) {
		StringBuilder sb = new StringBuilder(60);
		sb.append(":sout=#standard{access=http,mux=asf,");
		sb.append("dst=");
		sb.append(serverAddress);
		sb.append(':');
		sb.append(serverPort + "/stream/" + file);
		sb.append("}");
		return sb.toString();
	}

	public static InetAddress[] localHostAddresses() {
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			InetAddress addresses[] = InetAddress.getAllByName(localhost
					.getHostName());
			return addresses;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * This main method just for testing ...
	 * @param args
	 */
	public static void main(String[] args) {
		// location to ActiveStreaming/samples/
		Streamer.playMedia(
				"/media/netserv/NetServ/activestreaming/Hop.avi", "hop");
		System.out.println(Streamer.PLAYER.mediaPlayer.getVideoOutputs());
	}
}
