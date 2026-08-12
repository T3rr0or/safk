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
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.ApiStatus;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
//#if MC >= 26.0
//$$ import net.minecraft.world.inventory.ContainerInput;
//#else
import net.minecraft.world.inventory.ClickType;
//#endif
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import io.qxl.safk.api.state.SafkStatus;
import io.qxl.safk.impl.SaveAfk;
import io.qxl.safk.impl.player.safk.SafkEntry;
import io.qxl.safk.impl.player.safk.SafkEntryList;
import io.qxl.safk.impl.player.wrap.ProfileWrap;
import io.qxl.safk.impl.text.LegacyText;

/**
 * Read-only list of everyone currently AFK, drawn as a double chest.
 *
 * A vanilla client never reports scrolling to the server, so paging is the
 * only way to move through a list that outgrows the window. Five rows of heads
 * with a navigation row underneath means most servers never see page two.
 */
@ApiStatus.Internal
public class SafkListMenu extends ChestMenu
{
	public static final int ROWS = 6;
	public static final int PAGE_SIZE = 45;

	private static final int BUTTON_RIGHT = 1;
	private static final int SLOT_PREV = 45;
	private static final int SLOT_SORT = 49;
	private static final int SLOT_NEXT = 53;

	private final SimpleContainer view;
	private SortMode sort = SortMode.LONGEST;
	private int page;

	public SafkListMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull SimpleContainer view)
	{
		super(net.minecraft.world.inventory.MenuType.GENERIC_9x6, containerId, playerInventory, view, ROWS);
		this.view = view;
		this.redraw();
	}

	public static void open(@Nonnull ServerPlayer viewer)
	{
		SimpleContainer view = emptyView();

		viewer.openMenu(new SimpleMenuProvider(
				(id, inv, p) -> new SafkListMenu(id, inv, view),
				LegacyText.parse("§8AFK players")
		));
	}

	public static SimpleContainer emptyView()
	{
		return new SimpleContainer(ROWS * 9);
	}

	private List<SafkEntry> sorted()
	{
		ImmutableMap<java.util.UUID, SafkEntry> map = SafkEntryList.getInstance().shadowMapCopy();
		List<SafkEntry> entries = new ArrayList<>(map.values());

		entries.sort(this.sort.comparator());

		return entries;
	}

	private int pageCount(int total)
	{
		return Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
	}

	private void redraw()
	{
		List<SafkEntry> entries = this.sorted();
		int pages = this.pageCount(entries.size());

		this.page = Math.max(0, Math.min(this.page, pages - 1));

		for (int slot = 0; slot < ROWS * 9; slot++)
		{
			this.view.setItem(slot, ItemStack.EMPTY);
		}

		int first = this.page * PAGE_SIZE;

		for (int i = 0; i < PAGE_SIZE && first + i < entries.size(); i++)
		{
			this.view.setItem(i, this.render(entries.get(first + i)));
		}

		if (this.page > 0)
		{
			this.view.setItem(SLOT_PREV, SafkGuiItems.icon(Items.ARROW,
			                                               LegacyText.parse("§ePrevious page"),
			                                               List.of(LegacyText.parse("§7Page " + this.page + " of " + pages))));
		}

		if (this.page < pages - 1)
		{
			this.view.setItem(SLOT_NEXT, SafkGuiItems.icon(Items.ARROW,
			                                               LegacyText.parse("§eNext page"),
			                                               List.of(LegacyText.parse("§7Page " + (this.page + 2) + " of " + pages))));
		}

		this.view.setItem(SLOT_SORT, SafkGuiItems.icon(Items.COMPARATOR,
		                                               LegacyText.parse("§eSorted by " + this.sort.label()),
		                                               List.of(LegacyText.parse("§7" + entries.size() + " players AFK"),
		                                                       LegacyText.parse("§7Click to sort by " + this.sort.next().label()))));
		this.broadcastChanges();
	}

	private ItemStack render(@Nonnull SafkEntry entry)
	{
		GameProfile profile = entry.profile();
		Component name = LegacyText.parse("§e" + (profile != null ? ProfileWrap.name(profile) : entry.name().getString()));
		List<Component> lore = List.of(
				LegacyText.parse("§7AFK for: §a" + entry.durationFormatted()),
				LegacyText.parse("§7Time left: §a" + entry.timeoutFormatted()),
				LegacyText.parse("§7Reason: §f" + entry.reasonFormatted()),
				Component.empty(),
				LegacyText.parse("§8Left-click §7for options"),
				LegacyText.parse("§8Right-click §7to end this session")
		);

		if (profile == null)
		{
			return SafkGuiItems.icon(Items.SKELETON_SKULL, name, lore);
		}

		return SafkGuiItems.head(profile, name, lore);
	}

	/**
	 * Every slot is a button. Nothing here is a real inventory, so no click is
	 * ever allowed to move an item.
	 */
	//#if MC >= 26.0
	//$$ @Override
	//$$ public void clicked(int slotId, int button, @Nonnull ContainerInput type, @Nonnull Player player)
	//$$ {
		//$$ this.onSlotClicked(slotId, button, player);
	//$$ }
	//#else
	@Override
	public void clicked(int slotId, int button, @Nonnull ClickType type, @Nonnull Player player)
	{
		this.onSlotClicked(slotId, button, player);
	}
	//#endif

	private void onSlotClicked(int slotId, int button, @Nonnull Player viewer)
	{
		if (slotId == SLOT_PREV)
		{
			this.page--;
			this.redraw();
		}
		else if (slotId == SLOT_NEXT)
		{
			this.page++;
			this.redraw();
		}
		else if (slotId == SLOT_SORT)
		{
			this.sort = this.sort.next();
			this.page = 0;
			this.redraw();
		}
		else if (slotId >= 0 && slotId < PAGE_SIZE)
		{
			// Brigadier hands us the raw mouse button: 0 left, 1 right.
			if (button == BUTTON_RIGHT)
			{
				this.endSession(slotId, viewer);
			}
			else
			{
				this.openDetail(slotId, viewer);
			}
		}
	}

	private @Nullable SafkEntry entryAt(int slot)
	{
		List<SafkEntry> entries = this.sorted();
		int index = this.page * PAGE_SIZE + slot;

		return index < entries.size() ? entries.get(index) : null;
	}

	private void openDetail(int slot, @Nonnull Player viewer)
	{
		SafkEntry entry = this.entryAt(slot);

		if (entry == null || !(viewer instanceof ServerPlayer sp))
		{
			return;
		}

		GameProfile profile = entry.profile();

		if (profile == null)
		{
			return;
		}

		SafkPlayerMenu.open(sp, ProfileWrap.id(profile), ProfileWrap.name(profile));
	}

	/**
	 * Left click ends the clicked player's session. Nothing here is
	 * reversible, so it is gated on the same permission that opened the screen.
	 */
	private void endSession(int slot, @Nonnull Player viewer)
	{
		SafkEntry entry = this.entryAt(slot);

		if (entry == null)
		{
			return;
		}

		GameProfile profile = entry.profile();

		if (profile == null)
		{
			return;
		}

		String name = ProfileWrap.name(profile);

		SafkEntryList.getInstance().remove(ProfileWrap.id(profile), false, SafkStatus.TERMINATED);
		SaveAfk.LOGGER.info("{} ended the AFK session for {} from the list screen", viewer.getName().getString(), name);
		if (viewer instanceof ServerPlayer sp)
		{
			sp.sendSystemMessage(LegacyText.parse("§7Ended the AFK session for §e" + name + "§r"));
		}

		this.redraw();
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

	public enum SortMode
	{
		LONGEST,
		NAME;

		public SortMode next()
		{
			return this == LONGEST ? NAME : LONGEST;
		}

		public String label()
		{
			return this == LONGEST ? "longest AFK" : "name";
		}

		public Comparator<SafkEntry> comparator()
		{
			if (this == NAME)
			{
				return Comparator.comparing(e -> e.name().getString(), String.CASE_INSENSITIVE_ORDER);
			}

			// Earliest start time first, so the longest-running session leads.
			return Comparator.comparingLong(SafkEntry::startTimeMs);
		}
	}
}
