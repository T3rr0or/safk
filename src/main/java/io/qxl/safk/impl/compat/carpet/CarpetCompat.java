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

package io.qxl.safk.impl.compat.carpet;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.ApiStatus;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import io.qxl.safk.impl.compat.mixin.ModIds;

/**
 * Carpet's /player bots are ServerPlayer subclasses, so nothing in the vanilla
 * API separates them from a human who logged in. Left untracked they would end
 * up in the config as real players, and SaveAFK would respawn them after a
 * restart as its own bots, quietly taking them away from carpet's /player kill.
 *
 * Carpet is optional and never on the compile classpath, so the check walks the
 * class hierarchy by name rather than referencing the type.
 */
@ApiStatus.Internal
public class CarpetCompat
{
	private static final String FAKE_PLAYER_CLASS = "carpet.patches.EntityPlayerMPFake";
	private static Boolean carpetLoaded;

	public static boolean isCarpetLoaded()
	{
		if (carpetLoaded == null)
		{
			carpetLoaded = FabricLoader.getInstance().isModLoaded(ModIds.carpet);
		}

		return carpetLoaded;
	}

	public static boolean isFakePlayer(@Nonnull ServerPlayer player)
	{
		if (!isCarpetLoaded())
		{
			return false;
		}

		for (Class<?> clazz = player.getClass(); clazz != null; clazz = clazz.getSuperclass())
		{
			if (FAKE_PLAYER_CLASS.equals(clazz.getName()))
			{
				return true;
			}
		}

		return false;
	}
}
