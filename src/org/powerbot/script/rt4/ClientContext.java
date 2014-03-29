package org.powerbot.script.rt4;

import org.powerbot.bot.ScriptController;
import org.powerbot.bot.rt4.Bot;
import org.powerbot.bot.rt4.client.Client;
import org.powerbot.script.Mouse;
import org.powerbot.script.Script;

public class ClientContext extends org.powerbot.script.ClientContext<Client> {
	public final Script.Controller controller;

	public final Game game;
	public final GroundItems groundItems;
	public final Inventory inventory;
	public final Menu menu;
	public final Mouse<ClientContext> mouse;
	public final Movement movement;
	public final Npcs npcs;
	public final Objects objects;
	public final Players players;
	public final Varpbits varpbits;
	public final Widgets widgets;

	private ClientContext(final Bot bot) {
		super(bot);

		controller = new ScriptController<ClientContext>(this);

		game = new Game(this);
		groundItems = new GroundItems(this);
		inventory = new Inventory(this);
		menu = new Menu(this);
		mouse = new Mouse<ClientContext>(this);
		movement = new Movement(this);
		npcs = new Npcs(this);
		objects = new Objects(this);
		players = new Players(this);
		varpbits = new Varpbits(this);
		widgets = new Widgets(this);
	}

	public static ClientContext newContext(final Bot bot) {
		return new ClientContext(bot);
	}

	public ClientContext(final ClientContext ctx) {
		super(ctx.bot());

		controller = ctx.controller;

		game = ctx.game;
		groundItems = ctx.groundItems;
		inventory = ctx.inventory;
		menu = ctx.menu;
		mouse = ctx.mouse;
		movement = ctx.movement;
		npcs = ctx.npcs;
		objects = ctx.objects;
		players = ctx.players;
		varpbits = ctx.varpbits;
		widgets = ctx.widgets;
	}

	public Script.Controller controller() {
		return controller;
	}
}
