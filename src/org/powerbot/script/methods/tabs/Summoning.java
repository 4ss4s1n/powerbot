package org.powerbot.script.methods.tabs;

import org.powerbot.script.methods.ClientFactory;
import org.powerbot.script.methods.ClientLink;
import org.powerbot.script.util.Delay;
import org.powerbot.script.util.Filter;
import org.powerbot.script.util.Filters;
import org.powerbot.script.util.Timer;
import org.powerbot.script.wrappers.Actor;
import org.powerbot.script.wrappers.Component;
import org.powerbot.script.wrappers.Npc;
import org.powerbot.script.wrappers.Player;

public class Summoning extends ClientLink {
	public static final int WIDGET_FOLLOWER_DETAILS = 662;
	public static final int COMPONENT_SUMMONING_ORB = 747;
	public static final int COMPONENT_SET_LEFT = 880;
	public static final int COMPONENT_INTERACT = 1188;

	public Summoning(ClientFactory factory) {
		super(factory);
	}

	public int getPoints() {
		return ctx.skills.getLevel(Skills.SUMMONING);
	}

	public int getSpecialPoints() {
		return ctx.settings.get(1787);
	}

	public boolean select(final Option option) {
		return select(option.getText());
	}

	public boolean select(final String action) {
		final Component c = ctx.widgets.get(COMPONENT_SUMMONING_ORB, 2);
		if (c == null) return false;
		if (Option.RENEW_FAMILIAR.getText().toLowerCase().contains(action.toLowerCase())) {
			final Familiar familiar = getEnum();
			return familiar != null && familiar.getRequiredPoints() <= getPoints() && ctx.inventory.getCount(ctx.settings.get(1831)) > 0
					&& c.interact(action);
		}
		if (Option.DISMISS.getText().toLowerCase().contains(action.toLowerCase())) {
			if (c.interact(action)) {
				final Component c2 = ctx.widgets.get(COMPONENT_INTERACT, 3);
				if (c2 == null) return false;
				for (int i = 0; i < 50 && !c2.isValid(); i++) Delay.sleep(20);
				return c2.click(true);
			}
			return false;
		}
		if (Option.CAST.getText().toLowerCase().contains(action.toLowerCase())) {
			final Familiar familiar = getEnum();
			return familiar != null && familiar.getRequiredSpecialPoints() <= getSpecialPoints()
					&& c.interact(action);
		}
		return c.interact(action);
	}

	public Option getLeftClickOption() {
		for (Option o : Option.values()) if (ctx.settings.get(1789) == o.action()) return o;
		return Option.FOLLOWER_DETAILS;
	}

	public boolean setLeftClickOption(final Option option) {
		if (ctx.settings.get(1789) == option.action()) return true;
		final Component c = ctx.widgets.get(COMPONENT_SUMMONING_ORB, 2);
		if (c == null || !c.interact("Select")) return false;
		if (!isFamiliarSummoned()) {
			final Timer timer = new Timer(800);
			while (timer.isRunning() && !ctx.menu.isOpen()) {
				Delay.sleep(15);
			}
			if (!ctx.menu.click("Select")) {
				return false;
			}
		}
		final Component c2 = ctx.widgets.get(COMPONENT_SET_LEFT, 5);
		if (c2 == null) return false;
		final Timer timer = new Timer(2000);
		while (timer.isRunning() && !c2.isValid()) Delay.sleep(15);
		Delay.sleep(200);
		if (ctx.widgets.get(COMPONENT_SET_LEFT, option.getId()).interact("Select")) {
			final Timer t = new Timer(800);
			while (t.isRunning() && ctx.settings.get(1790) != option.selected()) Delay.sleep(15);
		} else {
			c2.interact("Confirm");
			return false;
		}
		return c2.interact("Confirm");
	}

	public boolean isCastOrAttackSelected() {
		final Component c = ctx.widgets.get(COMPONENT_SUMMONING_ORB, 2);
		return c != null && c.getBorderThickness() == 2;
	}

	public int getTimeLeft() {
		return Math.round((ctx.settings.get(1786) / (float) 2.13333333333));
	}

	public boolean isFamiliarSummoned() {
		return getTimeLeft() > 0 && ctx.settings.get(1831) > 0;
	}

	public boolean summonFamiliar(final Familiar familiar) {
		return ctx.inventory.getCount(familiar.getPouchId()) > 0 && ctx.skills.getRealLevel(Skills.SUMMONING) >= familiar.getRequiredLevel() &&
				getPoints() >= familiar.getRequiredPoints() && ctx.inventory.getItem(familiar.getPouchId()).getComponent().interact("Summon");
	}

	public Npc getFamiliar() {
		if (!isFamiliarSummoned()) {
			return null;
		}
		final Player local = ctx.players.getLocal();
		if (local == null) return null;
		Npc[] npcs = ctx.npcs.getLoaded();
		npcs = Filters.filter(npcs, new Filter<Npc>() {
			@Override
			public boolean accept(Npc npc) {
				final Actor actor;
				return npc.getId() == ctx.settings.get(1784) && (actor = npc.getInteracting()) != null && actor.equals(local);
			}
		});
		return Filters.nearest(npcs, ctx.players.getLocal());
	}

	public Familiar getEnum() {
		if (!isFamiliarSummoned()) return null;
		for (final Familiar f : Familiar.values()) if (f.getPouchId() == ctx.settings.get(1831)) return f;
		return null;
	}

	public boolean callFamiliar() {
		final Component c = ctx.widgets.get(WIDGET_FOLLOWER_DETAILS, 49);
		return c != null && isFamiliarSummoned() && c.isVisible() && c.interact("Call");
	}

	public boolean dismissFamiliar() {
		final Component c = ctx.widgets.get(WIDGET_FOLLOWER_DETAILS, 51);
		if (c == null || !isFamiliarSummoned() || !c.isVisible()) return false;
		final Component c2 = ctx.widgets.get(WIDGET_FOLLOWER_DETAILS, 51);
		if (c2 == null || !c2.interact("Dismiss")) return false;
		final Timer timer = new Timer(1500);
		final Component c3 = ctx.widgets.get(COMPONENT_INTERACT, 20);
		if (c3 == null) return false;
		while (timer.isRunning() && !c3.isValid()) Delay.sleep(15);
		return ctx.widgets.get(COMPONENT_INTERACT, 3).click(true);
	}

	public boolean takeBoB() {
		final Component c = ctx.widgets.get(WIDGET_FOLLOWER_DETAILS, 67);
		return c != null && isFamiliarSummoned() && c.isVisible() && c.interact("Take");
	}

	public boolean renewFamiliar() {
		final Component c = ctx.widgets.get(WIDGET_FOLLOWER_DETAILS, 69);
		return c != null && isFamiliarSummoned() && c.isVisible() && c.interact("Renew");
	}

	public boolean cast() {
		final Component c = ctx.widgets.get(WIDGET_FOLLOWER_DETAILS, 5);
		return c != null && isFamiliarSummoned() && c.isVisible() && c.interact("Cast");
	}

	public boolean attack() {
		final Component c = ctx.widgets.get(WIDGET_FOLLOWER_DETAILS, 65);
		return c != null && isFamiliarSummoned() && c.isVisible() && c.interact("Attack");
	}

	public static enum Familiar {
		SPIRIT_WOLF(12047, 1, 0, 1, 12533, 3),
		DREADFOWL(12043, 4, 0, 1, 12445, 3),
		MEERKATS(19622, 4, 0, 1, 19621, 12),
		SPIRIT_SPIDER(12059, 10, 0, 2, 12428, 6),
		THORNY_SNAIL(12019, 13, 3, 2, 12459, 3),
		GRANITE_CRAB(12009, 16, 0, 2, 12533, 12),
		SPIRIT_MOSQUITO(12778, 17, 0, 2, 12838, 3),
		DESERT_WYRM(12049, 18, 0, 1, 12460, 6),
		SPIRIT_SCORPION(12055, 19, 0, 2, 12432, 6),
		SPIRIT_TZ_KIH(12808, 22, 0, 3, 12839, 6),
		ALBINO_RAT(12067, 23, 0, 3, 12430, 6),
		SPIRIT_KALPHITE(12063, 25, 6, 3, 12446, 6),
		COMPOST_MOUND(12091, 28, 0, 6, 12440, 12),
		GIANT_CHINCHOMPA(12800, 29, 0, 1, 12834, 3),
		VAMPYRE_BAT(12053, 31, 0, 4, 12447, 4),
		HONEY_BADGER(12065, 32, 0, 4, 12433, 4),
		BEAVER(12021, 33, 0, 4, 12429, 3),
		VOID_RAVAGER(12818, 34, 0, 4, 12443, 3),
		VOID_SPINNER(12780, 34, 0, 4, 12443, 3),
		VOID_SHIFTER(12814, 34, 0, 4, 12443, 3),
		VOID_TORCHER(12798, 34, 0, 4, 12443, 3),
		BRONZE_MINOTAUR(12073, 36, 0, 9, 12462, 6),
		BULL_ANT(12087, 40, 8, 5, 12431, 12),
		MACAW(12071, 41, 0, 5, 12422, 12),
		EVIL_TURNIP(12051, 42, 0, 5, 12448, 6),
		SPIRIT_COCKATRICE(12095, 43, 0, 5, 12458, 3),
		SPIRIT_GUTHATRICE(12097, 43, 0, 5, 12458, 3),
		SPIRIT_SARATRICE(12099, 43, 0, 5, 12458, 3),
		SPIRIT_ZAMATRICE(12101, 43, 0, 5, 12458, 3),
		SPIRIT_PENGATRICE(12103, 43, 0, 5, 12458, 3),
		SPIRIT_CORAXATRICE(12105, 43, 0, 5, 12458, 3),
		SPIRIT_VULATRICE(12107, 43, 0, 5, 12458, 3),
		IRON_MINOTAUR(12075, 46, 0, 9, 12463, 6),
		PYRELORD(12816, 46, 0, 5, 12829, 6),
		MAGPIE(12041, 47, 0, 5, 12426, 3),
		BLOATED_LEECH(12061, 49, 0, 5, 12444, 6),
		SPIRIT_TERRORBIRD(12007, 52, 12, 6, 12441, 8),
		ABYSSAL_PARASITE(12035, 54, 7, 6, 12454, 6),
		SPIRIT_JELLY(12027, 55, 0, 6, 12453, 6),
		STEEL_MINOTAUR(12077, 56, 0, 9, 12464, 6),
		IBIS(12531, 56, 0, 6, 12424, 12),
		SPIRIT_GRAAHK(12810, 57, 0, 6, 12835, 3),
		SPIRIT_KYATT(12812, 57, 0, 6, 12836, 3),
		SPIRIT_LARUPIA(12784, 57, 0, 6, 12840, 6),
		KARAMTHULHU_OVERLORD(12023, 58, 0, 6, 12455, 3),
		SMOKE_DEVIL(12085, 61, 0, 7, 12468, 6),
		ABYSSAL_LURKER(12037, 62, 7, 7, 12427, 3),
		SPIRIT_COBRA(12015, 63, 0, 7, 12436, 3),
		STRANGER_PLANT(12045, 64, 0, 7, 12467, 6),
		MITHRIL_MINOTAUR(12079, 66, 0, 9, 12465, 6),
		BARKER_TOAD(12123, 66, 0, 7, 12452, 6),
		WAR_TORTOISE(12031, 67, 18, 7, 12439, 20),
		BUNYIP(12029, 68, 0, 7, 12438, 3),
		FRUIT_BAT(12033, 69, 0, 7, 12423, 6),
		RAVENOUS_LOCTUS(12820, 70, 0, 4, 12830, 12),
		ARTIC_BEAR(12057, 71, 0, 8, 12451, 6),
		PHOENIX(14623, 72, 0, 8, 14622, 5),
		OBSIDIAN_GOLEM(12792, 73, 0, 8, 12826, 12),
		GRANITE_LOBSTER(12069, 74, 0, 8, 12449, 6),
		PRAYING_MANTRIS(12011, 75, 0, 8, 12450, 6),
		ADAMANT_MINOTAUR(12081, 76, 0, 9, 12466, 6),
		FORGE_REGENT(12782, 76, 0, 9, 12841, 6),
		TALON_BEAST(12794, 77, 0, 9, 12831, 6),
		GIANT_ENT(12013, 78, 0, 8, 12457, 6),
		FIRE_TITAN(12802, 79, 0, 9, 12824, 20),
		ICE_TITAN(12806, 79, 0, 9, 12824, 20),
		MOSS_TITAN(12804, 79, 0, 9, 12824, 20),
		HYDRA(12025, 80, 0, 8, 12442, 6),
		SPIRIT_DAGANNOTH(12017, 83, 0, 9, 12456, 6),
		LAVA_TITAN(12788, 83, 0, 9, 12837, 4),
		SWAMP_TITAN(12776, 85, 0, 9, 12832, 6),
		RUNE_MINOTAUR(12083, 86, 0, 9, 12467, 6),
		GHAST_FAMILIAR(21444, 87, 0, 1, 21453, 20),
		UNICORN_STALLION(12039, 88, 0, 9, 12434, 20),
		GEYSER_TITAN(12786, 89, 0, 10, 12833, 6),
		WOLPERTINGER(12089, 92, 0, 10, 12437, 20),
		ABYSSAL_TITAN(12796, 93, 7, 10, 12827, 6),
		IRON_TITAN(12822, 95, 0, 10, 12828, 12),
		PACK_YAK(12093, 96, 30, 10, 12435, 12),
		STEEL_TITAN(12790, 99, 0, 10, 12825, 12);
		private final int pouch, level, space, points, scroll, special;

		Familiar(final int pouch, final int level, final int space, final int points, final int scroll, final int special) {
			this.pouch = pouch;
			this.level = level;
			this.space = space;
			this.points = points;
			this.special = special;
			this.scroll = scroll;
		}

		public int getPouchId() {
			return this.pouch;
		}

		public int getRequiredLevel() {
			return this.level;
		}

		public int getBoBSpace() {
			return this.space;
		}

		public int getRequiredPoints() {
			return this.points;
		}

		public int getScrollId() {
			return this.scroll;
		}

		public int getRequiredSpecialPoints() {
			return this.special;
		}

	}

	public static enum Option {
		FOLLOWER_DETAILS("Follower Details", 7, 0x10, 0x0),
		CAST("Cast", 9, 0x11, 0x1),
		ATTACK("Attack", 11, 0x12, 0x2),
		CALL_FOLLOWER("Call Follower", 13, 0x13, 0x3),
		DISMISS("Dismiss", 15, 0x14, 0x4),
		TAKE_BOB("Take BoB", 17, 0x15, 0x5),
		RENEW_FAMILIAR("Renew Familiar", 19, 0x16, 0x6),
		INTERACT("Interact", 25, 0x17, 0x7);
		private final String text;
		private final int id, setting, set;

		Option(final String text, final int id, final int setting, final int set) {
			this.text = text;
			this.id = id;
			this.setting = setting;
			this.set = set;
		}

		public String getText() {
			return this.text;
		}

		public int getId() {
			return id;
		}

		public int action() {
			return setting;
		}

		public int selected() {
			return set;
		}
	}
}
