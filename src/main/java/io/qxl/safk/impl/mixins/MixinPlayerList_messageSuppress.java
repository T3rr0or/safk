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

import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.qxl.safk.impl.events.PlayerEventsHandler;
import io.qxl.safk.impl.player.safk.SafkPlayerUtils;

@Mixin(PlayerList.class)
@ApiStatus.Internal
public abstract class MixinPlayerList_messageSuppress
{
	@Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"),
	        cancellable = true)
	private void safk$hideSystemBroadcasts(Component message, boolean bypassHiddenChat, CallbackInfo ci)
	{
		final boolean hide = PlayerEventsHandler.getInstance().shouldHideJoin(message.getString());
//		SaveAfk.debugLog("[System] hide: {} // message: [{}]", hide, message.getString());

		if (hide && SafkPlayerUtils.matchesJoinPattern(message))
		{
			ci.cancel();
		}
	}
}
