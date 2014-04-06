package org.powerbot.script;

import java.util.Collection;
import java.util.EventListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.powerbot.bot.ScriptEventDispatcher;

/**
 * A context class which interlinks all core classes for a {@link org.powerbot.script.Bot}.
 *
 * @param <C> the bot client
 */
public abstract class ClientContext<C extends Client> {
	private final AtomicReference<Bot<? extends ClientContext<C>>> bot;
	private final AtomicReference<C> client;

	/**
	 * A table of key/value pairs representing environmental properties.
	 */
	public final Map<String, String> properties;
	/**
	 * A collection representing the event listeners attached to the {@link org.powerbot.script.Bot}.
	 */
	public final Collection<EventListener> dispatcher;

	/**
	 * Creates a new context with the given {@link org.powerbot.script.Bot}.
	 *
	 * @param bot the bot
	 */
	protected ClientContext(final Bot<? extends ClientContext<C>> bot) {
		this.bot = new AtomicReference<Bot<? extends ClientContext<C>>>(bot);
		client = new AtomicReference<C>(null);
		properties = new ConcurrentHashMap<String, String>();
		dispatcher = new ScriptEventDispatcher<C, EventListener>(this);
	}

	/**
	 * Creates a chained context.
	 *
	 * @param ctx the parent context
	 */
	protected ClientContext(final ClientContext<C> ctx) {
		bot = ctx.bot;
		client = ctx.client;
		properties = ctx.properties;
		dispatcher = ctx.dispatcher;
	}

	/**
	 * Returns the client version.
	 *
	 * @return the client version, which is {@code 6} for {@code rt6} and {@code} 4 for {@code rt4}
	 */
	public abstract String rtv();

	/**
	 * Returns the bot.
	 *
	 * @return the bot
	 */
	public final Bot<? extends ClientContext<C>> bot() {
		return bot.get();
	}

	/**
	 * Returns the client.
	 *
	 * @return the client.
	 */
	public final C client() {
		return client.get();
	}

	/**
	 * Sets the client.
	 *
	 * @param c the new client
	 * @return the previous value, which may be {@code null}
	 */
	public final C client(final C c) {
		return client.getAndSet(c);
	}

	/**
	 * Returns the script controller.
	 *
	 * @return the script controller
	 */
	public abstract Script.Controller controller();

	/**
	 * Returns the property value for the specified key, or an empty string as the default value.
	 *
	 * @param k the key to lookup
	 * @return the value for the specified key, otherwise an empty string if the requested entry does not exist
	 * @see #properties
	 */
	public final String property(final String k) {
		return property(k, "");
	}

	/**
	 * Returns the property value for the specified key, or a default value.
	 *
	 * @param k the key to lookup
	 * @param d the default value
	 * @return the value for the specified key, otherwise the default value if the requested entry does not exist
	 * @see #properties
	 */
	public final String property(final String k, final String d) {
		if (k == null || k.isEmpty()) {
			return "";
		}
		final String v = properties.get(k);
		return v == null || v.isEmpty() ? d : v;
	}
}
