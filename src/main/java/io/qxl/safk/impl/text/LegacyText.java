/*
 * This file is part of the SaveAFK project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  T3rr0or and contributors
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

package io.qxl.safk.impl.text;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Turns legacy section-sign codes into a styled component.
 *
 * CoreLib's formatters leave the section sign sitting in the text, which chat
 * tolerates but the player list does not: a name carrying a raw code renders
 * with the marker missing entirely.
 */
@ApiStatus.Internal
public class LegacyText
{
	public static final char CODE = '§';

	/**
	 * Drops the section-sign codes, leaving the words. Placeholders hand data to
	 * other mods, which do their own styling.
	 */
	public static String strip(@Nonnull String text)
	{
		StringBuilder out = new StringBuilder(text.length());

		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);

			if (c == CODE && i + 1 < text.length() && ChatFormatting.getByCode(text.charAt(i + 1)) != null)
			{
				i++;
				continue;
			}

			out.append(c);
		}

		return out.toString();
	}

	public static MutableComponent parse(@Nonnull String text)
	{
		MutableComponent result = Component.empty();
		StringBuilder pending = new StringBuilder();
		Style style = Style.EMPTY;

		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);

			if (c != CODE || i + 1 >= text.length())
			{
				pending.append(c);
				continue;
			}

			ChatFormatting format = ChatFormatting.getByCode(text.charAt(++i));

			if (format == null)
			{
				// Not a code after all, so keep both characters as written.
				pending.append(c).append(text.charAt(i));
				continue;
			}

			if (!pending.isEmpty())
			{
				result.append(Component.literal(pending.toString()).withStyle(style));
				pending.setLength(0);
			}

			style = format == ChatFormatting.RESET ? Style.EMPTY : style.applyFormat(format);
		}

		if (!pending.isEmpty())
		{
			result.append(Component.literal(pending.toString()).withStyle(style));
		}

		return result;
	}
}
