package de.maexle.skyblockcounter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SkyblockCounterConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/skyblockcounter.json");

    private int hudX = 10;
    private int hudY = 10;
    private String lastMobId = "zealot_enderman_55";
    private String lastMobName = "Zealot";
    public List<MobEntry> mobEntries = new ArrayList<>();

    private String API_KEY = "";
    private String undashedUuid = "";

    public static class MobEntry {
        public String id;
        public String name;
        public String texture;

        public MobEntry(String id, String name, String texture) {
            this.id = id;
            this.name = name;
            this.texture = texture;
        }
    }

    public static SkyblockCounterConfig load() {
        if (!CONFIG_FILE.exists()) {
            return new SkyblockCounterConfig();
        }
        
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            SkyblockCounterConfig config = new SkyblockCounterConfig();
            
            if (json.has("hudX")) config.hudX = json.get("hudX").getAsInt();
            if (json.has("hudY")) config.hudY = json.get("hudY").getAsInt();
            if (json.has("lastMobId")) config.lastMobId = json.get("lastMobId").getAsString();
            if (json.has("lastMobName")) config.lastMobName = json.get("lastMobName").getAsString();
            if (json.has("API_KEY")) config.API_KEY = json.get("API_KEY").getAsString();
            if (json.has("undashedUuid")) config.undashedUuid = json.get("undashedUuid").getAsString();
            if (json.has("mobEntries")) {
                JsonArray entriesArray = json.getAsJsonArray("mobEntries");
                for (int i = 0; i < entriesArray.size(); i++) {
                    JsonObject entryObj = entriesArray.get(i).getAsJsonObject();
                    String id = entryObj.get("id").getAsString();
                    String name = entryObj.get("name").getAsString();
                    String texture = entryObj.has("texture") ? entryObj.get("texture").getAsString() : null;
                    config.mobEntries.add(new MobEntry(id, name, texture));
                }
            }
            
            // Default mobs if list is empty
            if (config.mobEntries.isEmpty()) {
                config.mobEntries.add(new MobEntry("treasure_hoarder_70", "Treasure Hoarder", "textures/gui/sprites/treasure_hoarder_head.png"));
                config.mobEntries.add(new MobEntry("team_treasurite_corleone_200", "Corleonite Boss", "textures/gui/sprites/boss_corleone_head.png"));
                config.mobEntries.add(new MobEntry("zealot_enderman_55", "Zealot", "textures/gui/sprites/zealot_enderman_head.png"));
            }

            return config;
        } catch (IOException e) {
            e.printStackTrace();
            return new SkyblockCounterConfig();
        }
    }
    
    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            JsonObject json = new JsonObject();
            json.addProperty("hudX", hudX);
            json.addProperty("hudY", hudY);
            json.addProperty("lastMobId", lastMobId);
            json.addProperty("lastMobName", lastMobName);

            json.addProperty("API_KEY", API_KEY);
            json.addProperty("undashedUuid", undashedUuid);

            JsonArray entriesArray = new JsonArray();
            for (MobEntry entry : mobEntries) {
                JsonObject entryObj = new JsonObject();
                entryObj.addProperty("id", entry.id);
                entryObj.addProperty("name", entry.name);
                if (entry.texture != null) {
                    entryObj.addProperty("texture", entry.texture);
                }
                entriesArray.add(entryObj);
            }
            json.add("mobEntries", entriesArray);
            
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public int getHudX() {
        return hudX;
    }
    
    public void setHudX(int hudX) {
        this.hudX = hudX;
    }
    
    public int getHudY() {
        return hudY;
    }
    
    public void setHudY(int hudY) {
        this.hudY = hudY;
    }
    
    public String getLastMobId() {
        return lastMobId;
    }
    
    public void setLastMobId(String lastMobId) {
        this.lastMobId = lastMobId;
    }
    
    public String getAPI_KEY() {
        return API_KEY;
    }
    
    public void setAPI_KEY(String API_KEYY) {
        this.API_KEY = API_KEYY;
    }

    public String getundashedUuid() {
        return undashedUuid;
    }

    public void setundashedUuid(String undashedUuidd) {
        this.undashedUuid = undashedUuidd;
    }

    public String getLastMobName() {
        return lastMobName;
    }

    public void setLastMobName(String lastMobName) {
        this.lastMobName = lastMobName;
    }
    
    public List<MobEntry> getMobEntries() {
        return mobEntries;
    }
    
    public void addMobEntry(String id, String name, String texture) {
        mobEntries.add(new MobEntry(id, name, texture));
    }
    
    public void removeMobEntry(String id) {
        mobEntries.removeIf(entry -> entry.id.equals(id));
    }
}
