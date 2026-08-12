/*
 * This file is part of the SaveAFK project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  T3rr0or and contributors
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

package io.qxl.safk.impl.commands;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.level.ServerPlayer;

import io.qxl.safk.impl.Reference;
import io.qxl.safk.impl.config.ConfigWrap;

/**
 * Per-player timeout allowances, granted as permission nodes.
 *
 * Named rather than numeric because a permission node can only be tested by
 * name, never enumerated. safk.timeout.90 would mean shipping a config list of
 * every number worth checking; a fixed set of names avoids that.
 *
 * A granted tier replaces maxSafkTimeout for that player, so it can raise a
 * donor above the server cap or hold a newcomer below it. Highest wins, and no
 * node at all leaves the player on maxSafkTimeout.
 */
@ApiStatus.Internal
public enum TimeoutTier
{
	HOUR("hour", 60),
	DAY("day", 1440),
	WEEK("week", 10080),
	MONTH("month", 43200),
	/** Whatever maxSafkTimeout currently is, rather than a fixed span. */
	MAX("max", -2);

	private static final int USE_CONFIGURED = -2;

	private final String suffix;
	private final int minutes;

	TimeoutTier(String suffix, int minutes)
	{
		this.suffix = suffix;
		this.minutes = minutes;
	}

	public String node()
	{
		return Reference.MOD_ID + ".timeout." + this.suffix;
	}

	public int minutes()
	{
		return this.minutes == USE_CONFIGURED ? ConfigWrap.safk().maxSafkTimeout : this.minutes;
	}

	/**
	 * @return the timeout ceiling for this player, or -1 when they are uncapped.
	 */
	public static int resolve(@Nonnull ServerPlayer player)
	{
		int configured = ConfigWrap.safk().maxSafkTimeout;
		int best = 0;
		boolean granted = false;

		for (TimeoutTier tier : values())
		{
			if (!PermsWrap.checkNode(player, tier.node()))
			{
				continue;
			}

			int allowed = tier.minutes();

			if (SafkLimits.unlimited(allowed))
			{
				return -1;
			}

			granted = true;
			best = Math.max(best, allowed);
		}

		return granted ? best : configured;
	}
}
