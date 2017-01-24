package com.blocktyper.magicdoors;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.nbt.NBTItem;

public class KeyPlaceListener implements Listener {

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onBlockPlace(BlockPlaceEvent event) {
		try {
			ItemStack itemInHand = event.getItemInHand();

			// if player is not holding a item, do not continue
			if (itemInHand == null) {
				MagicDoorsPlugin.getPlugin().debugInfo("Not holding an item");
				return;
			}
			NBTItem nbtItem = new NBTItem(itemInHand);
			String nbtRecipeKey = nbtItem.getString(MagicDoorsPlugin.RECIPES_KEY);
			if (nbtRecipeKey == null || !nbtRecipeKey.equals(MagicDoorsPlugin.doorKeyRecipe())) {
				MagicDoorsPlugin.getPlugin().debugInfo("Not holding magic door key.'");
				return;
			}

			event.setCancelled(true);

		} catch (Exception e) {
			MagicDoorsPlugin.getPlugin()
					.warning("Unexpected error in 'RootDoorListener.onBlockPlace'. Message: " + e.getMessage());
		}

	}

}
