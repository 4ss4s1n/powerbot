package org.powerbot.misc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.powerbot.Configuration;
import org.powerbot.gui.BotChrome;
import org.powerbot.script.AbstractScript;
import org.powerbot.script.Bot;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Script;
import org.powerbot.bot.InternalScript;
import org.powerbot.bot.ScriptClassLoader;
import org.powerbot.bot.ScriptController;
import org.powerbot.bot.rt6.daemon.Login;
import org.powerbot.util.Ini;
import org.powerbot.util.StringUtils;
import org.powerbot.util.TarReader;

public class ScriptList {
	private final static Logger log = Logger.getLogger(ScriptList.class.getName());

	public static List<ScriptBundle.Definition> getList() throws IOException {
		final List<ScriptBundle.Definition> list = new ArrayList<ScriptBundle.Definition>();

		if (NetworkAccount.getInstance().hasPermission(NetworkAccount.LOCALSCRIPTS)) {
			for (final String s : System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator))) {
				final File f = new File(s);
				if (f.isDirectory()) {
					getLocalList(list, f, null);
				}
			}
		}

		getNetworkList(list);

		return list;
	}

	private static void getNetworkList(final List<ScriptBundle.Definition> list) throws IOException {
		final Ini t = new Ini();
		InputStream is = null;
		try {
			is = NetworkAccount.getInstance().getScriptsList();
			t.read(is);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (final IOException ignored) {
				}
			}
		}

		for (final Map.Entry<String, Ini.Member> entry : t.entrySet()) {
			final Ini.Member params = entry.getValue();

			final ScriptBundle.Definition def = ScriptBundle.Definition.fromMap(params.getMap());
			if (def != null && params.has("link") && params.has("className") && params.has("key")) {
				def.source = params.get("link");
				def.className = params.get("className");

				final byte[] key = StringUtils.hexStringToByteArray(params.get("key")), kx = new byte[key.length * 2];
				new SecureRandom().nextBytes(kx);
				for (int i = 0; i < key.length; i++) {
					kx[i * 2] = key[i];
				}
				def.key = kx;

				list.add(def);
			}
		}
	}

	private static void getLocalList(final List<ScriptBundle.Definition> list, final File parent, final File dir) {
		if (!NetworkAccount.getInstance().hasPermission(NetworkAccount.LOCALSCRIPTS)) {
			return;
		}
		final File[] files = (dir == null ? parent : dir).listFiles();
		if (files == null || files.length == 0) {
			return;
		}

		for (final File file : files) {
			if (file.isDirectory()) {
				getLocalList(list, parent, file);
			} else if (file.isFile()) {
				final String name = file.getName();
				if (name.endsWith(".class") && name.indexOf('$') == -1) {
					try {
						final URL src = parent.getCanonicalFile().toURI().toURL();
						final ClassLoader cl = new URLClassLoader(new URL[]{src});
						String className = file.getCanonicalPath().substring(parent.getCanonicalPath().length() + 1);
						className = className.substring(0, className.lastIndexOf('.'));
						className = className.replace(File.separatorChar, '.');
						final Class<?> clazz;
						try {
							clazz = cl.loadClass(className);
						} catch (final Throwable ignored) {
							continue;
						}
						if (AbstractScript.class.isAssignableFrom(clazz) && !InternalScript.class.isAssignableFrom(clazz)) {
							final Class<? extends AbstractScript> script = clazz.asSubclass(AbstractScript.class);
							if (script.isAnnotationPresent(Script.Manifest.class)) {
								final Script.Manifest m = script.getAnnotation(Script.Manifest.class);
								final ScriptBundle.Definition def = new ScriptBundle.Definition(m);
								def.source = parent.getCanonicalFile().toString();
								def.className = className;
								def.local = true;

								final Class<?>[] superClass = {script, null};
								while ((superClass[1] = superClass[0].getSuperclass()) != AbstractScript.class
										&& superClass[1] != PollingScript.class && superClass[1] != Object.class) {
									superClass[0] = superClass[1];
								}

								final Type pt = superClass[0].getGenericSuperclass();
								if (pt instanceof ParameterizedType) {
									final Type[] t = ((ParameterizedType) pt).getActualTypeArguments();
									if (t != null && t.length > 0) {
										def.client = t[0];
									}
								}

								list.add(def);
							}
						}
					} catch (final IOException ignored) {
					}
				}
			}
		}

	}

	public static void load(final BotChrome chrome, final ScriptBundle.Definition def, final String username) {
		if (!NetworkAccount.getInstance().isLoggedIn()) {
			return;
		}

		CryptFile cache = null;
		final ClassLoader cl;
		if (def.local) {
			try {
				cl = new ScriptClassLoader(new File(def.source).toURI().toURL());
			} catch (final Exception ignored) {
				return;
			}
		} else {
			try {
				final byte[] buf = new byte[def.key.length / 2], key = new byte[16];
				for (int i = 0; i < buf.length; i++) {
					buf[i] = def.key[i * 2];
				}
				final Inflater inf = new Inflater();
				inf.setInput(buf);
				inf.inflate(key, 0, key.length);
				inf.end();
				final Cipher c = Cipher.getInstance("RC4");
				c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, 0, key.length, "ARCFOUR"));
				cache = new CryptFile("script.1-" + def.getID().replace('/', '-'), ScriptList.class, ScriptClassLoader.class);
				final InputStream in = cache.download(new URL(def.source));
				cl = new ScriptClassLoader(new TarReader(new GZIPInputStream(new CipherInputStream(in, c))));
			} catch (final Exception ignored) {
				log.severe("Could not download script");
				ignored.printStackTrace();
				return;
			}
		}

		final Class<? extends Script> script;
		try {
			script = cl.loadClass(def.className).asSubclass(Script.class);
		} catch (final Exception ignored) {
			if (cache != null) {
				cache.delete();
			}
			log.severe("Error loading script");
			if (!Configuration.FROMJAR) {
				ignored.printStackTrace();
			}
			return;
		}

		final Bot bot = chrome.bot.get();
		if (username != null) {
			bot.ctx().properties.put(Login.LOGIN_USER_PROPERTY, username);
		}

		log.info("Starting script: " + def.getName());
		int hours = 0;
		String msg = null;

		if (def.local) {
			final boolean dev = NetworkAccount.getInstance().hasPermission(NetworkAccount.DEVELOPER);
			hours = dev ? 3 : 1;
			if (!dev) {
				msg = "Apply for a developer account for extended time.";
			}
		} else if (!def.assigned && !NetworkAccount.getInstance().hasPermission(NetworkAccount.VIP)) {
			hours = 2;
			msg = "VIP subscribers and Premium scripts have no time limits.";
		}

		if (hours != 0) {
			msg = "The script will automatically stop after " + hours + " hour" + (hours == 1 ? "" : "s") + "." +
					(msg == null || msg.isEmpty() ? "" : "\n" + msg);
			log.warning(msg.replace('\n', ' '));

			if (!def.local) {
				final AtomicInteger res = new AtomicInteger(-1);
				final String txt = msg;
				final Runnable r = new Runnable() {
					@Override
					public void run() {
						res.set(JOptionPane.showConfirmDialog(chrome, txt, "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE));
					}
				};


				if (SwingUtilities.isEventDispatchThread()) {
					r.run();
				} else {
					try {
						SwingUtilities.invokeAndWait(r);
					} catch (final InterruptedException ignored) {
					} catch (final InvocationTargetException ignored) {
					}
				}

				if (res.get() != JOptionPane.OK_OPTION) {
					return;
				}
			}

			bot.ctx().properties.put(ScriptController.TIMEOUT_PROPERTY, Long.toString(TimeUnit.HOURS.toMillis(hours)));
		}

		final NetworkAccount n = NetworkAccount.getInstance();
		if (n.isLoggedIn()) {
			bot.ctx().properties.put("user.id", Integer.toString(n.getUID()));
			bot.ctx().properties.put("user.name", n.getDisplayName());
			bot.ctx().properties.put("user.vip", Boolean.toString(n.hasPermission(NetworkAccount.VIP)));
		}

		bot.ctx().properties.put(ScriptController.LOCAL_PROPERTY, Boolean.toString(def.local));
		final ScriptController c = (ScriptController) bot.ctx().controller();
		c.bundle.set(new ScriptBundle(def, script));
		c.run();
	}
}
