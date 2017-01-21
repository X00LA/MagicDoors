package com.blocktyper.magicdoors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.blocktyper.helpers.InvisibleLoreHelper;
import com.blocktyper.magicdoors.data.DimentionItemCount;
import com.blocktyper.magicdoors.data.MagicDoor;
import com.blocktyper.magicdoors.data.MagicDoorRepo;
import com.blocktyper.nbt.NBTItem;

import static com.blocktyper.magicdoors.MagicDoorsPlugin.*;

public class RootDoorListener implements Listener {

	public static String DOOR_NAME_PREFIXES = "magic.doors.door.name.prefixes";
	public static String DOOR_NAME_SUFFIXES = "magic.doors.door.name.suffixes";

	public static final String DATA_KEY_MAGIC_DOOR_DIMENTION_MAP = "root-doors-dimention";
	public static final String DATA_KEY_MAGIC_DOORS = "magic-doors";

	public static final String PARENT_ID_HIDDEN_LORE_PREFIX = "#MAGIC_DOOR_PARENT";

	private List<String> doorPrefixes;
	private List<String> doorSuffixes;

	private Random random = new Random();

	private static MagicDoorRepo magicDoorRepo;
	private static DimentionItemCount dimentionItemCount;


	public RootDoorListener() {

		doorPrefixes = MagicDoorsPlugin.getPlugin().getConfig().getStringList(DOOR_NAME_PREFIXES);
		doorSuffixes = MagicDoorsPlugin.getPlugin().getConfig().getStringList(DOOR_NAME_SUFFIXES);

		initMagicDoorRepo();
		initDimentionItemCount();
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onBlockPlace(BlockPlaceEvent event) {
		try {

			ItemStack itemInHand = event.getItemInHand();

			// if player is not holding a item, do not continue
			if (itemInHand == null) {
				MagicDoorsPlugin.getPlugin().debugWarning("Not holding an item");
				return;
			}

			NBTItem nbtItem = new NBTItem(itemInHand);
			String nbtRecipeKey = nbtItem.getString(MagicDoorsPlugin.RECIPES_KEY);

			if (nbtRecipeKey == null || nbtRecipeKey.isEmpty()) {
				MagicDoorsPlugin.getPlugin().debugInfo("Not holding magic door item.'");
				return;
			}

			// if the item name does not equal the name of the current Root Door
			// or Root Door Copy, do not continue
			if (!nbtRecipeKey.equals(rootDoorRecipe()) && !nbtRecipeKey.equals(ownedRootDoorRecipe())
					&& !nbtRecipeKey.startsWith((rootDoorCopyRecipe()))) {
				MagicDoorsPlugin.getPlugin().debugWarning("Not holding door with the magic door name: '" + nbtRecipeKey
						+ "' != '" + rootDoorRecipe() + "'");
				MagicDoorsPlugin.getPlugin().debugWarning("Not holding door with the magic door name: '" + nbtRecipeKey
						+ "' != '" + ownedRootDoorRecipe() + "'");
				MagicDoorsPlugin.getPlugin().debugWarning("Not holding door with the magic door name: '" + nbtRecipeKey
						+ "' != '" + rootDoorCopyRecipe() + "'");
				return;
			}

			String doorId = null;
			if (nbtRecipeKey.equals(rootDoorRecipe()) || nbtRecipeKey.equals(ownedRootDoorRecipe())) {
				// if this is a root door
				// look up a cool name from configured prefixes and suffixes
				doorId = getNewDoorId();

				if (doorId != null && magicDoorRepo.getMap().containsKey(doorId)) {
					// clash
					doorId = solveClash(doorId, magicDoorRepo, 1);
				}
			}

			// if we failed to get an ID at this point, just make a UUID
			if (doorId == null) {
				UUID uuid = UUID.randomUUID();
				doorId = uuid.toString();
			}

			// construct the door data
			MagicDoor doorToMake = new MagicDoor();
			doorToMake.setId(doorId);
			doorToMake.setWorld(event.getPlayer().getWorld().getName());

			if (nbtRecipeKey.equals(ownedRootDoorRecipe())) {
				doorToMake.setOwnerName(event.getPlayer().getName());
			} else {
				doorToMake.setOwnerName(null);
			}

			if (nbtRecipeKey.startsWith(rootDoorCopyRecipe())) {
				String parentId = null;

				//MagicDoorsPlugin.getPlugin().getInvisibleLoreHelper()
				List<String> lore = itemInHand.getItemMeta().getLore();
				if (lore != null && !lore.isEmpty()) {
					Optional<String> loreLineWithParentId = lore.stream().filter(l -> l != null && InvisibleLoreHelper.convertToVisibleString(l).contains(PARENT_ID_HIDDEN_LORE_PREFIX)).findFirst();
					if(loreLineWithParentId != null && loreLineWithParentId.isPresent() && loreLineWithParentId.get() != null){
						String loreLine = InvisibleLoreHelper.convertToVisibleString(loreLineWithParentId.get());
						parentId = loreLine.substring(loreLine.indexOf(PARENT_ID_HIDDEN_LORE_PREFIX) + PARENT_ID_HIDDEN_LORE_PREFIX.length());
					}
				}

				MagicDoor parentDoor = parentId != null ? magicDoorRepo.getMap().get(parentId) : null;

				if (parentDoor == null) {
					event.getPlayer().sendMessage(ChatColor.RED + MagicDoorsPlugin.getPlugin()
							.getLocalizedMessage("magic-doors-attempted-to-place-orphan-door", event.getPlayer()));
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

			event.getPlayer().sendMessage(ChatColor.GREEN + String.format(MagicDoorsPlugin.getPlugin()
					.getLocalizedMessage("magic-doors-you-placed-a-magic-door", event.getPlayer()), doorId));
		} catch (Exception e) {
			MagicDoorsPlugin.getPlugin()
					.warning("Unexpected error in 'RootDoorListener.onBlockPlace'. Message: " + e.getMessage());
		}

	}
	
	private String getParentIdFromLore(List<String> lore){
		
		String parentId = null;
		if (lore != null && !lore.isEmpty()) {
			Optional<String> loreLineWithParentId = lore.stream().filter(l -> l != null && InvisibleLoreHelper.convertToVisibleString(l).contains(PARENT_ID_HIDDEN_LORE_PREFIX)).findFirst();
			if(loreLineWithParentId != null && loreLineWithParentId.isPresent() && loreLineWithParentId.get() != null){
				String loreLine = InvisibleLoreHelper.convertToVisibleString(loreLineWithParentId.get());
				parentId = loreLine.substring(loreLine.indexOf(PARENT_ID_HIDDEN_LORE_PREFIX) + PARENT_ID_HIDDEN_LORE_PREFIX.length());
			}
		}
		
		return parentId;
	}
	
	private void sendNamedKeyRequiredMessage(HumanEntity player, String doorId){
		player
		.sendMessage(
				ChatColor.RED + String.format(
						MagicDoorsPlugin.getPlugin().getLocalizedMessage(
								"magic-doors-named-key-required", player),
						doorId));
	}
	
	private void sendMissingWorldMessage(HumanEntity player, String world){
		player
		.sendMessage(ChatColor.RED + new MessageFormat(MagicDoorsPlugin.getPlugin()
				.getLocalizedMessage("magic-doors-failed-to-find-world", player))
						.format(new Object[] { world }));
	}
	
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void blockDamage(PlayerInteractEvent event) {
		interactWithDoor(event, event.getPlayer(), MagicDoorsPlugin.getPlugin().getPlayerHelper().getItemInHand(event.getPlayer()), event.getClickedBlock());
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void blockDamage(BlockDamageEvent event) {
		interactWithDoor(event, event.getPlayer(), event.getItemInHand(), event.getBlock());
	}

	private void interactWithDoor(Cancellable event, HumanEntity player, ItemStack itemInHand, Block block) {

		if (itemInHand == null) {
			MagicDoorsPlugin.getPlugin().debugWarning("Not holding an item");
			return;
		}

		NBTItem nbtItem = new NBTItem(itemInHand);
		String nbtRecipeKey = nbtItem.getString(MagicDoorsPlugin.RECIPES_KEY);

		if (nbtRecipeKey == null || nbtRecipeKey.isEmpty()) {
			MagicDoorsPlugin.getPlugin().debugInfo("Not holding magic door item.'");
			return;
		}

		if (!nbtRecipeKey.equals(rootDoorCopyRecipe()) && !nbtRecipeKey.equals(doorKeyRecipe())
				&& !nbtRecipeKey.equals(skeletonKeyRecipe())) {
			String notHoldingMessage = "Not holding an item which has the recipe key: ";
			MagicDoorsPlugin.getPlugin().debugWarning(notHoldingMessage + rootDoorCopyRecipe());
			MagicDoorsPlugin.getPlugin().debugWarning(notHoldingMessage + doorKeyRecipe());
			MagicDoorsPlugin.getPlugin().debugWarning(notHoldingMessage + skeletonKeyRecipe());
			return;
		}

		boolean isSkeletonKey = nbtRecipeKey.equals(skeletonKeyRecipe());
		boolean isKey = isSkeletonKey || nbtRecipeKey.startsWith(doorKeyRecipe());
		boolean isPlainKey = nbtRecipeKey.equals(doorKeyRecipe());
		boolean isRootDoorCopy = nbtRecipeKey.equals(rootDoorCopyRecipe());

		if (!isRootDoorCopy && !isKey) {
			MagicDoorsPlugin.getPlugin().debugInfo("Not a magic door related object in hand");
			return;
		}

		initDimentionItemCount();

		if (dimentionItemCount == null || dimentionItemCount.getItemsInDimentionAtValue() == null
				|| dimentionItemCount.getItemsInDimentionAtValue().isEmpty()) {
			MagicDoorsPlugin.getPlugin().debugInfo("no dimention values recorded");
			return;
		}

		Map<String, Set<String>> matchesMap = new HashMap<String, Set<String>>();

		String lastDimention = null;
		for (String dimention : getDimentionList()) {
			if (!dimentionItemCount.getItemsInDimentionAtValue().containsKey(dimention)
					|| dimentionItemCount.getItemsInDimentionAtValue().get(dimention) == null
					|| dimentionItemCount.getItemsInDimentionAtValue().get(dimention).isEmpty()) {
				MagicDoorsPlugin.getPlugin().debugInfo("no " + dimention + " values recorded");
				return;
			}

			int coordValue = dimention.equals("x") ? block.getX()
					: (dimention.equals("y") ? block.getY() : block.getZ());

			if (!dimentionItemCount.getItemsInDimentionAtValue().get(dimention).containsKey(coordValue)
					|| dimentionItemCount.getItemsInDimentionAtValue().get(dimention).get(coordValue).isEmpty()) {
				MagicDoorsPlugin.getPlugin().debugInfo("no matching " + dimention + " value");
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
			MagicDoorsPlugin.getPlugin()
					.debugWarning("No match was found but we made it all the way through processing");
			return;
		}

		int index = 0;
		if (exactMatches.size() > 1) {
			MagicDoorsPlugin.getPlugin().debugInfo("There was more than one match found after processing");
			index = random.nextInt(exactMatches.size());
		}

		String match = exactMatches.get(index);

		initMagicDoorRepo();

		if (magicDoorRepo == null) {
			MagicDoorsPlugin.getPlugin().debugWarning("Failed to load magic-doors repo.");
			return;
		}

		if (!magicDoorRepo.getMap().containsKey(match)) {
			MagicDoorsPlugin.getPlugin().debugWarning("Failed to load door from magic-doors repo.");
			return;
		}

		MagicDoor magicDoor = magicDoorRepo.getMap().get(match);
		if (magicDoor == null) {
			MagicDoorsPlugin.getPlugin().debugWarning("Failed to load door from magic-doors repo.");
			return;
		}

		if (isRootDoorCopy && magicDoor.getParentId() == null) {
			ItemMeta meta = itemInHand.getItemMeta();
			
			List<String> lore = meta.getLore();
			
			if(lore != null){
				lore = lore.stream().filter(l -> l != null && !InvisibleLoreHelper.convertToVisibleString(l).contains(PARENT_ID_HIDDEN_LORE_PREFIX)).collect(Collectors.toList());
			}
			
			if(lore == null){
				lore = new ArrayList<>();
			}
			
			lore.add(InvisibleLoreHelper.convertToInvisibleString(PARENT_ID_HIDDEN_LORE_PREFIX) + magicDoor.getId());
			meta.setLore(lore);
			
			itemInHand.setItemMeta(meta);
			ItemStack rootDoorCopyItem = MagicDoorsPlugin.getPlugin().recipeRegistrar()
					.getItemFromRecipe(rootDoorCopyRecipe(), player, null, 1);
			String rootDoorCopyName = rootDoorCopyItem != null && rootDoorCopyItem.getItemMeta() != null
					&& rootDoorCopyItem.getItemMeta().getDisplayName() != null
							? rootDoorCopyItem.getItemMeta().getDisplayName() : "√RootDoorCopy";
			player.sendMessage(ChatColor.GREEN + String.format(MagicDoorsPlugin.getPlugin()
					.getLocalizedMessage("magic-doors-root-door-copy-imprinted", player), rootDoorCopyName));
			return;
		}

		if (magicDoor.getParentId() == null && isKey) {
			
			// the door owner is attempting to name some keys
			if (isPlainKey && magicDoor.getOwnerName() != null) {
				if (player.getName().equals(magicDoor.getOwnerName())) {
					ItemMeta plainKeyMeta = itemInHand.getItemMeta();
					
					List<String> lore = plainKeyMeta.getLore();
					if(lore == null){
						lore = new ArrayList<>();
					}
					
					String parentId = getParentIdFromLore(lore);
					
					if(parentId == null){
						lore.add(InvisibleLoreHelper.convertToInvisibleString(PARENT_ID_HIDDEN_LORE_PREFIX) + magicDoor.getId());
						plainKeyMeta.setLore(lore);
						
						itemInHand.setItemMeta(plainKeyMeta);
						player.sendMessage(ChatColor.GREEN + String.format(MagicDoorsPlugin.getPlugin()
								.getLocalizedMessage("magic-doors-key-imprinted", player), magicDoor.getId()));
						return;
					}
					
				}
			}

			if (magicDoor.getChildren() == null || magicDoor.getChildren().isEmpty()) {
				// This is a magic door with no children
				player.sendMessage(
								ChatColor.RED + String.format(
										MagicDoorsPlugin.getPlugin().getLocalizedMessage(
												"magic-doors-root-has-no-children", player),
										magicDoor.getId()));
				return;
			}

			if (magicDoor.getOwnerName() != null) {

				if (isSkeletonKey && magicDoor.getOwnerName().equals(player.getName())) {
					// this is the current player's door and they are using
					// their skeleton key
				} else {
					
					List<String> lore = itemInHand.getItemMeta().getLore();
					if(lore == null){
						lore = new ArrayList<>();
					}
					
					String parentId = getParentIdFromLore(lore);
					
					if(parentId == null || !parentId.equalsIgnoreCase(magicDoor.getId())){
						sendNamedKeyRequiredMessage(player, magicDoor.getId());
						return;
					}
				}
			}

			// this is a root door, get children and teleport randomly
			int childDoorNumber = random.nextInt(magicDoor.getChildren().size());
			String childId = magicDoor.getChildren().get(childDoorNumber);
			MagicDoor childDoor = magicDoorRepo.getMap().get(childId);

			if (childDoor == null) {
				player.sendMessage(ChatColor.RED + new MessageFormat(MagicDoorsPlugin.getPlugin()
						.getLocalizedMessage("magic-doors-failed-to-find-child-door-number", player)).format(
								new Object[] { (childDoorNumber + 1) + "", magicDoor.getChildren().size() + "" }));
				return;
			}
			World world = MagicDoorsPlugin.getPlugin().getServer().getWorld(childDoor.getWorld());

			if (world == null) {
				sendMissingWorldMessage(player, childDoor.getWorld());
				return;
			}

			Location destination = new Location(world, childDoor.getPlayerX() + 0.0, childDoor.getPlayerY() + 0.0,
					childDoor.getPlayerZ() + 0.0);
			player.teleport(destination);

			player.sendMessage(new MessageFormat(MagicDoorsPlugin.getPlugin()
							.getLocalizedMessage("magic-doors-you-have-been-teleported-to-child", player))
									.format(new Object[] { (childDoorNumber + 1) + "", magicDoor.getId() }));
		} else if (magicDoor.getParentId() != null && isKey) {
			// this is a child, teleport to parent

			MagicDoor parentDoor = magicDoorRepo.getMap().get(magicDoor.getParentId());

			if (parentDoor == null) {
				player.sendMessage(ChatColor.RED + String.format(
								MagicDoorsPlugin.getPlugin().getLocalizedMessage(
										"magic-doors-failed-to-find-parent-door-id", player),
								magicDoor.getParentId()));
				return;
			}

			if (parentDoor.getOwnerName() != null) {
				if (isSkeletonKey && parentDoor.getOwnerName().equals(player.getName())) {
					// this is the current player's door and they are using
					// their skeleton key
				} else {
					String parentId = getParentIdFromLore(itemInHand.getItemMeta().getLore());
					
					if(parentId == null || !parentId.equalsIgnoreCase(parentDoor.getId())){
						sendNamedKeyRequiredMessage(player, parentDoor.getId());
						return;
					}
				}
			}

			World world = MagicDoorsPlugin.getPlugin().getServer().getWorld(parentDoor.getWorld());

			if (world == null) {
				sendMissingWorldMessage(player, parentDoor.getWorld());
				return;
			}

			Location destination = new Location(world, parentDoor.getPlayerX() + 0.0, parentDoor.getPlayerY() + 0.0,
					parentDoor.getPlayerZ() + 0.0);
			player.teleport(destination);

			player.sendMessage(new MessageFormat(MagicDoorsPlugin.getPlugin()
							.getLocalizedMessage("magic-doors-you-have-been-teleported-to-root", player))
									.format(new Object[] { magicDoor.getParentId() }));
		}
	}

	////////////////////////////////////
	// PRIVATE HELPERS///////////////////
	////////////////////////////////////

	private void initMagicDoorRepo() {
		if (magicDoorRepo == null) {
			magicDoorRepo = MagicDoorsPlugin.getPlugin().getTypeData(DATA_KEY_MAGIC_DOORS, MagicDoorRepo.class);

			if (magicDoorRepo == null || magicDoorRepo.getMap() == null) {
				magicDoorRepo = new MagicDoorRepo();
				magicDoorRepo.setMap(new HashMap<String, MagicDoor>());
			}
			updateMagicDoorRepo();
		}
	}

	private void updateMagicDoorRepo() {
		MagicDoorsPlugin.getPlugin().setData(DATA_KEY_MAGIC_DOORS, magicDoorRepo, true);
	}

	private void initDimentionItemCount() {
		if (dimentionItemCount == null) {
			dimentionItemCount = MagicDoorsPlugin.getPlugin().getTypeData(DATA_KEY_MAGIC_DOOR_DIMENTION_MAP,
					DimentionItemCount.class);
			if (dimentionItemCount == null || dimentionItemCount.getItemsInDimentionAtValue() == null) {
				dimentionItemCount = new DimentionItemCount();
				dimentionItemCount.setItemsInDimentionAtValue(new HashMap<String, Map<Integer, Set<String>>>());
			}
			updateDimentionItemCount();
		}
	}

	private void updateDimentionItemCount() {
		MagicDoorsPlugin.getPlugin().setData(DATA_KEY_MAGIC_DOOR_DIMENTION_MAP, dimentionItemCount, true);
	}

	private List<String> getDimentionList() {
		List<String> dimentions = new ArrayList<String>();
		dimentions.add("x");
		dimentions.add("y");
		dimentions.add("z");
		return dimentions;
	}

	private boolean addDoor(MagicDoor magicDoor) {
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
			MagicDoorsPlugin.getPlugin().warning("Unexpected error while saving door: " + e.getMessage());
			return false;

		}

		return true;
	}

	private String solveClash(String id, MagicDoorRepo repo, int start) {
		String newId = id + "-" + start;

		if (repo.getMap().containsKey(newId)) {
			return solveClash(id, repo, start + 1);
		}

		return newId;
	}

	private String getNewDoorId() {

		String doorId = null;

		if (doorPrefixes == null || doorPrefixes.isEmpty()) {
			MagicDoorsPlugin.getPlugin().debugWarning("doorPrefixes was null or empty");
		} else {
			doorId = doorPrefixes.get(random.nextInt(doorPrefixes.size()));
			MagicDoorsPlugin.getPlugin().debugInfo("prefix chosen: " + doorId);
		}

		if (doorSuffixes == null || doorSuffixes.isEmpty()) {
			MagicDoorsPlugin.getPlugin().debugWarning("doorSuffixes was null or empty");
		} else {
			String suffix = doorSuffixes.get(random.nextInt(doorSuffixes.size()));

			MagicDoorsPlugin.getPlugin().debugInfo("suffix chosen: " + doorId);

			if (doorId == null || doorId.isEmpty()) {
				doorId = suffix;
			} else {
				doorId = doorId + "-" + suffix;
			}
		}

		if (doorId != null && !doorId.isEmpty()) {
			MagicDoorsPlugin.getPlugin().debugInfo("door id generated: " + doorId);
		}

		return doorId;
	}

	////////////////////////////////////////////////////
	//////////////// MODEL CLASSES///////////////////////
	////////////////////////////////////////////////////

}
