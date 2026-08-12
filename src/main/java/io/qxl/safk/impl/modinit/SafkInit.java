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

package io.qxl.safk.impl.modinit;

import org.jetbrains.annotations.ApiStatus;

import io.qxl.safk.impl.SaveAfk;
import io.qxl.safk.impl.Reference;
import io.qxl.safk.impl.commands.CommandRegister;
import io.qxl.safk.impl.compat.placeholder.SafkPlaceholders;
import io.qxl.safk.impl.config.SafkConfigHandler;
import io.qxl.safk.impl.config.ConfigWrap;
import io.qxl.safk.impl.events.PlayerEventsHandler;
import io.qxl.safk.impl.events.ServerEventsHandler;
import com.sakuraryoko.corelib.api.modinit.IModInitDispatcher;
import com.sakuraryoko.corelib.api.modinit.ModInitData;
import com.sakuraryoko.corelib.api.text.ITextHandler;
import com.sakuraryoko.corelib.impl.config.ConfigManager;
import com.sakuraryoko.corelib.impl.events.players.PlayerEventsManager;
import com.sakuraryoko.corelib.impl.events.server.ServerEventsManager;
import com.sakuraryoko.corelib.impl.text.BuiltinTextHandler;

@ApiStatus.Internal
public class SafkInit implements IModInitDispatcher
{
    private static final SafkInit INSTANCE = new SafkInit();
    public static SafkInit getInstance() { return INSTANCE; }

    private final ModInitData MOD_DATA;
    private boolean INIT = false;

    public SafkInit()
    {
        this.MOD_DATA = new ModInitData(Reference.MOD_ID);
        this.MOD_DATA.setTextHandler(this.getTextHandler());
    }

    @Override
    public ModInitData getModInit()
    {
        return this.MOD_DATA;
    }

    @Override
    public String getModId()
    {
        return Reference.MOD_ID;
    }

    @Override
    public ITextHandler getTextHandler()
    {
        return BuiltinTextHandler.getInstance();
    }

    @Override
    public boolean isDebug()
    {
        return Reference.DEBUG || ConfigWrap.mainOpt().debugMode;
    }

    @Override
    public boolean isInitComplete()
    {
        return this.INIT;
    }

    @Override
    public void reset()
    {
        // NO-OP
    }

    @Override
    public void onModInit()
    {
        SaveAfk.debugLog("Initializing Mod.");
        for (String s : this.getBasic(ModInitData.BASIC_INFO))
        {
            SaveAfk.LOGGER.info(s);
        }

        SaveAfk.debugLog("Config Initializing.");
        ConfigManager.getInstance().registerConfigDispatcher(SafkConfigHandler.getInstance());
        SaveAfk.debugLog("Registering commands.");
        CommandRegister.register();
        SaveAfk.debugLog("Registering Handlers.");

        ServerEventsManager.getInstance().registerEventDispatcher(ServerEventsHandler.getInstance());
        PlayerEventsManager.getInstance().registerPlayerEvents(PlayerEventsHandler.getInstance());
        SafkPlaceholders.register();

        SaveAfk.debugLog("All Tasks Done.");
        this.INIT = true;
    }
}
