package com.godze.fischfishing;

import java.util.ArrayList;
import java.util.List;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/** Turns the minigame off when another mod that replaces fishing is installed. */
public class Compat {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Mod ids of fishing overhaul mods. Check the real ids in the game's Mods screen
    // (or the mods.toml inside each jar) and edit this list.
    private static final List<String> CONFLICTING_MOD_IDS = List.of(
            "aquaculture",
            "tide",
            "starcatcher",
            "fishingfrenzy"
    );

    private static Boolean conflict = null;

    public static boolean hasConflict() {
        if (conflict == null) {
            List<String> found = new ArrayList<>();
            for (String id : CONFLICTING_MOD_IDS) {
                if (ModList.get().isLoaded(id)) found.add(id);
            }
            conflict = !found.isEmpty();
            if (conflict) {
                LOGGER.warn("Fisch Fishing minigame disabled because these fishing mods are loaded: {}", found);
            }
        }
        return conflict;
    }
}