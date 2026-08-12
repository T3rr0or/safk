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

package io.qxl.safk.impl;

import java.nio.file.Path;
import org.jetbrains.annotations.ApiStatus;

import net.fabricmc.loader.api.FabricLoader;

@ApiStatus.Internal
public class Reference
{
	public static final Path GAME_DIR = FabricLoader.getInstance().getGameDir();
	public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
	public static final String MOD_ID = "safk";
	public static final boolean DEBUG = false;

}
