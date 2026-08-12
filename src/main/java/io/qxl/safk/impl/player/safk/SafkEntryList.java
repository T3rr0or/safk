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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import net.minecraft.network.chat.Component;

import io.qxl.safk.impl.SaveAfk;
import io.qxl.safk.api.state.SafkState;
import io.qxl.safk.api.state.SafkStatus;

@ApiStatus.Internal
public class SafkEntryList
{
	private static final SafkEntryList INSTANCE = new SafkEntryList();
	public static SafkEntryList getInstance() { return INSTANCE; }
	private final ConcurrentHashMap<UUID, SafkEntry> map;

	private SafkEntryList()
	{
		this.map = new ConcurrentHashMap<>(16, 0.9f, 1);
	}

	public @Nullable SafkEntry get(@Nonnull SafkServerPlayer player)
	{
		return this.get(player.getUUID());
	}

	public @Nullable SafkEntry get(UUID uuid)
	{
		if (this.map.containsKey(uuid))
		{
			return this.map.get(uuid);
		}

		return null;
	}

	public @Nullable SafkEntry add(@Nonnull SafkServerPlayer player, SafkState state)
	{
		if (this.get(player) == null)
		{
			SafkEntry entry = SafkEntry.create(player);

			if (state.status() == SafkStatus.ACTIVE)
			{
				entry.updateState(state);
			}

			this.map.put(player.getUUID(), entry);
			SaveAfk.debugLog("SafkEntryList(): add({}) --> ADD", entry.name().getString());
			return entry;
		}

		return this.get(player);
	}

	public boolean contains(UUID uuid)
	{
		return this.map.containsKey(uuid);
	}

	public void updateFromSafk(@Nonnull SafkServerPlayer player)
	{
		UUID uuid = player.getUUID();

		if (this.map.containsKey(uuid))
		{
			SafkEntry entry = this.map.get(uuid);

			if (entry != null)
			{
				entry.setPlayer(player);
			}
		}
	}

	public void syncEntry(@Nonnull SafkServerPlayer player, SafkEntry entry)
	{
		UUID uuid = player.getUUID();

		if (entry.matches(uuid))
		{
			this.map.remove(uuid);
			entry.setPlayer(player);
			this.map.put(uuid, entry);
		}
	}

	public void remove(@Nonnull UUID uuid, boolean silent, SafkStatus reason)
	{
		SafkEntry entry = this.map.remove(uuid);

		if (entry != null)
		{
			this.map.remove(uuid);
			SaveAfk.debugLog("SafkEntryList(): remove({}) --> REMOVE", entry.name().getString());
			entry.handler().unregisterSaveAfk(silent, reason);
		}
	}

	public void remove(@Nonnull SafkServerPlayer player, boolean silent, SafkStatus reason)
	{
		this.remove(player.getUUID(), silent, reason);
	}

	@VisibleForTesting
	public ImmutableMap<UUID, SafkEntry> shadowMapCopy()
	{
		ImmutableMap.Builder<UUID, SafkEntry> builder = ImmutableMap.builder();
		this.map.forEach(builder::put);
		return builder.build();
	}

	@VisibleForTesting
	public Component getDebugFormatted(UUID uuid)
	{
		if (this.map.containsKey(uuid))
		{
			SafkEntry entry = this.map.get(uuid);

			if (entry != null)
			{
				return entry.debugFormatted();
			}
		}

		return Component.literal("§cAFK player not found§r");
	}
}
