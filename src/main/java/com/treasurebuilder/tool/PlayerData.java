package com.treasurebuilder.tool;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Ajustes por jugador (solo en memoria del servidor). */
public final class PlayerData {
    public static final int[] SCALE_STEPS = {1, 2, 4, 8, 16, 32};   // píxeles por clic
    public static final int[] ROT_STEPS = {1, 5, 15, 45, 90};       // grados por clic
    public static final int[] ALPHA_STEPS = {5, 10, 25};            // % de opacidad por clic

    private static final Map<UUID, PlayerData> MAP = new HashMap<>();

    public Block selected = Blocks.STONE;
    public int scaleIdx = 0;
    public int rotIdx = 2;
    public int alphaIdx = 1;

    public static PlayerData get(PlayerEntity p) {
        return MAP.computeIfAbsent(p.getUuid(), k -> new PlayerData());
    }

    public static void remove(UUID id) {
        MAP.remove(id);
    }
}
