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

package io.qxl.safk.impl.config.data;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.SerializedName;

import io.qxl.safk.impl.config.data.options.*;

import org.jetbrains.annotations.ApiStatus;

import com.sakuraryoko.corelib.api.config.IConfigData;

@ApiStatus.Internal
public class SafkConfigData implements IConfigData
{
    @SerializedName("___comment")
    public String comment = "SaveAFK Config";

    @SerializedName("config_date")
    public String config_date;

    @SerializedName("last_start")
    public Long last_start;

    @SerializedName("last_stop")
    public Long last_stop;

    @SerializedName("main")
    public MainOptions MAIN = new MainOptions();

    @SerializedName("commands")
    public CommandOptions COMMANDS = new CommandOptions();

    @SerializedName("safk")
    public SafkOptions SAFK = new SafkOptions();

    @SerializedName("messages")
    public MessageOptions MESS = new MessageOptions();

    @SerializedName("players")
    public List<PlayerOptions> PLAYERS = new ArrayList<>();

}
