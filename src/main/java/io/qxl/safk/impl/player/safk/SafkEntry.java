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

package io.qxl.safk.impl.player.safk;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import io.qxl.safk.impl.config.ConfigWrap;
import io.qxl.safk.impl.modinit.InitWrap;
import io.qxl.safk.impl.player.PlayerUtils;
import io.qxl.safk.api.state.SafkStatus;
import io.qxl.safk.impl.player.wrap.ProfileWrap;
import io.qxl.safk.api.state.SafkState;
import com.sakuraryoko.corelib.api.time.DurationFormat;
import com.sakuraryoko.corelib.api.time.TimeFormat;

@ApiStatus.Internal
public class SafkEntry
{
	private @Nullable SafkServerPlayer player;
	private final SafkEntryHandler handler;
	private SafkStatus status;
	private int time;
	private long startTimeMs;
	private long timeout;
	private String reason;

	@ApiStatus.Internal
	private SafkEntry()
	{
		this.player = null;
		this.status = SafkStatus.INACTIVE;
		this.time = 0;
		this.startTimeMs = -1L;
		this.timeout = -1L;
		this.reason = "";
		this.handler = new SafkEntryHandler(this);
	}

	@ApiStatus.Internal
	public static SafkEntry create(@Nonnull SafkServerPlayer player)
	{
		SafkEntry newEntry = new SafkEntry();
		newEntry.setPlayer(player);
		return newEntry;
	}

	@ApiStatus.Internal
	public SafkEntryHandler handler()
	{
		return this.handler;
	}

	@Nullable
	public SafkServerPlayer player()
	{
		return this.player;
	}

	public Component name()
	{
		return this.player != null
		       ? this.player.getName()
		       : Component.empty();
	}

	public @Nullable GameProfile profile()
	{
		return this.player != null
		       ? this.player.getGameProfile()
		       : null;
	}

	public SafkStatus status()
	{
		return this.status;
	}

	public int timer()
	{
		return this.time;
	}

	public long startTimeMs()
	{
		return this.startTimeMs;
	}

	public long timeout()
	{
		return this.timeout;
	}

	public String reason()
	{
		return this.reason;
	}

	public DurationFormat durationType()
	{
		DurationFormat format = DurationFormat.fromStringStatic(ConfigWrap.mess().duration.option.getName());

		if (format != null)
		{
			return format;
		}

		ConfigWrap.mess().duration.option = DurationFormat.PRETTY;
		return DurationFormat.REGULAR;
	}

	public TimeFormat timeDateType()
	{
		TimeFormat format = TimeFormat.fromStringStatic(ConfigWrap.mess().timeDate.option.getName());

		if (format != null)
		{
			return format;
		}

		ConfigWrap.mess().timeDate.option = TimeFormat.REGULAR;
		return TimeFormat.REGULAR;
	}

	public String startTimeString()
	{
		return this.timeDateType().formatTo(this.startTimeMs(), ConfigWrap.mess().duration.customFormat);
	}

	public String durationString()
	{
		return this.durationType().format((System.currentTimeMillis() - this.startTimeMs()), ConfigWrap.mess().duration.customFormat);
	}

    public String timeoutString()
    {
        return this.durationType().format((this.timeout), ConfigWrap.mess().duration.customFormat);
    }

	public String startTimeFormatted()
	{
		if (this.startTimeMs > 1L)
		{
			String result = this.startTimeString();
			return "§a" + result + "§r";
		}

		return "§eN/A§r";
	}

	public String durationFormatted()
	{
		if (this.startTimeMs() > 1L)
		{
			String result = this.durationString();
			return "§a" + result + "§r";
		}

		return "§eN/A§r";
	}

	public String timeoutFormatted()
	{
		if (this.timeout > 1L)
		{
			String result = this.timeoutString();
			return "§6" + result + "§r";
		}

		return "§eN/A§r";
	}

	public String reasonFormatted()
	{
		if (this.reason().isEmpty())
		{
			return "§e<>§r";
		}

		return "§e"+this.reason()+"§r";
	}

	@ApiStatus.Internal
	public void setPlayer(@Nonnull SafkServerPlayer player)
	{
		this.player = player;
	}

	@ApiStatus.Internal
	public void updateState(SafkState state)
	{
		this.status = state.status();
		this.setTimer(state.time());
		this.setTimeout(state.timeout());
		this.setReason(state.reason());

		final long startTime = state.startTime() > 0L ? state.startTime() : System.currentTimeMillis();

		if (this.startTimeMs() <= 1L)
		{
			this.setStartTimeMs(startTime);
		}
	}

	@ApiStatus.Internal
	public SafkState toState()
	{
		return new SafkState(this.status(), this.timer(), this.timeout(), this.startTimeMs(), this.reason());
	}

	@ApiStatus.Internal
	public void clearPlayer()
	{
		this.player = null;
	}

	@ApiStatus.Internal
	public void setTimer(int time)
	{
		this.time = time;
	}

	@ApiStatus.Internal
	public void setStartTimeMs(long time)
	{
		this.startTimeMs = time;
	}

	@ApiStatus.Internal
	public void setTimeout(long timeout)
    {
        this.timeout = Math.min(Math.max(timeout, 0L), Long.MAX_VALUE);
    }

	@ApiStatus.Internal
	public void setReason(String reason)
	{
		this.reason = reason;
	}

	@ApiStatus.Internal
    public boolean tickTimeout(final long tickDelta)
    {
        this.timeout -= tickDelta;
        return this.timeout > 0L;
    }

	public boolean matches(@Nonnull SafkServerPlayer player)
	{
		if (this.player == null) { return false; }
		return  this.player.getUUID().equals(player.getUUID()) ||
				this.player.equals(player);
	}

	public boolean matches(UUID uuid)
	{
		if (this.player == null) { return false; }
		return  this.player.getUUID().equals(uuid);
	}

	@VisibleForTesting
	public Component debugFormatted()
	{
		MutableComponent text = Component.literal("");

		if (this.player != null)
		{
			text.append(
					InitWrap.text().formatText("§r\n - §7Name: ")
			).append(
					PlayerUtils.formatSuggestKickCommand(this.name())
			);
		}
		else
		{
			text.append(
					InitWrap.text().formatText("§r\n - §cNO PLAYER")
			);
		}

		if (!ConfigWrap.mainOpt().reducedListDebugInfo)
		{
			text.append(
					InitWrap.text().formatText("§r\n - §7UUID: ")
			).append(
					InitWrap.text().formatText(
							this.profile() != null
							? ProfileWrap.id(Objects.requireNonNull(this.profile())).toString()
							: "§c<NULL>"
					)
			);
		}

		text.append(
				InitWrap.text().formatText("§r\n - §7Status: ")
		).append(
				InitWrap.text().formatText(SafkStatus.formatStatus(this.status()))
		);

		if (this.status() == SafkStatus.ACTIVE)
		{
			text.append(
					InitWrap.text().formatText("§r\n - §7Timeout: ")
			).append(
					InitWrap.text().formatText(this.timeoutFormatted())
			);

			if (!ConfigWrap.mainOpt().reducedListDebugInfo)
			{
				text.append(
						InitWrap.text().formatText("§r\n - §7Duration: ")
				).append(
						InitWrap.text().formatText(this.durationFormatted())
				).append(
						InitWrap.text().formatText("§r\n - §7Since: ")
				).append(
						InitWrap.text().formatText(this.startTimeFormatted())
				);
			}
			text.append(
					InitWrap.text().formatText("§r\n - §7Reason: ")
			).append(
					InitWrap.text().formatText(this.reasonFormatted())
			);
		}

		return text;
	}

	@ApiStatus.Internal
	public void reset()
	{
		this.status = SafkStatus.INACTIVE;
		this.time = 0;
		this.startTimeMs = -1L;
		this.timeout = -1L;
		this.reason = "";
	}
}
