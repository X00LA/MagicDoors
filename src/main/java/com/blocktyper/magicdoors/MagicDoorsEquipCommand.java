package com.blocktyper.magicdoors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.recipes.IRecipe;

import net.md_5.bungee.api.ChatColor;

public class MagicDoorsEquipCommand implements CommandExecutor{
	
	public static String COMMAND_MD_EQUIP = "md-equip";
	
	private MagicDoorsPlugin plugin;
	public MagicDoorsEquipCommand(MagicDoorsPlugin plugin){
		this.plugin = plugin;
		plugin.getCommand(COMMAND_MD_EQUIP).setExecutor(this);
		plugin.info("'/" + COMMAND_MD_EQUIP + "' registered");
	}
	
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		try {
			if (!(sender instanceof Player)) {
				return false;
			}

			Player player = (Player) sender;

			if (!player.isOp())
				return false;

			Set<Material> distinctMaterials = new HashSet<Material>();

			String itemKey = plugin.config().getConfig().getString(MagicDoorsPlugin.RECIPE_NAME_DOOR_KEY);
			IRecipe doorKeyRecipe = plugin.recipeRegistrar().getRecipeFromKey(itemKey);
			if(doorKeyRecipe == null)
				plugin.warning("doorKeyRecipe was null");
			
			itemKey = plugin.config().getConfig().getString(MagicDoorsPlugin.RECIPE_NAME_ROOT_DOOR_COPY);
			IRecipe rootDoorCopyRecipe = plugin.recipeRegistrar().getRecipeFromKey(itemKey);
			if(rootDoorCopyRecipe == null)
				plugin.warning("rootDoorCopyRecipe was null");
			
			itemKey = plugin.config().getConfig().getString(MagicDoorsPlugin.RECIPE_NAME_ROOT_DOOR);
			IRecipe rootDoorRecipe = plugin.recipeRegistrar().getRecipeFromKey(itemKey);
			if(rootDoorRecipe == null)
				plugin.warning("rootDoorRecipe was null");
			
			Set<Material> doorKeyRecipeItems = getDistinctMaterialsForRecipe(doorKeyRecipe);
			Set<Material> rootDoorCopyRecipeItems = getDistinctMaterialsForRecipe(rootDoorCopyRecipe);
			Set<Material> rootDoorRecipeItems = getDistinctMaterialsForRecipe(rootDoorRecipe);

			if (doorKeyRecipeItems != null) {
				distinctMaterials.addAll(doorKeyRecipeItems);
			}

			if (rootDoorCopyRecipeItems != null) {
				distinctMaterials.addAll(rootDoorCopyRecipeItems);
			}

			if (rootDoorRecipeItems != null) {
				distinctMaterials.addAll(rootDoorRecipeItems);
			}
			
			if(distinctMaterials == null || distinctMaterials.isEmpty()){
				plugin.warning("distinctMaterials was null");
				player.sendMessage(ChatColor.RED + "Error running '" + COMMAND_MD_EQUIP + "'. No distinct magic door item found.");
				return false;
			}

			List<ItemStack> items = new ArrayList<ItemStack>();
			for (Material mat : distinctMaterials) {
				int maxStackSize = mat.getMaxStackSize();
				ItemStack item = new ItemStack(mat);
				item.setAmount(maxStackSize);
				items.add(item);
			}

			if (items.size() <= 0) {
				player.sendMessage(ChatColor.RED + "Error running '" + COMMAND_MD_EQUIP + "'. No magic door item found.");
				return false;
			}

			int rows = (items.size() / 9) + 1;
			Inventory magicBlocksInventory = Bukkit.createInventory(null, rows * 9, "(Magic Doors)");
			
			int i = -1;
			for(ItemStack item : items){
				i++;
				magicBlocksInventory.setItem(i, item);
			}

			player.openInventory(magicBlocksInventory);

			return true;
		} catch (Exception e) {
			plugin.info("error running 'mb'equip':  " + e.getMessage());
			return false;
		}
	}
	
	
	
	private Set<Material> getDistinctMaterialsForRecipe(IRecipe recipe) {
		if (recipe == null)
			return null;

		Map<Material, Integer> materialCountMap = new HashMap<Material, Integer>();
		
		if(recipe.getMaterialMatrix() == null || recipe.getMaterialMatrix().isEmpty()){
			plugin.warning("Material matrix was null or empty for " + recipe.getName());
			return null;
		}

		for (Material mat : recipe.getMaterialMatrix()) {

			if (!materialCountMap.containsKey(mat)) {
				materialCountMap.put(mat, 1);
			} else {
				materialCountMap.put(mat, materialCountMap.get(mat) + 1);
			}
		}
		
		if(materialCountMap == null || materialCountMap.isEmpty()){
			plugin.warning("materialCountMap was null or empty for " + recipe.getName());
			return null;
		}

		return materialCountMap.keySet();
	}
}
