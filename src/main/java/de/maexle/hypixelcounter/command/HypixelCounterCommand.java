package de.maexle.hypixelcounter.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.maexle.hypixelcounter.HypixelCounterService;
import de.maexle.hypixelcounter.HypixelCounterConfig;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class HypixelCounterCommand {

    private static HypixelCounterConfig config;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("hyCounter")
                .then(literal("treasure_hoarder")
                        .executes(HypixelCounterCommand::switchToTreasureHoarder))
                .then(literal("corleonite_boss")
                        .executes(HypixelCounterCommand::switchToCorleoniteBoss))
                .then(literal("zealot")
                        .executes(HypixelCounterCommand::switchToZealot))
                .then(literal("list")
                        .executes(HypixelCounterCommand::listMobs))
                .then(literal("add")
                        .then(argument("id", StringArgumentType.string())
                                .then(argument("name", StringArgumentType.string())
                                        .executes(HypixelCounterCommand::addMob)
                                        .then(argument("texture", StringArgumentType.string())
                                                .executes(HypixelCounterCommand::addMobWithTexture)))))
                .then(literal("remove")
                        .then(argument("id", StringArgumentType.string())
                                .executes(HypixelCounterCommand::removeMob)))
                .then(literal("switch")
                        .then(argument("id", StringArgumentType.string())
                                .executes(HypixelCounterCommand::switchToMob)))
                .then(literal("position")
                        .then(argument("x", IntegerArgumentType.integer())
                                .then(argument("y", IntegerArgumentType.integer())
                                        .executes(HypixelCounterCommand::setHudPosition))))
                .then(literal("setAPI")
                        .then(argument("API key", StringArgumentType.string())
                            .executes(HypixelCounterCommand::setAPI)))
                .then(literal("setUndashedUuid")
                        .then(argument("Undashed uuid", StringArgumentType.string())
                            .executes(HypixelCounterCommand::setUndashedUuid)))
        );
    }

    private static int setAPI(CommandContext<FabricClientCommandSource> context) {
        HypixelCounterConfig config = HypixelCounterService.getConfig();
        String API_KEY = StringArgumentType.getString(context, "API key");
        config.setAPI_KEY(API_KEY);
        context.getSource().sendFeedback(Text.literal("API key was set"));
        config.save();
        return 1;

    }

    private static int setUndashedUuid(CommandContext<FabricClientCommandSource> context) {
        HypixelCounterConfig config = HypixelCounterService.getConfig();
        String undashedUuid = StringArgumentType.getString(context, "Undashed uuid");
        config.setundashedUuid(undashedUuid);
        context.getSource().sendFeedback(Text.literal("Undashed uuid was set"));
        config.save();
        return 1;

    }

    private static int switchToTreasureHoarder(CommandContext<FabricClientCommandSource> context) {
        HypixelCounterService.switchMob("treasure_hoarder_70", "Treasure Hoarder");
        context.getSource().sendFeedback(Text.literal("Switched to Treasure Hoarder"));
        return 1;
    }

    private static int switchToCorleoniteBoss(CommandContext<FabricClientCommandSource> context) {
        HypixelCounterService.switchMob("team_treasurite_corleone_200", "Corleonite Boss");
        context.getSource().sendFeedback(Text.literal("Switched to Corleonite Boss"));
        return 1;
    }

    private static int switchToZealot(CommandContext<FabricClientCommandSource> context) {
        HypixelCounterService.switchMob("zealot_enderman_55", "Zealot");
        context.getSource().sendFeedback(Text.literal("Switched to Zealot Lv.55"));
        return 1;
    }

    private static int addMob(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        String name = StringArgumentType.getString(context, "name");
        return addMobEntry(context, id, name, null);
    }

    private static int addMobWithTexture(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        String name = StringArgumentType.getString(context, "name");
        String texture = StringArgumentType.getString(context, "texture");
        return addMobEntry(context, id, name, texture);
    }

    private static int addMobEntry(CommandContext<FabricClientCommandSource> context, String id, String name, String texture) {
        HypixelCounterConfig config = HypixelCounterService.getConfig();
        config.addMobEntry(id, name, texture);
        config.save();
        HypixelCounterService.reloadMobTextures();
        String textureInfo = texture != null ? " (Texture: " + texture + ")" : "";
        context.getSource().sendFeedback(Text.literal("Mob hinzugefügt: " + name + " (" + id + ")" + textureInfo));
        return 1;
    }

    private static int removeMob(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        HypixelCounterConfig config = HypixelCounterService.getConfig();
        config.removeMobEntry(id);
        config.save();
        HypixelCounterService.reloadMobTextures();
        context.getSource().sendFeedback(Text.literal("Mob entfernt: " + id));
        return 1;
    }

    public static int listMobs(CommandContext<FabricClientCommandSource> context) {
        List<HypixelCounterConfig.MobEntry> entries = HypixelCounterService.getConfig().getMobEntries();
        if (entries.isEmpty()) {
            HypixelCounterConfig config = HypixelCounterService.getConfig();
            config.mobEntries.add(new HypixelCounterConfig.MobEntry("treasure_hoarder_70", "Treasure Hoarder", "textures/gui/sprites/treasure_hoarder_head.png"));
            config.mobEntries.add(new HypixelCounterConfig.MobEntry("team_treasurite_corleone_200", "Corleonite Boss", "textures/gui/sprites/boss_corleone_head.png"));
            config.mobEntries.add(new HypixelCounterConfig.MobEntry("zealot_enderman_55", "Zealot", "textures/gui/sprites/zealot_enderman_head.png"));
            config.save();
            HypixelCounterService.reloadMobTextures();
            return 1;
        }
        context.getSource().sendFeedback(Text.literal("Verfügbare Mobs:"));
        for (HypixelCounterConfig.MobEntry entry : entries) {
            String textureInfo = entry.texture != null ? " [Texture: " + entry.texture + "]" : "";
            context.getSource().sendFeedback(Text.literal("- " + entry.name + " (" + entry.id + ")" + textureInfo));
        }
        return 1;
    }

    private static int switchToMob(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        HypixelCounterConfig config = HypixelCounterService.getConfig();
        HypixelCounterConfig.MobEntry entry = config.getMobEntries().stream()
                .filter(e -> e.id.equals(id))
                .findFirst()
                .orElse(null);
        
        if (entry == null) {
            context.getSource().sendFeedback(Text.literal("Mob nicht gefunden: " + id));
            return 0;
        }
        
        HypixelCounterService.switchMob(entry.id, entry.name);
        context.getSource().sendFeedback(Text.literal("Switched to " + entry.name));
        return 1;
    }

    private static int showCurrentMob(CommandContext<FabricClientCommandSource> context) {
        String currentMob = HypixelCounterService.getCurrentMobName();
        int currentKills = HypixelCounterService.getCurrentKills();
        context.getSource().sendFeedback(Text.literal("Current mob: " + currentMob + " (Kills: " + currentKills + ")"));
        return 1;
    }

    private static int setHudPosition(CommandContext<FabricClientCommandSource> context) {
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        HypixelCounterService.setHudPosition(x, y);
        context.getSource().sendFeedback(Text.literal("HUD position set to X: " + x + ", Y: " + y));
        return 1;
    }

    private static int toggleSessionMode(CommandContext<FabricClientCommandSource> context) {
        boolean currentMode = HypixelCounterService.isShowSessionKills();
        HypixelCounterService.setShowSessionKills(!currentMode);
        String status = !currentMode ? "AN" : "AUS";
        context.getSource().sendFeedback(Text.literal("Session-Kills Modus: " + status));
        return 1;
    }

}
