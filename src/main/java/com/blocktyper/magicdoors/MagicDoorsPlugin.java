package com.blocktyper.magicdoors;

import java.util.ResourceBundle;

import com.blocktyper.plugin.BlockTyperPlugin;

public class MagicDoorsPlugin extends BlockTyperPlugin {

	public static final String RESOURCE_NAME = "com.blocktyper.magicdoors.resources.MagicDoorsMessages";

	public static String RECIPE_NAME_ROOT_DOOR = "magic.doors.recipe.name.root.door";
	public static String RECIPE_NAME_ROOT_DOOR_COPY = "magic.doors.recipe.name.door.copy";
	public static String RECIPE_NAME_DOOR_KEY = "magic.doors.recipe.name.door.key";

	

	public void onEnable() {
		super.onEnable();

		new MagicDoorsEquipCommand(this);
	}

	// begin localization
	private ResourceBundle bundle = null;

	public ResourceBundle getBundle() {
		if (bundle == null)
			bundle = ResourceBundle.getBundle(RESOURCE_NAME, locale);
		return bundle;
	}

	

	// end localization
}
