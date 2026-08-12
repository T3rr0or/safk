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

package io.qxl.safk.impl.commands;

//#if MC >= 1.16.5
//$$ import me.lucko.fabric.api.permissions.v0.Permissions;
//#endif

//#if MC >= 1.21.11
//$$ import net.minecraft.server.permissions.PermissionLevel;
//$$ import net.minecraft.util.Mth;
//#endif
import java.util.function.Predicate;
import javax.annotation.Nonnull;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;

import io.qxl.safk.impl.config.ConfigWrap;

/**
 * (Lucko) Fabric Permissions API support only begins with MC 1.16.4+
 */
@ApiStatus.Internal
public class PermsWrap
{
	public static Predicate<CommandSourceStack> check(@Nonnull String node, int level)
	{
//#if MC >= 1.16.5
//$$		return Permissions.require(node, permissionFromInt(level));
//#else
		return (src -> src.hasPermission(permissionFromInt(level)));
//#endif
	}

	/**
	 * Reads advancedAdminOptions when the command runs, not when the tree is
	 * built, so toggling it takes effect on the next /safk-admin reload rather
	 * than the next server restart.
	 */
	public static Predicate<CommandSourceStack> checkAdv(@Nonnull String node, int level)
	{
//#if MC >= 1.16.5
//$$		Predicate<CommandSourceStack> allowed = Permissions.require(node, permissionFromInt(level));
//#else
		Predicate<CommandSourceStack> allowed = (src -> src.hasPermission(permissionFromInt(level)));
//#endif

		return (src -> ConfigWrap.mainOpt().advancedAdminOptions && allowed.test(src));
	}

	/**
	 * Node check with no operator-level fallback, so a tier has to be granted
	 * on purpose rather than coming free with op.
	 */
	public static boolean checkNode(@Nonnull Entity entity, @Nonnull String node)
	{
//#if MC >= 1.16.5
//$$		return Permissions.check(entity, node);
//#else
		return false;
//#endif
	}

	public static boolean check(@Nonnull Entity entity, @Nonnull String node, int level)
	{
//#if MC >= 1.16.5
//$$		return Permissions.check(entity, node, permissionFromInt(level));
//#else
		return entity.hasPermissions(permissionFromInt(level));
//#endif
	}

	//#if MC >= 1.21.11
//$$	public static PermissionLevel permissionFromInt(int level)
//$$	{
//$$		return PermissionLevel.byId(Mth.clamp(level, 0, PermissionLevel.OWNERS.id()));
//$$	}
//#else
public static int permissionFromInt(int level)
{
	return level;
}
//#endif
}
