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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import net.fabricmc.api.ModInitializer;

import io.qxl.safk.impl.config.ConfigWrap;
import io.qxl.safk.impl.modinit.InitWrap;
import io.qxl.safk.impl.modinit.SafkInit;
import com.sakuraryoko.corelib.impl.modinit.ModInitManager;

@ApiStatus.Internal
public class SaveAfk implements ModInitializer
{
	public static Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

	public static void debugLog(String key, Object... args)
	{
		if (InitWrap.debug())
		{
			LOGGER.info(String.format("[DEBUG] %s", key), args);
		}
	}

	@Override
	public void onInitialize()
	{
		ModInitManager.getInstance().registerModInitHandler(SafkInit.getInstance());
	}
}
