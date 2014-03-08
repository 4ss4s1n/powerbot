package org.powerbot.script.os;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.powerbot.bot.os.client.Client;

public class Players extends PlayerQuery<Player> {
	public Players(final ClientContext ctx) {
		super(ctx);
	}

	@Override
	public List<Player> get() {
		final List<Player> r = new CopyOnWriteArrayList<Player>();
		final Client client = ctx.client();
		if (client == null) {
			return r;
		}
		final int[] indices = client.getPlayerIndices();
		final org.powerbot.bot.os.client.Player[] players = client.getPlayers();
		if (indices == null || players == null) {
			return r;
		}
		for (final int k : indices) {
			final org.powerbot.bot.os.client.Player p = players[k];
			if (p != null) {
				r.add(new Player(ctx, p));
			}
		}
		return r;
	}

	public Player local() {
		final Player r = new Player(ctx, null);
		final Client client = ctx.client();
		if (client == null) {
			return r;
		}
		return new Player(ctx, client.getPlayer());
	}

	@Override
	public Player getNil() {
		return new Player(ctx, null);
	}
}
