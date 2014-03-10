package org.powerbot.bot.rs3;

import org.powerbot.bot.rs3.client.Callback;
import org.powerbot.bot.rs3.client.RSInteractableLocation;
import org.powerbot.bot.rs3.client.RSObjectDef;
import org.powerbot.bot.rs3.client.Render;
import org.powerbot.bot.EventDispatcher;
import org.powerbot.script.MessageEvent;
import org.powerbot.script.rs3.Camera;
import org.powerbot.script.rs3.ClientContext;

public class AbstractCallback implements Callback {
	private final ClientContext ctx;
	private final EventDispatcher dispatcher;

	public AbstractCallback(final Bot bot) {
		ctx = bot.ctx;
		dispatcher = bot.dispatcher;
	}

	@Override
	public void updateRenderInfo(final Render render) {
		ctx.game.updateToolkit(render);
		try {
			ctx.menu.cache();
		} catch (final Throwable ignored) {
		}
	}

	@Override
	public void notifyMessage(final int id, final String sender, final String message) {
		dispatcher.dispatch(new MessageEvent(id, sender, message));
	}

	@Override
	public void notifyObjectDefinitionLoad(final RSObjectDef def) {
		ctx.objects.setType(def.getID(), def.getClippingType());
	}

	@Override
	public void updateCamera(final RSInteractableLocation offset, final RSInteractableLocation center) {
		final Camera camera = ctx.camera;
		camera.offset = new float[]{offset.getX(), offset.getY(), offset.getZ()};
		camera.center = new float[]{center.getX(), center.getY(), center.getZ()};
	}
}
