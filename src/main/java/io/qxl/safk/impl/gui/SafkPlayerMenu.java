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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

//#if MC >= 26.0
//$$ import net.minecraft.world.inventory.ContainerInput;
//#else
import net.minecraft.world.inventory.ClickType;
//#endif

import io.qxl.safk.api.state.SafkStatus;
import io.qxl.safk.impl.SaveAfk;
import io.qxl.safk.impl.player.safk.SafkEntry;
import io.qxl.safk.impl.player.safk.SafkEntryList;
import io.qxl.safk.impl.player.safk.SafkServerPlayer;
import io.qxl.safk.impl.player.wrap.ProfileWrap;
import io.qxl.safk.impl.text.LegacyText;

/**
 * Actions for one AFK player, reached by left-clicking a head on the list.
 *
 * Everything here acts on a bot that may vanish while the screen is open, so
 * each action re-reads the entry instead of trusting what was drawn.
 */
@ApiStatus.Internal
public class SafkPlayerMenu extends ChestMenu
{
	public static final int ROWS = 3;

	private static final int BUTTON_RIGHT = 1;
	private static final int SLOT_HEAD = 4;
	private static final int SLOT_GOTO = 11;
	private static final int SLOT_BRING = 13;
	private static final int SLOT_END = 15;
	private static final int SLOT_BACK = 22;

	private final SimpleContainer view;
	private final UUID target;

	public SafkPlayerMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull SimpleContainer view, @Nonnull UUID target)
	{
		super(MenuType.GENERIC_9x3, containerId, playerInventory, view, ROWS);
		this.view = view;
		this.target = target;
		this.redraw();
	}

	public static void open(@Nonnull ServerPlayer viewer, @Nonnull UUID target, @Nonnull String name)
	{
		SimpleContainer view = new SimpleContainer(ROWS * 9);

		viewer.openMenu(new SimpleMenuProvider(
				(id, inv, p) -> new SafkPlayerMenu(id, inv, view, target),
				LegacyText.parse("§8" + name)
		));
	}

	private @Nullable SafkEntry entry()
	{
		return SafkEntryList.getInstance().get(this.target);
	}

	private void redraw()
	{
		for (int slot = 0; slot < ROWS * 9; slot++)
		{
			this.view.setItem(slot, ItemStack.EMPTY);
		}

		SafkEntry entry = this.entry();

		if (entry == null)
		{
			this.view.setItem(SLOT_HEAD, SafkGuiItems.icon(Items.BARRIER,
			                                               LegacyText.parse("§cNo longer AFK"),
			                                               List.of(LegacyText.parse("§7This session ended"))));
			this.view.setItem(SLOT_BACK, SafkGuiItems.icon(Items.ARROW,
			                                               LegacyText.parse("§eBack to the list"),
			                                               List.of()));
			this.broadcastChanges();
			return;
		}

		this.view.setItem(SLOT_HEAD, this.headOf(entry));

		this.view.setItem(SLOT_GOTO, SafkGuiItems.icon(Items.ENDER_PEARL,
		                                               LegacyText.parse("§eTeleport to them"),
		                                               List.of(LegacyText.parse("§7Puts you where they are standing"))));

		this.view.setItem(SLOT_BRING, SafkGuiItems.icon(Items.ENDER_EYE,
		                                                LegacyText.parse("§eBring them here"),
		                                                List.of(LegacyText.parse("§7Moves the bot to you"),
		                                                        LegacyText.parse("§8Their farm stops working"))));

		this.view.setItem(SLOT_END, SafkGuiItems.icon(Items.BARRIER,
		                                              LegacyText.parse("§cEnd the AFK session"),
		                                              List.of(LegacyText.parse("§7Disconnects the bot"))));

		this.view.setItem(SLOT_BACK, SafkGuiItems.icon(Items.ARROW,
		                                               LegacyText.parse("§eBack to the list"),
		                                               List.of()));
		this.broadcastChanges();
	}

	private ItemStack headOf(@Nonnull SafkEntry entry)
	{
		GameProfile profile = entry.profile();
		SafkServerPlayer bot = entry.player();
		String name = profile != null ? ProfileWrap.name(profile) : entry.name().getString();
		List<Component> lore = new ArrayList<>();

		lore.add(LegacyText.parse("§7Status: §f" + entry.status().name()));
		lore.add(LegacyText.parse("§7AFK for: §a" + entry.durationFormatted()));
		lore.add(LegacyText.parse("§7Time left: §a" + entry.timeoutFormatted()));
		lore.add(LegacyText.parse("§7Since: §f" + entry.startTimeFormatted()));
		lore.add(LegacyText.parse("§7Reason: §f" + entry.reasonFormatted()));

		if (bot != null)
		{
			lore.add(Component.empty());
			lore.add(LegacyText.parse(String.format("§7At: §f%d %d %d", (int) bot.getX(), (int) bot.getY(), (int) bot.getZ())));
			//#if MC >= 1.20.1
			//$$ lore.add(LegacyText.parse("§7In: §f" + bot.serverLevel().dimension().location()));
			//#else
			lore.add(LegacyText.parse("§7In: §f" + bot.getLevel().dimension().location()));
			//#endif
		}

		return profile != null
			   ? SafkGuiItems.head(profile, LegacyText.parse("§e" + name), lore)
			   : SafkGuiItems.icon(Items.SKELETON_SKULL, LegacyText.parse("§e" + name), lore);
	}

	//#if MC >= 26.0
	//$$ @Override
	//$$ public void clicked(int slotId, int button, @Nonnull ContainerInput type, @Nonnull Player player)
	//$$ {
		//$$ this.onSlotClicked(slotId, player);
	//$$ }
	//#else
	@Override
	public void clicked(int slotId, int button, @Nonnull ClickType type, @Nonnull Player player)
	{
		this.onSlotClicked(slotId, player);
	}
	//#endif

	private void onSlotClicked(int slotId, @Nonnull Player viewer)
	{
		if (!(viewer instanceof ServerPlayer sp))
		{
			return;
		}

		if (slotId == SLOT_BACK)
		{
			SafkListMenu.open(sp);
			return;
		}

		SafkEntry entry = this.entry();

		if (entry == null)
		{
			this.redraw();
			return;
		}

		SafkServerPlayer bot = entry.player();
		String name = entry.name().getString();

		if (slotId == SLOT_GOTO && bot != null)
		{
			SafkTeleport.to(sp, bot);
			sp.sendSystemMessage(LegacyText.parse("§7Teleported to §e" + name + "§r"));
			SaveAfk.LOGGER.info("{} teleported to the AFK bot for {}", sp.getName().getString(), name);
		}
		else if (slotId == SLOT_BRING && bot != null)
		{
			SafkTeleport.to(bot, sp);
			sp.sendSystemMessage(LegacyText.parse("§7Brought §e" + name + "§7 to you§r"));
			SaveAfk.LOGGER.info("{} moved the AFK bot for {} to their position", sp.getName().getString(), name);
		}
		else if (slotId == SLOT_END)
		{
			SafkEntryList.getInstance().remove(this.target, false, SafkStatus.TERMINATED);
			sp.sendSystemMessage(LegacyText.parse("§7Ended the AFK session for §e" + name + "§r"));
			SaveAfk.LOGGER.info("{} ended the AFK session for {}", sp.getName().getString(), name);
			SafkListMenu.open(sp);
		}
	}

	@Override
	public ItemStack quickMoveStack(@Nonnull Player player, int slot)
	{
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(@Nonnull Player player)
	{
		return true;
	}
}
