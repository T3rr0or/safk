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

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import io.qxl.safk.api.state.GameState;
import io.qxl.safk.api.state.PosState;
import io.qxl.safk.api.state.SafkState;
import io.qxl.safk.api.state.SafkStatus;
import io.qxl.safk.impl.events.PlayerEventsHandler;
import io.qxl.safk.impl.events.ServerEventsHandler;
import io.qxl.safk.impl.modinit.InitWrap;
import io.qxl.safk.impl.player.PlayerManager;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
//#if MC >= 1.21.10
//$$ import org.slf4j.Logger;
//$$ import org.slf4j.LoggerFactory;
//#endif
//#if MC >= 1.20.2
//$$ import java.util.Optional;
//$$ import java.util.concurrent.CompletableFuture;
//#endif

import com.mojang.authlib.GameProfile;
//#if MC >= 1.20.6
//$$ import net.minecraft.world.entity.ai.attributes.Attributes;
//#endif
//#if MC >= 1.20.1
//$$ import net.minecraft.core.BlockPos;
//$$ import net.minecraft.world.level.block.state.BlockState;
//#endif
//#if MC >= 1.21.2
//$$ import net.minecraft.network.DisconnectionDetails;
//$$ import net.minecraft.network.chat.contents.TranslatableContents;
//#endif
//#if MC >= 1.21.10
//$$ import net.minecraft.core.UUIDUtil;
//$$ import net.minecraft.server.players.NameAndId;
//$$ import net.minecraft.server.players.OldUsersConverter;
//$$ import net.minecraft.world.item.component.ResolvableProfile;
//$$ import net.minecraft.world.level.storage.TagValueInput;
//$$ import net.minecraft.world.level.storage.ValueInput;
//$$ import net.minecraft.util.ProblemReporter;
//#endif
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//#if MC >= 1.21.2
//$$ import net.minecraft.world.level.portal.TeleportTransition;
//#elseif MC >= 1.21.0
//$$ import net.minecraft.world.level.portal.DimensionTransition;
//#endif
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
//#if MC >= 1.20.2
//$$ import net.minecraft.server.network.CommonListenerCookie;
//$$ import net.minecraft.server.level.ClientInformation;
//#else
import net.minecraft.world.entity.player.ProfilePublicKey;
//#endif
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

//#if MC >= 1.21.10
//$$ import io.qxl.safk.impl.Reference;
//#endif
import io.qxl.safk.impl.SaveAfk;
import io.qxl.safk.impl.config.ConfigWrap;
import io.qxl.safk.impl.config.data.options.PlayerOptions;
import io.qxl.safk.impl.player.wrap.*;
import com.sakuraryoko.corelib.impl.text.BuiltinTextHandler;

@ApiStatus.Internal
@SuppressWarnings("EntityConstructor")
public class SafkServerPlayer extends ServerPlayer
{
	public Runnable startingPosition = () -> {};
	private boolean freshPlayer;
	private long freshHoldTime;
	private int time = 0;
	private String reason;
	private long startTime = -1L;
	private long timeout = -1L;
	private long lastTick = -1L;
	private boolean isValid = false;
	private boolean expired = false;

	//#if MC >= 1.20.2
	//$$ public SafkServerPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation ci)
	//$$ {
		//$$ super(server, level, profile, ci);
	//$$ }
	//#elseif MC >= 1.19.3
	//$$ public SafkServerPlayer(MinecraftServer server, ServerLevel level, GameProfile profile)
	//$$ {
		//$$ super(server, level, profile);
	//$$ }
	//#else
	public SafkServerPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, @Nullable ProfilePublicKey profilePublicKey)
	{
		super(server, level, profile, profilePublicKey);
	}
	//#endif

	//#if MC >= 1.21.10
	//$$ private static CompletableFuture<GameProfile> fetchGameProfile(MinecraftServer server, final UUID uuid)
	//$$ {
		//$$ final ResolvableProfile resolver = ResolvableProfile.createUnresolved(uuid);
		//$$ return resolver.resolveProfile(server.services().profileResolver());
	//$$ }
	//#elseif MC >= 1.20.2
	//$$ private static CompletableFuture<Optional<GameProfile>> fetchGameProfile(final String name)
	//$$ {
		//$$ return SkullBlockEntity.fetchGameProfile(name);
	//$$ }
	//#endif

	public static void createFromConfig(MinecraftServer server, PlayerOptions opts)
	{
		UUID uuid = opts.uuid;
		String name = opts.name;
		SafkState state = opts.state;
		PosState pos = opts.pos;
		GameState game = opts.game;
		ResourceLocation id = ResourceLocation.tryParse(pos.location());
		AtomicReference<ResourceKey<Level>> ref = new AtomicReference<>(Level.OVERWORLD);

		if (id != null)
		{
			server.levelKeys().forEach(levelKey ->
			                           {
										   if (levelKey.location().equals(id))
										   {
											   ref.set(levelKey);
										   }
			                           });
		}

		ServerLevel level = server.getLevel(ref.get());
		//#if MC >= 1.21.10
		//$$ server.services().nameToIdCache().resolveOfflineUsers(false);
		//#else
		GameProfileCache.setUsesAuthentication(false);
		//#endif
		GameProfile profile;

		//#if MC >= 1.21.10
		//$$ UUID tempUUID = OldUsersConverter.convertMobOwnerIfNecessary(server, name);
		//$$ if (tempUUID != null && !tempUUID.equals(uuid))
		//$$ {
			//$$ uuid = tempUUID;
			//$$ opts.uuid = uuid;
		//$$ }
		//$$ if (uuid == null)
		//$$ {
			//$$ uuid = UUIDUtil.createOfflinePlayerUUID(name);
		//$$ }
		//$$ server.services().nameToIdCache().resolveOfflineUsers(server.isDedicatedServer() && server.usesAuthentication());
		//$$ profile = new GameProfile(uuid, name);
		//#else
		try
		{
			profile = server.getProfileCache().get(name).orElse(
					  server.getProfileCache().get(uuid).orElse(null)
			);
		}
		finally
		{
			GameProfileCache.setUsesAuthentication(server.isDedicatedServer() && server.usesAuthentication());
		}

		if (profile == null)
		{
			profile = new GameProfile(uuid, name);
		}
		//#endif

		//#if MC >= 1.21.10
		//$$ if (server.getPlayerList().getBans().isBanned(new NameAndId(profile)))
		//#else
		if (server.getPlayerList().getBans().isBanned(profile))
		//#endif
		{
			if (ConfigWrap.mainOpt().debugMode)
			{
				SaveAfk.LOGGER.warn("createFromConfig: Blocking banned player: ['{}'/{}]", name, uuid.toString());
			}

			PlayerManager.getInstance().remove(uuid, true, SafkStatus.TERMINATED);
			return;
		}

		if (!SafkPlayerUtils.ensureSafeForUUID(server, uuid))
		{
			if (ConfigWrap.mainOpt().debugMode)
			{
				SaveAfk.LOGGER.error("createFromConfig: Blocking player: ['{}'/{}] -- Perhaps a duplicate UUID?", name, uuid.toString());
			}

			PlayerManager.getInstance().remove(uuid, true, SafkStatus.TERMINATED);
			return;
		}

		if (pos.x() == 0 && pos.y() == 0 && pos.z() == 0)
		{
			if (ConfigWrap.mainOpt().debugMode)
			{
				SaveAfk.LOGGER.error("createFromConfig: Blocking player: ['{}'/{}] -- We don't want someone suffocating in a wall", name, uuid.toString());
			}

			PlayerManager.getInstance().resetState(profile);
			return;
		}

		//#if MC >= 1.21.10
		//$$ server.services().nameToIdCache().resolveOfflineUsers(server.isDedicatedServer() && server.usesAuthentication());
		//$$ fetchGameProfile(server, profile.id()).whenCompleteAsync((p, throwable) ->
		//$$ {
			//$$ if (throwable != null) { return; }
			//$$ GameProfile temp;
			//$$ if (p.name().isEmpty())
			//$$ {
				//$$ temp = profile;
			//$$ }
			//$$ else
			//$$ {
				//$$ temp = p;
			//$$ }
			//$$ createFromConfigPhase2(server, level, temp, state, pos, game);
		//$$ });
		//#elseif MC >= 1.20.2
		//$$ GameProfile tempProfile = profile;
		//$$ fetchGameProfile(profile.getName()).thenAccept(opt ->
		//$$ {
			//$$ GameProfile temp = tempProfile;
			//$$ if (opt.isPresent())
			//$$ {
				//$$ temp = opt.get();
			//$$ }
			//$$ createFromConfigPhase2(server, level, temp, state, pos, game);
		//$$ });
		//#else
		if (profile.getProperties().containsKey("textures"))
		{
			AtomicReference<GameProfile> result = new AtomicReference<>();
			SkullBlockEntity.updateGameprofile(profile, result::set);
			profile = result.get();
		}

		createFromConfigPhase2(server, level, profile, state, pos, game);
		//#endif
	}

	private static SafkServerPlayer createFromConfigPhase2(MinecraftServer server, ServerLevel level, GameProfile profile,
	                                                            SafkState state, PosState pos, GameState game)
	{
		GameType gameType = GameType.byName(game.gameMode(), GameType.DEFAULT_MODE);
		PlayerList pl = server.getPlayerList();
		final String name = ProfileWrap.name(profile);

		//#if MC >= 1.20.2
		//$$ SafkServerPlayer shadow = new SafkServerPlayer(server, level, profile, ClientInformation.createDefault());
		//#elseif MC >= 1.19.3
		//$$ SafkServerPlayer shadow = new SafkServerPlayer(server, level, profile);
		//#else
		SafkServerPlayer shadow = new SafkServerPlayer(server, level, profile, null);
		//#endif

		//#if MC >= 1.21.10
		//$$ shadow.startingPosition = () -> shadow.snapTo(pos.x(), pos.y(), pos.z(), pos.yaw(), pos.pitch());
		//#else
		shadow.startingPosition = () -> shadow.moveTo(pos.x(), pos.y(), pos.z(), pos.yaw(), pos.pitch());
		//#endif

		if (ConfigWrap.mess().hideSafkJoin)
		{
			PlayerEventsHandler.getInstance().addShouldHideJoin(name);
		}

		//#if MC >= 1.20.6
		//$$ pl.placeNewPlayer(new SafkConnection(PacketFlow.SERVERBOUND), shadow, new CommonListenerCookie(profile, 0, ClientInformation.createDefault(), true));
		//#elseif MC >= 1.20.2
		//$$ pl.placeNewPlayer(new SafkConnection(PacketFlow.SERVERBOUND), shadow, new CommonListenerCookie(profile, 0, ClientInformation.createDefault()));
		//#else
		pl.placeNewPlayer(new SafkConnection(PacketFlow.SERVERBOUND), shadow);
		//#endif

		//#if MC >= 1.21.10
		//$$ loadPlayerNbt(shadow);
		//#endif
		shadow.setHealth(20.0f);
		shadow.connection.teleport(pos.x(), pos.y(), pos.z(), pos.yaw(), pos.pitch());
		shadow.gameMode.changeGameModeForPlayer(gameType);
		shadow.unsetRemoved();
		//#if MC >= 1.20.6
		//$$ shadow.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6F);
		//#elseif MC >= 1.19.4
		//$$ shadow.setMaxUpStep(0.6F);
		//#else
		shadow.maxUpStep = 0.6f;
		//#endif
		shadow.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f);

		if (gameType.isSurvival())
		{
			// Survival players shouldn't be able to fly, or be invulnerable.
			shadow.getAbilities().flying = false;
			shadow.setInvulnerable(false);
		}
		else
		{
			shadow.getAbilities().flying = game.flying();
		}

		shadow.time = state.time();
		shadow.timeout = state.timeout();
		shadow.reason = state.reason();
		shadow.freshPlayer = true;
		shadow.freshHoldTime = System.currentTimeMillis();
		shadow.startTime = state.startTime() <= 0 ? shadow.freshHoldTime : state.startTime();

		if (shadow.getStartTime() != state.startTime())
		{
			state = new SafkState(state.status(), state.time(), state.timeout(), shadow.getStartTime(), state.reason());
		}

		PlayerManager.getInstance().setState(profile, state);
		SafkEntry entry = SafkEntryList.getInstance().add(shadow, state);

		if (entry != null)
		{
			entry.handler().registerSaveAfk(shadow, state);
			entry.setPlayer(shadow);
		}

		SaveAfk.debugLog("createFromConfigPhase2: player: ['{}'/{}], state: [{}]", ProfileWrap.name(profile), ProfileWrap.id(profile), state.toString());
		shadow.isValid = true;

		return shadow;
	}

	@Nullable
	public static SafkServerPlayer createFromPlayer(MinecraftServer server, ServerPlayer player, int time, String reason)
	{
		if (time <= 0)
		{
			time = 129600;      // Hard coded in case of stupid
		}

		final long timeout = (time * 60L) * 1000L;
		final String name = player.getName().getString();
		final String duration = ConfigWrap.mess().duration.option.format(timeout);
		final String kickStr = ConfigWrap.mess().safkKickMessage
				+ (ConfigWrap.mess().displayDuration
				   ? ConfigWrap.mess().whenSafkDurationPrefix + duration
				   : "");
		Component kickMsg = InitWrap.text().formatText(kickStr);
		PlayerList pl = server.getPlayerList();

		if (kickMsg == null || kickMsg.toString().isEmpty())
		{
			kickMsg = Component.translatable("multiplayer.disconnect.duplicate_login");
		}

		if (ConfigWrap.mess().hideSafkJoin)
		{
			PlayerEventsHandler.getInstance().addShouldHideJoin(name);
		}

		pl.remove(player);
		player.connection.disconnect(kickMsg);

		//#if MC >= 1.20.1
		//$$ ServerLevel level = player.serverLevel();
		//#else
		ServerLevel level = player.getLevel();
		//#endif
		GameProfile profile = player.getGameProfile();
		//#if MC >= 1.20.2
		//$$ SafkServerPlayer shadow = new SafkServerPlayer(server, level, profile, player.clientInformation());
		//#elseif MC >= 1.19.3
		//$$ SafkServerPlayer shadow = new SafkServerPlayer(server, level, profile);
		//#else
		SafkServerPlayer shadow = new SafkServerPlayer(server, level, profile, player.getProfilePublicKey());
		//#endif

		if (!SafkPlayerUtils.ensureSafeForUUID(server, player.getUUID()))
		{
			return null;
		}

		//#if MC >= 1.19.3
		//$$ shadow.setChatSession(player.getChatSession());
		//#endif

		//#if MC >= 1.20.6
		//$$ pl.placeNewPlayer(new SafkConnection(PacketFlow.SERVERBOUND), shadow, new CommonListenerCookie(profile, 0, player.clientInformation(), true));
		//#elseif MC >= 1.20.2
		//$$ pl.placeNewPlayer(new SafkConnection(PacketFlow.SERVERBOUND), shadow, new CommonListenerCookie(profile, 0, player.clientInformation()));
		//#else
		pl.placeNewPlayer(new SafkConnection(PacketFlow.SERVERBOUND), shadow);
		//#endif

		//#if MC >= 1.21.10
		//$$ loadPlayerNbt(shadow);
		//#endif
		shadow.setHealth(player.getHealth());
		shadow.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		shadow.gameMode.changeGameModeForPlayer(player.gameMode.getGameModeForPlayer());
		//#if MC >= 1.20.6
		//$$ shadow.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6F);
		//#elseif MC >= 1.19.4
		//$$ shadow.setMaxUpStep(0.6F);
		//#else
		shadow.maxUpStep = 0.6f;
		//#endif
		shadow.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, player.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));

		if (shadow.gameMode.isSurvival())
		{
			// Survival players shouldn't be able to fly, or be invulnerable.
			shadow.getAbilities().flying = false;
			shadow.setInvulnerable(false);
		}
		else
		{
			shadow.getAbilities().flying = player.getAbilities().flying;
		}

		shadow.time = time;
		shadow.timeout = timeout;
		shadow.reason = reason;
		shadow.freshPlayer = true;
		shadow.freshHoldTime = System.currentTimeMillis();
		shadow.startTime = shadow.freshHoldTime;

		SafkState state = new SafkState(SafkStatus.ACTIVE, time, shadow.timeout, shadow.startTime, reason);
		PlayerManager.getInstance().setState(profile, state);
		SafkEntry entry = SafkEntryList.getInstance().add(shadow, state);

		if (entry != null)
		{
			entry.handler().registerSaveAfk(shadow, state);
			entry.setPlayer(shadow);
		}

		SaveAfk.debugLog("createFromPlayer: player: ['{}'/{}], state: [{}]", ProfileWrap.name(profile), ProfileWrap.id(profile), state.toString());
		shadow.isValid = true;

		return shadow;
	}

	//#if MC >= 1.21.10
	//$$ private final static Logger DUMB_LOGGER = LoggerFactory.getLogger(Reference.MOD_ID);
	//$$ private static void loadPlayerNbt(SafkServerPlayer player)
	//$$ {
		//$$ try (ProblemReporter.ScopedCollector logger = new ProblemReporter.ScopedCollector(player.problemPath(), DUMB_LOGGER))
		//$$ {
			//$$ Optional<ValueInput> opt = player.level()
				//$$ .getServer().getPlayerList()
				//$$ .loadPlayerData(player.nameAndId())
				//$$ .map((nbt) ->
					//$$ TagValueInput.create(logger, player.registryAccess(), nbt)
				//$$ );
				//$$ opt.ifPresent((data) ->
				//$$ {
					//$$ player.load(data);
					//$$ player.loadAndSpawnEnderPearls(data);
					//$$ player.loadAndSpawnParentVehicle(data);
				//$$ });
			//$$ }
		//$$ }
	//#endif

	//#if MC >= 1.20.2
	//$$ public static SafkServerPlayer respawnSafk(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation ci)
	//$$ {
		//$$ SafkServerPlayer shadow = new SafkServerPlayer(server, level, profile, ci);
		//$$ SaveAfk.debugLog("respawnSafk: player: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
		//$$ shadow.isValid = true;
		//$$ return shadow;
	//$$ }
	//#elseif MC >= 1.19.3
	//$$ public static SafkServerPlayer respawnSafk(MinecraftServer server, ServerLevel level, GameProfile profile)
	//$$ {
		//$$ SafkServerPlayer shadow = new SafkServerPlayer(server, level, profile);
		//$$ SaveAfk.debugLog("respawnSafk: player: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
		//$$ shadow.isValid = true;
		//$$ return shadow;
	//$$ }
	//#else
	public static SafkServerPlayer respawnSafk(MinecraftServer server, ServerLevel level, GameProfile profile, @Nullable ProfilePublicKey profilePublicKey)
	{
		SafkServerPlayer shadow = new SafkServerPlayer(server, level, profile, profilePublicKey);
		SaveAfk.debugLog("respawnSafk: player: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
		shadow.isValid = true;
		return shadow;
	}
	//#endif

	private void createSafkPost(MinecraftServer server)
	{
		PlayerList pl = server.getPlayerList();
		GameProfile profile = this.getGameProfile();

		SaveAfk.debugLog("createSafkPost: player: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
		pl.broadcastAll(new ClientboundRotateHeadPacket(this, (byte) (this.yHeadRot * 256 / 360)),
				//#if MC >= 1.20.1
				//$$ this.serverLevel().dimension());
				//#else
                this.level.dimension());
				//#endif
		pl.broadcastAll(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, this));
//		pl.broadcastAll(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.UPDATE_GAME_MODE, this));
//		pl.broadcastAll(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.UPDATE_LATENCY, this));
		//#if MC >= 1.19.3
		//$$ if (!ConfigWrap.safk().safkHidePlayer)
		//$$ {
			//$$ pl.broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, this));
		//$$ }
		//#endif

		PlayerEventsHandler.getInstance().removeShouldHideJoin(this.getName().getString());
		SafkPlayerUtils.sendHidePlayerPacket(server, this);
	}

	public boolean isValid()
	{
		return this.isValid;
	}

	public int getTimer()
	{
		return this.time;
	}

	public long getTimeout()
	{
		return this.timeout;
	}

	public String getReason()
	{
		return this.reason;
	}

	public long getStartTime()
	{
		return this.startTime;
	}

	public void updateTimeOut(long timeout)
	{
		this.timeout = timeout;
	}

	public SafkState toState()
	{
		return new SafkState(this.isValid() ? SafkStatus.ACTIVE : SafkStatus.INTERRUPTED, this.getTimer(), this.getTimeout(), this.getStartTime(), this.getReason());
	}

	protected boolean fromState(SafkState state)
	{
		boolean dirty = false;

		if (this.isValid())
		{
			if (this.getTimer() != state.time() &&
				state.time() > 0)
			{
				this.time = state.time();
				dirty = true;
			}
			if (this.getTimeout() != state.timeout() &&
				state.timeout() > 0 &&
				state.timeout() < this.getTimeout())
			{
				this.timeout = state.timeout();
				dirty = true;
			}
			if (this.getStartTime() != state.startTime() &&
				state.startTime() > 0 &&
				state.startTime() < this.getStartTime())
			{
				this.startTime = state.startTime();
				dirty = true;
			}
			if (!Objects.equals(this.getReason(), state.reason()) &&
				!state.reason().isEmpty())
			{
				this.reason = state.reason();
				dirty = true;
			}

//			SaveAfk.debugLog("fromState: dirty: [{}]", dirty);
		}

		return dirty;
	}

	@Override
	public void onEquipItem(final @NonNull EquipmentSlot slot, final @NonNull ItemStack previous, final @NonNull ItemStack stack)
	{
		if (!this.isUsingItem())
		{
			super.onEquipItem(slot, previous, stack);
		}
	}

	@Override
	//#if MC >= 1.21.2
	//$$ public boolean hurtServer(@NonNull ServerLevel level, @NonNull DamageSource damageSource, float amount)
	//#else
	public boolean hurt(@NonNull DamageSource damageSource, float amount)
	//#endif
	{
		SafkEntry entry = SafkEntryList.getInstance().get(this);

		if (entry != null &&
			entry.status() == SafkStatus.ACTIVE &&
			ConfigWrap.safk().safkDisableDamage)
		{
			// If you want to be able to kill shadow bots;
			// then just disable this config.
			return false;
		}

		//#if MC >= 1.21.2
		//$$ return super.hurtServer(level, damageSource, amount);
		//#else
		return super.hurt(damageSource, amount);
		//#endif
	}

	@Override
	//#if MC >= 1.21.2
	//$$ public void kill(@NonNull ServerLevel level)
	//#else
	public void kill()
	//#endif
	{
		this.kill(BuiltinTextHandler.getInstance().formatTextSafe("Killed"));
	}

	public void kill(Component message)
	{
		this.dismount();
		this.killShadow(message);
		//#if MC >= 1.21.2
		//$$ if (message.getContents() instanceof TranslatableContents text && text.getKey().equals("multiplayer.disconnect.duplicate_login"))
		//$$ {
			//$$ this.connection.onDisconnect(new DisconnectionDetails(message));
		//$$ }
		//$$ else
		//$$ {
		//#endif
		//#if MC >= 1.21.8
			//$$ this.level().getServer().schedule(
				//$$ new TickTask(this.level().getServer().getTickCount(),
						//$$ () -> this.connection.onDisconnect(new DisconnectionDetails(message))
			//$$ ));
		//$$ }
		//#elseif MC >= 1.21.2
			//$$ this.server.schedule(
				//$$ new TickTask(this.server.getTickCount(),
						//$$ () -> this.connection.onDisconnect(new DisconnectionDetails(message))
			//$$ ));
		//$$ }
		//#else
		this.server.tell(
				new TickTask(this.server.getTickCount(),
				             () -> this.connection.disconnect(message)
				));
		//#endif
	}

	@Override
	public void tick()
	{
		//#if MC >= 1.21.8
		//$$ MinecraftServer server = this.level().getServer();
		//#else
		MinecraftServer server = this.getServer();
		//#endif

		if (server.getTickCount() % 10 == 0)
		{
			if (this.freshPlayer)
			{
				final long now = System.currentTimeMillis();

				// Delay sending the ADD_PLAYER packets;
				// ... because Mojang.
				if ((now - this.freshHoldTime) >= 200L)
				{
					this.createSafkPost(server);
					this.freshPlayer = false;
				}
			}

			// Remove invalid Shadows that are still ticking (Why?)
			if (!this.freshPlayer && !this.isValid())
			{
				final Component name = this.getName();
				final Component reason = InitWrap.text().formatTextSafe("Invalid");

				this.kill(reason);
				server.getPlayerList().remove(this);

				if (!ConfigWrap.mess().hideSafkJoin)
				{
					SafkPlayerUtils.sendLeaveMessage(server, name);
				}
			}

			this.tickSafk(server);
			this.connection.resetPosition();
			//#if MC >= 1.21.8
			//$$ this.level().getChunkSource().move(this);
			//#elseif MC >= 1.20.1
			//$$ this.serverLevel().getChunkSource().move(this);
			//#else
			this.getLevel().getChunkSource().move(this);
			//#endif
//			this.hasChangedDimension();
		}

		try
		{
			super.tick();
			this.doTick();
		}
		catch (NullPointerException ignored) {}
	}

	private void tickSafk(MinecraftServer server)
	{
		final long now = System.currentTimeMillis();

		if (this.lastTick < 0L)
		{
			this.lastTick = now;
		}

		final long tickDelta = now - this.lastTick;
		this.lastTick = now;

		SafkEntry entry = SafkEntryList.getInstance().get(this);

		if (entry != null)
		{
			if (entry.status() != SafkStatus.ACTIVE)
			{
				SafkState state = PlayerManager.getInstance().getState(this.getGameProfile());
				entry.updateState(state);
				entry.setTimeout(this.timeout);
			}
			else
			{
				this.timeout = entry.timeout();
			}

			PosState pos = PlayerManager.getInstance().getPos(this.uuid);
			BlockPos blockPos = this.blockPosition();

			if (blockPos.getX() != pos.x() || blockPos.getY() != pos.y() || blockPos.getZ() != pos.z())
			{
				PlayerManager.getInstance().updatePlayerData(this);
			}

			if (!entry.tickTimeout(tickDelta))
			{
				PlayerList pl = server.getPlayerList();
				String mess = ConfigWrap.mess().safkExpiredReason;

				if (mess == null || mess.isEmpty())
				{
					mess = "§eTimeout Expired§r";
				}

				final long delta = SafkPlayerUtils.getStartTimeDelta(this.getStartTime());
				final String name = this.getName().getString();

				this.reason = (ConfigWrap.mess().displayDuration
				               ? ConfigWrap.mess().safkSuccessfulPrefix
				                 + ConfigWrap.mess().duration.option.format(delta)
				                 + ConfigWrap.mess().safkSuccessfulSuffix
				               : ConfigWrap.mess().safkSuccessful)
						+ ConfigWrap.mess().safkSuccessfulPunctuation
						+ mess;

				SafkState newState = new SafkState(SafkStatus.EXPIRED, -1, -1L, -1L, this.getReason());
				entry.updateState(newState);
				SafkEntryList.getInstance().syncEntry(this, entry);
				PlayerManager.getInstance().setState(this.getGameProfile(), newState);
				SafkEntryList.getInstance().remove(this, false, SafkStatus.EXPIRED);
				this.expired = true;

				if (ConfigWrap.mess().hideSafkJoin)
				{
					PlayerEventsHandler.getInstance().addShouldHideJoin(name);
				}

				Component reason = InitWrap.text().formatTextSafe(mess);
				this.kill(reason);
				pl.remove(this);

				if (ConfigWrap.mess().hideSafkJoin)
				{
					PlayerEventsHandler.getInstance().removeShouldHideJoin(name);
				}
				//#if MC < 1.21.2
				//$$ else
				//$$ {
					//$$ SafkPlayerUtils.sendLeaveMessage(server, this.getName());
				//$$ }
				//#endif
			}
		}
	}

	@Override
	public void die(@NonNull DamageSource damageSource)
	{
		this.dismount();
		super.die(damageSource);

		if (ConfigWrap.safk().resetHealthUponDeath)
		{
			this.setHealth(20.0F);
			this.foodData = new FoodData();
		}
		else
		{
			this.killShadow(InitWrap.text().formatTextSafe("Player has Died"));
		}

		this.kill(this.getCombatTracker().getDeathMessage());
	}

	public void killShadow(Component message)
	{
		SafkEntry entry = SafkEntryList.getInstance().get(this);
		PlayerManager.getInstance().updatePlayerData(this);

		if (message.getString().equals(ConfigWrap.mess().safkReplaced) || this.expired)
		{
			this.isValid = false;
			return;
		}

		// Active meaning, they were killed unexpectedly.
		if (entry == null || entry.status() == SafkStatus.ACTIVE)
		{
			if (ServerEventsHandler.getInstance().isServerStopping())
			{
				return;
			}

			String mess = ConfigWrap.mess().safkTerminated;

			if (mess == null || mess.isEmpty())
			{
				mess = "§cAFK session terminated§r";
			}

			final long delta = SafkPlayerUtils.getStartTimeDelta(this.getStartTime());

			this.reason = ConfigWrap.mess().safkUnsuccessful
					+ (ConfigWrap.mess().displayDuration
					   ? ConfigWrap.mess().safkUnsuccessfulPrefix
					     + ConfigWrap.mess().duration.option.format(delta)
					   : "")
					+ ConfigWrap.mess().safkUnsuccessfulPunctuation
					+ mess;

			SafkState newState = new SafkState(SafkStatus.TERMINATED, -1, -1L, -1L, this.getReason());
			PlayerManager.getInstance().setState(this.getGameProfile(), newState);
			SafkEntryList.getInstance().remove(this, false, SafkStatus.TERMINATED);
		}

		this.isValid = false;
	}

	private void dismount()
	{
		if (this.getVehicle() != null)
		{
			if (this.getVehicle() instanceof Player)
			{
				this.stopRiding();
			}

			for (Entity entry : this.getVehicle().getPassengers())
			{
				if (entry instanceof Player)
				{
					entry.stopRiding();
				}
			}
		}
	}

//	@Override
//	public @NonNull String getIpAddress()
//	{
//		return "127.0.0.1";
//	}

//	//#if MC >= 1.20.1
//	//$$ @Override
//	//$$ protected void checkFallDamage(double y, boolean onGround, @NonNull BlockState state, @NonNull BlockPos pos)
//	//$$ {
//		//$$ this.doCheckFallDamage(0.0, y, 0.0, onGround);
//	//$$ }
//	//#endif

	@Override
	//#if MC >= 1.21.2
	//$$ public ServerPlayer teleport(@NonNull TeleportTransition transition)
	//$${
	//$$ super.teleport(transition);
	//#elseif MC >= 1.21.0
	//$$ public Entity changeDimension(@NonNull DimensionTransition transition)
	//$${
		//$$ super.changeDimension(transition);
	//#else
	public Entity changeDimension(@NonNull ServerLevel level)
	{
		super.changeDimension(level);
	//#endif

		// Handle freeing the End
		if (this.wonGame)
		{
			ServerboundClientCommandPacket packet = new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN);
			this.connection.handleClientCommand(packet);
		}

		if (this.connection.player.isChangingDimension())
		{
			this.connection.player.hasChangedDimension();
		}

		return this.connection.player;
	}
}
