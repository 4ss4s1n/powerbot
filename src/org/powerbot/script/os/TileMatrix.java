package org.powerbot.script.os;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;

import org.powerbot.script.Random;

/**
 * An interactive tile matrix.
 */
public final class TileMatrix extends Interactive implements Locatable {
	public static final Color TARGET_STROKE_COLOR = new Color(255, 0, 0, 75);
	private final Tile tile;

	TileMatrix(final ClientContext ctx, final Tile tile) {
		super(ctx);
		this.tile = tile;
	}

	public Point point(final int height) {
		return point(0.5d, 0.5d, height);
	}

	public Point point(final double modX, final double modY, final int height) {
		final Tile base = ctx.game.mapOffset();
		return base != null ? ctx.game.worldToScreen((int) ((tile.x() - base.x() + modX) * 128d), (int) ((tile.y() - base.y() + modY) * 128d), height) : new Point(-1, -1);
	}

	public Polygon getBounds() {
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

	@Override
	public Tile tile() {
		return tile;
	}

	@Override
	public boolean inViewport() {
		return isPolygonInViewport(getBounds());
	}

	private boolean isPolygonInViewport(final Polygon p) {
		for (int i = 0; i < p.npoints; i++) {
			if (!ctx.game.pointInViewport(p.xpoints[i], p.ypoints[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Point nextPoint() {
		final int x = Random.nextGaussian(0, 100, 5);
		final int y = Random.nextGaussian(0, 100, 5);
		return point(x / 100.0D, y / 100.0D, 0);
	}

	@Override
	public Point centerPoint() {
		return point(0);
	}

	@Override
	public boolean contains(final Point point) {
		final Polygon p = getBounds();
		return isPolygonInViewport(p) && p.contains(point);
	}

	@Override
	public boolean valid() {
		final Tile t = ctx.game.mapOffset();
		if (t == null) {
			return false;
		}
		final int x = tile.x() - t.x(), y = tile.y() - t.y();
		return x >= 0 && y >= 0 && x < 104 && y < 104;
	}
}
