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

package io.qxl.safk.impl.gui;

import java.util.List;
import javax.annotation.Nonnull;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

//#if MC >= 1.20.5
//$$ import net.minecraft.core.component.DataComponents;
//$$ import net.minecraft.world.item.component.ItemLore;
//$$ import net.minecraft.world.item.component.ResolvableProfile;
//#else
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
//#endif

/**
 * Item building for the AFK list screen.
 *
 * Display names, lore and skull owners all moved from NBT to data components
 * in 1.20.5, so every version split this GUI needs lives in this one file.
 */
@ApiStatus.Internal
public class SafkGuiItems
{
	public static ItemStack head(@Nonnull GameProfile profile, @Nonnull Component name, @Nonnull List<Component> lore)
	{
		ItemStack stack = new ItemStack(Items.PLAYER_HEAD);

		//#if MC >= 1.21.10
		//$$ stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
		//#elseif MC >= 1.20.5
		//$$ stack.set(DataComponents.PROFILE, new ResolvableProfile(profile));
		//#else
		stack.getOrCreateTag().putString("SkullOwner", profile.getName());
		//#endif

		return label(stack, name, lore);
	}

	public static ItemStack icon(@Nonnull net.minecraft.world.item.Item item, @Nonnull Component name, @Nonnull List<Component> lore)
	{
		return label(new ItemStack(item), name, lore);
	}

	private static ItemStack label(@Nonnull ItemStack stack, @Nonnull Component name, @Nonnull List<Component> lore)
	{
		//#if MC >= 1.20.5
		//$$ stack.set(DataComponents.CUSTOM_NAME, name);
		//$$
		//$$ if (!lore.isEmpty())
		//$$ {
			//$$ stack.set(DataComponents.LORE, new ItemLore(lore));
		//$$ }
		//#else
		stack.setHoverName(name);

		if (!lore.isEmpty())
		{
			ListTag lines = new ListTag();

			for (Component line : lore)
			{
				lines.add(StringTag.valueOf(Component.Serializer.toJson(line)));
			}

			CompoundTag display = stack.getOrCreateTagElement("display");
			display.put("Lore", lines);
		}
		//#endif

		return stack;
	}
}
