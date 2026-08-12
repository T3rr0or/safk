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

package io.qxl.safk.impl.commands;

import java.util.concurrent.atomic.AtomicBoolean;

import io.qxl.safk.impl.SaveAfk;
import io.qxl.safk.impl.commands.server.AfkCommand;
import io.qxl.safk.impl.config.ConfigWrap;
import org.jetbrains.annotations.ApiStatus;

import net.fabricmc.loader.api.FabricLoader;

import io.qxl.safk.impl.commands.server.SafkAdminCommand;
import io.qxl.safk.impl.commands.server.SafkCommand;
import com.sakuraryoko.corelib.impl.commands.CommandManager;

@ApiStatus.Internal
public class CommandRegister
{
    public static void register()
    {
        CommandManager.getInstance().registerCommandHandler(new SafkAdminCommand());

        if (ConfigWrap.mainOpt().safkEnabled)
        {
            if (ConfigWrap.cmdOpt().enableSafkCommand)
            {
                CommandManager.getInstance().registerCommandHandler(new SafkCommand());
            }

            if (ConfigWrap.cmdOpt().enableAfkCommand && checkForAfkModConflicts())
            {
                SaveAfk.LOGGER.error("/afk command have been registered by another mod, but your config has this command enabled; so it has been disabled.");
                ConfigWrap.cmdOpt().enableAfkCommand = false;

                if (!ConfigWrap.cmdOpt().enableSafkCommand)
                {
                    SaveAfk.LOGGER.warn("Re-Enabling the disabled '/safk' command so that users can use this mod.");
                    ConfigWrap.cmdOpt().enableSafkCommand = true;
                    CommandManager.getInstance().registerCommandHandler(new SafkCommand());
                }

                return;
            }

            if (ConfigWrap.cmdOpt().enableAfkCommand)
            {
                CommandManager.getInstance().registerCommandHandler(new AfkCommand());
            }
        }
    }

    private static boolean checkForAfkModConflicts()
    {
        AtomicBoolean conflict = new AtomicBoolean(false);

        FabricLoader.getInstance().getAllMods().forEach(mod ->
                                                        {
                                                            final String modId = mod.getMetadata().getId();

                                                            switch (modId)
                                                            {
                                                                case "afkplus", "antilogout", "sessility", "autoafk" -> conflict.set(true);
                                                            }
                                                        });

        return conflict.get();
    }
}
