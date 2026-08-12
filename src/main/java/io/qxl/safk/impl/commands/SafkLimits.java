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

import javax.annotation.Nullable;

import org.jetbrains.annotations.ApiStatus;

import io.qxl.safk.impl.config.ConfigWrap;
import io.qxl.safk.impl.player.safk.SafkEntryList;

/**
 * Server-owner limits on who may go AFK and for how long. A negative value in
 * either config option turns that limit off, which is how an owner opts out.
 *
 * Admin spawns bypass all of this; the checks here only guard the player
 * facing commands.
 */
@ApiStatus.Internal
public class SafkLimits
{
	public static boolean unlimited(int value)
	{
		return value < 0;
	}

	/**
	 * Trims a timeout the player never asked for. Reached only when the
	 * configured default runs past the cap, so refusing would leave a bare
	 * /safk permanently broken.
	 */
	public static int clampToMax(int minutes)
	{
		int max = ConfigWrap.safk().maxSafkTimeout;

		if (unlimited(max) || minutes <= max)
		{
			return minutes;
		}

		return max;
	}

	/**
	 * @return null when the request is allowed, otherwise the refusal to show.
	 */
	public static @Nullable String rejectTimeout(int minutes)
	{
		int max = ConfigWrap.safk().maxSafkTimeout;

		if (unlimited(max) || minutes <= max)
		{
			return null;
		}

		return "§cThe longest you can go AFK for is §e" + max + "§c minutes§r";
	}

	/**
	 * @return null when there is room for another bot, otherwise the refusal.
	 */
	public static @Nullable String rejectConcurrent()
	{
		int max = ConfigWrap.safk().maxConcurrentBots;

		if (unlimited(max))
		{
			return null;
		}

		int active = SafkEntryList.getInstance().shadowMapCopy().size();

		if (active < max)
		{
			return null;
		}

		return "§cThe server already has §e" + max + "§c AFK players, try again later§r";
	}
}
