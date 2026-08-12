/*
 * This file is part of the SaveAFK project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Sakura-Ryoko and contributors
 *
 * SaveAFK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SaveAFK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SaveAFK.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.qxl.safk.impl.events;

import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameType;

import io.qxl.safk.impl.SaveAfk;
import io.qxl.safk.impl.player.PlayerManager;
import io.qxl.safk.impl.player.safk.SafkPendingSpawns;
import com.sakuraryoko.corelib.api.events.IServerEventsDispatch;
import io.qxl.safk.impl.player.safk.SafkPlayerUtils;
import io.qxl.safk.impl.player.safk.SafkServerPlayer;

@ApiStatus.Internal
public class ServerEventsHandler implements IServerEventsDispatch
{
	private static final ServerEventsHandler INSTANCE = new ServerEventsHandler();
	public static ServerEventsHandler getInstance() { return INSTANCE; }
	private static final float TICK_RATE = 30.0f;
	private boolean tickingLock;
	private boolean spawnSafe;
	private boolean hideAllPlayers;
	private boolean unhideAllPlayers;
	private boolean serverStopping;
	private final long startupTime;
	private long lastTick;

	private ServerEventsHandler()
	{
		this.init();
		this.startupTime = System.currentTimeMillis();
		this.lastTick = this.startupTime;
	}

	private void init()
	{
		this.tickingLock = true;
		this.spawnSafe = false;
		this.unhideAllPlayers = false;
		this.hideAllPlayers = false;
		this.serverStopping = false;
	}

	@Override
	public void onStarting(MinecraftServer server)
	{
		this.init();
	}

	@Override
	public void onStarted(MinecraftServer server)
	{
		this.tickingLock = true;
		PlayerManager.getInstance().onServerStarted(server);
	}

	@Override
	public void onReloadComplete(MinecraftServer server, Collection<String> resources)
	{
		// TODO
	}

	@Override
	public void onDedicatedStarted(DedicatedServer server)
	{
		// TODO
	}

	@Override
	public void onIntegratedStarted(IntegratedServer server)
	{
		// TODO
	}

	@Override
	public void onOpenToLan(IntegratedServer server, GameType mode)
	{
		// TODO
	}

	private long tickRate()
	{
		return (long) (TICK_RATE * 1000L);
	}

	public void onTick(MinecraftServer server)
	{
		// Every Tick -->
		SafkPendingSpawns.INSTANCE.tick(server);
		final long now = System.currentTimeMillis();

		// Hold additional tick tasks until server has been running for at least 1 tick cycle.
		if (this.tickingLock)
		{
			if ((now - this.startupTime) > this.tickRate())
			{
				this.tickingLock = false;
			}

			PlayerManager.getInstance().onTick(server, false);
			return;
		}

		PlayerManager.getInstance().onTick(server, this.isSpawnSafe());

		if ((now - this.lastTick) > this.tickRate())
		{
			if (this.hideAllPlayers || this.unhideAllPlayers)
			{
				this.processAllHideOrUnhide(server);
			}

			this.lastTick = now;
		}
	}

	@ApiStatus.Internal
	private void processAllHideOrUnhide(MinecraftServer server)
	{
		PlayerList pl = server.getPlayerList();
		List<ServerPlayer> players = pl.getPlayers();

		SaveAfk.debugLog("processAllHideOrUnhide()");

		// Fom changing the config options
		for (ServerPlayer player : players)
		{
			if (this.hideAllPlayers && !(player instanceof SafkServerPlayer))
			{
				SafkPlayerUtils.hideAllSafkFromPlayer(server, player);
			}
			else if (this.unhideAllPlayers && !(player instanceof SafkServerPlayer))
			{
				SafkPlayerUtils.unhideAllSafkFromPlayer(server, player);
			}
		}

		this.hideAllPlayers = false;
		this.unhideAllPlayers = false;
	}

	@Override
	public void onDedicatedStopping(DedicatedServer server)
	{
		// TODO
	}

	@Override
	public void onStopping(MinecraftServer server)
	{
		this.tickingLock = true;
		this.serverStopping = true;
		this.toggleSpawnSafe(false);
		this.toggleHideAllPlayers(false);
		this.toggleUnhideAllPlayers(false);

		PlayerManager.getInstance().onServerStop(server);
	}

	@Override
	public void onStopped(MinecraftServer server)
	{
		// TODO
	}

	@ApiStatus.Internal
	public boolean isSpawnSafe()
	{
		return this.spawnSafe;
	}

	@ApiStatus.Internal
	public void toggleSpawnSafe(boolean toggle)
	{
		this.spawnSafe = toggle;
	}

	@ApiStatus.Internal
	public void toggleHideAllPlayers(boolean toggle)
	{
		this.hideAllPlayers = toggle;
	}

	@ApiStatus.Internal
	public void toggleUnhideAllPlayers(boolean toggle)
	{
		this.unhideAllPlayers = toggle;
	}

	@ApiStatus.Internal
	public boolean isServerStopping()
	{
		return this.serverStopping;
	}
}
