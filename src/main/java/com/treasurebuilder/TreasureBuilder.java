package com.treasurebuilder;

import com.treasurebuilder.net.Payloads;
import com.treasurebuilder.net.ServerHandlers;
import com.treasurebuilder.tool.ToolCommands;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class TreasureBuilder implements ModInitializer {
    public static final String MOD_ID = "treasurebuilder";

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModEntities.register();
        ModItems.register();
        Payloads.registerTypes();
        ServerHandlers.register();
        ToolCommands.register();
    }
}
