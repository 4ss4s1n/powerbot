package org.powerbot.script;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.zip.Adler32;

import javax.imageio.ImageIO;

import org.powerbot.Configuration;
import org.powerbot.gui.BotChrome;
import org.powerbot.misc.ScriptBundle;
import org.powerbot.bot.ScriptController;
import org.powerbot.util.HttpUtils;
import org.powerbot.util.IOUtils;
import org.powerbot.util.Ini;
import org.powerbot.util.StringUtils;

/**
 * An abstract implementation of {@link Script}.
 */
public abstract class AbstractScript<C extends ClientContext> implements Script, Comparable<AbstractScript> {
	/**
	 * The {@link Logger} which should be used to print debugging messages.
	 */
	public final Logger log = Logger.getLogger(getClass().getName());

	/**
	 * The {@link org.powerbot.script.rt6.ClientContext} for accessing client data.
	 */
	protected final C ctx;

	public static final BlockingQueue<ClientContext> contextProxy = new SynchronousQueue<ClientContext>();
	private static final AtomicInteger s = new AtomicInteger(0);
	private final int sq;

	/**
	 * The priority of this {@link Script} as a {@link java.lang.Runnable}.
	 */
	public final AtomicInteger priority;

	private final Map<State, Queue<Runnable>> exec;
	private final AtomicLong started, suspended;
	private final Queue<Long> suspensions;
	private final File dir;

	/**
	 * The user profile settings of this {@link AbstractScript}, which will be saved and reloaded between sessions.
	 */
	protected final Properties settings;

	/**
	 * Creates an instance of {@link AbstractScript}.
	 */
	public AbstractScript() {
		exec = new ConcurrentHashMap<State, Queue<Runnable>>(State.values().length);
		for (final State state : State.values()) {
			exec.put(state, new ConcurrentLinkedQueue<Runnable>());
		}

		sq = s.getAndIncrement();
		priority = new AtomicInteger(0);
		started = new AtomicLong(System.nanoTime());
		suspended = new AtomicLong(0);
		suspensions = new ConcurrentLinkedQueue<Long>();

		exec.get(State.START).add(new Runnable() {
			@Override
			public void run() {
				started.set(System.nanoTime());
			}
		});

		exec.get(State.SUSPEND).add(new Runnable() {
			@Override
			public void run() {
				suspensions.offer(System.nanoTime());
			}
		});

		exec.get(State.RESUME).add(new Runnable() {
			@Override
			public void run() {
				suspended.addAndGet(System.nanoTime() - suspensions.poll());
			}
		});

		try {
			@SuppressWarnings("unchecked")
			final C ctx = (C) contextProxy.take();
			this.ctx = ctx;
		} catch (final InterruptedException e) {
			throw new IllegalStateException(e);
		}

		final String[] ids = {null, getName(), getClass().getName()};
		String id = "-";

		final Controller c = ctx.controller();
		if (c instanceof ScriptController) {
			final ScriptController sc = ((ScriptController) c);
			final ScriptBundle bundle = sc.bundle != null ? (ScriptBundle) sc.bundle.get() : null;
			if (bundle != null && bundle.definition != null) {
				ids[0] = bundle.definition.getID().replace('/', '-');
			}
		}

		for (final String n : ids) {
			if (n != null && !n.isEmpty()) {
				id = n.replace("[^\\w\\s]", "_").trim();
				break;
			}
		}

		dir = new File(new File(Configuration.TEMP, Configuration.NAME), id);
		final File ini = new File(dir, "settings.1.ini");
		settings = new Properties();

		if (ini.isFile() && ini.canRead()) {
			settings.putAll(new Ini().read(ini).get().getMap());
		}

		exec.get(State.STOP).add(new Runnable() {
			@Override
			public void run() {
				if (settings.isEmpty()) {
					if (ini.isFile()) {
						ini.delete();
					}
				} else {
					if (!dir.isDirectory()) {
						dir.mkdirs();
					}

					final Map<String, String> map = new HashMap<String, String>(settings.size());
					synchronized (settings) {
						for (final Map.Entry<Object, Object> entry : settings.entrySet()) {
							map.put(entry.getKey().toString(), entry.getValue().toString());
						}
					}
					new Ini().put(map).write(ini);
				}
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final int compareTo(final AbstractScript o) {
		final int r = o.priority.get() - priority.get();
		return r == 0 ? sq < o.sq ? -1 : 1 : r;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Queue<Runnable> getExecQueue(final State state) {
		return exec.get(state);
	}

	/**
	 * Returns the total running time.
	 *
	 * @return the total runtime so far in milliseconds (including pauses)
	 */
	public long getTotalRuntime() {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started.get());
	}

	/**
	 * Returns the actual running time.
	 *
	 * @return the actual runtime so far in milliseconds
	 */
	public long getRuntime() {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started.get() - suspended.get());
	}

	/**
	 * Returns the designated storage folder.
	 *
	 * @return a directory path where files can be saved to and read from
	 */
	public File getStorageDirectory() {
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}
		return dir;
	}

	/**
	 * Returns the {@link org.powerbot.script.Script.Manifest} attached to this {@link Script} if present.
	 *
	 * @return the attached {@link org.powerbot.script.Script.Manifest} if it exists, or {@code null} otherwise
	 */
	public Manifest getManifest() {
		return getClass().isAnnotationPresent(Manifest.class) ? getClass().getAnnotation(Manifest.class) : null;
	}

	/**
	 * Returns the name of this {@link Script} as determined by its {@link org.powerbot.script.Script.Manifest}.
	 *
	 * @return the name of this {@link Script}
	 */
	public String getName() {
		final Manifest manifest = getManifest();
		return manifest == null || manifest.name() == null ? "" : manifest.name();
	}

	/**
	 * Returns a {@link java.io.File} from an abstract local file name.
	 *
	 * @param name a local file name, which may contain path separators
	 * @return the fully qualified {@link java.io.File} inside the {@link #getStorageDirectory()}
	 */
	public File getFile(final String name) {
		File f = getStorageDirectory();

		for (final String part : name.split("\\|/")) {
			f = new File(f, part);
		}

		f.getParentFile().mkdirs();

		return f;
	}

	/**
	 * Downloads a file via HTTP/HTTPS. Server side caching is supported to reduce bandwidth.
	 *
	 * @param url  the HTTP/HTTPS address of the remote resource to download
	 * @param name a local file name, path separators are supported
	 * @return the {@link java.io.File} of the downloaded resource
	 */
	public File download(final String url, final String name) {
		final File f = getFile(name);

		final URL u;
		try {
			u = new URL(url);
		} catch (final MalformedURLException ignored) {
			return f;
		}

		try {
			HttpUtils.download(u, f);
		} catch (final IOException ignored) {
			f.delete();
		}

		return f;
	}

	/**
	 * Reads a HTTP/HTTPS resource into a string.
	 *
	 * @param url the HTTP/HTTPS address of the remote resource to read
	 * @return a string representation of the downloaded resource
	 */
	public String downloadString(final String url) {
		final String name = "http/" + Integer.toHexString(url.hashCode());
		download(url, name);
		FileInputStream in = null;
		try {
			in = new FileInputStream(getFile(name));
			return IOUtils.readString(in);
		} catch (final IOException ignored) {
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (final IOException ignored) {
				}
			}
		}
		return "";
	}

	/**
	 * Returns a downloaded image resource as a usable {@link java.awt.image.BufferedImage}.
	 *
	 * @param url the HTTP/HTTPS address of the remote image file
	 * @return a {@link java.awt.image.BufferedImage}, which will be a blank 1x1 pixel if the remote image failed to download
	 */
	public BufferedImage downloadImage(final String url) {
		final Adler32 c = new Adler32();
		c.update(StringUtils.getBytesUtf8(url));
		final File f = download(url, "images/" + Long.toHexString(c.getValue()));
		try {
			return ImageIO.read(f);
		} catch (final IOException ignored) {
			f.delete();
			return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		}
	}

	public void openURL(final String url) {
		final String host;
		try {
			host = "." + new URL(url).getHost();
		} catch (final MalformedURLException ignored) {
			return;
		}

		final List<String> whitelist = new ArrayList<String>();
		whitelist.add(Configuration.URLs.DOMAIN);
		whitelist.add(Configuration.URLs.GAME);

		for (final String w : whitelist) {
			if (host.endsWith("." + w)) {
				BotChrome.openURL(url);
				return;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final String name = getName();
		return name == null || name.isEmpty() ? getClass().getSimpleName() : name;
	}
}
