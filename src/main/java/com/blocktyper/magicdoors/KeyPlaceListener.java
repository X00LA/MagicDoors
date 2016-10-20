package com.blocktyper.magicdoors;

import java.util.Random;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.plugin.BlockTyperPlugin;
import com.blocktyper.recipes.IRecipe;

/*
 * magic.doors.door=Magische Tür
magic.doors.key=Magie Türschlüssel
block.typer.loading.recipes=Laden Rezepte
 */

public class KeyPlaceListener implements Listener {

	private final BlockTyperPlugin plugin;

	Random random = new Random();

	private IRecipe doorKeyRecipe;

	public IRecipe getDoorKeyRecipe() {
		if (doorKeyRecipe == null) {
			doorKeyRecipe = getRecipeFromKey(MagicDoorsPlugin.RECIPE_NAME_DOOR_KEY);
		}
		return doorKeyRecipe;
	}

	private IRecipe getRecipeFromKey(String key) {
		String itemKey = plugin.config().getConfig().getString(key);

		plugin.debugInfo("loading recipe for " + key + ": '" + itemKey + "'");
		IRecipe recipe = plugin.recipeRegistrar().getRecipeFromKey(itemKey);
		if (recipe == null) {
			plugin.warning("recipe '" + itemKey + "' was not found");
		}
		return recipe;

	}

	public KeyPlaceListener() {
		plugin = BlockTyperPlugin.plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onBlockPlace(BlockPlaceEvent event) {
		try {
			plugin.debugInfo("BlockPlaceEvent - Material " + event.getBlock().getType().name());

			// if there is no root door recipe defined, do not continue
			if (getDoorKeyRecipe() == null) {
				plugin.debugWarning("No magic door key recipe.");
				return;
			}

			ItemStack itemInHand = event.getItemInHand();

			// if player is not holding a item, do not continue
			if (itemInHand == null) {
				plugin.debugWarning("Not holding an item");
				return;
			}

			// if player is not holding a magic door key, do not continue
			if (!itemInHand.getType().equals(getDoorKeyRecipe().getOutput())
					&& !itemInHand.getType().equals(getDoorKeyRecipe().getOutput())) {
				plugin.debugWarning("Not holding a magic door key");
				return;
			}

			// if the item does not have a display name, do not continue
			if (itemInHand.getItemMeta() == null || itemInHand.getItemMeta().getDisplayName() == null) {
				plugin.debugWarning("Not holding key with a name.");
				return;
			}

			String itemName = itemInHand.getItemMeta().getDisplayName();

			// if the item name does not equal the name of the current Root Door
			// or Root Door Copy, do not continue
			if (!itemName.equals(getDoorKeyRecipe().getName())) {
				plugin.debugWarning("Not holding door key with the magic door key name: '" + itemName + "' != '"
						+ getDoorKeyRecipe().getName() + "'");
				return;
			}

			event.setCancelled(true);

		} catch (Exception e) {
			plugin.warning("Unexpected error in 'RootDoorListener.onBlockPlace'. Message: " + e.getMessage());
		}

	}

}
