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

import java.util.List;

import io.qxl.safk.impl.config.data.options.*;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ConfigWrap
{
    public static Long lastStart()
    {
        return SafkConfigHandler.getInstance().getLastStart();
    }

    public static Long lastStop()
    {
        return SafkConfigHandler.getInstance().getLastStop();
    }

    public static MainOptions mainOpt()
    {
        return SafkConfigHandler.getInstance().getMainOptions();
    }

    public static CommandOptions cmdOpt()
    {
        return SafkConfigHandler.getInstance().getCommandOptions();
    }

    public static SafkOptions safk()
    {
        return SafkConfigHandler.getInstance().getSafkOptions();
    }

    public static MessageOptions mess()
    {
        return SafkConfigHandler.getInstance().getMessageOptions();
    }

    public static List<PlayerOptions> players()
    {
        return SafkConfigHandler.getInstance().getPlayerOptions();
    }
}
