package de.maexle.skyblockcounter.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.maexle.skyblockcounter.SkyblockCounterService;
import de.maexle.skyblockcounter.SkyblockCounterConfig;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class SkyblockCounterCommand {

    private static SkyblockCounterConfig config;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("skyCounter")
                .then(literal("treasure_hoarder")
                        .executes(SkyblockCounterCommand::switchToTreasureHoarder))
                .then(literal("corleonite_boss")
                        .executes(SkyblockCounterCommand::switchToCorleoniteBoss))
                .then(literal("zealot")
                        .executes(SkyblockCounterCommand::switchToZealot))
                .then(literal("list")
                        .executes(SkyblockCounterCommand::listMobs))
                .then(literal("add")
                        .then(argument("id", StringArgumentType.string())
                                .then(argument("name", StringArgumentType.string())
                                        .executes(SkyblockCounterCommand::addMob)
                                        .then(argument("texture", StringArgumentType.string())
                                                .executes(SkyblockCounterCommand::addMobWithTexture)))))
                .then(literal("remove")
                        .then(argument("id", StringArgumentType.string())
                                .executes(SkyblockCounterCommand::removeMob)))
                .then(literal("switch")
                        .then(argument("id", StringArgumentType.string())
                                .executes(SkyblockCounterCommand::switchToMob)))
                .then(literal("position")
                        .then(argument("x", IntegerArgumentType.integer())
                                .then(argument("y", IntegerArgumentType.integer())
                                        .executes(SkyblockCounterCommand::setHudPosition))))
                .then(literal("setAPI")
                        .then(argument("API key", StringArgumentType.string())
                            .executes(SkyblockCounterCommand::setAPI)))
                .then(literal("setUndashedUuid")
                        .then(argument("Undashed uuid", StringArgumentType.string())
                            .executes(SkyblockCounterCommand::setUndashedUuid)))
        );
    }

    private static int setAPI(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        String API_KEY = StringArgumentType.getString(context, "API key");
        config.setAPI_KEY(API_KEY);
        context.getSource().sendFeedback(Text.literal("API key was set"));
        config.save();
        return 1;

    }

    private static int setUndashedUuid(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        String undashedUuid = StringArgumentType.getString(context, "Undashed uuid");
        config.setundashedUuid(undashedUuid);
        context.getSource().sendFeedback(Text.literal("Undashed uuid was set"));
        config.save();
        return 1;

    }

    private static int switchToTreasureHoarder(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterService.switchMob("treasure_hoarder_70", "Treasure Hoarder");
        context.getSource().sendFeedback(Text.literal("Switched to Treasure Hoarder"));
        return 1;
    }

    private static int switchToCorleoniteBoss(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterService.switchMob("team_treasurite_corleone_200", "Corleonite Boss");
        context.getSource().sendFeedback(Text.literal("Switched to Corleonite Boss"));
        return 1;
    }

    private static int switchToZealot(CommandContext<FabricClientCommandSource> context) {
        SkyblockCounterService.switchMob("zealot_enderman_55", "Zealot");
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
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        config.addMobEntry(id, name, texture);
        config.save();
        SkyblockCounterService.reloadMobTextures();
        String textureInfo = texture != null ? " (Texture: " + texture + ")" : "";
        context.getSource().sendFeedback(Text.literal("Mob hinzugefügt: " + name + " (" + id + ")" + textureInfo));
        return 1;
    }

    private static int removeMob(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        config.removeMobEntry(id);
        config.save();
        SkyblockCounterService.reloadMobTextures();
        context.getSource().sendFeedback(Text.literal("Mob entfernt: " + id));
        return 1;
    }

    public static int listMobs(CommandContext<FabricClientCommandSource> context) {
        List<SkyblockCounterConfig.MobEntry> entries = SkyblockCounterService.getConfig().getMobEntries();
        if (entries.isEmpty()) {
            SkyblockCounterConfig config = SkyblockCounterService.getConfig();
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("treasure_hoarder_70", "Treasure Hoarder", "textures/gui/sprites/treasure_hoarder_head.png"));
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("team_treasurite_corleone_200", "Corleonite Boss", "textures/gui/sprites/boss_corleone_head.png"));
            config.mobEntries.add(new SkyblockCounterConfig.MobEntry("zealot_enderman_55", "Zealot", "textures/gui/sprites/zealot_enderman_head.png"));
            config.save();
            SkyblockCounterService.reloadMobTextures();
            return 1;
        }
        context.getSource().sendFeedback(Text.literal("Verfügbare Mobs:"));
        for (SkyblockCounterConfig.MobEntry entry : entries) {
            String textureInfo = entry.texture != null ? " [Texture: " + entry.texture + "]" : "";
            context.getSource().sendFeedback(Text.literal("- " + entry.name + " (" + entry.id + ")" + textureInfo));
        }
        return 1;
    }

    private static int switchToMob(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        SkyblockCounterConfig config = SkyblockCounterService.getConfig();
        SkyblockCounterConfig.MobEntry entry = config.getMobEntries().stream()
                .filter(e -> e.id.equals(id))
                .findFirst()
                .orElse(null);
        
        if (entry == null) {
            context.getSource().sendFeedback(Text.literal("Mob nicht gefunden: " + id));
            return 0;
        }
        
        SkyblockCounterService.switchMob(entry.id, entry.name);
        context.getSource().sendFeedback(Text.literal("Switched to " + entry.name));
        return 1;
    }

    private static int showCurrentMob(CommandContext<FabricClientCommandSource> context) {
        String currentMob = SkyblockCounterService.getCurrentMobName();
        int currentKills = SkyblockCounterService.getCurrentKills();
        context.getSource().sendFeedback(Text.literal("Current mob: " + currentMob + " (Kills: " + currentKills + ")"));
        return 1;
    }

    private static int setHudPosition(CommandContext<FabricClientCommandSource> context) {
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        SkyblockCounterService.setHudPosition(x, y);
        context.getSource().sendFeedback(Text.literal("HUD position set to X: " + x + ", Y: " + y));
        return 1;
    }

    private static int toggleSessionMode(CommandContext<FabricClientCommandSource> context) {
        boolean currentMode = SkyblockCounterService.isShowSessionKills();
        SkyblockCounterService.setShowSessionKills(!currentMode);
        String status = !currentMode ? "AN" : "AUS";
        context.getSource().sendFeedback(Text.literal("Session-Kills Modus: " + status));
        return 1;
    }

}
