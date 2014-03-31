package org.powerbot.script.rt4;

import java.awt.Point;
import java.util.concurrent.Callable;

import org.powerbot.bot.rt4.client.Client;
import org.powerbot.script.Condition;
import org.powerbot.script.Tile;

public class Game extends ClientAccessor {
	public static final int INDEX_MAP_LOADED = 30;
	public static final int INDEX_MAP_LOADING = 25;
	private static final int[] ARRAY_SIN = new int[2048];
	private static final int[] ARRAY_COS = new int[2048];

	static {
		for (int i = 0; i < 2048; i++) {
			ARRAY_SIN[i] = (int) (65536d * Math.sin(i * 0.0030679615d));
			ARRAY_COS[i] = (int) (65536d * Math.cos(i * 0.0030679615d));
		}
	}

	public enum Tab {
		ATTACK("Combat Options", 168),
		STATS("Stats", 898),
		QUESTS("Quest List", 776),
		INVENTORY("Inventory", 884),
		EQUIPMENT("Worn Equipment", 901),
		PRAYER("Prayer", 902),
		MAGIC("Magic", 903),
		CLAN_CHAT("Clan Chat", 895),
		FRIENDS_LIST("Friends List", 904),
		IGNORED_LIST("Ignored List", 905),
		LOGOUT("Logout", 906),
		OPTIONS("Options", 907),
		EMOTES("Emotes", 908),
		MUSIC("Music Player", 909),
		NONE("", -1);
		public final String tip;
		public final int texture;

		Tab(final String tip, final int texture) {
			this.tip = tip;
			this.texture = texture;
		}
	}

	public boolean tab(final Tab tab) {
		final Component c = getByTexture(tab.texture);
		return c != null && c.click() && Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				return tab() == tab;
			}
		}, 50, 10);
	}

	public Tab tab() {
		final Component c = getByTexture(1026);
		if (c != null) {
			final int t = c.widget().component(c.index() + 7).textureId();
			for (final Tab tab : Tab.values()) {
				if (tab.texture == t) {
					return tab;
				}
			}
		}
		return Tab.NONE;
	}

	private Component getByTexture(final int texture) {
		final Widget w = ctx.widgets.get(548);
		for (final Component c : w.components()) {
			if (c.textureId() == texture) {
				return c;
			}
		}
		return null;
	}

	public Game(final ClientContext ctx) {
		super(ctx);
	}

	public boolean loggedIn() {
		final int c = clientState();
		return c == INDEX_MAP_LOADED || c == INDEX_MAP_LOADING;
	}

	public int clientState() {
		final Client client = ctx.client();
		return client != null ? client.getClientState() : -1;
	}

	public int floor() {
		final Client client = ctx.client();
		return client != null ? client.getFloor() : -1;
	}

	public int crosshairIndex() {
		final Client client = ctx.client();
		return client != null ? client.getCrosshairIndex() : -1;
	}

	public Tile mapOffset() {
		final Client client = ctx.client();
		if (client == null) {
			return Tile.NIL;
		}
		return new Tile(client.getOffsetX(), client.getOffsetY(), client.getFloor());
	}

	public boolean pointInViewport(final Point p) {
		return pointInViewport(p.x, p.y);
	}

	public boolean pointInViewport(final int x, final int y) {
		return x >= 4 && y >= 4 && x <= 515 && y <= 337;
	}

	public HintArrow hintArrow() {
		//TODO: hint arrow
		final HintArrow r = new HintArrow();
		final Client client = ctx.client();
		if (client == null) {
			return r;
		}
		return r;
	}

	public Point tileToMap(final Tile tile) {
		final Client client = ctx.client();
		if (client == null) {
			return new Point(-1, -1);
		}
		final int rel = ctx.players.local().relativePosition();
		final int angle = client.getMinimapScale() + client.getMinimapAngle() & 0x7ff;
		final int[] d = {tile.x(), tile.y(), ARRAY_SIN[angle], ARRAY_COS[angle], -1, -1};
		d[0] = (d[0] - client.getOffsetX()) * 4 + 2 - (rel >> 16) / 32;
		d[1] = (d[1] - client.getOffsetY()) * 4 + 2 - (rel & 0xffff) / 32;
		final int offset = client.getMinimapOffset();
		d[2] = d[2] << 8 / (offset + 256);
		d[3] = d[3] << 8 / (offset + 256);
		d[4] = d[1] * d[2] + d[3] * d[0] >> 16;
		d[5] = d[2] * d[0] - d[1] * d[3] >> 16;
		return new Point(643 + d[4], 83 + d[5]);
	}

	public int tileHeight(final int relativeX, final int relativeZ) {
		final Client client = ctx.client();
		if (client == null) {
			return 0;
		}
		int floor = client.getFloor();
		int x = relativeX >> 7;
		int y = relativeZ >> 7;
		if (x < 0 || y < 0 || x > 103 || y > 103 ||
				floor < 0 || floor > 3) {
			return 0;
		}
		final byte[][][] meta = client.getLandscapeMeta();
		final int[][][] heights = client.getTileHeights();
		if (meta == null) {
			return 0;
		}
		if (floor < 3 && (meta[1][x][y] & 0x2) == 2) {
			floor++;
		}

		x &= 0x7f;
		y &= 0x7f;
		final int heightStart = x * heights[floor][1 + x][y] + heights[floor][x][y] * (128 - x) >> 7;
		final int heightEnd = (128 - x) * heights[floor][x][1 + y] + x * heights[floor][1 + x][y + 1] >> 7;
		return y * heightEnd + heightStart * (128 - y) >> 7;
	}

	public Point worldToScreen(final int relativeX, final int relativeZ, final int h) {
		final Client client = ctx.client();
		if (client == null) {
			return new Point(-1, -1);
		}
		return worldToScreen(relativeX, tileHeight(relativeX, relativeZ), relativeZ, h);
	}

	public Point worldToScreen(final int relativeX, final int relativeY, final int relativeZ, final int h) {
		final Client client = ctx.client();
		final Point r = new Point(-1, -1);
		if (relativeX < 128 || relativeX > 13056 ||
				relativeZ < 128 || relativeZ > 13056) {
			return r;
		}
		final int floor = client.getFloor();
		if (floor < 0) {
			return r;
		}
		final int height = relativeY - h;
		final int projectedX = relativeX - client.getCameraX(), projectedZ = relativeZ - client.getCameraZ(),
				projectedY = height - client.getCameraY();
		final int pitch = client.getCameraPitch(), yaw = client.getCameraYaw();
		final int[] c = {ARRAY_SIN[yaw], ARRAY_COS[yaw], ARRAY_SIN[pitch], ARRAY_COS[pitch]};
		final int rotatedX = c[0] * projectedZ + c[1] * projectedX >> 16;
		final int rotatedZ = c[1] * projectedZ - c[0] * projectedX >> 16;
		final int rolledY = c[3] * projectedY - c[2] * rotatedZ >> 16;
		final int rolledZ = c[3] * rotatedZ + c[2] * projectedY >> 16;
		if (rolledZ >= 50) {
			return new Point(
					(rotatedX << 9) / rolledZ + 256,
					(rolledY << 9) / rolledZ + 167
			);
		}
		return r;
	}
}
