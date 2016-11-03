package com.blocktyper.magicdoors.data;

import java.util.Map;

public class MagicDoor {
	private String id;
	private String parentId;
	private Map<Integer, String> children;
	private String world;
	private int x;
	private int y;
	private int z;
	private int playerX;
	private int playerY;
	private int playerZ;
	private String ownerName;
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	public Map<Integer, String> getChildren() {
		return children;
	}
	public void setChildren(Map<Integer, String> children) {
		this.children = children;
	}
	public String getWorld() {
		return world;
	}
	public void setWorld(String world) {
		this.world = world;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getZ() {
		return z;
	}
	public void setZ(int z) {
		this.z = z;
	}
	public int getPlayerX() {
		return playerX;
	}
	public void setPlayerX(int playerX) {
		this.playerX = playerX;
	}
	public int getPlayerY() {
		return playerY;
	}
	public void setPlayerY(int playerY) {
		this.playerY = playerY;
	}
	public int getPlayerZ() {
		return playerZ;
	}
	public void setPlayerZ(int playerZ) {
		this.playerZ = playerZ;
	}
	public String getOwnerName() {
		return ownerName;
	}
	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}
	
	
	
}
