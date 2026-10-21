package com.godze.fischfishing;

import net.minecraftforge.fml.common.Mod;

@Mod(FischMod.MODID)
public class FischMod {
    public static final String MODID = "fischfishing";

    public FischMod() {
        Network.register();
    }
}