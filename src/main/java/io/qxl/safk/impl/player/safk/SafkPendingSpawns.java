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

package io.qxl.safk.impl.player.safk;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.MinecraftServer;

import io.qxl.safk.impl.config.data.options.PlayerOptions;
import io.qxl.safk.impl.events.ServerEventsHandler;

@ApiStatus.Internal
public class SafkPendingSpawns
{
	public static final SafkPendingSpawns INSTANCE = new SafkPendingSpawns();
	private static final float TICK_RATE = 3.0f;
	private final List<PlayerOptions> pendingSpawnsList;
	private long lastTick;

	private SafkPendingSpawns()
	{
		this.pendingSpawnsList = new ArrayList<>();
		this.lastTick = System.currentTimeMillis();
	}

	public void scheduleSpawn(PlayerOptions opts)
	{
		UUID uuid = opts.uuid;

		// Deduplicate
		for (PlayerOptions spawn : this.pendingSpawnsList)
		{
			if (uuid.equals(spawn.uuid))
			{
				return;
			}
		}

		this.pendingSpawnsList.add(opts);
	}

	private boolean shouldTick()
	{
		return !this.pendingSpawnsList.isEmpty();
	}

	private void executeOneSpawn(@Nonnull MinecraftServer server)
	{
		if (!this.pendingSpawnsList.isEmpty())
		{
			PlayerOptions opts = this.pendingSpawnsList.removeFirst();
			opts.state = opts.state.ensureValid();

			SafkServerPlayer.createFromConfig(server, opts);

			if (this.pendingSpawnsList.isEmpty())
			{
				if (!ServerEventsHandler.getInstance().isSpawnSafe())
				{
					ServerEventsHandler.getInstance().toggleSpawnSafe(true);
				}
			}
		}
	}

	private long tickRate()
	{
		return (long) (TICK_RATE * 1000L);
	}

	public void tick(@Nonnull MinecraftServer server)
	{
		if (this.shouldTick())
		{
			final long now = System.currentTimeMillis();

			if ((now - this.lastTick) > this.tickRate())
			{
				this.executeOneSpawn(server);
				this.lastTick = now;
			}
		}
	}
}
