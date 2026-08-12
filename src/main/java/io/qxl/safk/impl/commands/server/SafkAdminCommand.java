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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import com.sakuraryoko.corelib.api.commands.IServerCommand;
import com.sakuraryoko.corelib.api.modinit.ModInitData;
import com.sakuraryoko.corelib.impl.config.ConfigManager;
import io.qxl.safk.impl.Reference;
import io.qxl.safk.impl.SaveAfk;
import io.qxl.safk.impl.commands.PermsWrap;
import io.qxl.safk.impl.config.ConfigWrap;
import io.qxl.safk.impl.config.SafkConfigHandler;
import io.qxl.safk.impl.config.data.options.PlayerOptions;
import io.qxl.safk.impl.events.PlayerEventsHandler;
import io.qxl.safk.impl.events.ServerEventsHandler;
import io.qxl.safk.impl.modinit.InitWrap;
import io.qxl.safk.impl.modinit.SafkInit;
import io.qxl.safk.impl.player.*;
import io.qxl.safk.api.state.SafkStatus;
import io.qxl.safk.impl.player.safk.*;
import io.qxl.safk.impl.player.wrap.ProfileWrap;
import io.qxl.safk.api.state.SafkState;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@ApiStatus.Internal
public class SafkAdminCommand implements IServerCommand
{
    public static final String COMMAND = "safk-admin";
    private static final String ADVANCED_OPTIONS_FIELD = "advancedAdminOptions";

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment)
    {
        dispatcher.register(
                literal(this.getName())
                        .requires(PermsWrap.check(this.getNode(), ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                        .executes(this::about)
                        .then(literal("save")
                                      .requires(PermsWrap.check(this.getNode()+".save", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                      .executes(this::save)
                        )
                        .then(literal("reload")
                                      .requires(PermsWrap.check(this.getNode()+".reload", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                      .executes(this::reload)
                        )
                        .then(literal("list")
                                      .requires(PermsWrap.check(this.getNode()+".list", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                      .executes(this::listSafkMap)
                                      .then(literal("players")
                                                    .requires(PermsWrap.checkAdv(this.getNode()+".list.players", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                                    .executes(this::listPlayerMap)
                                      )
                                      .then(literal("bots")
                                                    .requires(PermsWrap.checkAdv(this.getNode()+".list.bots", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                                    .executes(this::listSafkMap)
                                      )
                                      .then(literal("all")
                                                    .requires(PermsWrap.checkAdv(this.getNode()+".list.all", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                                    .executes(this::listAll)
                                      )
                        )
                        .then(literal("info")
                                      .requires(PermsWrap.checkAdv(this.getNode()+".info", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                      .executes(this::infoPlayer)
                                      .then(argument("player", EntityArgument.player())
                                                    .executes(ctx ->
                                                                      this.infoPlayer(ctx, EntityArgument.getPlayer(ctx, "player"))
                                                    )
                                      )
                        )
                        .then(literal("purge")
                                      .requires(PermsWrap.check(this.getNode()+".purge", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                      .executes(this::purgePlayers)
                        )
                        .then(literal("spawn")
                                      .requires(PermsWrap.check(this.getNode()+".spawn", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                      .then(argument("player", StringArgumentType.string())
                                                    .suggests(
                                                            (ctx, builder) ->
                                                                    SharedSuggestionProvider.suggest(
                                                                            PlayerManager.getInstance().getSpawnCommandSuggestions(ctx),
                                                                            builder,
                                                                            ProfileWrap::name,
                                                                            PlayerUtils::formatEntityTooltip
                                                                    )
                                                    )
                                                    .requires(PermsWrap.check(this.getNode()+".spawn", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                                    .executes(ctx ->
                                                              {
                                                                  String result = StringArgumentType.getString(ctx, "player");
                                                                  return this.createSafk(ctx, result, -1, "");
                                                              }
                                                    )
                                                    .then(argument("minutes", IntegerArgumentType.integer(1))
                                                                  .requires(PermsWrap.check(this.getNode()+".spawn", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                                                  .executes(ctx ->
                                                                            {
                                                                                String result = StringArgumentType.getString(ctx, "player");
                                                                                return this.createSafk(ctx, result, IntegerArgumentType.getInteger(ctx, "minutes"), "");
                                                                            }
                                                                  )
                                                                  .then(argument("reason", StringArgumentType.greedyString())
                                                                                .requires(PermsWrap.check(this.getNode()+".spawn", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                                                                .executes(ctx ->
                                                                                          {
                                                                                              String result = StringArgumentType.getString(ctx, "player");
                                                                                              return this.createSafk(ctx, result, IntegerArgumentType.getInteger(ctx, "minutes"), StringArgumentType.getString(ctx, "reason"));
                                                                                          }
                                                                                )
                                                                  )
                                                    )
                                      )
                        )
                        .then(literal("forget")
                                      .requires(PermsWrap.check(this.getNode()+".forget", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                      .then(argument("player", StringArgumentType.string())
                                                    .suggests(
                                                            (ctx, builder) ->
                                                                    SharedSuggestionProvider.suggest(
                                                                            ConfigWrap.players().stream().map(opt -> opt.name).toList(),
                                                                            builder
                                                                    )
                                                    )
                                                    .requires(PermsWrap.check(this.getNode()+".forget", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                                    .executes(ctx ->
                                                              this.forgetPlayer(ctx, StringArgumentType.getString(ctx, "player"))
                                                    )
                                      )
                        )
                        .then(literal("kick")
                                      .requires(PermsWrap.check(this.getNode()+".kick", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                      .then(argument("target", StringArgumentType.string())
                                                    .suggests(
                                                            (ctx, builder) ->
                                                                    SharedSuggestionProvider.suggest(
                                                                            PlayerManager.getInstance().getKickCommandSuggestions(ctx),
                                                                            builder,
                                                                            ProfileWrap::name,
                                                                            PlayerUtils::formatEntityTooltip
                                                                    )
                                                    )
                                                    .requires(PermsWrap.check(this.getNode()+".kick", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                                    .executes(ctx ->
                                                              {
                                                                  String result = StringArgumentType.getString(ctx, "target");
                                                                  return this.kickSafk(ctx, result);
                                                              }
                                                    )
                                      )
                        )
                        // Gated on permission alone; setConfig() applies the
                        // advancedAdminOptions rule, so the flag that turns
                        // advanced options off can still turn them back on.
                        .then(literal("set")
                                      .requires(PermsWrap.check(this.getNode()+".set", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                      .then(argument("config", StringArgumentType.string())
                                                    .suggests(
                                                            (ctx, builder) ->
                                                                    SharedSuggestionProvider.suggest(
                                                                            SafkConfigHandler.getInstance().configSuggestions(),
                                                                            builder,
                                                                            k -> k,
                                                                            Component::literal
                                                                    )
                                                    )
                                                    .requires(PermsWrap.check(this.getNode()+".set", ConfigWrap.cmdOpt().safkAdminCommandPermissions))
                                                    .then(argument("value", StringArgumentType.greedyString())
                                                                  .suggests((ctx, builder) ->
                                                                            {
                                                                                String configName = StringArgumentType.getString(ctx, "config");
                                                                                Pair<Field, Object> targetData = SafkConfigHandler.getInstance().getConfigInstanceByField(configName);

                                                                                if (targetData != null)
                                                                                {
                                                                                    Field targetField = targetData.getLeft();
                                                                                    Object targetInstance = targetData.getRight();

                                                                                    try
                                                                                    {
                                                                                        Class<?> type = targetField.getType();
                                                                                        Object currentValue = targetField.get(targetInstance);

                                                                                        if (currentValue != null)
                                                                                        {
                                                                                            // Illegal Character prevention.
                                                                                            if (type == String.class)
                                                                                            {
                                                                                                builder.suggest(currentValue.toString().replace('§', '&'));
                                                                                            }
                                                                                            else
                                                                                            {
                                                                                                builder.suggest(currentValue.toString());
                                                                                            }
                                                                                        }

                                                                                        if (type == boolean.class || type == Boolean.class)
                                                                                        {
                                                                                            builder.suggest("true");
                                                                                            builder.suggest("false");
                                                                                        }
                                                                                        else if (type.isEnum())
                                                                                        {
                                                                                            for (Object enumConstant : type.getEnumConstants())
                                                                                            {
                                                                                                builder.suggest(((Enum<?>) enumConstant).name());
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                    catch (Exception ignored) {}
                                                                                }

                                                                                return builder.buildFuture();
                                                                            })
                                                                  .executes(ctx ->
                                                                              {
                                                                                  String result = StringArgumentType.getString(ctx, "config");
                                                                                  return this.setConfig(ctx, result, StringArgumentType.getString(ctx, "value"));
                                                                              }
                                                                  )
                                                    )
                                      )
                        )
        );
    }

    @Override
    public String getName()
    {
        return COMMAND;
    }

    @Override
    public String getModId()
    {
        return Reference.MOD_ID;
    }

    private int about(CommandContext<CommandSourceStack> ctx)
    {
        List<Component> info = SafkInit.getInstance().getVanillaFormatted(ModInitData.ALL_INFO);
        MutableComponent text = Component.literal("");

        for (Component entry : info)
        {
            text.append(entry).append("\n");
        }

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> text, false);
        //#else
        ctx.getSource().sendSuccess(text, false);
        //#endif

        return 1;
    }

    private int save(CommandContext<CommandSourceStack> ctx)
    {
        PlayerManager.getInstance().flushToConfig(ctx.getSource().getServer());
        ConfigManager.getInstance().saveEach(SafkConfigHandler.getInstance());
        String user = ctx.getSource().getTextName();

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText("Saving config!"), false);
        //#else
        ctx.getSource().sendSuccess(InitWrap.text().formatText("Saving config!"), false);
        //#endif

        SaveAfk.LOGGER.info("{} has saved the configuration.", user);

        return 1;
    }

    private int reload(CommandContext<CommandSourceStack> ctx)
    {
        SafkConfigHandler.getInstance().toggleFromReloadCmd(true);
        ConfigManager.getInstance().reloadEach(SafkConfigHandler.getInstance());
        String user = ctx.getSource().getTextName();

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText("Reloaded config!"), false);
        //#else
        ctx.getSource().sendSuccess(InitWrap.text().formatText("Reloaded config!"), false);
        //#endif

        SaveAfk.LOGGER.info("{} has reloaded the configuration.", user);

        return 1;
    }

    private int listAll(CommandContext<CommandSourceStack> ctx)
    {
        if (!ConfigWrap.mainOpt().reducedListDebugInfo)
        {
            this.listPlayerMap(ctx);
        }

        this.listSafkMap(ctx);

        return 1;
    }

    private int listPlayerMap(CommandContext<CommandSourceStack> ctx)
    {
        if (ConfigWrap.mainOpt().reducedListDebugInfo)
        {
            final Component result = InitWrap.text().formatText("§dReduced debug info enabled; player listing disabled§r");

            //#if MC >= 1.20.1
            //$$ ctx.getSource().sendSuccess(() -> result, false);
            //#else
            ctx.getSource().sendSuccess(result, false);
            //#endif

            return 0;
        }

        ImmutableMap<UUID, PlayerEntry> playerMap = PlayerManager.getInstance().playerMapCopy();
        MutableComponent text = Component.literal("");
        int count = 0;

        text.append(
                InitWrap.text().formatText("§dPlayer Map:")
        );

        for (UUID key : playerMap.keySet())
        {
            PlayerEntry entry = playerMap.get(key);

            if (entry != null)
            {
                text.append(
                        InitWrap.text().formatText(
                                String.format("\n§9[Entry: %02d]", count)
                        )
                ).append(
                        entry.getDebugFormatted()
                );
            }

            count++;
        }

        text.append(
                String.format("\n§6(%d total)§r", count)
        ).append("\n");     // prefix for safk list

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> text, false);
        //#else
        ctx.getSource().sendSuccess(text, false);
        //#endif

        if (ctx.getSource().isPlayer() && ctx.getSource().getPlayer() instanceof ServerPlayer)
        {
            GameProfile profile = ctx.getSource().getPlayer().getGameProfile();
            SaveAfk.debugLog("listPlayerMap: by: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
        }
        else
        {
            SaveAfk.debugLog("listPlayerMap: by: [console/unknown]");
        }

        return 1;
    }

    private int listSafkMap(CommandContext<CommandSourceStack> ctx)
    {
        ImmutableMap<UUID, SafkEntry> map = SafkEntryList.getInstance().shadowMapCopy();
        MutableComponent text = Component.literal("");
        int count = 0;

        text.append(
                InitWrap.text().formatText("§dAFK Map:")
        );

        for (SafkEntry entry : map.values())
        {
            text.append(
                    InitWrap.text().formatText(
                            String.format("\n§9[Entry: %02d]", count)
                    )
            ).append(
                    entry.debugFormatted()
            );

            count++;
        }

        text.append(
                String.format("\n§6(%d total)§r", count)
        );

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> text, false);
        //#else
        ctx.getSource().sendSuccess(text, false);
        //#endif

        if (ctx.getSource().isPlayer() && ctx.getSource().getPlayer() instanceof ServerPlayer)
        {
            GameProfile profile = ctx.getSource().getPlayer().getGameProfile();
            SaveAfk.debugLog("listSafkMap: by: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
        }
        else
        {
            SaveAfk.debugLog("listSafkMap: by: [console/unknown]");
        }

        return 1;
    }

    private int infoPlayer(CommandContext<CommandSourceStack> ctx)
    {
        try
        {
            return this.infoPlayer(ctx, ctx.getSource().getPlayerOrException());
        }
        catch (CommandSyntaxException err)
        {
            SaveAfk.LOGGER.warn("CMD:infoPlayer: Syntax Error; {}", err.getLocalizedMessage());
            return 0;
        }
    }

    private int infoPlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer player)
    {
        MutableComponent text = Component.literal("");
        boolean sent = false;

        if (!ConfigWrap.mainOpt().reducedListDebugInfo)
        {
            text.append(
                    InitWrap.text().formatText("§9Player Info: ")
            ).append(
                    PlayerManager.getInstance().getDebugFormatted(player.getUUID())
            ).append("\n");

            sent = true;
        }

        if (SafkEntryList.getInstance().contains(player.getUUID()))
        {
            text.append(
                    InitWrap.text().formatText("§9AFK Info: ")
            ).append(
                    SafkEntryList.getInstance().getDebugFormatted(player.getUUID())
            );

            sent = true;
        }

        if (!sent)
        {
            text.append(
                    InitWrap.text().formatText("§6Player: '§r"+player.getName().getString()+"§6' is not AFK")
            );
        }

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> text, false);
        //#else
        ctx.getSource().sendSuccess(text, false);
        //#endif

        GameProfile profile = player.getGameProfile();

        if (ctx.getSource().isPlayer() && ctx.getSource().getPlayer() instanceof ServerPlayer)
        {
            GameProfile ctxProfile = ctx.getSource().getPlayer().getGameProfile();
            SaveAfk.debugLog("infoPlayer: by: ['{}'/{}] for player: ['{}'/{}]",
                                  ProfileWrap.name(ctxProfile), ProfileWrap.id(ctxProfile),
                                  ProfileWrap.name(profile), ProfileWrap.id(profile)
            );
        }
        else
        {
            SaveAfk.debugLog("infoPlayer: by: [console/unknown] for player: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
        }

        return 1;
    }

    /**
     * Drops a stored player record. Purge only resyncs against the live server,
     * so it leaves behind entries for players the server will never see again.
     */
    @ApiStatus.Internal
    private int forgetPlayer(CommandContext<CommandSourceStack> ctx, String name)
    {
        PlayerOptions target = ConfigWrap.players().stream()
                                         .filter(opt -> opt.name.equalsIgnoreCase(name))
                                         .findFirst()
                                         .orElse(null);
        String reply;

        if (target == null)
        {
            reply = "§cNo stored record for §e"+ name + "§r";
        }
        else if (SafkEntryList.getInstance().contains(target.uuid))
        {
            reply = "§e"+ target.name + "§c is AFK right now; kick them first§r";
        }
        else if (ctx.getSource().getServer().getPlayerList().getPlayer(target.uuid) != null)
        {
            // Forgetting someone who is online only makes the record come straight back.
            reply = "§e"+ target.name + "§c is still connected and has to leave first§r";
        }
        else
        {
            PlayerManager.getInstance().remove(target.uuid, true, SafkStatus.INACTIVE);
            ConfigManager.getInstance().saveEach(SafkConfigHandler.getInstance());
            reply = "§aForgot §e"+ target.name + "§r";
            SaveAfk.LOGGER.info("{} removed the stored record for {}", ctx.getSource().getTextName(), target.name);
        }

        final String finalReply = reply;

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText(finalReply), false);
        //#else
        ctx.getSource().sendSuccess(InitWrap.text().formatText(finalReply), false);
        //#endif

        return 1;
    }

    private int purgePlayers(CommandContext<CommandSourceStack> ctx)
    {
        ServerPlayer player = ctx.getSource().getPlayer();
        ImmutableMap<UUID, PlayerEntry> playerMap = PlayerManager.getInstance().playerMapCopy();
        ImmutableMap<UUID, SafkEntry> shadowMap = SafkEntryList.getInstance().shadowMapCopy();
        int count = 0;

//        PlayerManager.getInstance().flushToConfig(ctx.getSource().getServer());

        for (UUID uuid : playerMap.keySet())
        {
            if (player != null)
            {
                if (!uuid.equals(player.getUUID()))
                {
                    PlayerManager.getInstance().remove(uuid, true, SafkStatus.INTERRUPTED);
                    count++;
                }
            }
            else
            {
                // Via console command, probably.
                PlayerManager.getInstance().remove(uuid, true, SafkStatus.INTERRUPTED);
                count++;
            }
        }

        // Resync
        PlayerManager.getInstance().onServerResync(ctx.getSource().getServer(), playerMap, shadowMap);
        playerMap = PlayerManager.getInstance().playerMapCopy();
        shadowMap = SafkEntryList.getInstance().shadowMapCopy();
        String result = String.format("§ePurged: §c%d §eplayers, and then resynced §a%d §ecurrent players, with §6%d shadows§r", count, playerMap.size(), shadowMap.size());

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText(result), false);
        //#else
        ctx.getSource().sendSuccess(InitWrap.text().formatText(result), false);
        //#endif

        if (ctx.getSource().isPlayer() && ctx.getSource().getPlayer() instanceof ServerPlayer)
        {
            GameProfile profile = ctx.getSource().getPlayer().getGameProfile();
            SaveAfk.debugLog("purgePlayers: by: ['{}'/{}]", ProfileWrap.name(profile), ProfileWrap.id(profile));
        }
        else
        {
            SaveAfk.debugLog("purgePlayers: by: [console/unknown]");
        }

        return 1;
    }

    @ApiStatus.Internal
    private int createSafk(CommandContext<CommandSourceStack> ctx, String result, int time, String reason)
    {
        ImmutableList<GameProfile> list = PlayerManager.getInstance().getSpawnCommandSuggestions(ctx);
        boolean found = false;
        String reply = "";

        if (time < 0)
        {
            time = ConfigWrap.safk().defaultSafkTimeout;

            if (time < 0)
            {
                time = 129600;
            }
        }
        if (reason == null || reason.isEmpty())
        {
            reason = ConfigWrap.mess().defaultSafkReason;

            if (reason == null || reason.isEmpty())
            {
                reason = "§rnone";
            }
        }

        for (GameProfile entry : list)
        {
            if (ProfileWrap.name(entry).equals(result))
            {
                try
                {
                    PlayerOptions opts = ConfigWrap.players().stream()
                            .filter(opt -> opt.uuid.equals(ProfileWrap.id(entry)))
                                                   .findFirst()
                                                   .orElse(null);

                    if (opts == null)
                    {
                        reply = "§cNo stored record for §e"+ result + "§c to spawn from§r";
                    }
                    else
                    {
                        SaveAfk.debugLog("createSafk: Scheduling AFK player: ['{}'/{}]", opts.name, opts.uuid.toString());
                        reply = "§eScheduling AFK spawn for: §7"+ result + "§r";
                        opts.state = new SafkState(SafkStatus.ACTIVE, time, (time * 60L) * 1000L, System.currentTimeMillis(), reason);
                        PlayerManager.getInstance().setState(entry, opts.state);
                        PlayerManager.getInstance().flushToConfig(ctx.getSource().getServer());
                        ServerEventsHandler.getInstance().toggleSpawnSafe(false);
                        SafkPendingSpawns.INSTANCE.scheduleSpawn(opts);
                    }
                }
                catch (Exception e)
                {
                    reply = "§cException: "+ e.getLocalizedMessage() + "§r";
                }

                found = true;
                break;
            }
        }

        if (!found)
        {
            // Only offline players are offered, so an online name is a likely mistake
            // rather than an unknown player.
            reply = ctx.getSource().getServer().getPlayerList().getPlayerByName(result) != null
                    ? "§e"+ result + "§c is still connected and has to leave first§r"
                    : "§cNo matching player found§r";
        }

        final String finalReply = reply;

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText(finalReply), false);
        //#else
        ctx.getSource().sendSuccess(InitWrap.text().formatText(finalReply), false);
        //#endif

        if (ctx.getSource().isPlayer() && ctx.getSource().getPlayer() instanceof ServerPlayer)
        {
            GameProfile profile = ctx.getSource().getPlayer().getGameProfile();
            SaveAfk.debugLog("createSafk: by: ['{}'/{}] // result: {}", ProfileWrap.name(profile), ProfileWrap.id(profile), finalReply);
        }
        else
        {
            SaveAfk.debugLog("createSafk: by: [console/unknown] // result: {}", finalReply);
        }

        return 1;
    }

    @ApiStatus.Internal
    private int kickSafk(CommandContext<CommandSourceStack> ctx, String result)
    {
        ImmutableList<GameProfile> list = PlayerManager.getInstance().getKickCommandSuggestions(ctx);
        boolean found = false;
        String reply = "";

        for (GameProfile entry : list)
        {
            if (ProfileWrap.name(entry).equals(result))
            {
                try
                {
                    MinecraftServer server = ctx.getSource().getServer();
                    PlayerList playerList = server.getPlayerList();
                    List<ServerPlayer> players = playerList.getPlayers();

                    for (ServerPlayer player : players)
                    {
                        if (player.getUUID().equals(ProfileWrap.id(entry)) && player instanceof SafkServerPlayer sp)
                        {
                            SaveAfk.debugLog("kickSafk: Kicking AFK player: ['{}'/{}]", ProfileWrap.name(entry), ProfileWrap.id(entry).toString());
                            reply = " §7- Kicking AFK player: §e"+ ProfileWrap.name(entry) + "§r";

                            if (ConfigWrap.mess().hideSafkJoin)
                            {
                                PlayerEventsHandler.getInstance().addShouldHideJoin(result);
                            }

                            Component name = sp.getName();
                            Component message = Component.literal("Killed");
                            sp.kill(message);
                            playerList.remove(player);

                            if (ConfigWrap.mess().hideSafkJoin)
                            {
                                PlayerEventsHandler.getInstance().removeShouldHideJoin(name.getString());
                            }
                            //#if MC < 1.21.2
                            //$$ else
                            //$$ {
                                //$$ SafkPlayerUtils.sendLeaveMessage(server, name);
                            //$$ }
                            //#endif

                            break;
                        }
                    }
                }
                catch (Exception e)
                {
                    reply = "§cException: "+ e.getLocalizedMessage() + "§r";
                }

                found = true;
                break;
            }
        }

        if (!found)
        {
            reply = "§cNo matching AFK player found§r";
        }

        final String finalReply = reply;

        //#if MC >= 1.20.1
        //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText(finalReply), false);
        //#else
        ctx.getSource().sendSuccess(InitWrap.text().formatText(finalReply), false);
        //#endif

        if (ctx.getSource().isPlayer() && ctx.getSource().getPlayer() instanceof ServerPlayer)
        {
            GameProfile profile = ctx.getSource().getPlayer().getGameProfile();
            SaveAfk.debugLog("kickSafk: by: ['{}'/{}] // result: {}", ProfileWrap.name(profile), ProfileWrap.id(profile), finalReply);
        }
        else
        {
            SaveAfk.debugLog("kickSafk: by: [console/unknown] // result: {}", finalReply);
        }

        return 1;
    }

    @ApiStatus.Internal
    private int setConfig(CommandContext<CommandSourceStack> ctx, String config, String value)
    {
        Pair<Field, Object> target = SafkConfigHandler.getInstance().getConfigInstanceByField(config);
        String reply;

        if (!ConfigWrap.mainOpt().advancedAdminOptions && !config.equals(ADVANCED_OPTIONS_FIELD))
        {
            reply = "§cSet is off; enable §e"+ ADVANCED_OPTIONS_FIELD + "§c first§r";
            String finalReply = reply;

            //#if MC >= 1.20.1
            //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText(finalReply), false);
            //#else
            ctx.getSource().sendSuccess(InitWrap.text().formatText(finalReply), false);
            //#endif

            return 1;
        }

        if (target == null)
        {
            reply = "§cUnknown config: "+config+"§r";
            String finalReply = reply;

            //#if MC >= 1.20.1
            //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText(finalReply), false);
            //#else
            ctx.getSource().sendSuccess(InitWrap.text().formatText(finalReply), false);
            //#endif

            return 0;
        }

        Field targetField = target.getLeft();
        Object targetInstance = target.getRight();

        try
        {
            Class<?> fieldType = targetField.getType();
            Object parsedValue = null;

            if (fieldType == int.class || fieldType == Integer.class)
            {
                parsedValue = Integer.parseInt(value);
            }
            else if (fieldType == boolean.class || fieldType == Boolean.class)
            {
                if (value.equalsIgnoreCase("true"))
                {
                    parsedValue = true;
                }
                else if (value.equalsIgnoreCase("false"))
                {
                    parsedValue = false;
                }
                else
                {
                    reply = "§cInvalid boolean! Value must be 'true' or 'false'.§r";
                    String finalReply = reply;

                    //#if MC >= 1.20.1
                    //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText(finalReply), false);
                    //#else
                    ctx.getSource().sendSuccess(InitWrap.text().formatText(finalReply), false);
                    //#endif

                    return 0;
                }
            }
            else if (fieldType == long.class || fieldType == Long.class)
            {
                parsedValue = Long.parseLong(value);
            }
            else if (fieldType == float.class || fieldType == Float.class)
            {
                parsedValue = Float.parseFloat(value);
            }
            else if (fieldType == double.class || fieldType == Double.class)
            {
                parsedValue = Double.parseDouble(value);
            }
            else if (fieldType == String.class)
            {
                parsedValue = value.replace('&', '§');
            }
            else if (fieldType.isEnum())
            {
                boolean found = false;

                for (Object enumConst : fieldType.getEnumConstants())
                {
                    if (((Enum<?>) enumConst).name().equalsIgnoreCase(value))
                    {
                        parsedValue = enumConst;
                        found = true;
                        break;
                    }
                }

                if (!found)
                {
                    reply = "§cInvalid option! Valid options are: " + Arrays.toString(fieldType.getEnumConstants()) + "§r";
                    String finalReply = reply;

                    ctx.getSource().sendFailure(InitWrap.text().formatText(finalReply));
                    return 0;
                }
            }

            if (parsedValue != null)
            {
                targetField.set(targetInstance, parsedValue);
                reply = "§aConfig: '"+config+"' updated to "+value+".§r\n§cNOTE: Some settings may require a server restart.";
                String finalReply = reply;

                //#if MC >= 1.20.1
                //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText(finalReply), false);
                //#else
                ctx.getSource().sendSuccess(InitWrap.text().formatText(finalReply), false);
                //#endif

                this.save(ctx);
                this.reload(ctx);

                return 1;
            }
            else
            {
                reply = "§cUnsupported type for config: "+config+"§r";
                String finalReply = reply;

                //#if MC >= 1.20.1
                //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText(finalReply), false);
                //#else
                ctx.getSource().sendSuccess(InitWrap.text().formatText(finalReply), false);
                //#endif

                return 0;
            }
        }
        catch (NumberFormatException e)
        {
            reply = "§cInvalid number format for config: "+config+"§r";
            String finalReply = reply;

            //#if MC >= 1.20.1
            //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText(finalReply), false);
            //#else
            ctx.getSource().sendSuccess(InitWrap.text().formatText(finalReply), false);
            //#endif

            return 0;
        }
        catch (Exception e)
        {
            reply = "§cAn error occurred setting the config. Check logs.§r";
            SaveAfk.LOGGER.error("setConfig: Exception setting config '{}' to '{}'; {}", config, value, e.getLocalizedMessage());
            String finalReply = reply;

            //#if MC >= 1.20.1
            //$$ ctx.getSource().sendSuccess(() -> InitWrap.text().formatText(finalReply), false);
            //#else
            ctx.getSource().sendSuccess(InitWrap.text().formatText(finalReply), false);
            //#endif

            return 0;
        }
    }
}
