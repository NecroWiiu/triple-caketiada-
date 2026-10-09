package com.treasurebuilder.net;

import com.treasurebuilder.entity.BuildPartEntity;
import com.treasurebuilder.tool.PartToolItem;
import com.treasurebuilder.tool.PlayerData;
import com.treasurebuilder.tool.ToolActions;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public final class ServerHandlers {
    private ServerHandlers() {}

    public static void register() {
        // Menú de bloques -> selección
        ServerPlayNetworking.registerGlobalReceiver(Payloads.Select.ID, (payload, ctx) -> {
            ServerPlayerEntity p = ctx.player();
            Block b = Registries.BLOCK.get(payload.block());
            if (b == Blocks.AIR || b.asItem() == Items.AIR) return;
            PlayerData.get(p).selected = b;
        });

        // Tecla V -> cambiar paso
        ServerPlayNetworking.registerGlobalReceiver(Payloads.Cycle.ID, (payload, ctx) ->
                ToolActions.cycleStep(ctx.player()));

        // Sincronizar al entrar / limpiar al salir
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ServerPlayNetworking.send(handler.player,
                        new Payloads.Sync(Registries.BLOCK.getId(PlayerData.get(handler.player).selected))));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                PlayerData.remove(handler.player.getUuid()));

        // Clic derecho sobre un bloque con una herramienta: usarla en vez de interactuar con el bloque
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            ItemStack s = player.getStackInHand(hand);
            if (player.isSpectator() || !(s.getItem() instanceof PartToolItem tool)) return ActionResult.PASS;
            if (!world.isClient && player instanceof ServerPlayerEntity sp) {
                ToolActions.run(sp, tool, tool.kind, true);
            }
            return ActionResult.SUCCESS;
        });

        // Clic izquierdo sobre un bloque con una herramienta: nunca romper bloques
        AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) ->
                player.getStackInHand(hand).getItem() instanceof PartToolItem ? ActionResult.FAIL : ActionResult.PASS);

        // Clic izquierdo sobre una pieza: el cliente deja pasar el paquete, el servidor actúa
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            ItemStack s = player.getStackInHand(hand);
            if (player.isSpectator() || !(entity instanceof BuildPartEntity) || !(s.getItem() instanceof PartToolItem tool)) {
                return ActionResult.PASS;
            }
            if (world.isClient) return ActionResult.PASS;
            if (player instanceof ServerPlayerEntity sp) ToolActions.run(sp, tool, tool.kind, false);
            return ActionResult.FAIL;
        });
    }
}
