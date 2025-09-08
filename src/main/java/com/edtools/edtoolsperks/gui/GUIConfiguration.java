package com.edtools.edtoolsperks.gui;

import java.util.List;
import java.util.Map;

public class GUIConfiguration {
    
    private final String title;
    private final int size;
    private final Map<String, GUIItem> items;
    private final Map<String, Object> settings;
    
    public GUIConfiguration(String title, int size, Map<String, GUIItem> items, Map<String, Object> settings) {
        this.title = title;
        this.size = size;
        this.items = items;
        this.settings = settings;
    }
    
    public String getTitle() {
        return title;
    }
    
    public int getSize() {
        return size;
    }
    
    public Map<String, GUIItem> getItems() {
        return items;
    }
    
    public GUIItem getItem(String key) {
        return items.get(key);
    }
    
    public Object getSetting(String key) {
        return settings.getOrDefault(key, null);
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getStringListSetting(String key) {
        Object value = settings.get(key);
        if (value instanceof List<?>) {
            return (List<String>) value;
        }
        return List.of();
    }
    
    public String getStringSetting(String key) {
        Object value = settings.get(key);
        return value != null ? value.toString() : "";
    }
}