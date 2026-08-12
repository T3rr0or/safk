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

import java.util.UUID;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import io.qxl.safk.api.SafkEvents;
import io.qxl.safk.impl.config.ConfigWrap;
import io.qxl.safk.impl.modinit.InitWrap;
import io.qxl.safk.impl.player.PlayerManager;
import io.qxl.safk.impl.player.interfaces.IPlayerInvoker;
import io.qxl.safk.api.state.SafkStatus;
import io.qxl.safk.api.state.SafkState;

@ApiStatus.Internal
public record SafkEntryHandler(SafkEntry entry)
{
    public SafkEntryHandler(@Nonnull SafkEntry entry)
    {
        this.entry = entry;
    }

    @ApiStatus.Internal
    public void registerSaveAfk(@Nonnull SafkServerPlayer player, SafkState state)
    {
        int time = state.time();
        String reason = state.reason();
        long shadowTimeout = -1L;

        if (time > 0)
        {
            // Time is represented in Minutes
            shadowTimeout = (time * 60L) * 1000L;
        }

        if ((reason == null && ConfigWrap.mess().defaultSafkReason == null) || (reason == null || reason.isEmpty()))
        {
            this.entry().setReason("");

            if (!ConfigWrap.safk().safkHidePlayer)
            {
                String mess1 = this.player() + ConfigWrap.mess().safkStarted;
                Component mess2 = InitWrap.text().formatTextSafe(mess1);
                this.sendMessage(mess2);
            }
        }
        else
        {
            this.entry().setReason(reason);

            if (!ConfigWrap.safk().safkHidePlayer)
            {
                String mess1 = this.player() + ConfigWrap.mess().safkStarted
                        + ConfigWrap.mess().safkPunctuation
                        + reason;
                Component mess2 = InitWrap.text().formatTextSafe(mess1);
                this.sendMessage(mess2);
            }
        }

        if (state.status() != SafkStatus.ACTIVE || shadowTimeout != state.timeout())
        {
            SafkState newState = new SafkState(SafkStatus.ACTIVE, time, shadowTimeout, state.startTime(), reason);

            if (!newState.equals(state))
            {
                this.entry().updateState(newState);
                PlayerManager.getInstance().setState(player.getGameProfile(), newState);
            }
        }

        SafkEvents.SAFK_START.invoker().onSafkEvent(player.getUUID(), state);
    }

    @ApiStatus.Internal
    public void unregisterSaveAfk(boolean silent, SafkStatus reason)
    {
        if (!ConfigWrap.safk().safkHidePlayer &&
//            !ConfigWrap.mess().hideSafkJoin &&
            !silent)
        {
            String retPrefix;

            if (reason == SafkStatus.EXPIRED)
            {
                retPrefix = this.player() + ConfigWrap.mess().whenSafkExpired;
            }
            else if (reason == SafkStatus.INTERRUPTED)
            {
                retPrefix = this.player() + ConfigWrap.mess().whenSafkInterrupted;
            }
            else if (reason == SafkStatus.TERMINATED)
            {
                retPrefix = this.player() + ConfigWrap.mess().whenSafkTerminated;
            }
            else
            {
                retPrefix = this.player() + ConfigWrap.mess().whenSafkReturned;
            }

            if (ConfigWrap.mess().displayDuration)
            {
                String ret = retPrefix
                        + ConfigWrap.mess().whenReturnDurationPrefix
                        + this.entry().durationString()
                        + ConfigWrap.mess().whenReturnDurationSuffix + "§r";

                Component mess = InitWrap.text().formatTextSafe(ret);
                this.sendMessage(mess);
            }
            else
            {
                Component mess = InitWrap.text().formatTextSafe(retPrefix);
                this.sendMessage(mess);
            }
        }

        final UUID uuid = this.entry().player() != null ? this.entry().player().getUUID() : null;
        SafkEvents.SAFK_END.invoker().onSafkEvent(uuid, new SafkState(reason, this.entry().timer(), this.entry().timeout(), this.entry().startTimeMs(), this.entry().reason()));
        this.entry().clearPlayer();
        this.entry().reset();
    }

    @ApiStatus.Internal
    private String player()
    {
        return ConfigWrap.mess().safkPlayerPrefix + this.entry().name().getString() + ConfigWrap.mess().safkPlayerSuffix;
    }

    @ApiStatus.Internal
    private void sendMessage(Component message)
    {
        if (!ConfigWrap.mess().broadcastMessages || message.getString().trim().isEmpty())
        {
            return;
        }

        this.invoker().safk$server().sendSystemMessage(message);     // Server Log

        for (ServerPlayer player : this.invoker().safk$server().getPlayerList().getPlayers())
        {
            player.sendSystemMessage(message);                          // Broadcast
        }
    }

    @ApiStatus.Internal
    private IPlayerInvoker invoker()
    {
        return (IPlayerInvoker) this.entry().player();
    }
}
