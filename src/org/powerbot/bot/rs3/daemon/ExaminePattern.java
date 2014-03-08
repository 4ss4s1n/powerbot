package org.powerbot.bot.rs3.daemon;

import org.powerbot.script.Filter;
import org.powerbot.script.rs3.tools.ClientContext;
import org.powerbot.script.rs3.tools.Menu;
import org.powerbot.script.Random;
import org.powerbot.script.rs3.tools.GameObject;
import org.powerbot.script.rs3.tools.Interactive;
import org.powerbot.script.rs3.tools.Npc;

public class ExaminePattern extends Antipattern.Module {

	public ExaminePattern(final ClientContext ctx) {
		super(ctx);
		freq.set(1);
	}

	@Override
	public void run() {
		if (isAggressive()) {
			for (final Npc n : ctx.npcs.select().select(new Filter<Npc>() {
				@Override
				public boolean accept(final Npc npc) {
					return npc.isInViewport();
				}
			}).shuffle().limit(isAggressive() ? 1 : Random.nextInt(1, 3))) {
				hover(n);
			}

			return;
		}

		for (final GameObject o : ctx.objects.select().select(new Filter<GameObject>() {
			@Override
			public boolean accept(final GameObject o) {
				return o.getType() == GameObject.Type.INTERACTIVE && o.isInViewport();
			}
		}).shuffle().limit(isAggressive() ? 1 : Random.nextInt(1, 3))) {
			hover(o);
		}
	}

	private void hover(final Interactive o) {
		final boolean a = isAggressive();
		for (int i = a ? 0 : 1; i < 2 && o.hover(); i++) {
			if (ctx.menu.click(Menu.filter("Examine")) && a) {
				try {
					Thread.sleep(300, 2000);
				} catch (final InterruptedException ignored) {
				}
			}
		}
	}
}
