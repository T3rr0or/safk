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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import com.sakuraryoko.corelib.impl.config.ConfigManager;
import io.qxl.safk.api.state.GameState;
import io.qxl.safk.api.state.PosState;
import io.qxl.safk.api.state.SafkState;
import io.qxl.safk.api.state.SafkStatus;
import io.qxl.safk.impl.Reference;
import io.qxl.safk.impl.SaveAfk;
import io.qxl.safk.impl.config.ConfigWrap;
import io.qxl.safk.impl.config.SafkConfigHandler;
import io.qxl.safk.impl.config.data.options.PlayerOptions;
import io.qxl.safk.impl.events.ServerEventsHandler;
import io.qxl.safk.impl.modinit.InitWrap;
import io.qxl.safk.impl.modinit.SafkInit;
import io.qxl.safk.impl.player.wrap.*;
import io.qxl.safk.impl.player.safk.*;

@ApiStatus.Internal
public class PlayerManager
{
	private static final PlayerManager INSTANCE = new PlayerManager();
	public static PlayerManager getInstance() { return INSTANCE; }

	private static final float TICK_RATE = 5.0f;
	private final ConcurrentHashMap<UUID, PlayerEntry> players;
	private long lastTick;

	@ApiStatus.Internal
	private PlayerManager()
	{
		this.players = new ConcurrentHashMap<>(16, 0.9f, 1);
		this.lastTick = System.currentTimeMillis();
	}

	@ApiStatus.Internal
	public void syncProfile(GameProfile profile)
	{
		if (profile == null) { return; }
		List<PlayerOptions> config = ConfigWrap.players();
		UUID uuid = ProfileWrap.id(profile);

		SaveAfk.debugLog("syncProfile: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));

		for (PlayerOptions opt : config)
		{
			if (opt.uuid.equals(uuid))
			{
				this.addOrUpdateProfile(profile, opt.state);
				return;
			}
		}

		// Doesn't exist in config --> Add
		this.addConfig(profile);
		this.addOrUpdateProfile(profile, SafkState.DEFAULT);
	}

	@ApiStatus.Internal
	public void resetFromConfig()
	{
		this.players.clear();
	}

	@ApiStatus.Internal
	public void syncFromConfig(@Nonnull PlayerOptions opt)
	{
		SaveAfk.debugLog("syncFromConfig: ['{}'/{}]", opt.name, opt.uuid.toString());
		this.addOrUpdateProfile(ProfileWrap.profile(opt.uuid, opt.name), opt.state, opt.pos, opt.game);
	}

	@ApiStatus.Internal
	private void addOrUpdateProfile(@Nonnull GameProfile profile, SafkState state)
	{
		this.addOrUpdateProfile(profile, state, PosWrap.defaultPos(), GameWrap.defMode());
	}

	@ApiStatus.Internal
	private void addOrUpdateProfile(@Nonnull GameProfile profile, SafkState state, PosState pos, GameState game)
	{
		UUID uuid = ProfileWrap.id(profile);
		String name = ProfileWrap.name(profile);
		PlayerEntry newEntry = new PlayerEntry(uuid, name, state, pos, game);

		if (this.players.containsKey(uuid))
		{
			PlayerEntry entry = this.players.get(uuid);

			if (entry != null && !entry.state().equals(state))
			{
				entry = this.players.remove(uuid);
				newEntry = entry.updateState(state);
				this.players.put(uuid, newEntry);
			}
		}
		else
		{
			this.players.put(uuid, newEntry);
		}

		SaveAfk.debugLog("addOrUpdateProfile: player: ['{}'/{}] state: {}", ProfileWrap.name(profile), ProfileWrap.id(profile), state.toString());
	}

	@ApiStatus.Internal
	private void addConfig(@Nonnull GameProfile profile)
	{
		List<PlayerOptions> config = new ArrayList<>(ConfigWrap.players());
		UUID uuid = ProfileWrap.id(profile);
		boolean exists = false;

		for (PlayerOptions entry : config)
		{
			if (entry.uuid.equals(uuid))
			{
				exists = true;
				break;
			}
		}

		if (!exists)
		{
			PlayerOptions opt = PlayerOptions.fromProfile(profile, SafkState.DEFAULT);

			if (this.players.containsKey(uuid))
			{
				PlayerEntry entry = this.players.get(uuid);

				if (entry != null)
				{
					opt.pos = entry.pos();
					opt.game = entry.game();
				}
			}

			ConfigWrap.players().add(opt);
		}

		SaveAfk.debugLog("addConfig: player: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
	}

	@ApiStatus.Internal
	private void setConfig(@Nonnull GameProfile profile, SafkState state)
	{
		List<PlayerOptions> config = new ArrayList<>(ConfigWrap.players());
		UUID uuid = ProfileWrap.id(profile);
		boolean dirty = false;

		for (PlayerOptions entry : config)
		{
			if (entry.uuid.equals(uuid))
			{
				entry.state = state;
				entry.name = ProfileWrap.name(profile);

				if (this.players.containsKey(uuid))
				{
					PlayerEntry playerEntry = this.players.get(uuid);

					if (playerEntry != null)
					{
						entry.pos = playerEntry.pos();
						entry.game = playerEntry.game();
					}

					break;
				}

				dirty = true;
			}
		}

		if (dirty)
		{
			ConfigWrap.players().clear();

			for (PlayerOptions entry : config)
			{
				PlayerOptions opt = new PlayerOptions(entry);

				if (this.players.containsKey(uuid))
				{
					PlayerEntry playerEntry = this.players.get(uuid);

					if (playerEntry != null)
					{
						entry.pos = playerEntry.pos();
						entry.game = playerEntry.game();
					}

					break;
				}

				ConfigWrap.players().add(opt);
			}
		}

		SaveAfk.debugLog("setConfig: player: ['{}'/{}] state: {}", ProfileWrap.name(profile), ProfileWrap.id(profile), state.toString());
	}

	public SafkState getState(@Nonnull GameProfile profile)
	{
		UUID uuid = ProfileWrap.id(profile);

		if (this.players.containsKey(uuid))
		{
			PlayerEntry entry = this.players.get(uuid);

			if (entry != null)
			{
				return entry.state();
			}
		}

		SaveAfk.debugLog("getState: player: ['{}'/{}] failure; adding new entry", ProfileWrap.name(profile), ProfileWrap.id(profile));
		this.addOrUpdateProfile(profile, SafkState.DEFAULT);
		this.addConfig(profile);
		return SafkState.DEFAULT;
	}

	@ApiStatus.Internal
	public SafkState getState(@Nonnull UUID uuid)
	{
		if (this.players.containsKey(uuid))
		{
			PlayerEntry entry = this.players.get(uuid);

			if (entry != null)
			{
				return entry.state();
			}
		}

		SaveAfk.debugLog("getState: UUID: [{}] failure; returning default state", uuid.toString());
		return SafkState.DEFAULT;
	}

	@ApiStatus.Internal
	public void setState(@Nonnull GameProfile profile, SafkState state)
	{
		this.addOrUpdateProfile(profile, state);
		this.setConfig(profile, state);
		SaveAfk.debugLog("setState: player: ['{}'/{}] state: {}", ProfileWrap.name(profile), ProfileWrap.id(profile), state.toString());
	}

	public void resetState(@Nonnull ServerPlayer player)
	{
		this.resetState(player.getGameProfile());
	}

	public void resetState(@Nonnull GameProfile profile)
	{
		this.setState(profile, SafkState.DEFAULT);
	}

	public void remove(@Nonnull UUID uuid, boolean silent, SafkStatus reason)
	{
		SafkEntryList.getInstance().remove(uuid, silent, reason);
		this.players.remove(uuid);
		ConfigWrap.players().removeIf(opt -> opt.uuid.equals(uuid));
		SaveAfk.debugLog("remove: UUID: [{}], silent: {}, reason: {}", uuid.toString(), silent, reason.toString());
	}

	public PosState getPos(@Nonnull UUID uuid)
	{
		List<PlayerOptions> config = new ArrayList<>(ConfigWrap.players());

		if (this.players.containsKey(uuid))
		{
			PlayerEntry entry = this.players.get(uuid);

			if (entry != null)
			{
				return entry.pos();
			}
		}

//		List<PlayerOptions> config = new ArrayList<>(ConfigWrap.players());

		for (PlayerOptions entry : config)
		{
			if (entry.uuid.equals(uuid))
			{
				return entry.pos;
			}
		}

		return PosWrap.defaultPos();
	}

	public GameState getGameMode(@Nonnull UUID uuid)
	{
		if (this.players.containsKey(uuid))
		{
			PlayerEntry entry = this.players.get(uuid);

			if (entry != null)
			{
				return entry.game();
			}
		}

		List<PlayerOptions> config = new ArrayList<>(ConfigWrap.players());

		for (PlayerOptions entry : config)
		{
			if (entry.uuid.equals(uuid))
			{
				return entry.game;
			}
		}

		return GameWrap.defMode();
	}

	@ApiStatus.Internal
	public void updatePlayerData(@Nonnull ServerPlayer player)
	{
		PosState pos = PosWrap.of(player);
		GameState game = GameWrap.of(player);
		UUID uuid = player.getUUID();
		GameProfile profile = player.getGameProfile();

		if (this.players.containsKey(uuid))
		{
			PlayerEntry entry = this.players.remove(uuid);

			if (entry != null)
			{
				if (entry.state().status() == SafkStatus.ACTIVE)
				{
					SafkEntry safk = SafkEntryList.getInstance().get(uuid);

					if (safk != null)
					{
						entry = entry.updateAll(safk.toState(), player.getName().getString(), pos, game);
					}
					else
					{
						entry = entry.updatePlayerData(player.getName().getString(), pos, game);
					}
				}
				else
				{
					entry = entry.updatePlayerData(player.getName().getString(), pos, game);
				}

				if (Reference.DEBUG)
				{
					SaveAfk.debugLog("updatePlayerData: player: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
				}

				this.players.put(uuid, entry);
			}
		}
	}

	@ApiStatus.Internal
	public ImmutableList<GameProfile> getSpawnCommandSuggestions(@Nonnull CommandContext<CommandSourceStack> ctx)
	{
		ImmutableList.Builder<GameProfile> builder = ImmutableList.builder();
		MinecraftServer server = ctx.getSource().getServer();
		PlayerList playerList = server.getPlayerList();
		final List<ServerPlayer> players = playerList.getPlayers();

		this.players.forEach(
				(uuid, entry) ->
				{
					boolean found = false;

					for (ServerPlayer player : players)
					{
						if (player.getUUID().equals(uuid))
						{
							found = true;
							break;
						}
					}

					if (!found)
					{
						builder.add(ProfileWrap.profile(entry.uuid(), entry.name()));
					}
				}
		);

		return builder.build();
	}

	@ApiStatus.Internal
	public ImmutableList<GameProfile> getKickCommandSuggestions(@Nonnull CommandContext<CommandSourceStack> ctx)
	{
		ImmutableList.Builder<GameProfile> builder = ImmutableList.builder();
		MinecraftServer server = ctx.getSource().getServer();
		PlayerList playerList = server.getPlayerList();
		final List<ServerPlayer> players = playerList.getPlayers();

		this.players.forEach(
				(uuid, entry) ->
				{
					boolean found = false;

					if (entry.state().status() == SafkStatus.ACTIVE)
					{
						for (ServerPlayer player : players)
						{
							if (player.getUUID().equals(uuid))
							{
								found = true;
								break;
							}
						}
					}
					else
					{
						for (ServerPlayer player : players)
						{
							if (player.getUUID().equals(uuid) && player instanceof SafkServerPlayer sp)
							{
								// Fix desynced player
								SafkState newState = new SafkState(SafkStatus.ACTIVE, sp.getTimer(), sp.getTimeout(), sp.getStartTime(), sp.getReason());
								this.setState(player.getGameProfile(), newState);
								found = true;
								break;
							}
						}
					}

					if (found)
					{
						builder.add(ProfileWrap.profile(entry.uuid(), entry.name()));
					}
				}
		);

		return builder.build();
	}

	@VisibleForTesting
	public ImmutableMap<UUID, PlayerEntry> playerMapCopy()
	{
		ImmutableMap.Builder<UUID, PlayerEntry> map = ImmutableMap.builder();
		this.players.forEach(map::put);
		return map.build();
	}

	@VisibleForTesting
	public Component getDebugFormatted(UUID uuid)
	{
		if (this.players.containsKey(uuid))
		{
			PlayerEntry entry = this.players.get(uuid);

			if (entry != null)
			{
				return entry.getDebugFormatted();
			}
		}

		return Component.literal("§cPlayer not found§r");
	}

	@ApiStatus.Internal
	private boolean syncConfig(@Nonnull MinecraftServer server, boolean stop)
	{
		PlayerList playerList = server.getPlayerList();
		List<ServerPlayer> players = playerList.getPlayers();
		boolean dirty = false;

		for (ServerPlayer player : players)
		{
			if (this.syncConfigEach(player))
			{
				dirty = true;
			}
		}

		if (stop) { return dirty; }

		// Spawn shadow configured players
		ImmutableList<PlayerOptions> config = ImmutableList.copyOf(ConfigWrap.players());
		final long serverDelta = PlayerUtils.getServerStartDelta();

		for (PlayerOptions entry : config)
		{
			boolean found = false;

			for (ServerPlayer player : players)
			{
				if (entry.uuid.equals(player.getUUID()))
				{
					found = true;
					break;
				}
			}

			if (!found && entry.state.status() == SafkStatus.ACTIVE)
			{
				boolean schedule = true;

				// Calculate Server Start / Stop timeout offset.
				if (serverDelta > 0L)
				{
					SafkState oldState = entry.state;
					SafkState newState;
					final long oldTimeout = oldState.timeout();
					final long newTimeout = oldTimeout - serverDelta;

					if (newTimeout < 1L)
					{
						String mess = ConfigWrap.mess().safkExpiredReason;

						if (mess == null || mess.isEmpty())
						{
							mess = "§eTimeout Expired§r";
						}

						final long delta = SafkPlayerUtils.getStartTimeDelta(entry.state.startTime());
						final String reason = (ConfigWrap.mess().displayDuration
						                       ? ConfigWrap.mess().safkSuccessfulPrefix
						                         + ConfigWrap.mess().duration.option.format(delta)
						                         + ConfigWrap.mess().safkSuccessfulSuffix
						                       : ConfigWrap.mess().safkSuccessful)
								+ mess;
						newState = new SafkState(SafkStatus.EXPIRED, -1, -1L, -1L, reason);
						SaveAfk.debugLog("syncConfig: player: ['{}'/{}], serverDelta expired session [diff: {}]", entry.name, entry.uuid.toString(), (oldTimeout - newTimeout));
						schedule = false;
					}
					else
					{
						newState = new SafkState(oldState.status(), oldState.time(), newTimeout, oldState.startTime(), oldState.reason());
						SaveAfk.debugLog("syncConfig: player: ['{}'/{}], add serverDelta to timeout [{} -> {}], diff: [{}]", entry.name, entry.uuid.toString(), oldTimeout, newTimeout, (oldTimeout - newTimeout));
					}

					this.setState(ProfileWrap.profile(entry.uuid, entry.name), newState);
					SafkEntryList.getInstance().remove(entry.uuid, true, SafkStatus.EXPIRED);
					dirty = true;
				}

				if (schedule)
				{
					ServerEventsHandler.getInstance().toggleSpawnSafe(false);
					SaveAfk.debugLog("syncConfig: Scheduling AFK player: ['{}'/{}], state: [{}]", entry.name, entry.uuid.toString(), entry.state.toString());
					SafkPendingSpawns.INSTANCE.scheduleSpawn(entry);
				}
			}
		}

		return dirty;
	}

	@ApiStatus.Internal
	private boolean syncConfigEach(ServerPlayer player)
	{
		ImmutableList<PlayerOptions> oldConfig = ImmutableList.copyOf(ConfigWrap.players());
		List<PlayerOptions> newConfig = new ArrayList<>();
		String name = player.getName().getString();
		UUID uuid = player.getUUID();
		PosState pos = PosWrap.of(player);
		GameState game = GameWrap.of(player);
		SafkState state = this.getState(uuid).ensureValid();

		if (player instanceof SafkServerPlayer sp)
		{
			SafkEntry entry = SafkEntryList.getInstance().get(sp);

			if (entry == null)
			{
				if (state.isEmpty())
				{
					state = sp.toState();
				}

				entry = SafkEntryList.getInstance().add(sp, state);
			}

			if (entry != null)
			{
				state = new SafkState(entry.status(), entry.timer(), sp.getTimeout(), sp.getStartTime(), entry.reason());
			}
			else
			{
				state = new SafkState(sp.isValid() ? SafkStatus.ACTIVE : SafkStatus.INTERRUPTED, sp.getTimer(), sp.getTimeout(), sp.getStartTime(), sp.getReason());
			}
		}

		boolean found = false;
		boolean dirty = false;

		SaveAfk.debugLog("syncConfigEach(): Player ['{}'/{}]; [CALULATED] state: [{}], pos: [{}], game: [{}]", name, uuid.toString(), state.toString(), pos.toString(), game.toString());

		for (PlayerOptions entry : oldConfig)
		{
			PlayerOptions newEntry = new PlayerOptions(entry);

			if (newEntry.uuid.equals(uuid))
			{
				if (newEntry.state.isEmpty() || !newEntry.state.equals(state))
				{
					newEntry.state = state;
					dirty = true;
				}
				if (newEntry.pos.isEmpty() || !newEntry.pos.equals(pos))
				{
					newEntry.pos = pos;
					dirty = true;
				}
				if (newEntry.game.isEmpty() || !newEntry.game.equals(game))
				{
					newEntry.game = game;
					dirty = true;
				}
				if (newEntry.name.isEmpty() || newEntry.name.equals(uuid.toString()) || !newEntry.name.equals(name))
				{
					newEntry.name = name;
					dirty = true;
				}

				found = true;
			}

//			SaveAfk.debugLog("syncConfigEach(): Player ['{}'/{}]; [NEW-ENTRY] newEntry // state: [{}], pos: [{}], game: [{}]", newEntry.name, newEntry.uuid.toString(), newEntry.state.toString(), newEntry.pos.toString(), newEntry.game.toString());
			newConfig.add(newEntry);
		}

		if (!found)
		{
			PlayerOptions newEntry = PlayerOptions.fromProfile(player.getGameProfile(), state);
			newEntry.state = state;
			newEntry.pos = pos;
			newEntry.game = game;
			newConfig.add(newEntry);
//			SaveAfk.debugLog("syncConfigEach(): Player ['{}'/{}]; [NOT-FOUND] newEntry // state: [{}], pos: [{}], game: [{}]", newEntry.name, newEntry.uuid.toString(), newEntry.state.toString(), newEntry.pos.toString(), newEntry.game.toString());
			dirty = true;
		}

		if (dirty)
		{
			ConfigWrap.players().clear();

			for (PlayerOptions entry : newConfig)
			{
				PlayerOptions newEntry = new PlayerOptions(entry);

				// Double Verify
				if (newEntry.uuid.equals(uuid))
				{
					if (!newEntry.state.equals(state))
					{
						newEntry.state = state;
					}
					if (!newEntry.pos.equals(pos))
					{
						newEntry.pos = pos;
					}
					if (!newEntry.game.equals(game))
					{
						newEntry.game = game;
					}
				}

//				SaveAfk.debugLog("syncConfigEach(): Player ['{}'/{}]; [DIRTY] newEntry // state: [{}], pos: [{}], game: [{}]", newEntry.name, newEntry.uuid.toString(), newEntry.state.toString(), newEntry.pos.toString(), newEntry.game.toString());
				ConfigWrap.players().add(newEntry);
			}

			return true;
		}

		return false;
	}

	@ApiStatus.Internal
	public void flushToConfig(@Nonnull MinecraftServer server)
	{
		List<PlayerOptions> newConfig = new ArrayList<>();
		PlayerList playerList = server.getPlayerList();
		List<ServerPlayer> players = playerList.getPlayers();

		this.players.forEach(
				(uuid, playerEntry) ->
				{
					PlayerOptions newEntry = new PlayerOptions();
					newEntry.uuid = uuid;
					newEntry.name = playerEntry.name();
					newEntry.state = playerEntry.state();
					newEntry.pos = playerEntry.pos();
					newEntry.game = playerEntry.game();

					// Copy state from an active AFK player
					for (ServerPlayer player : players)
					{
						if (player.getUUID().equals(uuid))
						{
							if (player instanceof SafkServerPlayer sp)
							{
								newEntry.state = sp.toState();
							}

							break;
						}
					}

					newConfig.add(newEntry);
				}
		);

		ConfigWrap.players().clear();
		for (PlayerOptions entry : newConfig)
		{
			ConfigWrap.players().add(entry);
		}

		SaveAfk.debugLog("flushToConfig(): Done");
	}

	@ApiStatus.Internal
	public void onServerStop(@Nonnull MinecraftServer server)
	{
		SaveAfk.debugLog("onServerStop --> syncConfig()");
		SafkConfigHandler.getInstance().setStopTime();

		this.flushToConfig(server);
//		SaveAfk.debugLog("onServerStop(): flushing config changes ...");
		ConfigManager.getInstance().saveEach(SafkConfigHandler.getInstance());
	}

	@ApiStatus.Internal
	public void onServerStarted(@Nonnull MinecraftServer server)
	{
		SaveAfk.debugLog("onServerStarted --> syncConfig()");
		SafkConfigHandler.getInstance().setStartTime();

		if (this.syncConfig(server, false))
		{
//			SaveAfk.debugLog("onServerStarted(): flushing config changes ...");
			ConfigManager.getInstance().saveEach(SafkConfigHandler.getInstance());
		}
	}

	@ApiStatus.Internal
	public void onServerResync(@Nonnull MinecraftServer server, ImmutableMap<UUID, PlayerEntry> playerMap, ImmutableMap<UUID, SafkEntry> shadowMap)
	{
		boolean dirty = false;
		// Same as stop, really; just with a different message
		SaveAfk.debugLog("onServerResync --> syncConfig()");

		if (this.syncConfig(server, true))
		{
			dirty = true;
		}

		if (this.syncEntries(server, playerMap, shadowMap))
		{
			dirty = true;
		}

		if (dirty)
		{
//			SaveAfk.debugLog("onServerResync(): flushing config changes ...");
			ConfigManager.getInstance().saveEach(SafkConfigHandler.getInstance());
		}
	}

	private boolean syncEntries(@Nonnull MinecraftServer server, ImmutableMap<UUID, PlayerEntry> playerMap, ImmutableMap<UUID, SafkEntry> shadowMap)
	{
		PlayerList playerList = server.getPlayerList();
		List<ServerPlayer> players = playerList.getPlayers();
		boolean dirty = false;
		boolean updated = false;

		SaveAfk.debugLog("syncEntries --> count: {}", shadowMap.size());

		for (ServerPlayer player : players)
		{
			UUID uuid = player.getUUID();

			if (player instanceof SafkServerPlayer sp)
			{
				if (shadowMap.containsKey(uuid))
				{
					SafkEntry entry = shadowMap.get(uuid);

					if (entry != null)
					{
						SafkState newState = null;

						if (playerMap.containsKey(uuid))
						{
							PlayerEntry playerEntry = playerMap.get(uuid);

							if (playerEntry != null)
							{
								newState = playerEntry.state();
							}
						}

						if (newState == null)
						{
							newState = sp.toState();
						}

//						SaveAfk.debugLog("syncEntries --> sync: ['{}'/{}], state: [{}]", sp.getName().getString(), uuid.toString(), newState.toString());
						this.setState(sp.getGameProfile(), newState);
						entry.updateState(newState);
						SafkEntryList.getInstance().syncEntry(sp, entry);
						dirty = true;
					}
				}
			}

			PosState pos = PlayerManager.getInstance().getPos(uuid);

			if (!pos.matches(player))
			{
				this.updatePlayerData(player);
				updated = true;
			}
		}

		if (updated)
		{
			this.flushToConfig(server);
		}

		return dirty;
	}

	@ApiStatus.Internal
	public void onTick(@Nonnull MinecraftServer server, boolean spawnSafe)
	{
		final long now = System.currentTimeMillis();

		if ((now - this.lastTick) > this.onTickTimeout())
		{
			this.onTickCycle(server, spawnSafe);
			this.lastTick = now;
		}
	}

	private long onTickTimeout()
	{
		return (long) (TICK_RATE * 1000L);
	}

	private void onTickCycle(@Nonnull MinecraftServer server, boolean spawnSafe)
	{
		PlayerList playerList = server.getPlayerList();
		List<ServerPlayer> players = playerList.getPlayers();
		boolean dirty = false;

		for (ServerPlayer player : players)
		{
			if (this.onTickEach(player))
			{
				dirty = true;
			}
		}

		// Only spawn if marked as "safe to spawn";
		// which means there are pending spawns already scheduled, or
		// the Server is currently still starting up.
		if (spawnSafe)
		{
			ImmutableMap<UUID, PlayerEntry> playerMap = this.playerMapCopy();

			for (UUID uuid : playerMap.keySet())
			{
				PlayerEntry playerEntry = playerMap.get(uuid);

				if (playerEntry == null)
				{
					continue;
				}

				if (playerEntry.state().status() == SafkStatus.ACTIVE)
				{
					boolean found = false;

					for (ServerPlayer entry : players)
					{
						if (entry.getUUID().equals(uuid))
						{
							found = true;
							break;
						}
					}

					if (!found)
					{
						ImmutableList<PlayerOptions> config = ImmutableList.copyOf(ConfigWrap.players());
						ServerEventsHandler.getInstance().toggleSpawnSafe(false);

						for (PlayerOptions opt : config)
						{
							if (opt.uuid.equals(uuid))
							{
								if (InitWrap.debug())
								{
									SaveAfk.debugLog("onTickCycle: Scheduling AFK player: ['{}'/{}], state: [{}]", opt.name, opt.uuid.toString(), opt.state.toString());
								}
								else
								{
									SaveAfk.LOGGER.info("PlayerManager#onTickCycle: Scheduling AFK player: ['{}'/{}]", opt.name, opt.uuid.toString());
								}

								SafkPendingSpawns.INSTANCE.scheduleSpawn(opt);
								break;
							}
						}
					}
				}
			}
		}

		if (dirty)
		{
//			SaveAfk.debugLog("onServerResync(): flushing config changes ...");
			ConfigManager.getInstance().saveEach(SafkConfigHandler.getInstance());
		}
	}

	private boolean onTickEach(ServerPlayer player)
	{
		UUID uuid = player.getUUID();

		if (!this.players.containsKey(uuid))
		{
			SafkState newState = SafkState.DEFAULT;

			if (player instanceof SafkServerPlayer sp)
			{
				newState = sp.toState();
				this.addOrUpdateProfile(player.getGameProfile(), newState, PosWrap.of(player), GameWrap.of(player));

				if (!SafkEntryList.getInstance().contains(uuid))
				{
					SafkEntryList.getInstance().add(sp, newState);
				}
			}
			else
			{
				this.addOrUpdateProfile(player.getGameProfile(), newState, PosWrap.of(player), GameWrap.of(player));
			}

			SaveAfk.debugLog("onTickEach() sync: ['{}'/{}] --> added missing player", player.getName().getString(), uuid.toString());
			return true;
		}

		return false;
	}
}
