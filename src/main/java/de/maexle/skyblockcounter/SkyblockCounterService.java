package de.maexle.skyblockcounter;

import org.json.JSONArray;
import org.json.JSONObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;

public class SkyblockCounterService {

    private static final Logger LOGGER = LoggerFactory.getLogger("skyblockcounter");

    private static volatile int currentKills = -1;
    private static volatile boolean guiVisible = false;
    private static volatile String currentMobId = "treasure_hoarder_70";
    private static volatile String currentMobName = "Treasure Hoarder";
    private static volatile String API_KEY = "";
    private static volatile String undashedUuid = "";
    private static volatile int hudX;
    private static volatile int hudY;
    private static volatile int startSessionKills = 0;
    private static volatile boolean showSessionKills = false;

    private static final Identifier TREASURE_HOARDER_HEAD = Identifier.of("skyblockcounter", "textures/gui/sprites/treasure_hoarder_head.png");
    private static final Identifier CORLEONE_HEAD = Identifier.of("skyblockcounter", "textures/gui/sprites/boss_corleone_head.png");
    private static final Identifier ZEALOT_HEAD = Identifier.of("skyblockcounter", "textures/gui/sprites/zealot_enderman_head.png");
    private static final int HEAD_SIZE = 16;

    List<SkyblockCounterConfig.MobEntry> entries = SkyblockCounterService.getConfig().getMobEntries();

    private long lastUnloadTrigger = 0;

    private static final Map<String, Identifier> MOB_TEXTURES = new HashMap<>();
    private static SkyblockCounterConfig config;

    static {
        config = SkyblockCounterConfig.load();
        hudX = config.getHudX();
        API_KEY = config.getAPI_KEY();
        undashedUuid = config.getundashedUuid();
        hudY = config.getHudY();
        currentMobId = config.getLastMobId();
        currentMobName = config.getLastMobName();
        
        // Build MOB_TEXTURES from config entries
        for (SkyblockCounterConfig.MobEntry entry : config.getMobEntries()) {
            if (entry.texture != null) {
                MOB_TEXTURES.put(entry.id, Identifier.of("skyblockcounter", entry.texture));
            }
        }
    }
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("SykblockCounter-Timer");
        return thread;
    });

    public void startEventTracking() {
        LOGGER.info("BestiaryService initialisiert (Rein Clientseitig)");
        LOGGER.info("Config geladen: HUD Position (" + hudX + ", " + hudY + "), Mob: " + currentMobName);

        registerHudRenderer();

        // Angepasster Event-Listener
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity.getCustomName() != null) {
                String name = entity.getCustomName().getString();

                if (name.contains("Corleone") || name.contains("Treasure Hoarder")) {
                    long now = System.currentTimeMillis();
                    // 2 Sekunden Cooldown, damit nicht mehrere Trigger gleichzeitig feuern
                    if (now - lastUnloadTrigger > 2000) {
                        lastUnloadTrigger = now;
                        LOGGER.info("[DEBUG] " + name + " aus der Welt geladen! API-Update in 1 Sekunde...");

                        // Führe das API-Update verzögert nach genau 1 Sekunde aus
                        scheduler.schedule(this::fetchCurrentBestiaryKills, 1, TimeUnit.SECONDS);
                    }
                }
            }
        });

        // Alle 30 Sekunden standardmäßig abfragen
        scheduler.scheduleAtFixedRate(this::fetchCurrentBestiaryKills, 0, 30, TimeUnit.SECONDS);
    }

    private void fetchCurrentBestiaryKills() {
        retrieveBestiaryKillsAsync(undashedUuid, currentMobId)
                .thenAccept(kills -> {
                    if (kills >= 0) {
                        updateGameInterface(kills);
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.error("Fehler bei automatischer Bestiary-Abfrage: " + ex.getMessage());
                    return null;
                });
    }

    private void registerHudRenderer() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (guiVisible) {
                renderHud(drawContext);
            }
        });
    }

    private void renderHud(DrawContext drawContext) {
        if (entries.isEmpty()) {
            SkyblockCounterConfig config = SkyblockCounterService.getConfig();
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("treasure_hoarder_70", "Treasure Hoarder", "textures/gui/sprites/treasure_hoarder_head.png"));
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("team_treasurite_corleone_200", "Corleonite Boss", "textures/gui/sprites/boss_corleone_head.png"));
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("zealot_enderman_55", "Zealot", "textures/gui/sprites/zealot_enderman_head.png"));
            config.save();
            SkyblockCounterService.reloadMobTextures();
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null || mc.options.hudHidden) {
            return;
        }

        int xPos = hudX;
        int yPos = hudY;
        int displayKills = showSessionKills ? (currentKills - startSessionKills) : currentKills;
        String text = /*currentMobName + " Kills: " +*/ "" + displayKills;

        Identifier headTexture = MOB_TEXTURES.getOrDefault(currentMobId, TREASURE_HOARDER_HEAD);

        drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, headTexture, xPos, yPos, 0, 0, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE);

        int textX = xPos + HEAD_SIZE + 4;
        drawContext.drawTextWithShadow(mc.textRenderer, text, textX, yPos + (HEAD_SIZE / 4), 0xFFFFFFFF);
    }

    private static CompletableFuture<Integer> retrieveBestiaryKillsAsync(String uuid, String mobId) {
        return CompletableFuture.supplyAsync(() -> {
            String spec = "https://api.hypixel.net/v2/skyblock/profiles?uuid=" + uuid;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(spec);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("API-Key", API_KEY);
                LOGGER.info("[DEBUG] Hypixel API Key: " + API_KEY);

                int responseCode = connection.getResponseCode();
                LOGGER.info("[DEBUG] Hypixel API Response Code: " + responseCode);

                if (responseCode == 200) {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }

                        String jsonRaw = response.toString();

                        JSONObject data = new JSONObject(jsonRaw);
                        if (!data.optBoolean("success", false)) {
                            LOGGER.warn("[DEBUG] API-Aufruf war laut JSON 'success: false'.");
                            return -1;
                        }

                        JSONArray profiles = data.optJSONArray("profiles");
                        if (profiles == null || profiles.length() == 0) {
                            LOGGER.warn("[DEBUG] Keine Profile ('profiles') im JSON gefunden oder Array leer.");
                            return -1;
                        }

                        String pureUuid = uuid.replace("-", "");

                        for (int i = 0; i < profiles.length(); i++) {
                            JSONObject profile = profiles.getJSONObject(i);

                            if (profile.optBoolean("selected", false)) {
                                JSONObject members = profile.optJSONObject("members");
                                if (members != null) {
                                    String targetKey = members.has(pureUuid) ? pureUuid : (members.has(uuid) ? uuid : null);

                                    if (targetKey != null) {
                                        JSONObject memberData = members.getJSONObject(targetKey);
                                        JSONObject bestiary = memberData.optJSONObject("bestiary");
                                        if (bestiary != null) {
                                            JSONObject kills = bestiary.optJSONObject("kills");
                                            if (kills != null) {
                                                int mobKills = kills.optInt(mobId, 0);
                                                LOGGER.info("[DEBUG] Gefunden im selektierten Profil! Kills für " + mobId + ": " + mobKills);
                                                return mobKills;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        LOGGER.info("[DEBUG] Kein 'selected' Profil gefunden, versuche erstes Profil in der Liste...");
                        JSONObject firstProfile = profiles.getJSONObject(0);
                        JSONObject members = firstProfile.optJSONObject("members");
                        if (members != null) {
                            String targetKey = members.has(pureUuid) ? pureUuid : (members.has(uuid) ? uuid : null);
                            if (targetKey != null) {
                                JSONObject memberData = members.getJSONObject(targetKey);
                                JSONObject bestiary = memberData.optJSONObject("bestiary");
                                if (bestiary != null) {
                                    JSONObject kills = bestiary.optJSONObject("kills");
                                    if (kills != null) {
                                        int mobKills = kills.optInt(mobId, 0);
                                        LOGGER.info("[DEBUG] Gefunden im ersten Profil! Kills für " + mobId + ": " + mobKills);
                                        return mobKills;
                                    }
                                }
                            }
                        }
                        LOGGER.warn("[DEBUG] Struktur 'members -> uuid -> bestiary -> kills' wurde im JSON nicht wie erwartet gefunden.");
                    }
                } else {
                    try (BufferedReader err = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                        StringBuilder errResponse = new StringBuilder();
                        String line;
                        while ((line = err.readLine()) != null) {
                            errResponse.append(line);
                        }
                        LOGGER.error("[DEBUG] API Fehler-Antwort: " + errResponse.toString());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[DEBUG] Ausnahme beim API-Aufruf oder JSON-Parsing:", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return -1;
        });
    }

    private void updateGameInterface(int kills) {
        currentKills = kills;
        LOGGER.info("Aktuelle " + currentMobName + " Kills aktualisiert: " + kills);
    }

    public static boolean isGuiVisible() {
        return guiVisible;
    }

    public static void setGuiVisible(boolean visible) {
        guiVisible = visible;
    }

    public static int getCurrentKills() {
        return currentKills;
    }

    public static String getCurrentMobName() {
        return currentMobName;
    }

    public static int getHudX() {
        return hudX;
    }

    public static int getHudY() {
        return hudY;
    }

    public static void setHudPosition(int x, int y) {
        hudX = x;
        hudY = y;
        config.setHudX(x);
        config.setHudY(y);
        config.save();
        LOGGER.info("HUD Position geändert und gespeichert: X=" + x + ", Y=" + y);
    }

    public static boolean isShowSessionKills() {
        return showSessionKills;
    }

    public static void setShowSessionKills(boolean show) {
        if (show && !showSessionKills) {
            // Beim Aktivieren: Start-Kills auf aktuelle Kills setzen
            startSessionKills = currentKills;
            LOGGER.info("Session-Kills Modus aktiviert. Start-Kills: " + startSessionKills);
        } else if (!show) {
            LOGGER.info("Session-Kills Modus deaktiviert");
        }
        showSessionKills = show;
    }

    public static int getSessionKills() {
        return currentKills - startSessionKills;
    }

    public static void setStartSessionKills(int kills) {
        startSessionKills = kills;
        LOGGER.info("Start-Kills manuell gesetzt: " + kills);
    }

    public static void switchMob(String mobId, String mobName) {
        currentMobId = mobId;
        currentMobName = mobName;
        config.setLastMobId(mobId);
        config.setLastMobName(mobName);
        config.setAPI_KEY(config.getAPI_KEY());
        config.setundashedUuid(config.getundashedUuid());
        config.save();
        currentKills = -1;
        LOGGER.info("Switched to mob: " + mobName + " (" + mobId + ") und gespeichert");

        retrieveBestiaryKillsAsync(undashedUuid, mobId)
                .thenAccept(kills -> {
                    if (kills >= 0) {
                        currentKills = kills;
                        LOGGER.info("Updated " + mobName + " Kills: " + kills);
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.error("Fehler bei asynchroner Datenverarbeitung (switchMob): " + ex.getMessage());
                    return null;
                });
    }

    public static SkyblockCounterConfig getConfig() {
        return config;
    }

    public static void saveConfig() {
        config.save();
    }

    public static void reloadMobTextures() {
        MOB_TEXTURES.clear();
        for (SkyblockCounterConfig.MobEntry entry : config.getMobEntries()) {
            if (entry.texture != null) {
                MOB_TEXTURES.put(entry.id, Identifier.of("skyblockcounter", entry.texture));
            }
        }
        LOGGER.info("Mob-Textures neu geladen: " + MOB_TEXTURES.size() + " Mobs");
    }
}