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

import org.jetbrains.annotations.ApiStatus;

import com.sakuraryoko.corelib.api.config.IConfigOption;
import com.sakuraryoko.corelib.api.time.DurationFormat;
import com.sakuraryoko.corelib.api.time.DurationOption;
import com.sakuraryoko.corelib.api.time.TimeDateOption;
import com.sakuraryoko.corelib.api.time.TimeFormat;

@ApiStatus.Internal
public class MessageOptions implements IConfigOption
{
	public boolean broadcastMessages;
	public boolean hideSafkJoin;
	public boolean displayDuration;
	public boolean displayReturnFeedback;
	public String defaultSafkReason;
	public String safkPlayerPrefix;
	public String safkPlayerSuffix;
	public String safkKickMessage;
	public String safkExpiredReason;
	public String safkStarted;
	public String safkPunctuation;
	public String safkReplaced;
	public String safkTerminated;
	public String safkUnsuccessful;
	public String safkUnsuccessfulPrefix;
	public String safkUnsuccessfulPunctuation;
	public String safkSuccessful;
	public String safkSuccessfulPrefix;
	public String safkSuccessfulSuffix;
	public String safkSuccessfulPunctuation;
	public String whenSafkReturned;
	public String whenSafkExpired;
	public String whenSafkInterrupted;
	public String whenSafkTerminated;
	public String whenSafkDurationPrefix;
	public String whenSafkDurationSuffix;
	public String whenReturnDurationPrefix;
	public String whenReturnDurationSuffix;
	public DurationOption duration;
	public TimeDateOption timeDate;

	public MessageOptions()
	{
		this.defaults();
	}

	@Override
	public void defaults()
	{
		this.broadcastMessages = false;
		this.hideSafkJoin = false;
		this.displayDuration = false;
		this.displayReturnFeedback = false;
		this.defaultSafkReason = "";
		this.safkPlayerPrefix = "§e";
		this.safkPlayerSuffix = "§r";
		this.safkKickMessage = "§6Your player will be AFK§r";
		this.safkExpiredReason = "§eTimeout expired§r";
		this.safkStarted = " §eis now AFK§r";
		this.safkPunctuation = "§e,§r ";
		this.safkReplaced = "§6Replaced by player§r";
		this.safkTerminated = "§cAFK session terminated§r";
		this.safkUnsuccessful = "§eYour AFK session was interrupted§r";
		this.safkUnsuccessfulPrefix = " §eafter:§a ";
		this.safkUnsuccessfulPunctuation = "\n §7- For:§r ";
		this.safkSuccessful = "§eYour Session was successful.§r";
		this.safkSuccessfulPrefix = "§eYour §a";
		this.safkSuccessfulSuffix = " §eSession was successful.§r";
		this.safkSuccessfulPunctuation = "\n §7- For:§r ";
		this.whenSafkReturned = " §ehas returned§r";
		this.whenSafkExpired = " §eAFK session expired§r";
		this.whenSafkInterrupted = " §eAFK session interrupted§r";
		this.whenSafkTerminated = " §eAFK session terminated§r";
		this.whenSafkDurationPrefix = " §6for: §a";
		this.whenSafkDurationSuffix = "§7 minutes)";
		this.whenReturnDurationPrefix = " §7(Gone for: §a";
		this.whenReturnDurationSuffix = "§7)§r";
		this.duration = new DurationOption();
		this.duration.option = DurationFormat.PRETTY;
		this.timeDate = new TimeDateOption();
		this.timeDate.option = TimeFormat.RFC1123;
	}

	@Override
	public MessageOptions copy(IConfigOption opt)
	{
		MessageOptions opts = (MessageOptions) opt;

		this.broadcastMessages = opts.broadcastMessages;
		this.hideSafkJoin = opts.hideSafkJoin;
		this.displayDuration = opts.displayDuration;
		this.displayReturnFeedback = opts.displayReturnFeedback;
		this.defaultSafkReason = opts.defaultSafkReason;
		this.safkPlayerPrefix = opts.safkPlayerPrefix;
		this.safkPlayerSuffix = opts.safkPlayerSuffix;
		this.safkKickMessage = opts.safkKickMessage;
		this.safkExpiredReason = opts.safkExpiredReason;
		this.safkStarted = opts.safkStarted;
		this.safkPunctuation = opts.safkPunctuation;
		this.safkReplaced = opts.safkReplaced;
		this.safkTerminated = opts.safkTerminated;
		this.safkUnsuccessful = opts.safkUnsuccessful;
		this.safkUnsuccessfulPrefix = opts.safkUnsuccessfulPrefix;
		this.safkUnsuccessfulPunctuation = opts.safkUnsuccessfulPunctuation;
		this.safkSuccessful = opts.safkSuccessful;
		this.safkSuccessfulPrefix = !opts.safkSuccessfulPrefix.isEmpty() ? opts.safkSuccessfulPrefix : "§eYour §a";
		this.safkSuccessfulSuffix = !opts.safkSuccessfulSuffix.isEmpty() ? opts.safkSuccessfulSuffix : " §eSession was successful.§r";
		this.safkSuccessfulPunctuation = !opts.safkSuccessfulPunctuation.isEmpty() ? opts.safkSuccessfulPunctuation : "\n §7- For:§r ";
		this.whenSafkReturned = !opts.whenSafkReturned.isEmpty() ? opts.whenSafkReturned : " §ehas returned§r";
		this.whenSafkExpired = !opts.whenSafkExpired.isEmpty() ? opts.whenSafkExpired : " §eAFK session expired§r";
		this.whenSafkInterrupted = !opts.whenSafkInterrupted.isEmpty() ? opts.whenSafkInterrupted : " §eAFK session interrupted§r";
		this.whenSafkTerminated = !opts.whenSafkTerminated.isEmpty() ? opts.whenSafkTerminated : " §eAFK session terminated§r";
		this.whenSafkDurationPrefix = !opts.whenSafkDurationPrefix.isEmpty() ? opts.whenSafkDurationPrefix : " §6for: §a";
		this.whenSafkDurationSuffix = !opts.whenSafkDurationSuffix.isEmpty() ? opts.whenSafkDurationSuffix : "§7 minutes)";
		this.whenReturnDurationPrefix = !opts.whenReturnDurationPrefix.isEmpty() ? opts.whenReturnDurationPrefix : " §7(Gone for: §a";
		this.whenReturnDurationSuffix = !opts.whenReturnDurationSuffix.isEmpty() ? opts.whenReturnDurationSuffix : "§7)§r";
		this.duration.copy(opts.duration);
		this.timeDate.copy(opts.timeDate);

		return this;
	}
}
