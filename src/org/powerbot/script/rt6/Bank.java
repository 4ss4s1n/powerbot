package org.powerbot.script.rt6;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.powerbot.script.Filter;
import org.powerbot.script.Condition;
import org.powerbot.script.Locatable;
import org.powerbot.script.Random;
import org.powerbot.script.Tile;
import org.powerbot.script.Viewport;
import org.powerbot.util.StringUtils;

/**
 * Utilities pertaining to the bank.
 *
 */
public class Bank extends ItemQuery<Item> implements Viewport {
	public static final int[] BANK_NPC_IDS = new int[]{
			44, 45, 166, 494, 495, 496, 497, 498, 499, 553, 909, 953, 958, 1036, 1360, 1702, 2163, 2164, 2354, 2355,
			2568, 2569, 2570, 2617, 2618, 2619, 2718, 2759, 3046, 3198, 3199, 3293, 3416, 3418, 3824, 4456, 4457,
			4458, 4459, 4519, 4907, 5257, 5258, 5259, 5260, 5488, 5776, 5777, 5901, 6200, 6362, 7049, 7050, 7605,
			8948, 9710, 13932, 14707, 14923, 14924, 14925, 15194, 16603, 16602, 19086
	};
	public static final int[] BANK_BOOTH_IDS = new int[]{
			782, 2213, 3045, 5276, 6084, 10517, 11338, 11758, 12759, 12798, 12799, 14369, 14370,
			16700, 19230, 20325, 20326, 20327, 20328, 22819, 24914, 25808, 26972, 29085, 34752, 35647,
			36262, 36786, 37474, 49018, 49019, 52397, 52589, 76274, 69024, 69023, 69022, 25688
	};
	public static final int[] BANK_COUNTER_IDS = new int[]{
			42217, 42377, 42378, 2012, 66665, 66666, 66667
	};
	public static final int[] BANK_CHEST_IDS = new int[]{
			2693, 4483, 8981, 12308, 14382, 20607, 21301, 27663, 42192, 57437, 62691, 83634, 81756, 79036, 83954
	};
	public static final Tile[] UNREACHABLE_BANK_TILES = new Tile[]{
			new Tile(3191, 3445, 0), new Tile(3180, 3433, 0)
	};
	private static final Filter<Interactive> UNREACHABLE_FILTER = new Filter<Interactive>() {
		@Override
		public boolean accept(final Interactive interactive) {
			if (interactive instanceof Locatable) {
				final Tile tile = ((Locatable) interactive).tile();
				for (final Tile bad : UNREACHABLE_BANK_TILES) {
					if (tile.equals(bad)) {
						return false;
					}
				}
			}
			return true;
		}
	};
	public static final int WIDGET = 762;
	public static final int COMPONENT_BUTTON_CLOSE = 50;
	public static final int COMPONENT_CONTAINER_ITEMS = 39;
	public static final int COMPONENT_BUTTON_WITHDRAW_MODE = 8;
	public static final int COMPONENT_BUTTON_DEPOSIT_INVENTORY = 12;
	public static final int COMPONENT_BUTTON_DEPOSIT_MONEY = 14;
	public static final int COMPONENT_BUTTON_DEPOSIT_EQUIPMENT = 16;
	public static final int COMPONENT_BUTTON_DEPOSIT_FAMILIAR = 18;
	public static final int COMPONENT_SCROLL_BAR = 40;
	public static final int SETTING_BANK_STATE = 110;
	public static final int SETTING_WITHDRAW_MODE = 160;

	public Bank(final ClientContext factory) {
		super(factory);
	}

	private Interactive getBank() {
		final Player p = ctx.players.local();
		final Tile t = p.tile();
		final Filter<Interactive> f = Interactive.areInViewport();

		ctx.npcs.select().id(BANK_NPC_IDS).select(f).select(UNREACHABLE_FILTER).nearest();
		ctx.objects.select().id(BANK_BOOTH_IDS, BANK_COUNTER_IDS, BANK_CHEST_IDS).select(f).select(UNREACHABLE_FILTER).nearest();
		if (!ctx.property("bank.antipattern").equals("disable")) {
			final Npc npc = ctx.npcs.poll();
			final GameObject object = ctx.objects.poll();
			return t.distanceTo(npc) < t.distanceTo(object) ? npc : object;
		}
		final double dist = Math.min(t.distanceTo(ctx.npcs.peek()), t.distanceTo(ctx.objects.peek()));
		final double d2 = Math.min(2d, Math.max(0d, dist - 1d));
		final List<Interactive> interactives = new ArrayList<Interactive>();
		ctx.npcs.within(dist + Random.nextInt(2, 5)).within(ctx.npcs.peek(), d2);
		ctx.objects.within(dist + Random.nextInt(2, 5)).within(ctx.objects.peek(), d2);
		ctx.npcs.addTo(interactives);
		ctx.objects.addTo(interactives);
		final int len = interactives.size();
		return len == 0 ? ctx.npcs.nil() : interactives.get(Random.nextInt(0, len));
	}

	/**
	 * Returns the absolute nearest bank for walking purposes. Do not use this to open the bank.
	 *
	 * @return the {@link Locatable} of the nearest bank or {@link Tile#NIL}
	 * @see #open()
	 */
	public Locatable nearest() {
		Locatable nearest = ctx.npcs.select().select(UNREACHABLE_FILTER).id(BANK_NPC_IDS).nearest().poll();

		final Tile loc = ctx.players.local().tile();
		for (final GameObject object : ctx.objects.select().select(UNREACHABLE_FILTER).
				id(BANK_BOOTH_IDS, BANK_COUNTER_IDS, BANK_CHEST_IDS).nearest().limit(1)) {
			if (loc.distanceTo(object) < loc.distanceTo(nearest)) {
				nearest = object;
			}
		}
		if (nearest.tile() != Tile.NIL) {
			return nearest;
		}
		return Tile.NIL;
	}

	/**
	 * Determines if a bank is present in the loaded region.
	 *
	 * @return <tt>true</tt> if a bank is present; otherwise <tt>false</tt>
	 */
	public boolean present() {
		return nearest() != Tile.NIL;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean inViewport() {
		return getBank().valid();
	}

	/**
	 * Determines if the bank is open.
	 *
	 * @return <tt>true</tt> is the bank is open; otherwise <tt>false</tt>
	 */
	public boolean opened() {
		return ctx.widgets.component(WIDGET, COMPONENT_CONTAINER_ITEMS).valid();
	}

	/**
	 * Opens a random in-view bank.
	 *
	 * Do not continue execution within the current poll after this method so BankPin may activate.
	 *
	 * @return <tt>true</tt> if the bank was opened; otherwise <tt>false</tt>
	 */
	public boolean open() {
		if (opened()) {
			return true;
		}
		final Interactive interactive = getBank();
		final int id;
		if (interactive.valid()) {
			if (interactive instanceof Npc) {
				id = ((Npc) interactive).id();
			} else if (interactive instanceof GameObject) {
				id = ((GameObject) interactive).id();
			} else {
				id = -1;
			}
		} else {
			id = -1;
		}
		if (id == -1) {
			return false;
		}
		int index = -1;
		final int[][] ids = {BANK_NPC_IDS, BANK_BOOTH_IDS, BANK_CHEST_IDS, BANK_COUNTER_IDS};
		for (int i = 0; i < ids.length; i++) {
			Arrays.sort(ids[i]);
			if (Arrays.binarySearch(ids[i], id) >= 0) {
				index = i;
				break;
			}
		}
		if (index == -1) {
			return false;
		}
		final Filter<Menu.Entry> f = new Filter<Menu.Entry>() {
			@Override
			public boolean accept(final Menu.Entry entry) {
				final String s = entry.action;
				return s.equalsIgnoreCase("Use") || s.equalsIgnoreCase("Open") || s.equalsIgnoreCase("Bank");
			}
		};
		final String[] actions = {"Bank", "Bank", null, "Bank"};
		final String[] options = {null, "Bank booth", null, "Counter"};
		if (actions[index] == null) {
			if (interactive.hover()) {
				Condition.wait(new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						return ctx.menu.indexOf(f) != -1;
					}
				}, 100, 3);
			}
		}
		final String action = actions[index];
		if (action != null ? interactive.interact(actions[index], options[index]) :
				interactive.interact(f)) {
			do {
				Condition.wait(new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						return ctx.widgets.widget(13).valid() || opened();
					}
				}, 150, 15);
			} while (ctx.players.local().inMotion());

			Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return ctx.widgets.widget(13).valid() || opened();
				}
			}, 100, 15);
		}
		return opened();
	}

	/**
	 * Closes the bank by the 'X'.
	 *
	 * @return <tt>true</tt> if the bank was closed; otherwise <tt>false</tt>
	 */
	public boolean close() {
		return !opened() || ctx.widgets.component(WIDGET, COMPONENT_BUTTON_CLOSE).interact("Close") && Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return !opened();
			}
		}, 150);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Item> get() {
		final Component c = ctx.widgets.component(WIDGET, COMPONENT_CONTAINER_ITEMS);
		if (c == null || !c.valid()) {
			return new ArrayList<Item>();
		}
		final Component[] components = c.components();
		final List<Item> items = new ArrayList<Item>(components.length);
		for (final Component i : components) {
			if (i.itemId() != -1) {
				items.add(new Item(ctx, i));
			}
		}
		return items;
	}

	/**
	 * Grabs the {@link Item} at the provided index.
	 *
	 * @param index the index of the item to grab
	 * @return the {@link Item} at the specified index; or {@link org.powerbot.script.rt6.Bank#nil()}
	 */
	public Item itemAt(final int index) {
		final Component i = ctx.widgets.component(WIDGET, COMPONENT_CONTAINER_ITEMS).component(index);
		if (i.itemId() != -1) {
			return new Item(ctx, i);
		}
		return nil();
	}

	/**
	 * Returns the first index of the provided item id.
	 *
	 * @param id the id of the item
	 * @return the index of the item; otherwise {@code -1}
	 */
	public int indexOf(final int id) {
		final Component items = ctx.widgets.component(WIDGET, COMPONENT_CONTAINER_ITEMS);
		if (items == null || !items.valid()) {
			return -1;
		}
		final Component[] comps = items.components();
		for (int i = 0; i < comps.length; i++) {
			if (comps[i].itemId() == id) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * @return the index of the current bank tab
	 */
	public int currentTab() {
		return ((ctx.varpbits.varpbit(SETTING_BANK_STATE) >>> 24) - 136) / 8;
	}

	/**
	 * Changes the current tab to the provided index.
	 *
	 * @param index the index desired
	 * @return <tt>true</tt> if the tab was successfully changed; otherwise <tt>false</tt>
	 */
	public boolean currentTab(final int index) {
		final Component c = ctx.widgets.component(WIDGET, 37 - (index * 2));
		return c.click() && Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return currentTab() == index;
			}
		}, 100, 8);
	}

	/**
	 * Returns the item in the specified tab if it exists.
	 *
	 * @param index the tab index
	 * @return the {@link Item} displayed in the tab; otherwise {@link org.powerbot.script.rt6.Bank#nil()}
	 */
	public Item tabItem(final int index) {
		final Component c = ctx.widgets.component(WIDGET, 37 - (index * 2));
		if (c != null && c.valid()) {
			return new Item(ctx, c);
		}
		return nil();
	}

	/**
	 * Withdraws an item with the provided id and amount.
	 *
	 * @param id     the id of the item
	 * @param amount the amount to withdraw
	 * @return <tt>true</tt> if the item was withdrew, does not determine if amount was matched; otherwise <tt>false</tt>
	 */
	public boolean withdraw(final int id, final Amount amount) {
		return withdraw(id, amount.getValue());
	}

	/**
	 * Withdraws an item with the provided id and amount.
	 *
	 * @param id     the id of the item
	 * @param amount the amount to withdraw
	 * @return <tt>true</tt> if the item was withdrew, does not determine if amount was matched; otherwise <tt>false</tt>
	 */
	public boolean withdraw(final int id, final int amount) {//TODO: anti pattern
		final Component component = ctx.widgets.component(WIDGET, COMPONENT_CONTAINER_ITEMS);
		final Item item = select().id(id).poll();
		if (!component.valid() || !item.valid()) {
			return false;
		}
		final Component c = item.component();
		if (c.relativePoint().y == 0) {
			if (!currentTab(0) && Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return c.relativePoint().y != 0;
				}
			}, 100, 10)) {
				return false;
			}
		}
		final Rectangle vr = component.viewportRect();
		if (!vr.contains(c.viewportRect()) && !ctx.widgets.scroll(c, ctx.widgets.component(WIDGET, COMPONENT_SCROLL_BAR),
				vr.contains(ctx.mouse.getLocation()))) {
			return false;
		}

		String action = "Withdraw-" + amount;
		if (amount == 0 ||
				(item.stackSize() <= amount && amount != 1 && amount != 5 && amount != 10)) {
			action = "Withdraw-All";
		} else if (amount == -1 || amount == (item.stackSize() - 1)) {
			action = "Withdraw-All but one";
		}
		final int inv = ctx.backpack.moneyPouchCount() + ctx.backpack.select().count(true);
		if (!containsAction(c, action)) {
			if (c.interact("Withdraw-X") && Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return isInputWidgetOpen();
				}
			})) {
				Random.sleep();
				ctx.keyboard.sendln(amount + "");
			}
		} else {
			if (!c.interact(action)) {
				return false;
			}
		}
		return Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return ctx.backpack.moneyPouchCount() + ctx.backpack.select().count(true) != inv;
			}
		});
	}

	/**
	 * Deposits an item with the provided id and amount.
	 *
	 * @param id     the id of the item
	 * @param amount the amount to deposit
	 * @return <tt>true</tt> if the item was deposited, does not determine if amount was matched; otherwise <tt>false</tt>
	 */
	public boolean deposit(final int id, final Amount amount) {
		return deposit(id, amount.getValue());
	}

	/**
	 * Deposits an item with the provided id and amount.
	 *
	 * @param id     the id of the item
	 * @param amount the amount to deposit
	 * @return <tt>true</tt> if the item was deposited, does not determine if amount was matched; otherwise <tt>false</tt>
	 */
	public boolean deposit(final int id, final int amount) {
		if (!opened() || amount < 0) {
			return false;
		}
		final Item item = ctx.backpack.select().id(id).shuffle().poll();
		if (!item.valid()) {
			return false;
		}
		String action = "Deposit-" + amount;
		final int count = ctx.backpack.select().id(id).count(true);
		if (count == 1) {
			action = "Deposit";
		} else if (amount == 0 || count <= amount) {
			action = "Deposit-All";
		}
		final int cache = ctx.backpack.select().count(true);
		final Component component = item.component();
		if (!containsAction(component, action)) {
			if (component.interact("Deposit-X") && Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return isInputWidgetOpen();
				}
			})) {
				Random.sleep();
				ctx.keyboard.sendln(amount + "");
			} else {
				return false;
			}
		} else {
			if (!component.interact(action)) {
				return false;
			}
		}
		return Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return cache != ctx.backpack.select().count(true);
			}
		});
	}

	/**
	 * Deposits the inventory via the button.
	 *
	 * @return <tt>true</tt> if the button was clicked, not if the inventory is empty; otherwise <tt>false</tt>
	 */
	public boolean depositInventory() {
		return ctx.backpack.select().isEmpty() || ctx.widgets.component(WIDGET, COMPONENT_BUTTON_DEPOSIT_INVENTORY).click();
	}

	/**
	 * Deposits equipment via the button.
	 *
	 * @return <tt>true</tt> if the button was clicked; otherwise <tt>false</tt>
	 */
	public boolean depositEquipment() {
		return ctx.widgets.component(WIDGET, COMPONENT_BUTTON_DEPOSIT_EQUIPMENT).click();
	}

	/**
	 * Deposits familiar inventory via the button.
	 *
	 * @return <tt>true</tt> if the button was clicked; otherwise <tt>false</tt>
	 */
	public boolean depositFamiliar() {
		return ctx.widgets.component(WIDGET, COMPONENT_BUTTON_DEPOSIT_FAMILIAR).click();
	}

	/**
	 * Deposits the money pouch via the button.
	 *
	 * @return <tt>true</tt> if the button was clicked; otherwise <tt>false</tt>
	 */
	public boolean depositMoneyPouch() {
		return ctx.backpack.moneyPouchCount() == 0 || ctx.widgets.component(WIDGET, COMPONENT_BUTTON_DEPOSIT_MONEY).click();
	}

	/**
	 * Changes the withdraw mode.
	 *
	 * @param noted <tt>true</tt> for noted items; otherwise <tt>false</tt>
	 * @return <tt>true</tt> if the withdraw mode was successfully changed; otherwise <tt>false</tt>
	 */
	public boolean withdrawMode(final boolean noted) {
		return withdrawMode() == noted || ctx.widgets.component(WIDGET, COMPONENT_BUTTON_WITHDRAW_MODE).click() && Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return withdrawMode() == noted;
			}
		});
	}

	/**
	 * Determines if the withdraw mode is noted mode.
	 *
	 * @return <tt>true</tt> if withdrawing as notes; otherwise <tt>false</tt>
	 */
	public boolean withdrawMode() {
		return ctx.varpbits.varpbit(SETTING_WITHDRAW_MODE) == 0x1;
	}

	private boolean containsAction(final Component c, final String action) {
		final String[] actions = c.actions();
		for (final String a : actions) {
			if (a != null && StringUtils.stripHtml(a).trim().equalsIgnoreCase(action)) {
				return true;
			}
		}
		return false;
	}

	private boolean isInputWidgetOpen() {
		return ctx.widgets.component(1469, 2).visible();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Item nil() {
		return new Item(ctx, -1, -1, null);
	}

	/**
	 * An enumeration providing standard bank amount options.
	 */
	public static enum Amount {
		ONE(1), FIVE(5), TEN(10), ALL_BUT_ONE(-1), ALL(0);

		private final int value;

		private Amount(final int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}
}
