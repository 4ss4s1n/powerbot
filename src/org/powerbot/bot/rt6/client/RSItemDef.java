package org.powerbot.bot.rt6.client;

public interface RSItemDef {
	public String[] getActions();

	public RSItemDefLoader getLoader();

	public int getID();

	public String getName();

	public boolean isMembersObject();

	public String[] getGroundActions();
}
