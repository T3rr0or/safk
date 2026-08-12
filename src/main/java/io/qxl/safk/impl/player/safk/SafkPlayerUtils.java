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

import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.ApiStatus;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
//#if MC >= 1.19.3
//$$ import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
//#endif
//#if MC >= 1.21.11
//$$ import net.minecraft.server.permissions.Permissions;
//#endif
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
//#if MC >= 1.21.6
//$$ import net.minecraft.server.waypoints.ServerWaypointManager;
//$$ import io.qxl.safk.impl.player.interfaces.IWaypointManagerInvoker;
//#endif

import io.qxl.safk.api.SafkEvents;
import io.qxl.safk.impl.SaveAfk;
import io.qxl.safk.impl.config.ConfigWrap;
import io.qxl.safk.impl.events.PlayerEventsHandler;
import io.qxl.safk.impl.modinit.InitWrap;
import io.qxl.safk.impl.player.PlayerManager;
import io.qxl.safk.impl.player.wrap.ProfileWrap;
import io.qxl.safk.api.state.SafkState;
import io.qxl.safk.api.state.SafkStatus;

@ApiStatus.Internal
public class SafkPlayerUtils
{
	@ApiStatus.Internal
	public static boolean ensureSafeForUUID(@Nonnull MinecraftServer server, @Nonnull UUID uuid)
	{
		PlayerList playerList = server.getPlayerList();
		List<ServerPlayer> players = playerList.getPlayers();
		boolean isSafe = true;

		for (ServerPlayer player : players)
		{
			if (player.getUUID().equals(uuid))
			{
				isSafe = false;
				break;
			}
		}

		return isSafe;
	}

	@ApiStatus.Internal
	public static ImmutableList<SafkServerPlayer> getShadows(@Nonnull MinecraftServer server)
	{
		ImmutableList.Builder<SafkServerPlayer> builder = ImmutableList.builder();
		PlayerList pl = server.getPlayerList();
		List<ServerPlayer> players = pl.getPlayers();

		for (ServerPlayer player : players)
		{
			if (player instanceof SafkServerPlayer sp)
			{
				builder.add(sp);
			}
		}

		return builder.build();
	}

	@ApiStatus.Internal
	public static void hideAllSafkFromPlayer(@Nonnull MinecraftServer server, @Nonnull ServerPlayer player)
	{
		if (ConfigWrap.safk().safkHidePlayer)
		{
			ImmutableList<SafkServerPlayer> shadows = getShadows(server);
			boolean result = false;

			if (ConfigWrap.safk().safkHideFromOps && isOpWrap(player))
			{
				result = true;
			}
			else if (!isOpWrap(player))
			{
				result = true;
			}

			if (result)
			{
				for (SafkServerPlayer shadow : shadows)
				{
					sendRemovePacketToPlayerWrap(shadow, player);
				}
			}
		}
	}

	@ApiStatus.Internal
	public static void unhideAllSafkFromPlayer(@Nonnull MinecraftServer server, @Nonnull ServerPlayer player)
	{
		if (!ConfigWrap.safk().safkHidePlayer ||
			(!ConfigWrap.safk().safkHideFromOps) && isOpWrap(player))
		{
			ImmutableList<SafkServerPlayer> shadows = getShadows(server);

			for (SafkServerPlayer shadow : shadows)
			{
				sendAddPacketToPlayerWrap(shadow, player);

				// Note, that the difference between hiding from
				// Ops vs all players; is indistinguishable for Waypoints

				//#if MC >= 1.21.6
				//$$ if (!ConfigWrap.safk().safkHidePlayer)
				//$$ {
					//$$ player.level().getWaypointManager().addPlayer(shadow);
				//$$ }
				//#endif
			}
		}
	}

	@ApiStatus.Internal
	protected static void sendHidePlayerPacket(@Nonnull MinecraftServer server, @Nonnull SafkServerPlayer sp)
	{
		if (ConfigWrap.safk().safkHidePlayer)
		{
			PlayerList pl = server.getPlayerList();
			List<ServerPlayer> players = pl.getPlayers();

			for (ServerPlayer player : players)
			{
				boolean result = false;

				if (ConfigWrap.safk().safkHideFromOps && isOpWrap(player))
				{
					result = true;
				}
				else if (!isOpWrap(player))
				{
					result = true;
				}

				if (result)
				{
					sendRemovePacketToPlayerWrap(sp, player);
				}
			}
		}
	}

	@ApiStatus.Internal
	protected static boolean isOpWrap(@Nonnull ServerPlayer player)
	{
		//#if MC >= 1.21.11
		//$$ return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
		//#else
		return player.hasPermissions(2);
		//#endif
	}

	@ApiStatus.Internal
	protected static void sendAddPacketToPlayerWrap(@Nonnull SafkServerPlayer sp, @Nonnull ServerPlayer player)
	{
		player.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, sp));
		//#if MC >= 1.19.3
		//$$ player.connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, sp));
		//#endif
	}

	@ApiStatus.Internal
	protected static void sendRemovePacketToPlayerWrap(@Nonnull SafkServerPlayer sp, @Nonnull ServerPlayer player)
	{
		//#if MC >= 1.19.3
		//$$ player.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(sp.getUUID())));
		//#else
		player.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, List.of(sp)));
		//#endif
	}

	//#if MC >= 1.21.6
	//$$ @ApiStatus.Internal
	//$$ public static void onAddOrUpdateWaypoint(ServerWaypointManager manager, @Nonnull ServerPlayer player)
	//$$ {
		//$$ if (ConfigWrap.safk().safkHidePlayer && player instanceof SafkServerPlayer sp)
		//$$ {
			//$$ if (sp.isValid())
			//$$ {
				//$$ boolean result = false;

				//$$ if (ConfigWrap.safk().safkHideFromOps && isOpWrap(player))
				//$$ {
					//$$ result = true;
				//$$ }
				//$$ else if (!isOpWrap(player))
				//$$ {
					//$$ result = true;
				//$$ }

				//$$ if (result)
				//$$ {
					//$$ ((IWaypointManagerInvoker) manager).safk$removePlayer(player);
				//$$ }
			//$$ }
		//$$ }
	//$$ }

	//$$ @ApiStatus.Internal
	//$$ public static void onUnhideWaypoint(ServerWaypointManager manager, @Nonnull ServerPlayer player)
	//$$ {
		//$$ if (!ConfigWrap.safk().safkHidePlayer && player instanceof SafkServerPlayer sp)
		//$$ {
			//$$ if (sp.isValid())
			//$$ {
				//$$ ((IWaypointManagerInvoker) manager).safk$addPlayer(player);
			//$$ }
		//$$ }
	//$$ }
	//#endif

	@ApiStatus.Internal
	public static void checkForSafkAtPreLogin(PlayerList playerList, GameProfile profile, ServerPlayer player)
	{
		if (player instanceof SafkServerPlayer sp)
		{
			if (sp.isValid())
			{
				SafkEntry entry = SafkEntryList.getInstance().get(sp);

				if (entry != null)
				{
					SafkEntryList.getInstance().remove(sp, false, SafkStatus.INTERRUPTED);
				}

				final long delta = getStartTimeDelta(sp.getStartTime());
				final String reason = ConfigWrap.mess().safkUnsuccessful
						+ (ConfigWrap.mess().displayDuration
						   ? ConfigWrap.mess().safkUnsuccessfulPrefix
						     + ConfigWrap.mess().duration.option.format(delta)
						: "")
						+ ConfigWrap.mess().safkUnsuccessfulPunctuation
						+ ConfigWrap.mess().safkReplaced;

				SafkState newState = new SafkState(SafkStatus.INTERRUPTED, -1, -1, -1L, reason);
				PlayerManager.getInstance().setState(profile, newState);
			}

			if (player.isInvulnerable() && player.gameMode.isSurvival())
			{
				player.setInvulnerable(false);
			}

			final String name = ProfileWrap.name(profile);

			if (ConfigWrap.mess().hideSafkJoin)
			{
				PlayerEventsHandler.getInstance().addShouldHideJoin(name);
			}

			String str = ConfigWrap.mess().safkReplaced;
			sp.kill(InitWrap.text().formatText(str));
			playerList.remove(player);
		}
	}

	@ApiStatus.Internal
	public static void respawnSaveAfk(GameProfile profile, SafkServerPlayer oldSp, SafkServerPlayer newSp)
	{
		newSp.updateTimeOut(oldSp.getTimeout());
		SafkEntryList.getInstance().updateFromSafk(newSp);
		SafkEntry entry = SafkEntryList.getInstance().get(newSp);
		SafkState state = PlayerManager.getInstance().getState(profile);
		SafkState oldState = oldSp.toState();
		SafkState newState;
//		final long now = System.currentTimeMillis();
		boolean dirty = false;

		if (state.status() == SafkStatus.ACTIVE && oldState.status() != SafkStatus.ACTIVE)
		{
			newState = state;
			dirty = true;
		}
		else if (oldState.status() == SafkStatus.ACTIVE && state.status() != SafkStatus.ACTIVE)
		{
			newState = oldState;
			dirty = true;
		}
//		else if (state.status() != SafkStatus.ACTIVE)
//		{
//			newState = new SafkState(SafkStatus.ACTIVE, state.time(), oldSp.getTimeout(), now, state.reason());
//			dirty = true;
//		}
		else
		{
			newState = oldState;
		}

		if (dirty)
		{
			PlayerManager.getInstance().setState(profile, newState);

			if (entry != null)
			{
				entry.updateState(newState);
			}
		}

		newSp.fromState(newState);
		SafkEvents.SAFK_RESPAWN.invoker().onSafkEvent(ProfileWrap.id(profile), newState);
	}

	@ApiStatus.Internal
	public static long getStartTimeDelta(final long startTime)
	{
		return (System.currentTimeMillis() - startTime);
	}

	@ApiStatus.Internal
	public static boolean matchesJoinPattern(Component message)
	{
//		SaveAfk.debugLog("matchesJoinPattern(): {}", message.getString());
		if (message.getContents() instanceof TranslatableContents text)
		{
			String key = text.getKey();
			return (key.equals("multiplayer.player.joined") || key.equals("multiplayer.player.joined.renamed") || key.equals("multiplayer.player.left"));
		}

		return false;
	}

	@ApiStatus.Internal
	public static void sendJoinMessage(MinecraftServer server, Component name)
	{
		if (!ConfigWrap.mess().hideSafkJoin)
		{
			server.getPlayerList().broadcastSystemMessage(Component.translatable("multiplayer.player.joined", name).withStyle(ChatFormatting.YELLOW), false);
		}
	}

	@ApiStatus.Internal
	public static void sendLeaveMessage(MinecraftServer server, Component name)
	{
		if (!ConfigWrap.mess().hideSafkJoin)
		{
			server.getPlayerList().broadcastSystemMessage(Component.translatable("multiplayer.player.left", name).withStyle(ChatFormatting.YELLOW), false);
		}
	}
}
