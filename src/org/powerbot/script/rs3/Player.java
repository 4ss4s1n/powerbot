package org.powerbot.script.rs3;

import java.awt.Color;
import java.awt.Graphics;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import org.powerbot.bot.rs3.client.Client;
import org.powerbot.bot.rs3.client.RSPlayer;
import org.powerbot.bot.rs3.client.RSPlayerComposite;

public class Player extends Actor {
	public static final Color TARGET_COLOR = new Color(255, 0, 0, 15);
	private final WeakReference<RSPlayer> player;

	public Player(final ClientContext ctx, final RSPlayer player) {
		super(ctx);
		this.player = new WeakReference<RSPlayer>(player);
	}

	@Override
	protected RSPlayer getAccessor() {
		return player.get();
	}

	@Override
	public String getName() {
		final RSPlayer player = getAccessor();
		return player != null ? player.getName() : "";
	}

	@Override
	public int getLevel() {
		final RSPlayer player = getAccessor();
		return player != null ? player.getLevel() : -1;
	}

	public int getTeam() {
		final RSPlayer player = getAccessor();
		return player != null ? player.getTeam() : -1;
	}

	public int getPrayerIcon() {
		final int[] a1 = getOverheadArray1(), a2 = getOverheadArray2();
		final int len = a1.length;
		if (len != a2.length) {
			return -1;
		}

		for (int i = 0; i < len; i++) {
			if (a1[i] == 440) {
				return a2[i];
			}
		}
		return -1;
	}

	public int getSkullIcon() {
		return -1;
	}

	private int[] getOverheadArray1() {
		final RSPlayer player = getAccessor();
		if (player != null) {
			final int[] arr = player.getOverheadArray1();
			if (arr != null) {
				return arr;
			}
		}
		return new int[0];
	}

	private int[] getOverheadArray2() {
		final RSPlayer player = getAccessor();
		if (player != null) {
			final int[] arr = player.getOverheadArray2();
			if (arr != null) {
				return arr;
			}
		}
		return new int[0];
	}

	public int getNpcId() {
		final RSPlayer player = getAccessor();
		final RSPlayerComposite composite;
		return player != null && (composite = player.getComposite()) != null ? composite.getNPCID() : -1;
	}

	public int[] getAppearance() {
		final RSPlayer player = getAccessor();
		final RSPlayerComposite composite = player != null ? player.getComposite() : null;
		if (composite != null) {
			final int[] appearance = composite.getEquipment().clone();
			for (int i = 0; i < appearance.length; i++) {
				if ((appearance[i] & 0x40000000) > 0) {
					appearance[i] &= 0x3fffffff;
				} else {
					appearance[i] = -1;
				}
			}
			return appearance;
		}
		return new int[0];
	}

	@Override
	public boolean isValid() {
		final Client client = ctx.client();
		if (client == null) {
			return false;
		}
		final RSPlayer character = getAccessor();
		final RSPlayer[] players = client.getRSPlayerArray();
		return character != null && players != null && Arrays.asList(players).contains(character);
	}

	@Override
	public void draw(final Graphics render) {
		draw(render, 15);
	}

	@Override
	public void draw(final Graphics render, final int alpha) {
		Color c = TARGET_COLOR;
		final int rgb = c.getRGB();
		if (((rgb >> 24) & 0xff) != alpha) {
			c = new Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, alpha);
		}
		render.setColor(c);
		final BoundingModel m2 = boundingModel.get();
		if (m2 != null) {
			m2.drawWireFrame(render);
		} else {
			final Model m = getModel();
			if (m != null) {
				m.drawWireFrame(render);
			}
		}
	}

	@Override
	public String toString() {
		return Player.class.getSimpleName() + "[name=" + getName() + ",level=" + getLevel() + "]";
	}
}
