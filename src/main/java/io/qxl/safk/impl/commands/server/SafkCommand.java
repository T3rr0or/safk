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

package io.qxl.safk.impl.commands.server;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//#if MC >= 1.21.10
//$$ import net.minecraft.server.players.NameAndId;
//#endif

import io.qxl.safk.impl.Reference;
import io.qxl.safk.impl.SaveAfk;
import io.qxl.safk.impl.commands.PermsWrap;
import io.qxl.safk.impl.commands.SafkLimits;
import io.qxl.safk.impl.config.ConfigWrap;
import io.qxl.safk.impl.modinit.InitWrap;
import io.qxl.safk.impl.player.safk.SafkServerPlayer;
import com.sakuraryoko.corelib.api.commands.IServerCommand;
import io.qxl.safk.impl.player.wrap.ProfileWrap;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@ApiStatus.Internal
public class SafkCommand implements IServerCommand
{
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment)
    {
        dispatcher.register(
                literal(this.getName())
                        .requires(PermsWrap.check(this.getNode(), ConfigWrap.cmdOpt().safkCommandPermissions))
                        .executes(ctx -> this.setSaveAfk(ctx, -1, ""))
                        .then(argument("minutes", IntegerArgumentType.integer(1))
                                      .requires(PermsWrap.check(this.getNode(), ConfigWrap.cmdOpt().safkCommandPermissions))
                                      .executes(ctx -> this.setSaveAfk(ctx, IntegerArgumentType.getInteger(ctx, "minutes"), ""))
                                      .then(argument("reason", StringArgumentType.greedyString())
                                                    .requires(PermsWrap.check(this.getNode(), ConfigWrap.cmdOpt().safkCommandPermissions))
                                                    .executes(ctx -> this.setSaveAfk(ctx, IntegerArgumentType.getInteger(ctx, "minutes"), StringArgumentType.getString(ctx, "reason")))
                                      )
                        )
        );
    }

    @Override
    public String getName()
    {
        return "safk";
    }

    @Override
    public String getModId()
    {
        return Reference.MOD_ID;
    }

    private int setSaveAfk(CommandContext<CommandSourceStack> context, int time, String reason)
    {
        CommandSourceStack src = context.getSource();
        if (src.getPlayer() == null) { return 0; }

        if (!ConfigWrap.mainOpt().safkEnabled)
        {
            String msg = "§c/"+this.getName()+" Command is not enabled§r";
            //#if MC >= 1.20.1
            //$$ context.getSource().sendSuccess(() -> InitWrap.text().formatTextSafe(msg), false);
            //#else
            context.getSource().sendSuccess(InitWrap.text().formatTextSafe(msg), false);
            //#endif
            return 1;
        }

        MinecraftServer server = src.getServer();
        ServerPlayer player = src.getPlayer();
        GameProfile profile = player.getGameProfile();

        //#if MC >= 1.21.10
        //$$ if (server.isSingleplayerOwner(new NameAndId(profile)))
        //#else
        if (server.isSingleplayerOwner(profile))
        //#endif
        {
            String msg = "§cCan't use /"+this.getName()+" as the single player server owner§r";
            //#if MC >= 1.20.1
            //$$ context.getSource().sendSuccess(() -> InitWrap.text().formatTextSafe(msg), false);
            //#else
            context.getSource().sendSuccess(InitWrap.text().formatTextSafe(msg), false);
            //#endif
            return 1;
        }

        if (time < 0)
        {
            time = ConfigWrap.safk().defaultSafkTimeout;

            if (time < 0)
            {
                time = 129600;
            }

            // The player asked for nothing in particular, so trim rather than refuse.
            time = SafkLimits.clampToMax(time, player);
        }
        else
        {
            String tooLong = SafkLimits.rejectTimeout(time, player);

            if (tooLong != null)
            {
                //#if MC >= 1.20.1
                //$$ context.getSource().sendSuccess(() -> InitWrap.text().formatTextSafe(tooLong), false);
                //#else
                context.getSource().sendSuccess(InitWrap.text().formatTextSafe(tooLong), false);
                //#endif
                return 1;
            }
        }

        String noRoom = SafkLimits.rejectConcurrent();

        if (noRoom != null)
        {
            //#if MC >= 1.20.1
            //$$ context.getSource().sendSuccess(() -> InitWrap.text().formatTextSafe(noRoom), false);
            //#else
            context.getSource().sendSuccess(InitWrap.text().formatTextSafe(noRoom), false);
            //#endif
            return 1;
        }

        if (reason == null || reason.isEmpty())
        {
            reason = ConfigWrap.mess().defaultSafkReason;

            if (reason == null || reason.isEmpty())
            {
                reason = "";
            }
        }

        if (SafkServerPlayer.createFromPlayer(server, player, time, reason) == null)
        {
            SaveAfk.LOGGER.error("Error creating AFK player from: {}", player.getName().getString());
            return 0;
        }

        SaveAfk.debugLog("setSaveAfk: player: ['{}'/{}] // T: {}m, R: '{}'", ProfileWrap.name(profile), ProfileWrap.id(profile), time, reason);
        return 1;
    }
}
