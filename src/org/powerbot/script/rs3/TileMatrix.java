package org.powerbot.script.rs3;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;

import org.powerbot.script.Locatable;
import org.powerbot.script.Random;
import org.powerbot.script.Tile;

/**
 * An interactive tile matrix.
 */
public final class TileMatrix extends Interactive implements Locatable, Drawable {
	private final Tile tile;

	public TileMatrix(final ClientContext ctx, final Tile tile) {
		super(ctx);
		this.tile = tile;
	}

	@Override
	public void bounds(final int x1, final int x2, final int y1, final int y2, final int z1, final int z2) {
		boundingModel.set(new BoundingModel(ctx, x1, x2, y1, y2, z1, z2) {
			@Override
			public int x() {
				final Tile base = ctx.game.mapOffset();
				return ((tile.x() - base.x()) * 512) + 256;
			}

			@Override
			public int z() {
				final Tile base = ctx.game.mapOffset();
				return ((tile.y() - base.y()) * 512) + 256;
			}
		});
	}

	public Point point(final int height) {
		return point(0.5d, 0.5d, height);
	}

	public Point point(final double modX, final double modY, final int height) {
		final Tile base = ctx.game.mapOffset();
		return base != null ? ctx.game.groundToScreen((int) ((tile.x() - base.x() + modX) * 512d), (int) ((tile.y() - base.y() + modY) * 512d), tile.z(), height) : new Point(-1, -1);
	}

	public Polygon bounds() {
		final Point tl = point(0.0D, 0.0D, 0);
		final Point tr = point(1.0D, 0.0D, 0);
		final Point br = point(1.0D, 1.0D, 0);
		final Point bl = point(0.0D, 1.0D, 0);
		return new Polygon(
				new int[]{tl.x, tr.x, br.x, bl.x},
				new int[]{tl.y, tr.y, br.y, bl.y},
				4
		);
	}

	public Point mapPoint() {
		return ctx.game.tileToMap(tile);
	}

	public boolean onMap() {
		final Point p = mapPoint();
		return p.x != -1 && p.y != -1;
	}

	public boolean reachable() {
		return ctx.movement.reachable(ctx.players.local().tile(), tile);
	}

	@Override
	public Tile tile() {
		return tile;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean inViewport() {
		final BoundingModel model2 = boundingModel.get();
		if (model2 != null) {
			return ctx.game.inViewport(nextPoint());
		}
		return isPolygonInViewport(bounds());
	}

	private boolean isPolygonInViewport(final Polygon p) {
		for (int i = 0; i < p.npoints; i++) {
			if (!ctx.game.inViewport(p.xpoints[i], p.ypoints[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Point nextPoint() {
		final BoundingModel model2 = boundingModel.get();
		if (model2 != null) {
			return model2.nextPoint();
		}
		final int x = Random.nextGaussian(0, 100, 5);
		final int y = Random.nextGaussian(0, 100, 5);
		return point(x / 100.0D, y / 100.0D, 0);
	}

	public Point centerPoint() {
		final BoundingModel model2 = boundingModel.get();
		if (model2 != null) {
			return model2.centerPoint();
		}
		return point(0);
	}

	@Override
	public boolean contains(final Point point) {
		final BoundingModel model2 = boundingModel.get();
		if (model2 != null) {
			return model2.contains(point);
		}
		final Polygon p = bounds();
		return isPolygonInViewport(p) && p.contains(point);
	}

	@Override
	public boolean valid() {
		final Tile t = ctx.game.mapOffset();
		if (t == null || tile == Tile.NIL) {
			return false;
		}
		final int x = tile.x() - t.x(), y = tile.y() - t.y();
		return x >= 0 && y >= 0 && x < 104 && y < 104;
	}

	@Override
	public void draw(final Graphics render) {
		draw(render, 75);
	}

	@Override
	public void draw(final Graphics render, final int alpha) {
		final Polygon p = bounds();
		if (!isPolygonInViewport(p)) {
			return;
		}

		Color c = Tile.TARGET_COLOR;
		final int rgb = c.getRGB();
		if (((rgb >> 24) & 0xff) != alpha) {
			c = new Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, alpha);
		}
		render.setColor(c);
		final BoundingModel m2 = boundingModel.get();
		if (m2 != null) {
			m2.drawWireFrame(render);
			return;
		}
		render.drawPolygon(p);
		render.setColor(new Color(0, 0, 0, 20));
		render.fillPolygon(p);
	}

	@Override
	public String toString() {
		return tile.toString();
	}
}
