package netserv.apps.activestreaming;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 * This is the Active Streaming Node main class.
 * We need to save the stream from Streaming server
 * and pass it to connected client.
 * Columbia University
 * @since - 03/03/2011
 * @version - 1.0.0
 */

public class ActiveStreamNode extends HttpServlet {

	private static final long serialVersionUID = 1L;
	ActiveStreamMap singleton = ActiveStreamMap.getInstance();
	public static final int FRAMES = 10000;
	public static final int FRAME_SIZE = 1024;
	private static final Logger log = Logger
			.getLogger(ActiveStreamNode.class.getName());

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		this.doGet(request, response);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		final String url = request.getParameter("url");
		final String mode = request.getParameter("mode");
		if (url == null && mode == null) {
			try {
				response.getWriter().print(
						"URL and mode are both required parameters.. ");
				response.getWriter().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		CacheVideo cacheVideo = singleton.addURL(url);
		long total = cacheVideo.getTotalByteSaved();
		File cacheFile = cacheVideo.getCacheFile();

		synchronized (cacheVideo.activeConn) {
			if (!mode.equalsIgnoreCase("live"))
				cacheVideo.activeConn += 1;
		}

		if (cacheVideo.getState() == CacheVideo.INITIAL) {
			log.info("Request received for streaming " + url + " from "
					+ request.getRemoteAddr());
			VideoWriter writer = new VideoWriter(cacheVideo);
			Thread writerThread = new Thread(writer);
			writerThread.setPriority(Thread.MAX_PRIORITY);
			writerThread.start();
			cacheVideo.writerInstance = writer;
			cacheVideo.setState(CacheVideo.LIVE);
			// this.addClient(response, cacheVideo);
			this.serveURL(cacheVideo, response);

		} else if (cacheVideo.activeConn < CacheVideo.STORAGE_THRESHOLD) {
			log.info("GET : Active connections less than threshold,read from video buffer");
			// this.addClient(response, cacheVideo);
			this.serveURL(cacheVideo, response);
		} else if (cacheVideo.activeConn == CacheVideo.STORAGE_THRESHOLD) {
			log.info("GET : Threshold reached, local storage.."
					+ cacheFile.getName());
			cacheVideo.setStoreCache(true);
			serveFromFile(cacheVideo, response, total);
		} else if (mode.equalsIgnoreCase("live")) {
			log.info("GET : Going to live mode.." + cacheFile.getName());
			serveFromFile(cacheVideo, response, total);
		} else if (cacheVideo.activeConn > CacheVideo.STORAGE_THRESHOLD) {
			log.info("GET : Serving via local cache from starting.."
					+ cacheFile.getName());
			serveFromFile(cacheVideo, response, 0);
		} else if (mode.equalsIgnoreCase("stop")) {
			log.info("GET : Stop writer");
			cacheVideo.writerInstance.stop_write();
		}
	}

	public void addClient(HttpServletResponse response, CacheVideo cv) {
		File localFile = cv.getCacheFile();
		response.setHeader("Content-Disposition", "inline; filename="
				+ localFile.getName());
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Expires", "-1");

		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		int readerPos = 0;
		long readerFrame = 0;
		synchronized (cv.writerFrame) {
			readerPos = getReaderPos(ActiveStreamNode.FRAMES / 2, cv);
			readerFrame = getReaderFrame(ActiveStreamNode.FRAMES / 2, cv);
		}

		log.info("Reader: Buffer Position: " + readerPos + " & Frame : "
				+ readerFrame);
		while (cv.getState() != CacheVideo.LOCAL) {
			if (readerFrame < cv.writerFrame) {
				try {
					OutputStream out_stream = response.getOutputStream();
					out_stream.write(cv.videoBuffer, readerPos * FRAME_SIZE,
							FRAME_SIZE);
					log.info("Writing to stream");
				} catch (org.mortbay.jetty.EofException e) {
					log.warning("Reader : Browser connection closed !");
					break;
				} catch (IOException e) {
					log.severe("Reader: Error while flusing data to client :(");
					e.printStackTrace();
					break;
				}
				readerFrame++;
			} else {
				try {
					log.info("Reader: Going to sleep :(");
					Thread.sleep(3000);
					synchronized (cv.writerFrame) {
						readerPos = getReaderPos(
								ActiveStreamNode.FRAMES / 2, cv);
						readerFrame = getReaderFrame(
								ActiveStreamNode.FRAMES / 2, cv);
					}
				} catch (InterruptedException e) {
					log.info("Reader: Thread interrupted from sleep");
					e.printStackTrace();
					break;
				}
			}
		}
	}

	private int getReaderPos(long windowSize, CacheVideo cv) {
		long pos = 0;
		int r1 = FRAMES / 2, r2 = FRAMES;
		long frame = cv.writerFrame % ActiveStreamNode.FRAMES;
		if (!cv.onceFilled) {
			if (0 <= frame && frame < r1) {
				pos = 0;
			} else if (r1 <= frame && frame < r2) {
				pos = frame - windowSize;
			}
		} else {
			if (frame < windowSize) {
				pos = ActiveStreamNode.FRAMES - (windowSize - frame);
			} else {
				pos = frame - windowSize;
			}
		}
		return (int) pos;
	}

	private long getReaderFrame(long windowSize, CacheVideo cv) {
		long pos = 0;
		int r1 = FRAMES / 2, r2 = FRAMES;
		if (!cv.onceFilled) {
			if (0 <= cv.writerFrame && cv.writerFrame < r1) {
				pos = 0;
			} else if (r1 <= cv.writerFrame && cv.writerFrame < r2) {
				pos = cv.writerFrame - windowSize;
			}
		} else {
			pos = cv.writerFrame - windowSize;
		}
		return pos;
	}

	public void serveURL(CacheVideo cv, HttpServletResponse response) {
		InputStream in = null;
		try {
			File localFile = cv.getCacheFile();
			response.setHeader("Content-Disposition", "inline; filename="
					+ localFile.getName());
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Expires", "-1");

			log.info("serveURL : Serving directly from origin stream");
			byte[] buf = new byte[FRAME_SIZE];
			int count = 0;
			URL urlstream = cv.getVideoURL();
			in = urlstream.openStream();
			OutputStream out_stream = response.getOutputStream();
			while ((count = in.read(buf)) > 0) {
				out_stream.write(buf, 0, count);
			}
		} catch (org.mortbay.jetty.EofException e) {
			log.warning("serveURL : Browser connection closed !");
		} catch (IOException e) {
			log.warning("serveURL : IOExcetion");
			e.printStackTrace();
		}

		try {
			if (in != null)
				in.close();
		} catch (IOException e) {
			log.warning("serveURL : Error closing IO streams");
			e.printStackTrace();
		}
	}

	/**
	 * Serves the stream from local repository
	 */
	public void serveFromFile(CacheVideo cv, HttpServletResponse response,
			long skipBytes) {
		File localFile = cv.getCacheFile();
		response.setHeader("Content-Disposition", "inline; filename="
				+ localFile.getName());
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Expires", "-1");
		try {
			FileInputStream in = new FileInputStream(localFile);
			FileChannel in_channel = in.getChannel();
			OutputStream out = response.getOutputStream();

			// Copy the contents of the file to the output stream
			byte[] buf = new byte[FRAME_SIZE];
			ByteBuffer buf_wrap = ByteBuffer.wrap(buf);

			long count = 0;
			if (skipBytes > 0) {
				log.info("ServeFromInputStream : Moving file position to "
						+ skipBytes);
				in_channel.position(skipBytes - 5 * FRAME_SIZE);
				while (in_channel.position() >= cv.getTotalByteSaved())
					Thread.yield();

				while (cv.getState() != CacheVideo.LOCAL) {
					count = in_channel.read(buf_wrap);
					if (count > 0) {
						out.write(buf);
					}
					buf_wrap.clear();
				}
			} else {
				while ((count = in_channel.read(buf_wrap)) >= 0) {
					out.write(buf, 0, (int) count);
					buf_wrap.clear();
				}
			}
			log.info("ServeFromInputStream : Reached here");
			in_channel.close();
			in.close();
			out.flush();
			out.close();
		} catch (org.mortbay.jetty.EofException e) {
			log.warning("ServeFromInputStream : Browser connection closed !");
		} catch (IOException e) {
			log.warning("ServeFromInputStream : While writing to browser's stream.");
		}
	}

	protected class VideoWriter implements Runnable {
		boolean stopped;
		CacheVideo cv;

		private VideoWriter(CacheVideo cv) {
			this.cv = cv;
		}

		@Override
		public void run() {
			log.info("Writer: Contacting streaming server & saving locally.. ");
			byte[] buf = new byte[FRAME_SIZE];
			int count = 0;
			FileOutputStream out_file = null;
			File cacheFile = cv.getCacheFile();
			try {
				out_file = new FileOutputStream(cacheFile);
				URL urlstream = cv.getVideoURL();
				cv.originInputStream = urlstream.openStream();
				int frame = 0;
				while ((count = cv.originInputStream.read(buf)) > 0) {
					if (frame < FRAMES) {
						System.arraycopy(buf, 0, cv.videoBuffer, frame
								* FRAME_SIZE, buf.length);
					} else {
						frame = 0;
						System.arraycopy(buf, 0, cv.videoBuffer, frame
								* FRAME_SIZE, buf.length);
						if (!cv.onceFilled) {
							cv.onceFilled = true;
							log.info("Writer: Video buffer is once filled !");
						}
					}

					synchronized (cv.writerFrame) {
						cv.writerFrame++;
						frame++;
					}
					out_file.write(buf, 0, count);
					out_file.flush();
					cv.incrementTotalBytes(count);
					if (stopped) {
						break;
					}
				}
			} catch (IOException e) {
				log.severe("Writer: Problem occurred in writing to file :(");
				e.printStackTrace();
			} finally {
				// check if we want to save the file
				if (cv.isStoreCache()) {
					cv.setState(CacheVideo.LOCAL);
				} else {
					// remove the file from cache
					cacheFile.delete();
				}
			}
		}

		synchronized void stop_write() {
			stopped = true;
			notify();
		}
	}

	public static void main(String[] args) {
		Server server = new Server(8888);
		Context root = new Context(server, "/", Context.SESSIONS);
		root.addServlet(new ServletHolder(new ActiveStreamNode()),
				"/stream-cdn/*");
		log.info("NetServ node started..");
		try {
			server.start();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
