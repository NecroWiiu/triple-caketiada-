package com.treasurebuilder.net;

import com.treasurebuilder.TreasureBuilder;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class Payloads {
    private Payloads() {}

    /** Cliente -> servidor: el jugador eligió un bloque en el menú. */
    public record Select(Identifier block) implements CustomPayload {
        public static final CustomPayload.Id<Select> ID = new CustomPayload.Id<>(TreasureBuilder.id("select_block"));
        public static final PacketCodec<RegistryByteBuf, Select> CODEC =
                PacketCodec.tuple(Identifier.PACKET_CODEC, Select::block, Select::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Cliente -> servidor: cambiar el paso de la herramienta (tecla V). */
    public record Cycle() implements CustomPayload {
        public static final CustomPayload.Id<Cycle> ID = new CustomPayload.Id<>(TreasureBuilder.id("cycle_step"));
        public static final PacketCodec<RegistryByteBuf, Cycle> CODEC = PacketCodec.unit(new Cycle());

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Servidor -> cliente: bloque seleccionado actualmente. */
    public record Sync(Identifier block) implements CustomPayload {
        public static final CustomPayload.Id<Sync> ID = new CustomPayload.Id<>(TreasureBuilder.id("sync_selection"));
        public static final PacketCodec<RegistryByteBuf, Sync> CODEC =
                PacketCodec.tuple(Identifier.PACKET_CODEC, Sync::block, Sync::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void registerTypes() {
        PayloadTypeRegistry.playC2S().register(Select.ID, Select.CODEC);
        PayloadTypeRegistry.playC2S().register(Cycle.ID, Cycle.CODEC);
        PayloadTypeRegistry.playS2C().register(Sync.ID, Sync.CODEC);
    }
}
