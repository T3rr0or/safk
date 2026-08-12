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

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.qxl.safk.impl.player.interfaces.IWaypointManagerInvoker;
import io.qxl.safk.impl.player.safk.SafkPlayerUtils;

@Mixin(ServerWaypointManager.class)
public abstract class MixinServerWaypointManager implements IWaypointManagerInvoker
{
	@Shadow
	public abstract void removePlayer(ServerPlayer player);

	@Shadow
	public abstract void addPlayer(ServerPlayer player);

	@Inject(method = "addPlayer", at = @At("HEAD"))
	private void safk$onAddPlayerWaypoint(ServerPlayer player, CallbackInfo ci)
	{
		SafkPlayerUtils.onAddOrUpdateWaypoint((ServerWaypointManager) (Object) this, player);
	}

	@Inject(method = "updatePlayer", at = @At("HEAD"))
	private void safk$onUpdatePlayerWaypoint(ServerPlayer player, CallbackInfo ci)
	{
		SafkPlayerUtils.onAddOrUpdateWaypoint((ServerWaypointManager) (Object) this, player);
	}

	@Override
	public void safk$addPlayer(ServerPlayer player)
	{
		this.addPlayer(player);
	}

	@Override
	public void safk$removePlayer(ServerPlayer player)
	{
		this.removePlayer(player);
	}
}
