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

package io.qxl.safk.api;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;

import io.qxl.safk.api.state.SafkState;
import io.qxl.safk.api.state.SafkStatus;
import io.qxl.safk.impl.player.PlayerManager;
import io.qxl.safk.impl.player.safk.SafkEntry;
import io.qxl.safk.impl.player.safk.SafkEntryList;

/**
 * SaveAFK API
 */
public interface SafkAPI
{
	/**
	 * Return whether the player with this UUID is currently AFK.
	 * @param uuid The UUID of the Player
	 * @return True or False
	 */
	static boolean isSafk(@Nonnull UUID uuid)
	{
		Optional<SafkStatus> opt = getSafkStatus(uuid);
		return opt.map(s -> s
				          .equals(SafkStatus.ACTIVE)).orElse(false);
	}

	/**
	 * Return the {@link SafkStatus} of the Player specified by UUID
	 * @param uuid The UUID of the Player
	 * @return Optional of {@link SafkStatus}
	 */
	static Optional<SafkStatus> getSafkStatus(@Nonnull UUID uuid)
	{
		Optional<SafkState> opt = getSafkState(uuid);
		return opt.map(SafkState::status);
	}

	/**
	 * Return the {@link SafkState} of the Player specified by UUID
	 * @param uuid The UUID of the Player; whether or not they are AFK.
	 * @return Optional of {@link SafkState}
	 */
	static Optional<SafkState> getSafkState(@Nonnull UUID uuid)
	{
		Optional<SafkEntry> opt = Optional.ofNullable(SafkEntryList.getInstance().get(uuid));
		return opt.map(SafkEntry::toState)
		          .or(() -> Optional.ofNullable(PlayerManager.getInstance().getState(uuid)));
	}
}
