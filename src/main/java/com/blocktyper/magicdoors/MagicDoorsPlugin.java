package com.blocktyper.magicdoors;

import java.util.Locale;
import java.util.ResourceBundle;

import com.blocktyper.plugin.BlockTyperPlugin;

public class MagicDoorsPlugin extends BlockTyperPlugin {

	public static final String RESOURCE_NAME = "com.blocktyper.magicdoors.resources.MagicDoorsMessages";

	public static String RECIPE_NAME_ROOT_DOOR = "magic.doors.recipe.name.root.door";
	public static String RECIPE_NAME_OWNED_ROOT_DOOR = "magic.doors.recipe.name.owned.root.door";
	public static String RECIPE_NAME_ROOT_DOOR_COPY = "magic.doors.recipe.name.door.copy";
	public static String RECIPE_NAME_DOOR_KEY = "magic.doors.recipe.name.door.key";
	public static String RECIPE_NAME_SKELETON_KEY = "magic.doors.recipe.name.skeleton.key";

	

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
}
