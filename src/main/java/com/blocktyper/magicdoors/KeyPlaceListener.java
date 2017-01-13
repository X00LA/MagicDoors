package com.blocktyper.magicdoors;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.recipes.IRecipe;

public class KeyPlaceListener implements Listener {

	private IRecipe doorKeyRecipe;

	public IRecipe getDoorKeyRecipe() {
		if (doorKeyRecipe == null) {
			doorKeyRecipe = getRecipeFromKey(MagicDoorsPlugin.RECIPE_NAME_DOOR_KEY);
		}
		return doorKeyRecipe;
	}

	private IRecipe getRecipeFromKey(String key) {
		String itemKey = MagicDoorsPlugin.getPlugin().config().getConfig().getString(key);

		MagicDoorsPlugin.getPlugin().debugInfo("loading recipe for " + key + ": '" + itemKey + "'");
		IRecipe recipe = MagicDoorsPlugin.getPlugin().recipeRegistrar().getRecipeFromKey(itemKey);
		if (recipe == null) {
			MagicDoorsPlugin.getPlugin().warning("recipe '" + itemKey + "' was not found");
		}
		return recipe;

	}

	public KeyPlaceListener() {

	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onBlockPlace(BlockPlaceEvent event) {
		try {
			MagicDoorsPlugin.getPlugin().debugInfo("BlockPlaceEvent - Material " + event.getBlock().getType().name());

			// if there is no root door recipe defined, do not continue
			if (getDoorKeyRecipe() == null) {
				MagicDoorsPlugin.getPlugin().debugWarning("No magic door key recipe.");
				return;
			}

			ItemStack itemInHand = event.getItemInHand();

			// if player is not holding a item, do not continue
			if (itemInHand == null) {
				MagicDoorsPlugin.getPlugin().debugWarning("Not holding an item");
				return;
			}

			// if player is not holding a magic door key, do not continue
			if (!itemInHand.getType().equals(getDoorKeyRecipe().getOutput())
					&& !itemInHand.getType().equals(getDoorKeyRecipe().getOutput())) {
				MagicDoorsPlugin.getPlugin().debugWarning("Not holding a magic door key");
				return;
			}

			// if the item does not have a display name, do not continue
			if (itemInHand.getItemMeta() == null || itemInHand.getItemMeta().getDisplayName() == null) {
				MagicDoorsPlugin.getPlugin().debugWarning("Not holding key with a name.");
				return;
			}

			String itemName = itemInHand.getItemMeta().getDisplayName();

			// if the item name does not equal the name of the current Root Door
			// or Root Door Copy, do not continue
			if (!itemName.equals(getDoorKeyRecipe().getName())) {
				MagicDoorsPlugin.getPlugin().debugWarning("Not holding door key with the magic door key name: '"
						+ itemName + "' != '" + getDoorKeyRecipe().getName() + "'");
				return;
			}

			event.setCancelled(true);

		} catch (Exception e) {
			MagicDoorsPlugin.getPlugin()
					.warning("Unexpected error in 'RootDoorListener.onBlockPlace'. Message: " + e.getMessage());
		}

	}

}
