package com.blocktyper.magicdoors;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.recipes.IRecipe;
import static com.blocktyper.magicdoors.MagicDoorsPlugin.*;

import net.md_5.bungee.api.ChatColor;

public class MagicDoorsEquipCommand implements CommandExecutor {

	public static String COMMAND_MD_EQUIP = "md-equip";

	private MagicDoorsPlugin plugin;

	public MagicDoorsEquipCommand(MagicDoorsPlugin plugin) {
		this.plugin = plugin;
		plugin.getCommand(COMMAND_MD_EQUIP).setExecutor(this);
		plugin.debugInfo("'/" + COMMAND_MD_EQUIP + "' registered");
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		try {
			if (!(sender instanceof Player)) {
				return false;
			}

			Player player = (Player) sender;

			if (!player.isOp())
				return false;

			Set<Material> distinctMaterials = getDistinctMatsForRecipes(doorKeyRecipe(),
					rootDoorCopyRecipe(), rootDoorCopyRecipe(),
					ownedRootDoorRecipe(), skeletonKeyRecipe());

			if (distinctMaterials == null || distinctMaterials.isEmpty()) {
				plugin.warning("distinctMaterials was null");
				player.sendMessage(
						ChatColor.RED + "Error running '" + COMMAND_MD_EQUIP + "'. No distinct magic door item found.");
				return false;
			}

			List<ItemStack> items = distinctMaterials.parallelStream().map(material -> getMaxItemStack(material))
					.collect(Collectors.toList());

			if (items == null || items.size() <= 0) {
				player.sendMessage(
						ChatColor.RED + "Error running '" + COMMAND_MD_EQUIP + "'. No magic door item found.");
				return false;
			}

			int rows = (items.size() / 9) + 1;
			Inventory magicBlocksInventory = Bukkit.createInventory(null, rows * 9, "(Magic Doors)");

			plugin.debugInfo("[md-equip] items size: " + items.size());

			int i = -1;
			for (ItemStack item : items) {
				if (item == null || item.getType().equals(Material.AIR))
					continue;

				i++;
				plugin.debugInfo("  -item[" + i + "]: " + item != null ? item.getType().name() : "null");
				magicBlocksInventory.setItem(i, item);
			}

			player.openInventory(magicBlocksInventory);

			return true;
		} catch (Exception e) {
			plugin.info("error running 'mb'equip':  " + e.getMessage());
			return false;
		}
	}

	private Set<Material> getDistinctMatsForRecipes(String... recipeNames) {
		Set<Material> distinctMaterials = null;
		if (recipeNames != null) {
			distinctMaterials = Arrays.asList(recipeNames).parallelStream()
					.map(recipeName -> getDistinctMaterialsFromRecipeFromName(recipeName))
					.flatMap(sets -> sets.stream()).collect(Collectors.toSet());
		}
		return distinctMaterials;
	}

	private Set<Material> getDistinctMaterialsFromRecipeFromName(String recipeKey) {
		IRecipe recipe = plugin.recipeRegistrar().getRecipeFromKey(recipeKey);
		return getDistinctMaterialsForRecipe(recipe);
	}

	private ItemStack getMaxItemStack(Material materiall) {
		int maxStackSize = materiall.getMaxStackSize();
		ItemStack itemStack = new ItemStack(materiall);
		itemStack.setAmount(maxStackSize);
		return itemStack;
	}

	private Set<Material> getDistinctMaterialsForRecipe(IRecipe recipe) {
		if (recipe == null)
			return null;

		if (recipe.getMaterialMatrix() == null || recipe.getMaterialMatrix().isEmpty()) {
			plugin.warning("Material matrix was null or empty for " + recipe.getName());
			return null;
		}

		Set<Material> materialSet = recipe.getMaterialMatrix().parallelStream().collect(Collectors.toSet());

		if (materialSet == null || materialSet.isEmpty()) {
			plugin.warning("materialSet was null or empty for " + recipe.getName());
			return null;
		}

		return materialSet;
	}
}
