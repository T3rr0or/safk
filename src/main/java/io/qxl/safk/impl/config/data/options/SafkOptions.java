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

package io.qxl.safk.impl.config.data.options;

import com.sakuraryoko.corelib.api.config.IConfigOption;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SafkOptions implements IConfigOption
{
	public int defaultSafkTimeout;
	public boolean resetHealthUponDeath;
	public boolean safkDisableDamage;
	public boolean safkHidePlayer;
	public boolean safkHideFromOps;

	public SafkOptions()
	{
		this.defaults();
	}

	@Override
	public void defaults()
	{
		this.defaultSafkTimeout = 129600;
		this.resetHealthUponDeath = false;
		this.safkDisableDamage = false;
		this.safkHidePlayer = false;
		this.safkHideFromOps = false;
	}

	@Override
	public SafkOptions copy(IConfigOption opt)
	{
		SafkOptions opts = (SafkOptions) opt;

		this.defaultSafkTimeout = opts.defaultSafkTimeout;
		this.resetHealthUponDeath = opts.resetHealthUponDeath;
		this.safkDisableDamage = opts.safkDisableDamage;
		this.safkHidePlayer = opts.safkHidePlayer;
		this.safkHideFromOps = opts.safkHideFromOps;

		return this;
	}
}
