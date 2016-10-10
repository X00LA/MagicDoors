package com.blocktyper.magicdoors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.blocktyper.magicdoors.data.DimentionItemCount;
import com.blocktyper.plugin.BlockTyperPlugin;
import com.blocktyper.recipes.IRecipe;



public class RootDoorListener implements Listener {

	public static String RECIPE_NAME_ROOT_DOOR = "magic.doors.recipe.name.root.door";
	public static String RECIPE_NAME_ROOT_DOOR_COPY = "magic.doors.recipe.name.door.copy";
	public static String RECIPE_NAME_DOOR_KEY = "magic.doors.recipe.name.door.key";
	
	public static String DOOR_NAME_PREFIXES = "magic.doors.door.name.prefixes";
	public static String DOOR_NAME_SUFFIXES = "magic.doors.door.name.suffixes";

	public static final String DATA_KEY_MAGIC_DOOR_DIMENTION_MAP = "root-doors-dimention";
	public static final String DATA_KEY_MAGIC_DOORS = "magic-doors";

	public static final String PARENT_ID_EQUALS = "parentId=";

	private final BlockTyperPlugin plugin;
	private List<String> doorPrefixes;
	private List<String> doorSuffixes;

	Random random = new Random();

	private IRecipe rootDoorRecipe;
	private IRecipe rootDoorCopyRecipe;
	private IRecipe doorKeyRecipe;
	
	

	public IRecipe getRootDoorRecipe() {
		if(rootDoorRecipe == null){
			rootDoorRecipe = getRecipeFromKey(RECIPE_NAME_ROOT_DOOR);
		}
		return rootDoorRecipe;
	}

	public IRecipe getRootDoorCopyRecipe() {
		if(rootDoorCopyRecipe == null){
			rootDoorCopyRecipe = getRecipeFromKey(RECIPE_NAME_ROOT_DOOR_COPY);
		}
		return rootDoorCopyRecipe;
	}

	public IRecipe getDoorKeyRecipe() {
		if(doorKeyRecipe == null){
			doorKeyRecipe = getRecipeFromKey(RECIPE_NAME_DOOR_KEY);
		}
		return doorKeyRecipe;
	}

	private IRecipe getRecipeFromKey(String key) {
		String itemKey = plugin.config().getConfig().getString(key);
		
		plugin.info("loading recipe for " + key + ": '" + itemKey + "'");
		IRecipe recipe = plugin.recipeRegistrar().getRecipeFromKey(itemKey);
		if (recipe == null) {
			plugin.warning("recipe '" + itemKey + "' was not found");
		}
		return recipe;

	}

	public RootDoorListener() {
		plugin = BlockTyperPlugin.plugin;
		
		doorPrefixes = plugin.getConfig().getStringList(DOOR_NAME_PREFIXES);
		doorSuffixes = plugin.getConfig().getStringList(DOOR_NAME_SUFFIXES);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void entityShootBow(EntityShootBowEvent event) {
		plugin.debugInfo("EntityShootBowEvent");
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onBlockPlace(BlockPlaceEvent event) {
		plugin.debugInfo("BlockPlaceEvent - Material " + event.getBlock().getType().name());

		if (getRootDoorRecipe() == null) {
			plugin.debugWarning("No magic door recipe.");
			return;
		}

		if (getRootDoorCopyRecipe() == null) {
			plugin.debugWarning("No magic door copy recipe.");
			return;
		}

		ItemStack itemInHand = event.getItemInHand();

		if (itemInHand == null) {
			plugin.debugWarning("Not holding an item");
			return;
		}

		if (!itemInHand.getType().equals(getRootDoorRecipe().getOutput()) && !itemInHand.getType().equals(getRootDoorCopyRecipe().getOutput())) {
			plugin.debugWarning(
					"Not holding a magic door");
			return;
		}

		if (itemInHand.getItemMeta() == null || itemInHand.getItemMeta().getDisplayName() == null) {
			plugin.debugWarning("Not holding door with a name.");
			return;
		}

		String itemName = itemInHand.getItemMeta().getDisplayName();

		if (!itemName.equals(getRootDoorRecipe().getName()) && !itemName.startsWith((getRootDoorCopyRecipe().getName()))) {
			plugin.debugWarning("Not holding door with the magic door name: '" + itemName + "' != '"
					+ getRootDoorRecipe().getName() + "'");
			plugin.debugWarning("Not holding door with the magic door name: '" + itemName + "' != '"
					+ getRootDoorCopyRecipe().getName() + "'");
			return;
		}
		
		
		MagicDoorRepo magicDoorRepo = plugin.getTypeData(DATA_KEY_MAGIC_DOORS, MagicDoorRepo.class);
		
		if(magicDoorRepo == null)
			magicDoorRepo = new MagicDoorRepo();
		
		if(magicDoorRepo.map == null)
			magicDoorRepo.map = new HashMap<String, RootDoorListener.MagicDoor>();

		
		
		String doorId = null;
		
		
		if(itemName.equals(getRootDoorRecipe().getName())){
			//root door gets cool name from configured prefixes and suffixes
			doorId = getNewDoorId();
			
			if(doorId != null && magicDoorRepo.map.containsKey(doorId)){
				//clash
				doorId = solveClash(doorId, magicDoorRepo, 1);
			}
		}
		
		if(doorId == null){
			UUID uuid = UUID.randomUUID();
			doorId = uuid.toString();
		}

		event.getPlayer().sendMessage(ChatColor.GREEN + " you placed a magic door. ID: " + doorId);

		DimentionItemCount itemCountPerDimention = plugin.getTypeData(DATA_KEY_MAGIC_DOOR_DIMENTION_MAP,
				DimentionItemCount.class);
		if (itemCountPerDimention == null) {
			itemCountPerDimention = new DimentionItemCount();
			itemCountPerDimention.setItemsInDimentionAtValue(new HashMap<String, Map<Integer, Set<String>>>());
		}

		List<String> dimentions = new ArrayList<String>();
		dimentions.add("x");
		dimentions.add("y");
		dimentions.add("z");

		for (String dimention : dimentions) {
			if (itemCountPerDimention.getItemsInDimentionAtValue().get(dimention) == null) {
				itemCountPerDimention.getItemsInDimentionAtValue().put(dimention, new HashMap<Integer, Set<String>>());
			}

			int value = dimention.equals("x") ? event.getBlock().getX()
					: (dimention.equals("y") ? event.getBlock().getY() : event.getBlock().getZ());

			if (itemCountPerDimention.getItemsInDimentionAtValue().get(dimention).get(value) == null) {
				itemCountPerDimention.getItemsInDimentionAtValue().get(dimention).put(value, new HashSet<String>());
			}
			itemCountPerDimention.getItemsInDimentionAtValue().get(dimention).get(value).add(doorId);
		}

		MagicDoor doorToMake = new MagicDoor();
		doorToMake.id = doorId;
		doorToMake.world = event.getPlayer().getWorld().getName();

		if (itemName.startsWith(getRootDoorCopyRecipe().getName())) {
			String parentId = null;

			String displayName = itemInHand.getItemMeta().getDisplayName();
			if (displayName != null && displayName.contains(PARENT_ID_EQUALS)) {
				parentId = displayName.substring(displayName.indexOf(PARENT_ID_EQUALS) + PARENT_ID_EQUALS.length());
			}

			MagicDoor parentDoor = parentId != null ? magicDoorRepo.map.get(parentId) : null;

			if (parentDoor == null) {
				event.getPlayer().sendMessage(ChatColor.RED + "You have attempted to place an an orphan door");
				event.setCancelled(true);
				return;
			} else {
				if (parentDoor.children == null)
					parentDoor.children = new HashMap<Integer, String>();

				parentDoor.children.put(parentDoor.children.size(), doorId);
				doorToMake.parentId = parentDoor.id;

				magicDoorRepo.map.put(parentId, parentDoor);
			}

		} else {
			doorToMake.parentId = null;
		}

		doorToMake.x = event.getBlock().getX();
		doorToMake.y = event.getBlock().getX();
		doorToMake.z = event.getBlock().getX();

		doorToMake.playerX = event.getPlayer().getLocation().getBlockX();
		doorToMake.playerY = event.getPlayer().getLocation().getBlockY();
		doorToMake.playerZ = event.getPlayer().getLocation().getBlockZ();

		magicDoorRepo.map.put(doorId, doorToMake);

		plugin.setData(DATA_KEY_MAGIC_DOORS, magicDoorRepo);
		plugin.setData(DATA_KEY_MAGIC_DOOR_DIMENTION_MAP, itemCountPerDimention, true);

		event.getPlayer().sendMessage(ChatColor.GREEN + " you placed a magic door. ID: " + doorId);

	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void blockDamage(BlockDamageEvent event) {
		plugin.debugInfo("BlockDamageEvent - Material " + event.getBlock().getType().name());

		ItemStack itemInHand = event.getItemInHand();

		if (itemInHand == null) {
			plugin.debugWarning("Not holding an item");
			return;
		}

		if (!itemInHand.getType().equals(getRootDoorCopyRecipe().getOutput()) &&
				!itemInHand.getType().equals(getDoorKeyRecipe().getOutput())) {
			plugin.debugWarning(
					"Not holding an item which is the same type as the magic door type: " + getRootDoorCopyRecipe().getOutput());
			plugin.debugWarning(
					"Not holding an item which is the same type as the magic door type: " + getDoorKeyRecipe().getOutput());
			return;
		}

		if (itemInHand.getItemMeta() == null || itemInHand.getItemMeta().getDisplayName() == null) {
			plugin.debugWarning("Not holding door with a name.");
			return;
		}

		String itemName = itemInHand.getItemMeta().getDisplayName();

		boolean isKey = itemName.equals(getDoorKeyRecipe().getName());
		boolean isRootDoorCopy = itemName.equals(getRootDoorCopyRecipe().getName());

		if (!isRootDoorCopy && !isKey) {
			plugin.debugInfo("Not a magic door related object in hand");
			return;
		}

		DimentionItemCount itemCountPerDimention = plugin.getTypeData(DATA_KEY_MAGIC_DOOR_DIMENTION_MAP,
				DimentionItemCount.class);

		if (itemCountPerDimention == null || itemCountPerDimention.getItemsInDimentionAtValue() == null
				|| itemCountPerDimention.getItemsInDimentionAtValue().isEmpty()) {
			plugin.debugInfo("no dimention values recorded");
			return;
		}

		List<String> dimentions = new ArrayList<String>();
		dimentions.add("x");
		dimentions.add("y");
		dimentions.add("z");

		Map<String, Set<String>> matchesMap = new HashMap<String, Set<String>>();

		String lastDimention = null;
		for (String dimention : dimentions) {
			if (!itemCountPerDimention.getItemsInDimentionAtValue().containsKey(dimention)
					|| itemCountPerDimention.getItemsInDimentionAtValue().get(dimention) == null
					|| itemCountPerDimention.getItemsInDimentionAtValue().get(dimention).isEmpty()) {
				plugin.debugInfo("no " + dimention + " values recorded");
				return;
			}

			int coordValue = dimention.equals("x") ? event.getBlock().getX()
					: (dimention.equals("y") ? event.getBlock().getY() : event.getBlock().getZ());

			if (!itemCountPerDimention.getItemsInDimentionAtValue().get(dimention).containsKey(coordValue)
					|| itemCountPerDimention.getItemsInDimentionAtValue().get(dimention).get(coordValue).isEmpty()) {
				plugin.debugInfo("no matching " + dimention + " value");
				return;
			} else {

				Set<String> newMatchesList = new HashSet<String>();
				if (lastDimention == null || matchesMap.containsKey(lastDimention)) {
					for (String uuid : itemCountPerDimention.getItemsInDimentionAtValue().get(dimention).get(coordValue)) {
						if (lastDimention == null || matchesMap.get(lastDimention).contains(uuid)) {
							newMatchesList.add(uuid);
						}
					}
				}

				matchesMap.put(dimention, newMatchesList);
			}
			lastDimention = dimention;
		}

		List<String> exactMatches = null;
		if (lastDimention != null && matchesMap.containsKey(lastDimention)) {
			exactMatches = new ArrayList<String>(matchesMap.get(lastDimention));
		}

		if (exactMatches == null || exactMatches.isEmpty()) {
			plugin.debugWarning("No match was found but we made it all the way through processing");
			return;
		}

		int index = 0;
		if (exactMatches.size() > 1) {
			plugin.info("There was more than one match found after processing");
			index = random.nextInt(exactMatches.size());
		}

		String match = exactMatches.get(index);

		MagicDoorRepo magicDoorRepo = plugin.getTypeData(DATA_KEY_MAGIC_DOORS, MagicDoorRepo.class);

		if (magicDoorRepo == null) {
			plugin.debugWarning("Failed to load magic-doors repo.");
			return;
		}

		if (!magicDoorRepo.map.containsKey(match)) {
			plugin.debugWarning("Failed to load door from magic-doors repo.");
			return;
		}

		MagicDoor magicDoor = magicDoorRepo.map.get(match);
		if (magicDoor == null) {
			plugin.debugWarning("Failed to load door from magic-doors repo.");
			return;
		}

		if (isRootDoorCopy && magicDoor.parentId == null) {
			ItemMeta meta = itemInHand.getItemMeta();
					
			meta.setDisplayName(itemInHand.getItemMeta().getDisplayName() + "-" + PARENT_ID_EQUALS + magicDoor.id);
			itemInHand.setItemMeta(meta);
			event.getPlayer().sendMessage(ChatColor.GREEN + "This copy has been lined and is ready to be placed");
			return;
		}

		if (isKey && magicDoor.parentId == null && (magicDoor.children == null || magicDoor.children.isEmpty())) {
			// This is a magic door with no children
			event.getPlayer().sendMessage(ChatColor.RED + magicDoor.id + " has no children yet.");
			return;
		}

		if (magicDoor.parentId == null && isKey) {
			// this is a root door, get children and teleport randomly
			int childDoorNumber = random.nextInt(magicDoor.children.size());
			String childId = magicDoor.children.get(childDoorNumber);
			MagicDoor childDoor = magicDoorRepo.map.get(childId);

			if (childDoor == null) {
				event.getPlayer().sendMessage(ChatColor.RED + "Failed to find child door #" + (childDoorNumber + 1)
						+ " of " + magicDoor.children.size());
				return;
			}
			World world = plugin.getServer().getWorld(childDoor.world);

			if (world == null) {
				event.getPlayer().sendMessage(ChatColor.RED + "Failed to find world '" + childDoor.world
						+ "' for child door #" + (childDoorNumber + 1) + " of " + magicDoor.children.size());
				return;
			}

			Location destination = new Location(world, childDoor.playerX + 0.0, childDoor.playerY + 0.0,
					childDoor.playerZ + 0.0);
			event.getPlayer().teleport(destination);
			event.getPlayer().sendMessage("You have been teleported to child door #" + childDoorNumber + " in the '"
					+ magicDoor.id + "' magic door familiy");
		} else if (magicDoor.parentId != null && isKey) {
			// this is a child, teleport to parent

			MagicDoor parentDoor = magicDoorRepo.map.get(magicDoor.parentId);

			if (parentDoor == null) {
				event.getPlayer()
						.sendMessage(ChatColor.RED + "Failed to find parent door with id: " + magicDoor.parentId);
				return;
			}
			World world = plugin.getServer().getWorld(parentDoor.world);

			if (world == null) {
				event.getPlayer().sendMessage(ChatColor.RED + "Failed to find world '" + parentDoor.world
						+ "' for parent door with id: " + magicDoor.parentId);
				return;
			}

			Location destination = new Location(world, parentDoor.playerX + 0.0, parentDoor.playerY + 0.0,
					parentDoor.playerZ + 0.0);
			event.getPlayer().teleport(destination);
			event.getPlayer().sendMessage(
					"You have been teleported to the root door in the '" + magicDoor.parentId + "' magic door familiy");
		}
	}
	
	private String solveClash(String id, MagicDoorRepo repo, int start){
		String newId = id + "-" + start;
		
		if(repo.map.containsKey(newId)){
			return solveClash(id, repo, start + 1);
		}
		
		return newId;
	}
	private String getNewDoorId(){
		
		String doorId = null;
		
		if(doorPrefixes == null || doorPrefixes.isEmpty()){
			plugin.debugWarning("doorPrefixes was null or empty");
		}else{
			doorId = doorPrefixes.get(random.nextInt(doorPrefixes.size()));
			plugin.debugInfo("prefix chosen: " + doorId);
		}
		
		if(doorSuffixes == null || doorSuffixes.isEmpty()){
			plugin.debugWarning("doorSuffixes was null or empty");
		}else{
			String suffix = doorSuffixes.get(random.nextInt(doorSuffixes.size()));
			
			plugin.debugInfo("suffix chosen: " + doorId);
			
			if(doorId == null || doorId.isEmpty()){
				doorId = suffix;
			}else{
				doorId = doorId + "-" + suffix;
			}
		}
		
		if(doorId != null && !doorId.isEmpty()){
			plugin.debugInfo("door id generated: " + doorId);
		}
		
		return doorId;
	}



	public class MagicDoorRepo {
		public Map<String, MagicDoor> map;
	}

	public class MagicDoor {
		public String id;
		public String parentId;
		public Map<Integer, String> children;
		public String world;
		public int x;
		public int y;
		public int z;
		public int playerX;
		public int playerY;
		public int playerZ;
	}

}
