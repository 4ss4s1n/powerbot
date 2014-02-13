package org.powerbot.os.api.wrappers;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.Callable;

import org.powerbot.os.api.ClientContext;
import org.powerbot.os.api.methods.Game;
import org.powerbot.os.api.util.Condition;
import org.powerbot.os.api.util.Random;

public class TilePath extends Path {
	protected Tile[] tiles;
	protected Tile[] orig;
	private boolean end;
	private Tile last;

	public TilePath(final ClientContext ctx, final Tile[] tiles) {
		super(ctx);
		orig = tiles;
		this.tiles = Arrays.copyOf(tiles, tiles.length);
	}

	@Override
	public boolean traverse(final EnumSet<TraversalOption> options) {
		final Player local = ctx.players.local();
		final Tile next = next();
		if (next == null || local == null) {
			return false;
		}
		final Tile dest = ctx.movement.getDestination();
		if (next.equals(end())) {
			if (next.distanceTo(ctx.players.local()) <= 2) {
				return false;
			}
			if (end && (local.isInMotion() || dest.equals(next))) {
				return false;
			}
			end = true;
		} else {
			end = false;
		}
		if (options != null) {
			if (options.contains(TraversalOption.HANDLE_RUN) && !ctx.movement.isRunning() && ctx.movement.getEnergyLevel() > Random.nextInt(45, 60)) {
				ctx.movement.setRunning(true);
			}
			if (options.contains(TraversalOption.SPACE_ACTIONS) && local.isInMotion() && dest.distanceTo(last) < 3d) {
				if (dest.distanceTo(ctx.players.local()) > (double) Random.nextInt(5, 12)) {//TODO: revise this distance to not be detectable!!!
					return true;
				}
			}
		}
		last = next;
		if (ctx.movement.stepTowards(next)) {
			if (local.isInMotion()) {
				return Condition.wait(new Callable<Boolean>() {
					@Override
					public Boolean call() {
						return ctx.movement.getDestination().distanceTo(next) < 3;
					}
				}, 60, 10);
			}
			return next.distanceTo(ctx.players.local()) < 5d || Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() {
					return ctx.players.local().isInMotion() && ctx.movement.getDestination().distanceTo(next) < 3;
				}
			}, 125, 10);
		}
		return false;
	}

	@Override
	public boolean isValid() {
		return tiles.length > 0 && next() != null && end().distanceTo(ctx.players.local()) > Math.sqrt(2);
	}

	@Override
	public Tile next() {
		/* Wait for map not to be loading */
		final int state = ctx.game.getClientState();
		if (state == Game.INDEX_MAP_LOADING) {
			Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return ctx.game.getClientState() != Game.INDEX_MAP_LOADING;
				}
			});
			return next();
		}
		if (state != Game.INDEX_MAP_LOADED) {
			return null;
		}
		/* Get current destination */
		final Tile dest = ctx.movement.getDestination();
		/* Label main loop for continuing purposes */
		out:
		/* Iterate over all tiles but the first tile (0) starting with the last (length - 1). */
		for (int i = tiles.length - 1; i > 0; --i) {
			/* The tiles not in view, go to the next. */
			if (!tiles[i].getMatrix(ctx).isOnMap()) {
				continue;
			}
			/* If our destination is NIL, assume mid path and continue there. */
			/* LARGELY SPACED PATH SUPPORT: If the current destination is the tile on the map, return that tile
			 * as the next one will be coming soon (we hope/assume this, as short spaced paths should never experience
			 * this condition as one will be on map before it reaches the current target). */
			if (dest == Tile.NIL || tiles[i].distanceTo(dest) < 3d) {
				return tiles[i];
			}
			/* Tile is on map and isn't currently "targeted" (dest), let's check it out.
			 * Iterate over all tiles succeeding it. */
			for (int a = i - 1; a >= 0; --a) {
				/* The tile before the tile on map isn't on map.  Break out to the next tile.
				 * Explanation: Path wraps around something and must be followed.
				 * We cannot suddenly click out of a "pathable" region (104x104).
				 * In these cases, we can assume a better tile will become available. */
				if (!tiles[a].getMatrix(ctx).isOnMap()) {
					continue out;
				}
				/* If a tile (successor) is currently targeted, return the tile that was the "best"
				 * on the map for getNext as we can safely assume we're following our path. */
				if (tiles[a].distanceTo(dest) < 3d) {
					return tiles[i];
				}
			}
		}
		/* Well, we've made it this far.  Return the first tile if nothing else is on our map.
		* CLICKING BACK AND FORTH PREVENTION: check for dest not to be null if we're just starting
		 * our path.  If our destination isn't null and we somehow got to our first tile then
		 * we can safely assume lag is being experienced and return null until next call of getNext.
		 * TELEPORTATION SUPPORT: If destination is set but but we're not moving, assume
		 * invalid destination tile from teleportation reset and return first tile. */
		final Player p = ctx.players.local();
		if (p != null && !p.isInMotion() && dest != Tile.NIL) {
			for (int i = tiles.length - 1; i >= 0; --i) {
				if (tiles[i].getMatrix(ctx).isOnMap()) {
					return tiles[i];
				}
			}
		}
		if (tiles.length == 0 || !tiles[0].getMatrix(ctx).isOnMap()) {
			return null;
		}
		return tiles[0];
	}

	@Override
	public Tile start() {
		return tiles[0];
	}

	@Override
	public Tile end() {
		return tiles[tiles.length - 1];
	}

	public TilePath randomize(final int maxX, final int maxY) {
		for (int i = 0; i < tiles.length; ++i) {
			tiles[i] = orig[i].derive(Random.nextInt(-maxX, maxX + 1), Random.nextInt(-maxY, maxY + 1));
		}
		return this;
	}

	public TilePath reverse() {
		Tile[] reversed = new Tile[tiles.length];
		for (int i = 0; i < orig.length; ++i) {
			reversed[i] = orig[tiles.length - 1 - i];
		}
		orig = reversed;
		reversed = new Tile[tiles.length];
		for (int i = 0; i < tiles.length; ++i) {
			reversed[i] = tiles[tiles.length - 1 - i];
		}
		tiles = reversed;
		return this;
	}

	public Tile[] toArray() {
		final Tile[] a = new Tile[tiles.length];
		System.arraycopy(tiles, 0, a, 0, tiles.length);
		return a;
	}
}