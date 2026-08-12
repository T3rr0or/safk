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

package io.qxl.safk.impl.mixins;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
//#if MC >= 1.21.8
//$$ import net.minecraft.server.MinecraftServer;
//#endif
//#if MC >= 1.20.2
//$$ import net.minecraft.server.level.ClientInformation;
//#else
import net.minecraft.world.entity.player.ProfilePublicKey;
//#endif
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.qxl.safk.impl.events.PlayerEventsHandler;
import io.qxl.safk.impl.player.safk.SafkPlayerUtils;

@Mixin(ServerPlayer.class)
@ApiStatus.Internal
public abstract class MixinServerPlayer_messageSuppress extends Player
{
	//#if MC >= 1.21.8
	//$$ public MixinServerPlayer_messageSuppress(MinecraftServer server, Level level, GameProfile gameProfile, ClientInformation ci)
	//$$ {
		//$$ super(level, gameProfile);
	//$$ }
	//#elseif MC >= 1.20.2
	//$$ public MixinServerPlayer_messageSuppress(Level level, BlockPos pos, float yRot, GameProfile gameProfile, ClientInformation ci)
	//$$ {
		//$$ super(level, pos, yRot, gameProfile);
	//$$ }
	//#elseif MC >= 1.19.3
	//$$ public MixinServerPlayer_messageSuppress(Level level, BlockPos pos, float yRot, GameProfile gameProfile)
	//$$ {
		//$$ super(level, pos, yRot, gameProfile);
	//$$ }
	//#else
	public MixinServerPlayer_messageSuppress(Level level, BlockPos pos, float yRot, GameProfile gameProfile, @Nullable ProfilePublicKey profilePublicKey)
	{
		super(level, pos, yRot, gameProfile, profilePublicKey);
	}
	//#endif

	@Inject(method = "sendSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), cancellable = true)
	private void safk$onSendSystemMessage(Component component, boolean bypassHiddenChat, CallbackInfo ci)
	{
		boolean hide = PlayerEventsHandler.getInstance().shouldHideJoin(component.getString());
//		SaveAfk.debugLog("onSendSystemMessage(): hide: [{}] // message: [{}]", hide, component.getString());

		if (hide && SafkPlayerUtils.matchesJoinPattern(component))
		{
			ci.cancel();
		}
	}
}
