package org.powerbot;

import java.io.File;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.zip.Adler32;

import org.powerbot.misc.Resources;
import org.powerbot.util.StringUtils;

public class Configuration {
	public static final String NAME = "RSBot";
	public static final int VERSION = 5068;

	public static final boolean FROMJAR;
	public static final OperatingSystem OS;
	public static final File HOME, TEMP;

	public enum OperatingSystem {
		MAC, WINDOWS, LINUX, UNKNOWN
	}

	private static final String PROTO;

	public static final class URLs {
		static final boolean TESTING = false;

		public static final String DOMAIN = "powerbot.org";
		static final String DOMAIN_SITE_LIVE = "www." + DOMAIN;
		static final String DOMAIN_SITE_CDN = "cdn." + DOMAIN;
		static final String DOMAIN_SITE_TESTING = DOMAIN + ".localdomain";
		static final String DOMAIN_SITE = TESTING ? DOMAIN_SITE_TESTING : DOMAIN_SITE_LIVE;

		public static final String VERSION = "http://" + DOMAIN_SITE_CDN + "/rsbot/version.txt";
		public static final String CLIENTPATCH = Configuration.PROTO + DOMAIN_SITE_CDN + "/rsbot/ts/%s.ts";
		public static final String CLIENTBUCKET = "http://buckets." + DOMAIN + "/process/?hash=%s";
		public static final String SCRIPTSCOLLECTION = Configuration.PROTO + DOMAIN_SITE + "/scripts/api/collection/?{POST}a=%s";
		public static final String SIGNIN = Configuration.PROTO + DOMAIN_SITE + "/rsbot/login/?{POST}u=%s&p=%s&a=%s";
		public static final String LICENSE = "http://" + DOMAIN_SITE_LIVE + "/terms/license/";
		public static final String ADS = "http://" + DOMAIN_SITE_CDN + "/rsbot/ads.txt";

		public static final String GAME = "runescape.com";
		public static final String GAME_SERVICES_DOMAIN = "services." + GAME;
	}

	static {
		FROMJAR = Configuration.class.getClassLoader().getResource(Resources.Paths.ICON) != null;

		final String os = System.getProperty("os.name");
		if (os.contains("Mac")) {
			OS = OperatingSystem.MAC;
		} else if (os.contains("Windows")) {
			OS = OperatingSystem.WINDOWS;
		} else if (os.contains("Linux")) {
			OS = OperatingSystem.LINUX;
		} else {
			OS = OperatingSystem.UNKNOWN;
		}

		final String jre = System.getProperty("java.version");
		PROTO = OS == OperatingSystem.MAC && jre != null && jre.startsWith("1.6") ? "http://" : "https://";

		if (OS == OperatingSystem.WINDOWS) {
			HOME = new File(System.getenv("APPDATA"), NAME);
		} else {
			final String user = System.getProperty("user.home");
			final File lib = new File(user, "/Library/");
			if (OS == OperatingSystem.MAC && lib.isDirectory()) {
				HOME = new File(lib, NAME);
			} else {
				HOME = new File(System.getProperty("user.home"), "." + NAME.toLowerCase());
			}
		}

		if (!HOME.isDirectory()) {
			HOME.mkdirs();
		}

		TEMP = new File(System.getProperty("java.io.tmpdir"));
	}

	public static long getUID() {
		final Adler32 c = new Adler32();
		c.update(StringUtils.getBytesUtf8(Configuration.NAME));

		final Enumeration<NetworkInterface> e;
		try {
			e = NetworkInterface.getNetworkInterfaces();
		} catch (final SocketException ignored) {
			return c.getValue();
		}

		while (e.hasMoreElements()) {
			byte[] a = null;
			try {
				a = e.nextElement().getHardwareAddress();
			} catch (final SocketException ignored) {
			}
			if (a != null) {
				c.update(a);
			}
		}

		return c.getValue();
	}
}
