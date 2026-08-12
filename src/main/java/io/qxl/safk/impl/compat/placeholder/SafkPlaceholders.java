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

package io.qxl.safk.impl.compat.placeholder;

import java.util.UUID;
import javax.annotation.Nullable;

import org.jetbrains.annotations.ApiStatus;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;

import io.qxl.safk.impl.Reference;
import io.qxl.safk.impl.SaveAfk;
import io.qxl.safk.impl.config.ConfigWrap;
import io.qxl.safk.impl.player.safk.SafkEntry;
import io.qxl.safk.impl.player.safk.SafkEntryList;
import io.qxl.safk.impl.text.LegacyText;

/**
 * Exposes AFK state to Placeholder API so a nametag or tab-list mod can render
 * it.
 *
 * SaveAFK deliberately does not write scoreboard teams itself. A player only
 * gets one team, so owning it would mean fighting whatever rank mod the server
 * runs; handing the state out instead lets the mod that owns nametags compose
 * "[AFK] rank Name" on its own terms.
 */
@ApiStatus.Internal
public class SafkPlaceholders
{
	private static final String MOD_ID = "placeholder-api";

	public static void register()
	{
		if (!FabricLoader.getInstance().isModLoaded(MOD_ID))
		{
			return;
		}

		try
		{
			registerAll();
			SaveAfk.LOGGER.info("Registered SaveAFK placeholders");
		}
		catch (Throwable e)
		{
			// A broken integration must never take the mod down with it.
			SaveAfk.LOGGER.warn("Could not register SaveAFK placeholders: {}", e.toString());
		}
	}

	private static void registerAll()
	{
		//#if MC >= 26.1
		//$$ each("marker", ctx -> marker(entryOf(ctx.hasServerPlayer() ? ctx.serverPlayer() : null)));
		//$$ each("is_afk", ctx -> String.valueOf(entryOf(ctx.hasServerPlayer() ? ctx.serverPlayer() : null) != null));
		//$$ each("duration", ctx -> text(entryOf(ctx.hasServerPlayer() ? ctx.serverPlayer() : null), Field.DURATION));
		//$$ each("remaining", ctx -> text(entryOf(ctx.hasServerPlayer() ? ctx.serverPlayer() : null), Field.REMAINING));
		//$$ each("reason", ctx -> text(entryOf(ctx.hasServerPlayer() ? ctx.serverPlayer() : null), Field.REASON));
		//#else
		Placeholders.register(id("marker"), (ctx, arg) ->
				PlaceholderResult.value(marker(entryOf(ctx.hasPlayer() ? ctx.player() : null))));
		Placeholders.register(id("is_afk"), (ctx, arg) ->
				PlaceholderResult.value(String.valueOf(entryOf(ctx.hasPlayer() ? ctx.player() : null) != null)));
		Placeholders.register(id("duration"), (ctx, arg) ->
				PlaceholderResult.value(text(entryOf(ctx.hasPlayer() ? ctx.player() : null), Field.DURATION)));
		Placeholders.register(id("remaining"), (ctx, arg) ->
				PlaceholderResult.value(text(entryOf(ctx.hasPlayer() ? ctx.player() : null), Field.REMAINING)));
		Placeholders.register(id("reason"), (ctx, arg) ->
				PlaceholderResult.value(text(entryOf(ctx.hasPlayer() ? ctx.player() : null), Field.REASON)));
		//#endif
	}

	//#if MC >= 26.1
	//$$ private static void each(String name, java.util.function.Function<eu.pb4.placeholders.api.ServerPlaceholderContext, String> fn)
	//$$ {
		//$$ Placeholders.registerServer(id(name), (ctx, arg) -> PlaceholderResult.value(fn.apply(ctx)));
	//$$ }
	//#endif

	private static ResourceLocation id(String path)
	{
		//#if MC >= 1.21.0
		//$$ return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path);
		//#else
		return new ResourceLocation(Reference.MOD_ID, path);
		//#endif
	}

	/**
	 * @return the live AFK entry for this player, or null when they are not AFK.
	 */
	private static @Nullable SafkEntry entryOf(@Nullable ServerPlayer player)
	{
		if (player == null)
		{
			return null;
		}

		UUID uuid = player.getUUID();

		return SafkEntryList.getInstance().contains(uuid) ? SafkEntryList.getInstance().get(uuid) : null;
	}

	/**
	 * Empty for anyone who is not AFK, so a nametag format can interpolate this
	 * unconditionally without leaving a stray marker on everybody.
	 *
	 * Carries its own trailing space when present. A format string cannot put a
	 * separator behind a value that is sometimes empty, so the space has to
	 * travel with the marker or every name picks up a stray gap.
	 */
	private static String marker(@Nullable SafkEntry entry)
	{
		if (entry == null)
		{
			return "";
		}

		String prefix = ConfigWrap.mess().tabListPrefix;

		if (prefix == null)
		{
			return "";
		}

		String plain = LegacyText.strip(prefix).trim();

		return plain.isEmpty() ? "" : plain + " ";
	}

	private static String text(@Nullable SafkEntry entry, Field field)
	{
		if (entry == null)
		{
			return "";
		}

		return switch (field)
		{
			case DURATION -> entry.durationFormatted();
			case REMAINING -> entry.timeoutFormatted();
			case REASON -> entry.reasonFormatted();
		};
	}

	private enum Field
	{
		DURATION,
		REMAINING,
		REASON
	}
}
