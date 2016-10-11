package com.blocktyper.magicdoors;

import java.text.MessageFormat;
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
import com.blocktyper.magicdoors.data.MagicDoor;
import com.blocktyper.magicdoors.data.MagicDoorRepo;
import com.blocktyper.plugin.BlockTyperPlugin;
import com.blocktyper.recipes.IRecipe;


/*
 * magic.doors.door=Magische Tür
magic.doors.key=Magie Türschlüssel
block.typer.loading.recipes=Laden Rezepte
 */


public class RootDoorListener implements Listener {

	
	
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
	
	private static MagicDoorRepo magicDoorRepo;
	private static DimentionItemCount dimentionItemCount;
	

	public IRecipe getRootDoorRecipe() {
		if(rootDoorRecipe == null){
			rootDoorRecipe = getRecipeFromKey(MagicDoorsPlugin.RECIPE_NAME_ROOT_DOOR);
		}
		return rootDoorRecipe;
	}

	public IRecipe getRootDoorCopyRecipe() {
		if(rootDoorCopyRecipe == null){
			rootDoorCopyRecipe = getRecipeFromKey(MagicDoorsPlugin.RECIPE_NAME_ROOT_DOOR_COPY);
		}
		return rootDoorCopyRecipe;
	}

	public IRecipe getDoorKeyRecipe() {
		if(doorKeyRecipe == null){
			doorKeyRecipe = getRecipeFromKey(MagicDoorsPlugin.RECIPE_NAME_DOOR_KEY);
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
		
		initMagicDoorRepo();
		initDimentionItemCount();
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void entityShootBow(EntityShootBowEvent event) {
		plugin.debugInfo("EntityShootBowEvent");
	}
	
	

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onBlockPlace(BlockPlaceEvent event) {
		try{
			plugin.debugInfo("BlockPlaceEvent - Material " + event.getBlock().getType().name());

			//if there is no root door recipe defined, do not continue
			if (getRootDoorRecipe() == null) {
				plugin.debugWarning("No magic door recipe.");
				return;
			}

			//if there is no root door copy recipe defined, do not continue
			if (getRootDoorCopyRecipe() == null) {
				plugin.debugWarning("No magic door copy recipe.");
				return;
			}

			ItemStack itemInHand = event.getItemInHand();

			//if player is not holding a item, do not continue
			if (itemInHand == null) {
				plugin.debugWarning("Not holding an item");
				return;
			}

			//if player is not holding a magic door, do not continue
			if (!itemInHand.getType().equals(getRootDoorRecipe().getOutput()) && !itemInHand.getType().equals(getRootDoorCopyRecipe().getOutput())) {
				plugin.debugWarning(
						"Not holding a magic door");
				return;
			}

			//if the item does not have a display name, do not continue
			if (itemInHand.getItemMeta() == null || itemInHand.getItemMeta().getDisplayName() == null) {
				plugin.debugWarning("Not holding door with a name.");
				return;
			}

			String itemName = itemInHand.getItemMeta().getDisplayName();

			//if the item name does not equal the name of the current Root Door or Root Door Copy, do not continue
			if (!itemName.equals(getRootDoorRecipe().getName()) && !itemName.startsWith((getRootDoorCopyRecipe().getName()))) {
				plugin.debugWarning("Not holding door with the magic door name: '" + itemName + "' != '"
						+ getRootDoorRecipe().getName() + "'");
				plugin.debugWarning("Not holding door with the magic door name: '" + itemName + "' != '"
						+ getRootDoorCopyRecipe().getName() + "'");
				return;
			}
					
			
			
			String doorId = null;
			if(itemName.equals(getRootDoorRecipe().getName())){
				//if this is a root door
				//look up a cool name from configured prefixes and suffixes
				doorId = getNewDoorId();
				
				if(doorId != null && magicDoorRepo.getMap().containsKey(doorId)){
					//clash
					doorId = solveClash(doorId, magicDoorRepo, 1);
				}
			}
			
			//if we failed to get an ID at this point, just make a UUID
			if(doorId == null){
				UUID uuid = UUID.randomUUID();
				doorId = uuid.toString();
			}
	

			//construct the door data
			MagicDoor doorToMake = new MagicDoor();
			doorToMake.setId(doorId);
			doorToMake.setWorld(event.getPlayer().getWorld().getName());

			if (itemName.startsWith(getRootDoorCopyRecipe().getName())) {
				String parentId = null;

				String displayName = itemInHand.getItemMeta().getDisplayName();
				if (displayName != null && displayName.contains(PARENT_ID_EQUALS)) {
					parentId = displayName.substring(displayName.indexOf(PARENT_ID_EQUALS) + PARENT_ID_EQUALS.length());
				}

				MagicDoor parentDoor = parentId != null ? magicDoorRepo.getMap().get(parentId) : null;

				if (parentDoor == null) {
					event.getPlayer().sendMessage(ChatColor.RED + plugin.getLocalizedMessage("magic.doors.attempted.to.place.orphan.door"));
					event.setCancelled(true);
					return;
				} else {
					if (parentDoor.getChildren() == null)
						parentDoor.setChildren(new HashMap<Integer, String>());

					parentDoor.getChildren().put(parentDoor.getChildren().size(), doorId);
					doorToMake.setParentId(parentDoor.getId());

					magicDoorRepo.getMap().put(parentId, parentDoor);
				}

			} else {
				doorToMake.setParentId(null);
			}

			doorToMake.setX(event.getBlock().getX());
			doorToMake.setY(event.getBlock().getY());
			doorToMake.setZ(event.getBlock().getZ());

			doorToMake.setPlayerX(event.getPlayer().getLocation().getBlockX());
			doorToMake.setPlayerY(event.getPlayer().getLocation().getBlockY());
			doorToMake.setPlayerZ(event.getPlayer().getLocation().getBlockZ());
			
			addDoor(doorToMake);


			event.getPlayer().sendMessage(ChatColor.GREEN + String.format(plugin.getLocalizedMessage("magic.doors.you.placed.a.magic.door"), doorId));
		}catch(Exception e){
			plugin.warning("Unexpected error in 'RootDoorListener.onBlockPlace'. Message: " + e.getMessage());
		}
		
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
		
		initDimentionItemCount();

		if (dimentionItemCount == null || dimentionItemCount.getItemsInDimentionAtValue() == null
				|| dimentionItemCount.getItemsInDimentionAtValue().isEmpty()) {
			plugin.debugInfo("no dimention values recorded");
			return;
		}


		Map<String, Set<String>> matchesMap = new HashMap<String, Set<String>>();

		String lastDimention = null;
		for (String dimention : getDimentionList()) {
			if (!dimentionItemCount.getItemsInDimentionAtValue().containsKey(dimention)
					|| dimentionItemCount.getItemsInDimentionAtValue().get(dimention) == null
					|| dimentionItemCount.getItemsInDimentionAtValue().get(dimention).isEmpty()) {
				plugin.debugInfo("no " + dimention + " values recorded");
				return;
			}

			int coordValue = dimention.equals("x") ? event.getBlock().getX()
					: (dimention.equals("y") ? event.getBlock().getY() : event.getBlock().getZ());

			if (!dimentionItemCount.getItemsInDimentionAtValue().get(dimention).containsKey(coordValue)
					|| dimentionItemCount.getItemsInDimentionAtValue().get(dimention).get(coordValue).isEmpty()) {
				plugin.debugInfo("no matching " + dimention + " value");
				return;
			} else {

				Set<String> newMatchesList = new HashSet<String>();
				if (lastDimention == null || matchesMap.containsKey(lastDimention)) {
					for (String uuid : dimentionItemCount.getItemsInDimentionAtValue().get(dimention).get(coordValue)) {
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
		
		initMagicDoorRepo();

		if (magicDoorRepo == null) {
			plugin.debugWarning("Failed to load magic-doors repo.");
			return;
		}

		if (!magicDoorRepo.getMap().containsKey(match)) {
			plugin.debugWarning("Failed to load door from magic-doors repo.");
			return;
		}

		MagicDoor magicDoor = magicDoorRepo.getMap().get(match);
		if (magicDoor == null) {
			plugin.debugWarning("Failed to load door from magic-doors repo.");
			return;
		}

		if (isRootDoorCopy && magicDoor.getParentId() == null) {
			ItemMeta meta = itemInHand.getItemMeta();
					
			meta.setDisplayName(itemInHand.getItemMeta().getDisplayName() + "-" + PARENT_ID_EQUALS + magicDoor.getId());
			itemInHand.setItemMeta(meta);
			event.getPlayer().sendMessage(ChatColor.GREEN + String.format(plugin.getLocalizedMessage("magic.doors.root.door.copy.imprinted"), getRootDoorCopyRecipe().getName()));
			return;
		}

		if (isKey && magicDoor.getParentId() == null && (magicDoor.getChildren() == null || magicDoor.getChildren().isEmpty())) {
			// This is a magic door with no children
			event.getPlayer().sendMessage(ChatColor.RED + String.format(plugin.getLocalizedMessage("magic.doors.root.has.no.children"), magicDoor.getId()));
			return;
		}

		if (magicDoor.getParentId() == null && isKey) {
			// this is a root door, get children and teleport randomly
			int childDoorNumber = random.nextInt(magicDoor.getChildren().size());
			String childId = magicDoor.getChildren().get(childDoorNumber);
			MagicDoor childDoor = magicDoorRepo.getMap().get(childId);

			if (childDoor == null) {
				event.getPlayer().sendMessage(ChatColor.RED + new MessageFormat(plugin.getLocalizedMessage("magic.doors.failed.to.find.child.door.number")).format(new Object[]{(childDoorNumber + 1)+"", magicDoor.getChildren().size()+""}));
				return;
			}
			World world = plugin.getServer().getWorld(childDoor.getWorld());

			if (world == null) {

				event.getPlayer().sendMessage(ChatColor.RED + new MessageFormat(plugin.getLocalizedMessage("magic.doors.failed.to.find.world.child")).format(new Object[]{childDoor.getWorld(), (childDoorNumber + 1)+"",magicDoor.getChildren().size()+""}));
				return;
			}

			Location destination = new Location(world, childDoor.getPlayerX() + 0.0, childDoor.getPlayerY() + 0.0,
					childDoor.getPlayerZ() + 0.0);
			event.getPlayer().teleport(destination);
			
			event.getPlayer().sendMessage(new MessageFormat(plugin.getLocalizedMessage("magic.doors.you.have.been.teleported.to.child")).format(new Object[]{(childDoorNumber+1)+"",magicDoor.getId()}));
		} else if (magicDoor.getParentId() != null && isKey) {
			// this is a child, teleport to parent

			MagicDoor parentDoor = magicDoorRepo.getMap().get(magicDoor.getParentId());

			if (parentDoor == null) {
				event.getPlayer()
						.sendMessage(ChatColor.RED + String.format(plugin.getLocalizedMessage("magic.doors.failed.to.find.parent.door.id"),magicDoor.getParentId()));
				return;
			}
			World world = plugin.getServer().getWorld(parentDoor.getWorld());

			if (world == null) {
				event.getPlayer().sendMessage(ChatColor.RED + new MessageFormat(plugin.getLocalizedMessage("magic.doors.failed.to.find.world.parent")).format(new Object[]{parentDoor.getWorld(),magicDoor.getParentId()}));
				return;
			}

			Location destination = new Location(world, parentDoor.getPlayerX() + 0.0, parentDoor.getPlayerY() + 0.0,
					parentDoor.getPlayerZ() + 0.0);
			event.getPlayer().teleport(destination);
			
			//magic.doors.you.have.been.teleported.to.root
			event.getPlayer().sendMessage(
					new MessageFormat(plugin.getLocalizedMessage("magic.doors.you.have.been.teleported.to.root")).format(new Object[]{magicDoor.getParentId()}) );
		}
	}
	
	
	////////////////////////////////////
	//PRIVATE HELPERS///////////////////
	////////////////////////////////////
	
	private void initMagicDoorRepo(){
		if(magicDoorRepo == null){
			magicDoorRepo = plugin.getTypeData(DATA_KEY_MAGIC_DOORS, MagicDoorRepo.class);

			if(magicDoorRepo == null || magicDoorRepo.getMap() == null){
				magicDoorRepo = new MagicDoorRepo();
				magicDoorRepo.setMap(new HashMap<String, MagicDoor>());
				updateMagicDoorRepo();
			}
		}
	}
	
	private void updateMagicDoorRepo(){
		plugin.setData(DATA_KEY_MAGIC_DOORS, magicDoorRepo);
	}
	
	private void initDimentionItemCount(){
		if (dimentionItemCount == null) {
			dimentionItemCount = plugin.getTypeData(DATA_KEY_MAGIC_DOOR_DIMENTION_MAP, DimentionItemCount.class);
			if(dimentionItemCount == null || dimentionItemCount.getItemsInDimentionAtValue() == null){
				dimentionItemCount = new DimentionItemCount();
				dimentionItemCount.setItemsInDimentionAtValue(new HashMap<String, Map<Integer, Set<String>>>());
				updateDimentionItemCount();
			}
		}
	}
	
	private void updateDimentionItemCount(){
		plugin.setData(DATA_KEY_MAGIC_DOOR_DIMENTION_MAP, dimentionItemCount, true);
	}
	
	private List<String> getDimentionList(){
		List<String> dimentions = new ArrayList<String>();
		dimentions.add("x");
		dimentions.add("y");
		dimentions.add("z");
		return dimentions;
	}
	
	private boolean addDoor(MagicDoor magicDoor){
		try {

			initMagicDoorRepo();
			initDimentionItemCount();

			for (String dimention : getDimentionList()) {
				if (dimentionItemCount.getItemsInDimentionAtValue().get(dimention) == null) {
					dimentionItemCount.getItemsInDimentionAtValue().put(dimention, new HashMap<Integer, Set<String>>());
				}

				int value = dimention.equals("x") ? magicDoor.getX()
						: (dimention.equals("y") ? magicDoor.getY() : magicDoor.getZ());

				if (dimentionItemCount.getItemsInDimentionAtValue().get(dimention).get(value) == null) {
					dimentionItemCount.getItemsInDimentionAtValue().get(dimention).put(value, new HashSet<String>());
				}
				dimentionItemCount.getItemsInDimentionAtValue().get(dimention).get(value).add(magicDoor.getId());
			}
			magicDoorRepo.getMap().put(magicDoor.getId(), magicDoor);
			updateDimentionItemCount();
			updateMagicDoorRepo();
		} catch (Exception e) {
			plugin.warning("Unexpected error while saving door: " + e.getMessage());
			return false;
			
		}
		
		return true;
	}
	
	private String solveClash(String id, MagicDoorRepo repo, int start){
		String newId = id + "-" + start;
		
		if(repo.getMap().containsKey(newId)){
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
	
	
	
	
	////////////////////////////////////////////////////
	////////////////MODEL CLASSES///////////////////////
	////////////////////////////////////////////////////

}
