package org.powerbot.script.rt6;

import java.awt.Color;
import java.awt.Graphics;
import java.lang.ref.WeakReference;

import org.powerbot.bot.rt6.client.Client;
import org.powerbot.bot.rt6.client.OverheadSprites;
import org.powerbot.bot.rt6.client.RSNPC;
import org.powerbot.bot.rt6.client.RSNPCDef;
import org.powerbot.bot.rt6.client.RSNPCNode;
import org.powerbot.script.Identifiable;

public class Npc extends Actor implements Identifiable {
	public static final Color TARGET_COLOR = new Color(255, 0, 255, 15);
	private final WeakReference<RSNPC> npc;

	public Npc(final ClientContext ctx, final RSNPC npc) {
		super(ctx);
		this.npc = new WeakReference<RSNPC>(npc);
	}

	@Override
	protected RSNPC getAccessor() {
		return npc.get();
	}

	@Override
	public String name() {
		final RSNPC npc = getAccessor();
		final RSNPCDef def;
		return npc != null && (def = npc.getRSNPCDef()) != null ? def.getName() : "";
	}

	@Override
	public int combatLevel() {
		final RSNPC npc = getAccessor();
		final RSNPCDef def;
		return npc != null && (def = npc.getRSNPCDef()) != null ? def.getLevel() : -1;
	}

	@Override
	public int id() {
		final RSNPC npc = getAccessor();
		final RSNPCDef def;
		return npc != null && (def = npc.getRSNPCDef()) != null ? def.getID() : -1;
	}

	public String[] actions() {
		final RSNPC npc = getAccessor();
		final RSNPCDef def;
		return npc != null && (def = npc.getRSNPCDef()) != null ? def.getActions() : new String[0];
	}

	public int prayerIcon() {
		final int[] a1 = getOverheadArray1();
		final short[] a2 = getOverheadArray2();
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

	private int[] getOverheadArray1() {
		final RSNPC npc = getAccessor();
		if (npc != null) {
			final OverheadSprites sprites = npc.getOverhead();
			int[] arr;
			if (sprites != null && (arr = sprites.getArray1()) != null) {
				return arr;
			}
			final RSNPCDef def = npc.getRSNPCDef();
			if (def != null && (arr = def.getOverheadArray1()) != null) {
				return arr;
			}
		}
		return new int[0];
	}


	private short[] getOverheadArray2() {
		final RSNPC npc = getAccessor();
		if (npc != null) {
			final OverheadSprites sprites = npc.getOverhead();
			short[] arr;
			if (sprites != null && (arr = sprites.getArray2()) != null) {
				return arr;
			}
			final RSNPCDef def = npc.getRSNPCDef();
			if (def != null && (arr = def.getOverheadArray2()) != null) {
				return arr;
			}
		}
		return new short[0];
	}

	@Override
	public boolean valid() {
		final Client client = ctx.client();
		if (client == null) {
			return false;
		}
		final RSNPC npc = getAccessor();
		if (npc != null) {
			final int[] indices = client.getRSNPCIndexArray();
			final org.powerbot.bot.rt6.client.HashTable npcTable = client.getRSNPCNC();
			for (final int index : indices) {
				Object node = ctx.game.lookup(npcTable, index);
				if (node == null) {
					continue;
				}
				if (node instanceof RSNPCNode) {
					node = ((RSNPCNode) node).getRSNPC();
				}
				if (node instanceof RSNPC) {
					if (node.equals(npc)) {
						return true;
					}
				}
			}
		}
		return false;
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
			final Model m = model();
			if (m != null) {
				m.drawWireFrame(render);
			}
		}
	}

	@Override
	public String toString() {
		return Npc.class.getSimpleName() + "[id=" + id() + ",name=" + name() + ",level=" + combatLevel() + "]";
	}
}
