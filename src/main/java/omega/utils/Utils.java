package omega.utils;

import arc.files.Fi;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import mindustry.game.Team;
import mindustry.maps.Map;
import mindustry.maps.MapException;
import mindustry.net.WorldReloader;
import mindustry.type.UnitType;
import mindustry.world.Block;

import static mindustry.Vars.*;

public class Utils {

    // region common

    public static int voteChoice(String vote) {
        return switch (stripFooCharacters(vote.toLowerCase())) {
            case "y" -> 1;
            case "n" -> -1;
            default -> 0;
        };
    }

    public static <T> T notNullElse(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    // endregion
    // region available

    public static Seq<Map> getAvailableMaps() {
        return maps.customMaps().isEmpty() ? maps.defaultMaps() : maps.customMaps();
    }

    public static Seq<Fi> getAvailableSaves() {
        return saveDirectory.seq().filter(fi -> fi.extEquals(saveExtension));
    }

    public static boolean isAvailable(UnitType type) {
        return type != null && !type.internal && !state.rules.isBanned(type) && type.supportsEnv(state.rules.env);
    }

    public static boolean isAvailable(Block block) {
        return block != null && block.inEditor && !state.rules.isBanned(block) && block.supportsEnv(state.rules.env);
    }

    // region strings

    public static String coloredTeam(Team team) {
        return "[#" + team.color + "]" + team.emoji + team.name + "[]";
    }

    public static String stripAll(String text) {
        return Strings.stripColors(Strings.stripGlyphs(text));
    }

    public static String stripDiscord(String text) {
        return stripFooCharacters(text).replace("`", "");
    }

    public static String stripFooCharacters(String text) {
        var builder = new StringBuilder(text);

        for (int i = text.length() - 1; i >= 0; i--)
            if (builder.charAt(i) >= 0xF80 && builder.charAt(i) <= 0x107F)
                builder.deleteCharAt(i);

        return builder.toString();
    }

    public static boolean deepEquals(String first, String second) {
        first = stripAll(first);
        second = stripAll(second);
        return first.equalsIgnoreCase(second) || first.toLowerCase().contains(second.toLowerCase());
    }

    // endregion
    // region world

    public static void reloadWorld(Runnable runnable) {
        try {
            var reloader = new WorldReloader();
            reloader.begin();

            runnable.run();
            state.rules = state.map.applyRules(state.rules.mode());
            logic.play();

            reloader.end();
        } catch (MapException e) {
            Log.err("@: @", e.map.name(), e.getMessage());
        }
    }

    // endregion
}