package com.blocktyper.magicdoors;

import java.util.Locale;
import java.util.ResourceBundle;

import com.blocktyper.plugin.BlockTyperPlugin;

public class MagicDoorsPlugin extends BlockTyperPlugin {
	
	public static final String RECIPES_KEY = "MAGIC_DOORS_RECIPE_KEY";

	public static final String RESOURCE_NAME = "com.blocktyper.magicdoors.resources.MagicDoorsMessages";

	private static String RECIPE_NAME_ROOT_DOOR = "magic.doors.recipe.name.root.door";
	private static String RECIPE_NAME_OWNED_ROOT_DOOR = "magic.doors.recipe.name.owned.root.door";
	private static String RECIPE_NAME_ROOT_DOOR_COPY = "magic.doors.recipe.name.door.copy";
	private static String RECIPE_NAME_DOOR_KEY = "magic.doors.recipe.name.door.key";
	private static String RECIPE_NAME_SKELETON_KEY = "magic.doors.recipe.name.skeleton.key";
	
	public static String rootDoorRecipe(){
		return getPlugin().getConfig().getString(RECIPE_NAME_ROOT_DOOR);
	}
	public static String ownedRootDoorRecipe(){
		return getPlugin().getConfig().getString(RECIPE_NAME_OWNED_ROOT_DOOR);
	}
	public static String rootDoorCopyRecipe(){
		return getPlugin().getConfig().getString(RECIPE_NAME_ROOT_DOOR_COPY);
	}
	public static String doorKeyRecipe(){
		return getPlugin().getConfig().getString(RECIPE_NAME_DOOR_KEY);
	}
	public static String skeletonKeyRecipe(){
		return getPlugin().getConfig().getString(RECIPE_NAME_SKELETON_KEY);
	}

	private static MagicDoorsPlugin plugin;
	
	public MagicDoorsPlugin(){
		if(plugin == null){
			plugin = this;
		}
	}

	public void onEnable() {
		super.onEnable();
		new MagicDoorsEquipCommand(this);
	}

	// begin localization
	@Override
	public ResourceBundle getBundle(Locale locale) {
		return ResourceBundle.getBundle(RESOURCE_NAME, locale);
	}
	// end localization
	
	public static MagicDoorsPlugin getPlugin(){
		return plugin;
	}

	@Override
	public String getRecipesNbtKey() {
		return RECIPES_KEY;
	}
}
