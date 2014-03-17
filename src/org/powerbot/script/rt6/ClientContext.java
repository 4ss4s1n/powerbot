package org.powerbot.script.rt6;

import java.util.concurrent.atomic.AtomicReference;

import org.powerbot.bot.KeyboardSimulator;
import org.powerbot.bot.ScriptController;
import org.powerbot.bot.rt6.Bot;
import org.powerbot.bot.rt6.client.Client;
import org.powerbot.bot.rt6.client.Constants;
import org.powerbot.bot.rt6.daemon.Antipattern;
import org.powerbot.bot.rt6.daemon.BankPin;
import org.powerbot.bot.rt6.daemon.Login;
import org.powerbot.bot.rt6.daemon.TicketDestroy;
import org.powerbot.bot.rt6.daemon.WidgetCloser;
import org.powerbot.bot.rt6.tools.Items;
import org.powerbot.bot.rt6.tools.Map;
import org.powerbot.script.Mouse;
import org.powerbot.script.Script;

public class ClientContext extends org.powerbot.script.ClientContext<Client> {
	public final AtomicReference<KeyboardSimulator> inputHandler;
	public final AtomicReference<Constants> constants;

	public final Script.Controller controller;

	public final CombatBar combatBar;
	public final Bank bank;
	public final Camera camera;
	public final Chat chat;
	public final DepositBox depositBox;
	public final Equipment equipment;
	public final Game game;
	public final GroundItems groundItems;
	public final HintArrows hintArrows;
	public final Hud hud;
	public final Backpack backpack;
	public final Keyboard keyboard;
	public final Lobby lobby;
	public final Menu menu;
	public final Mouse mouse;
	public final Movement movement;
	public final Npcs npcs;
	public final Objects objects;
	public final Players players;
	public final Powers powers;
	public final Projectiles projectiles;
	public final Varpbits varpbits;
	public final Skills skills;
	public final Summoning summoning;
	public final Widgets widgets;

	final Items items;
	final Map map;

	private ClientContext(final Bot bot) {
		super(bot);
		inputHandler = new AtomicReference<KeyboardSimulator>(null);
		constants = new AtomicReference<Constants>(null);

		controller = new ScriptController<ClientContext>(this);
		((ScriptController) controller).daemons = new Class[]{
				Login.class,
				WidgetCloser.class,
				TicketDestroy.class,
				BankPin.class,
				Antipattern.class,
		};

		combatBar = new CombatBar(this);
		backpack = new Backpack(this);
		bank = new Bank(this);
		camera = new Camera(this);
		chat = new Chat(this);
		depositBox = new DepositBox(this);
		equipment = new Equipment(this);
		game = new Game(this);
		groundItems = new GroundItems(this);
		hintArrows = new HintArrows(this);
		hud = new Hud(this);
		keyboard = new Keyboard(this);
		lobby = new Lobby(this);
		menu = new Menu(this);
		mouse = new Mouse(this);
		movement = new Movement(this);
		npcs = new Npcs(this);
		objects = new Objects(this);
		players = new Players(this);
		powers = new Powers(this);
		projectiles = new Projectiles(this);
		varpbits = new Varpbits(this);
		skills = new Skills(this);
		summoning = new Summoning(this);
		widgets = new Widgets(this);

		items = new Items(this);
		map = new Map(this);
	}

	public static ClientContext newContext(final Bot bot) {
		return new ClientContext(bot);
	}

	public ClientContext(final ClientContext ctx) {
		super(ctx.bot());

		inputHandler = ctx.inputHandler;
		constants = ctx.constants;

		controller = ctx.controller;

		combatBar = ctx.combatBar;
		backpack = ctx.backpack;
		bank = ctx.bank;
		camera = ctx.camera;
		chat = ctx.chat;
		depositBox = ctx.depositBox;
		equipment = ctx.equipment;
		game = ctx.game;
		groundItems = ctx.groundItems;
		hintArrows = ctx.hintArrows;
		hud = ctx.hud;
		keyboard = ctx.keyboard;
		lobby = ctx.lobby;
		menu = ctx.menu;
		mouse = ctx.mouse;
		movement = ctx.movement;
		npcs = ctx.npcs;
		objects = ctx.objects;
		players = ctx.players;
		powers = ctx.powers;
		projectiles = ctx.projectiles;
		varpbits = ctx.varpbits;
		skills = ctx.skills;
		summoning = ctx.summoning;
		widgets = ctx.widgets;

		items = ctx.items;
		map = ctx.map;
	}

	public final Script.Controller controller() {
		return controller;
	}
}
