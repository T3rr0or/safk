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

package io.qxl.safk.impl.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;

import com.sakuraryoko.corelib.api.config.IConfigData;
import com.sakuraryoko.corelib.api.config.IConfigDispatch;
import com.sakuraryoko.corelib.api.time.TimeFormat;
import io.qxl.safk.impl.Reference;
import io.qxl.safk.impl.SaveAfk;
import io.qxl.safk.impl.config.data.SafkConfigData;
import io.qxl.safk.impl.config.data.options.*;
import io.qxl.safk.impl.events.ServerEventsHandler;
import io.qxl.safk.impl.modinit.SafkInit;
import io.qxl.safk.impl.player.PlayerManager;

@ApiStatus.Internal
public class SafkConfigHandler implements IConfigDispatch
{
    private static final SafkConfigHandler INSTANCE = new SafkConfigHandler();
    public static SafkConfigHandler getInstance() { return INSTANCE; }
    private SafkConfigData CONFIG = newConfig();
    private final String CONFIG_ROOT = ".";
    private final String CONFIG_NAME = Reference.MOD_ID;
    private boolean loaded = false;
    private boolean hideAllPlayers = false;
    private boolean unhideAllPlayers = false;
    private boolean fromReloadCmd = false;
    private boolean commandWarn = false;

    @Override
    public String getConfigRoot()
    {
        return this.CONFIG_ROOT;
    }

    @Override
    public boolean useRootDir()
    {
        return true;
    }

    @Override
    public String getConfigName()
    {
        return this.CONFIG_NAME;
    }

    @Override
    public SafkConfigData newConfig()
    {
        return new SafkConfigData();
    }

    @Override
    public SafkConfigData getConfig()
    {
        return CONFIG;
    }

    public MainOptions getMainOptions()
    {
        return CONFIG.MAIN;
    }

    public CommandOptions getCommandOptions()
    {
        return CONFIG.COMMANDS;
    }

    public SafkOptions getSafkOptions()
    {
        return CONFIG.SAFK;
    }

    public MessageOptions getMessageOptions()
    {
        return CONFIG.MESS;
    }

    public List<PlayerOptions> getPlayerOptions()
    {
        return CONFIG.PLAYERS;
    }

    @Override
    public boolean isLoaded()
    {
        return this.loaded;
    }

    @Override
    public void initConfig()
    {
        SaveAfk.debugLog("SafkConfigHandler#initConfig()");
    }

    @Override
    public void onPreLoadConfig()
    {
        this.loaded = false;
    }

    @Override
    public void onPostLoadConfig()
    {
        this.loaded = true;
    }

    @Override
    public void onPreSaveConfig()
    {
        this.loaded = false;
    }

    @Override
    public void onPostSaveConfig()
    {
        this.loaded = true;
    }

    @Override
    public SafkConfigData defaults()
    {
        SafkConfigData config = this.newConfig();
        SaveAfk.debugLog("SafkConfigHandler#defaults(): Setting default config.");

        // Set default values
        config.config_date = TimeFormat.RFC1123.formatNow(null);
        config.MAIN = new MainOptions();
        config.COMMANDS = new CommandOptions();
        config.SAFK = new SafkOptions();
        config.MESS = new MessageOptions();
        config.PLAYERS = new ArrayList<>();

        return config;
    }

    @Override
    public SafkConfigData update(IConfigData newConfig)
    {
        SafkConfigData newConf = (SafkConfigData) newConfig;
        SaveAfk.debugLog("SafkConfigHandler#update(): Refresh config.");

        // Refresh
        CONFIG.comment = SafkInit.getInstance().getModVersionString() + " Config";
        CONFIG.config_date = TimeFormat.RFC1123.formatNow(null);

        if (CONFIG.last_start == null || CONFIG.last_start < 1L)
        {
            CONFIG.last_start = System.currentTimeMillis();
        }

	    CONFIG.last_stop = Objects.requireNonNullElse(newConf.last_stop, -1L);

        if (CONFIG.last_stop < 1L)
        {
            // last_stop should never be < 1L (Or else thing break)
            CONFIG.last_stop = CONFIG.last_start - 60000L;     // 1 minute offset
        }

//        SaveAfk.debugLog("SafkConfigHandler#update(): save_date: {} --> {}", newConf.config_date, CONFIG.config_date);

        if (CONFIG.SAFK.safkHidePlayer && !newConf.SAFK.safkHidePlayer)
        {
            this.unhideAllPlayers = true;
        }
        else if (!CONFIG.SAFK.safkHidePlayer && newConf.SAFK.safkHidePlayer)
        {
            this.hideAllPlayers = true;
        }
        if (CONFIG.SAFK.safkHideFromOps && !newConf.SAFK.safkHideFromOps)
        {
            this.unhideAllPlayers = true;
        }
        else if (!CONFIG.SAFK.safkHideFromOps && newConf.SAFK.safkHideFromOps)
        {
            this.hideAllPlayers = true;
        }

        if (CONFIG.COMMANDS.enableAfkCommand && !newConf.COMMANDS.enableAfkCommand)
        {
            this.commandWarn = true;
        }
        if (CONFIG.COMMANDS.enableSafkCommand && !newConf.COMMANDS.enableSafkCommand)
        {
            this.commandWarn = true;
        }

        // Copy Incoming Config
        CONFIG.MAIN.copy(newConf.MAIN);
        CONFIG.COMMANDS.copy(newConf.COMMANDS);
        CONFIG.SAFK.copy(newConf.SAFK);
        CONFIG.MESS.copy(newConf.MESS);

        // Copy Players Config
        CONFIG.PLAYERS.clear();
        newConf.PLAYERS.forEach(
                player ->
                {
                    PlayerOptions newEntry = new PlayerOptions(player);

                    if (!newEntry.pos.equals(player.pos))
                    {
                        newEntry.pos = player.pos;
                    }
                    if (!newEntry.game.equals(player.game))
                    {
                        newEntry.game = player.game;
                    }

                    CONFIG.PLAYERS.add(newEntry);
                }
        );      // Deep copy

        return CONFIG;
    }

    @Override
    public void execute(boolean fromInit)
    {
        SaveAfk.debugLog("SafkConfigHandler#execute(): Execute config.");

        // Load data into Player Manager.
        PlayerManager.getInstance().resetFromConfig();

        CONFIG.PLAYERS.forEach(
                player ->
                        PlayerManager.getInstance().syncFromConfig(player)
        );

        if (this.unhideAllPlayers)
        {
            if (!fromInit && this.fromReloadCmd)
            {
                ServerEventsHandler.getInstance().toggleUnhideAllPlayers(true);
            }

            this.unhideAllPlayers = false;
        }
        if (this.hideAllPlayers)
        {
            if (!fromInit && this.fromReloadCmd)
            {
                ServerEventsHandler.getInstance().toggleHideAllPlayers(true);
            }

            this.hideAllPlayers = false;
        }

        this.toggleFromReloadCmd(false);

        if (this.commandWarn)
        {
            SaveAfk.LOGGER.warn("SafkConfigHandler#execute(): You need to restart the server to enable or disable commands.");
            this.commandWarn = false;
        }

        // Do this when the Config gets finalized.
//        SaveAfk.debugLog("SafkConfigHandler#execute(): new config_date: {}", CONFIG.config_date);
    }

    public void toggleFromReloadCmd(boolean toggle)
    {
        this.fromReloadCmd = toggle;
    }

    public void setStartTime()
    {
//        SaveAfk.debugLog("SafkConfigHandler#setStartTime()");
        this.CONFIG.last_start = System.currentTimeMillis();
    }

    public void setStopTime()
    {
//        SaveAfk.debugLog("SafkConfigHandler#setStopTime()");
        this.CONFIG.last_stop = System.currentTimeMillis();
    }

    public long getLastStart()
    {
        return this.CONFIG.last_start;
    }

    public long getLastStop()
    {
        return this.CONFIG.last_stop;
    }

    public ImmutableList<String> configSuggestions()
    {
        ImmutableList.Builder<String> builder = ImmutableList.builder();

	    Field[] mainFields = MainOptions.class.getDeclaredFields();
        Field[] cmdFields = CommandOptions.class.getDeclaredFields();
        Field[] msgFields = MessageOptions.class.getDeclaredFields();
        Field[] safkFields = SafkOptions.class.getDeclaredFields();

        for (Field field : mainFields)
        {
            builder.add(field.getName());
        }

        for (Field field : cmdFields)
        {
            builder.add(field.getName());
        }

        for (Field field : msgFields)
        {
            if (field.getType().getSimpleName().equals("DurationOption"))
            {
                builder.add(field.getName() + ".option");
                builder.add(field.getName() + ".customFormat");
            }
            else if (field.getType().getSimpleName().equals("TimeDateOption"))
            {
                builder.add(field.getName() + ".option");
                builder.add(field.getName() + ".customFormat");
            }
            else
            {
                builder.add(field.getName());
            }
        }

        for (Field field : safkFields)
        {
            builder.add(field.getName());
        }

        return builder.build();
    }

    public Pair<Field, Object> getConfigInstanceByField(String fieldName)
    {
        String parentName = fieldName;
        String childName = null;

        // Check if the user is trying to access a nested field (e.g., duration.customFormat)
        if (fieldName.contains("."))
        {
            String[] parts = fieldName.split("\\.", 2);
            parentName = parts[0];
            childName = parts[1];
        }

        Pair<Field, Object> parentData = null;

        try
        {
            parentData = Pair.of(MainOptions.class.getDeclaredField(parentName), this.CONFIG.MAIN);
        }
        catch (NoSuchFieldException ignored) {}

        if (parentData == null)
        {
            try
            {
                parentData = Pair.of(CommandOptions.class.getDeclaredField(parentName), this.CONFIG.COMMANDS);
            }
            catch (NoSuchFieldException ignored) {}
        }

        if (parentData == null)
        {
            try
            {
                parentData = Pair.of(SafkOptions.class.getDeclaredField(parentName), this.CONFIG.SAFK);
            }
            catch (NoSuchFieldException ignored) {}
        }

        if (parentData == null)
        {
            try
            {
                parentData = Pair.of(MessageOptions.class.getDeclaredField(parentName), this.CONFIG.MESS);
            }
            catch (NoSuchFieldException ignored) {}
        }

        if (parentData != null && childName != null)
        {
            try
            {
                Field parentField = parentData.getLeft();
                Object parentInstance = parentData.getRight();
                Object wrapperInstance = parentField.get(parentInstance);
                Field childField = parentField.getType().getDeclaredField(childName);

                return Pair.of(childField, wrapperInstance);
            }
            catch (Exception e)
            {
                return null;
            }
        }

        return parentData;
    }
}
