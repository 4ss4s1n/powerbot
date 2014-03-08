package org.powerbot.script.os;

import java.awt.Point;

import org.powerbot.bot.os.client.Client;

public abstract class Actor extends Interactive implements Locatable, Nameable, Validatable {
	Actor(final ClientContext ctx) {
		super(ctx);
	}

	protected abstract org.powerbot.bot.os.client.Actor getActor();

	public abstract String name();

	public abstract int combatLevel();

	public int animation() {
		final org.powerbot.bot.os.client.Actor actor = getActor();
		return actor != null ? actor.getAnimation() : -1;
	}

	public int speed() {
		final org.powerbot.bot.os.client.Actor actor = getActor();
		return actor != null ? actor.getSpeed() : -1;
	}

	public String overheadMessage() {
		final org.powerbot.bot.os.client.Actor actor = getActor();
		final String str = actor != null ? actor.getOverheadMessage() : "";
		return str != null ? str : "";
	}

	public boolean inMotion() {
		return speed() > 0;
	}

	public int relativePosition() {
		final org.powerbot.bot.os.client.Actor actor = getActor();
		final int x, z;
		if (actor != null) {
			x = actor.getX();
			z = actor.getZ();
		} else {
			x = z = 0;
		}
		return (x << 16) | z;
	}

	@Override
	public Tile tile() {
		final Client client = ctx.client();
		final org.powerbot.bot.os.client.Actor actor = getActor();
		if (client != null && actor != null) {
			return new Tile(client.getOffsetX() + (actor.getX() >> 7), client.getOffsetY() + (actor.getZ() >> 7), client.getFloor());
		}
		return new Tile(-1, -1, -1);
	}

	@Override
	public Point nextPoint() {
		final org.powerbot.bot.os.client.Actor actor = getActor();
		if (actor == null) {
			return new Point(-1, -1);
		}
		final ActorCuboid cuboid = new ActorCuboid(ctx, actor);
		return cuboid.nextPoint();
	}

	@Override
	public Point centerPoint() {
		final org.powerbot.bot.os.client.Actor actor = getActor();
		if (actor == null) {
			return new Point(-1, -1);
		}
		final ActorCuboid cuboid = new ActorCuboid(ctx, actor);
		return cuboid.centerPoint();
	}

	@Override
	public boolean contains(final Point point) {
		final org.powerbot.bot.os.client.Actor actor = getActor();
		if (actor == null) {
			return false;
		}
		final ActorCuboid cuboid = new ActorCuboid(ctx, actor);
		return cuboid.contains(point);
	}

	@Override
	public int hashCode() {
		final org.powerbot.bot.os.client.Actor actor = getActor();
		return actor != null ? System.identityHashCode(actor) : super.hashCode();
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || !Actor.class.isAssignableFrom(o.getClass())) {
			return false;
		}
		final Actor actor = Actor.class.cast(o);
		return actor.getActor() == getActor();
	}
}
