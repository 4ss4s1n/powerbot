package org.powerbot.event.debug;

import java.awt.Graphics;

import org.powerbot.event.TextPaintListener;
import org.powerbot.gui.BotChrome;
import org.powerbot.script.methods.MethodContext;
import org.powerbot.script.wrappers.Tile;

import static org.powerbot.event.debug.DebugHelper.drawLine;

public class TMapBase implements TextPaintListener {
	protected final MethodContext ctx;

	public TMapBase(final MethodContext ctx) {
		this.ctx = ctx;
	}

	public int draw(int idx, final Graphics render) {
		final Tile t = ctx.game.getMapBase();
		drawLine(render, idx++, "Map base: " + (t != null ? t.toString() : ""));
		return idx;
	}
}
