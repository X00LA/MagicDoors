package com.blocktyper.magicdoors.data;

import java.util.Map;
import java.util.Set;

public class DimentionItemCount {
	
	public DimentionItemCount(){
		name = "tony";
	}
	
	private Map<String, Map<Integer, Set<String>>> itemsInDimentionAtValue;
	private String name;

	public Map<String, Map<Integer, Set<String>>> getItemsInDimentionAtValue() {
		return itemsInDimentionAtValue;
	}

	public void setItemsInDimentionAtValue(Map<String, Map<Integer, Set<String>>> itemsInDimentionAtValue) {
		this.itemsInDimentionAtValue = itemsInDimentionAtValue;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
	
	

}
