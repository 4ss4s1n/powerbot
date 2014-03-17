package org.powerbot.script.rt6;

import java.awt.Point;

import org.powerbot.script.Identifiable;
import org.powerbot.script.Nameable;
import org.powerbot.util.StringUtils;

public class Item extends Interactive implements Displayable, Identifiable, Nameable, Stackable {
	private final int id;
	private int stack;
	private final Component component;

	public Item(final ClientContext ctx, final Component component) {
		this(ctx, component.itemId(), component.itemStackSize(), component);
	}

	public Item(final ClientContext ctx, final int id, final int stack, final Component component) {
		super(ctx);
		this.id = id;
		this.stack = stack;
		this.component = component;
	}

	@Override
	public void bounds(final int x1, final int x2, final int y1, final int y2, final int z1, final int z2) {
	}

	@Override
	public int id() {
		return this.id;
	}

	@Override
	public int stackSize() {
		if (component == null) {
			return stack;
		}
		final int stack = component.itemStackSize();
		if (component.visible() && component.itemId() == this.id) {
			return this.stack = stack;
		}
		return this.stack;
	}

	@Override
	public String name() {
		final String name;
		if (component != null && component.itemId() == this.id) {
			name = component.itemName();
		} else {
			name = ItemDefinition.getDef(ctx, this.id).getName();
		}
		return StringUtils.stripHtml(name);
	}

	public boolean members() {
		return ItemDefinition.getDef(ctx, id()).isMembers();
	}

	public String[] actions() {
		return ItemDefinition.getDef(ctx, id()).getActions();
	}

	public String[] groundActions() {
		return ItemDefinition.getDef(ctx, id()).getGroundActions();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Component component() {
		return component;
	}

	@Override
	public Point nextPoint() {
		if (component == null) {
			return new Point(-1, -1);
		}
		return component.nextPoint();
	}

	public Point centerPoint() {
		if (component == null) {
			return new Point(-1, -1);
		}
		return component.centerPoint();
	}

	@Override
	public boolean contains(final Point point) {
		return component != null && component.contains(point);
	}

	@Override
	public boolean valid() {
		return this.id != -1 && this.component != null && this.component.valid() &&
				(!this.component.visible() || this.component.itemId() == this.id);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "/" + stack + "]@" + component;
	}

	@Override
	public int hashCode() {
		if (component == null) {
			return -1;
		}
		return this.id * 31 + this.component.getIndex();
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || !(o instanceof Item)) {
			return false;
		}
		final Item i = (Item) o;
		return this.id == i.id &&
				((this.component != null && this.component.equals(i.component))
						|| (this.component == null && i.component == null));
	}
}
