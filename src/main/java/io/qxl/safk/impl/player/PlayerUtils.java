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

package io.qxl.safk.impl.player;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
//#if MC >= 26.2
//$$ import net.minecraft.world.entity.EntityTypes;
//#endif

import io.qxl.safk.impl.commands.server.SafkAdminCommand;
import io.qxl.safk.impl.config.ConfigWrap;
import io.qxl.safk.impl.player.wrap.ProfileWrap;

@ApiStatus.Internal
public class PlayerUtils
{
	public static Component formatEntityTooltip(GameProfile profile)
	{
		MutableComponent result = Component.literal(ProfileWrap.name(profile));
		//#if MC >= 1.21.5
		//$$ HoverEvent hoverEvent = new HoverEvent.ShowEntity(
		//#else
		HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_ENTITY,
				//#endif
				                               new HoverEvent.EntityTooltipInfo(getEntityTypeWrap(),
				                                                                ProfileWrap.id(profile),
				                                                                Component.literal(ProfileWrap.name(profile))
				                               )
		);
		result.withStyle(style -> style.withHoverEvent(hoverEvent));
		return result;
	}

	public static EntityType<?> getEntityTypeWrap()
	{
		//#if MC >= 26.2
		//$$ return EntityTypes.PLAYER;
		//#else
		return EntityType.PLAYER;
		//#endif
	}

	public static Component formatSuggestSpawnCommand(final String name)
	{
		MutableComponent result = Component.literal(name);
		ClickEvent clickEvent;
		HoverEvent hoverEvent;

		//#if MC >= 1.21.5
		//$$ clickEvent = new ClickEvent.SuggestCommand(
		//#else
		clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
		//#endif
				                    "/"+SafkAdminCommand.COMMAND+ " spawn " + name);
		//#if MC >= 1.21.5
		//$$ hoverEvent = new HoverEvent.ShowText(
		//#else
		hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
		//#endif
		                            Component.literal("Spawn"));

		result.withStyle(style ->
				                 style.withClickEvent(clickEvent)
				                      .withHoverEvent(hoverEvent)
		);

		return result;
	}

	public static Component formatSuggestKickCommand(final Component name)
	{
		MutableComponent result = name.copy();
		ClickEvent clickEvent;
		HoverEvent hoverEvent;

		//#if MC >= 1.21.5
		//$$ clickEvent = new ClickEvent.SuggestCommand(
		//#else
		clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
				//#endif
				                    "/"+SafkAdminCommand.COMMAND+ " kick " + name.getString());
		//#if MC >= 1.21.5
		//$$ hoverEvent = new HoverEvent.ShowText(
		//#else
		hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
				//#endif
				                    Component.literal("Kick"));

		result.withStyle(style ->
				                 style.withClickEvent(clickEvent)
				                      .withHoverEvent(hoverEvent)
		);

		return result;
	}

	public static long getServerStartDelta()
	{
		if (ConfigWrap.lastStart() > 0L && ConfigWrap.lastStop() > 0L)
		{
			return ConfigWrap.lastStart() - ConfigWrap.lastStop();
		}

		// Indeterminate.
		return 0L;
	}
}
