package org.powerbot.bot.rt6;

import java.applet.Applet;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.powerbot.Configuration;
import org.powerbot.bot.loader.GameAppletLoader;
import org.powerbot.bot.loader.GameLoader;
import org.powerbot.bot.loader.GameStub;
import org.powerbot.bot.loader.LoaderUtils;
import org.powerbot.bot.loader.Transformer;
import org.powerbot.bot.loader.transform.TransformSpec;
import org.powerbot.bot.rt6.client.Client;
import org.powerbot.bot.rt6.client.Constants;
import org.powerbot.bot.rt6.event.EventDispatcher;
import org.powerbot.bot.rt6.loader.AbstractBridge;
import org.powerbot.bot.rt6.loader.Application;
import org.powerbot.bot.rt6.loader.bytecode.AbstractProcessor;
import org.powerbot.bot.rt6.loader.bytecode.AppletTransform;
import org.powerbot.bot.rt6.loader.bytecode.ClassLoaderTransform;
import org.powerbot.bot.rt6.loader.bytecode.ListClassesTransform;
import org.powerbot.gui.BotChrome;
import org.powerbot.script.Condition;
import org.powerbot.script.rt6.ClientContext;

public final class Bot extends org.powerbot.script.Bot {
	private static final String GV = "6";
	private static final Logger log = Logger.getLogger(Bot.class.getName());
	public final ClientContext ctx;

	public Bot(final BotChrome chrome) {
		super(chrome, new EventDispatcher());
		ctx = ClientContext.newContext(this);
	}

	@Override
	public ClientContext ctx() {
		return ctx;
	}

	@Override
	public void run() {
		log.info("Loading bot");
		final GameCrawler gameCrawler = new GameCrawler();
		if (!gameCrawler.call()) {
			log.severe("Failed to crawl game");
			return;
		}
		final AppletTransform appletTransform = new AppletTransform();
		final GameLoader game = new GameLoader(gameCrawler.archive, gameCrawler.game) {
			@Override
			protected Transformer transformer() {
				return new AbstractProcessor(appletTransform,
						new ClassLoaderTransform(appletTransform), new ListClassesTransform(appletTransform)
				);
			}
		};

		final ClassLoader loader;
		try {
			loader = game.call();
		} catch (final Exception ignored) {
			return;
		}
		if (loader == null) {
			log.severe("Failed to load game");
			return;
		}
		if (gameCrawler.properties.containsKey("title")) {
			chrome.setTitle(gameCrawler.properties.get("title"));
		}
		final GameAppletLoader bootstrap = new GameAppletLoader(loader, gameCrawler.clazz) {
			@Override
			protected void sequence(final Applet applet) {
				Bot.this.sequence(game, gameCrawler, applet);
			}
		};
		Thread.currentThread().setContextClassLoader(loader);
		final Thread t = new Thread(threadGroup, bootstrap);
		t.setContextClassLoader(loader);
		t.start();
	}

	private void sequence(final GameLoader game, final GameCrawler gameCrawler, final Applet applet) {
		final byte[] inner = game.resource("inner.pack.gz");
		final String h;
		if (inner == null || (h = LoaderUtils.hash(inner)) == null) {
			return;
		}
		TransformSpec spec;
		try {
			spec = LoaderUtils.get(GV, h);
			spec.adapt();
		} catch (final IOException e) {
			if (!(e.getCause() instanceof IllegalStateException)) {
				log.severe("Failed to load transform specification");
				return;
			}
			spec = null;
		}

		final AbstractBridge bridge = new AbstractBridge(spec) {
			@Override
			public void instance(final Object client) {
				ctx.client((Client) client);
			}
		};
		((Application) applet).setBridge(bridge);

		this.applet = applet;
		final GameStub stub = new GameStub(gameCrawler.parameters, gameCrawler.archive);
		applet.setStub(stub);
		applet.setSize(BotChrome.PANEL_MIN_WIDTH, BotChrome.PANEL_MIN_HEIGHT);
		applet.setMinimumSize(applet.getSize());
		applet.init();

		if (spec == null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					LoaderUtils.submit(log, GV, h, bridge.loaded);
				}
			}).start();
			return;
		}

		setClient(ctx.client(), spec);
		applet.start();
		new Thread(threadGroup, dispatcher, dispatcher.getClass().getName()).start();

		final boolean safemode;
		safemode = Configuration.OS == Configuration.OperatingSystem.MAC && !System.getProperty("java.version").startsWith("1.6");

		if (safemode) {
			new Thread(threadGroup, new SafeMode()).start();
		}

		display();
	}

	private void setClient(final Client client, final TransformSpec spec) {
		ctx.client(client);
		client.setCallback(new AbstractCallback(this));
		ctx.constants.set(new Constants(spec.constants));
	}

	private final class SafeMode implements Runnable {
		@Override
		public void run() {
			if (Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					final java.awt.Component c = ctx.client().getCanvas();
					return c != null && c.getKeyListeners().length > 0;//TODO: ??
				}
			})) {
				ctx.keyboard.send("s");
			}
		}
	}
}
