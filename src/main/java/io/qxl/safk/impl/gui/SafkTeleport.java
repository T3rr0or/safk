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

package io.qxl.safk.impl.gui;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

//#if MC >= 1.21.2
//$$ import java.util.Set;
//#endif

/**
 * Moving a player to another player's spot.
 *
 * teleportTo grew a relative-movement set and a dismount flag in 1.21.2, so the
 * two shapes live here rather than in the menu code.
 */
@ApiStatus.Internal
public class SafkTeleport
{
	public static void to(@Nonnull ServerPlayer who, @Nonnull ServerPlayer target)
	{
		//#if MC >= 1.20.1
		//$$ ServerLevel level = target.serverLevel();
		//#else
		ServerLevel level = target.getLevel();
		//#endif

		//#if MC >= 1.21.2
		//$$ who.teleportTo(level, target.getX(), target.getY(), target.getZ(), Set.of(), target.getYRot(), target.getXRot(), true);
		//#else
		who.teleportTo(level, target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
		//#endif
	}
}
