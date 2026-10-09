package com.treasurebuilder.tool;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.treasurebuilder.ModItems;
import com.treasurebuilder.entity.BuildPartEntity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public final class ToolCommands {
    private ToolCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // /tbkit -> te da todas las herramientas
            dispatcher.register(CommandManager.literal("tbkit")
                    .requires(src -> src.hasPermissionLevel(2))
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                        for (Item i : ModItems.ALL) p.getInventory().offerOrDrop(new ItemStack(i));
                        return 1;
                    }));

            // /tbclear <radio> -> borra todas las piezas alrededor
            dispatcher.register(CommandManager.literal("tbclear")
                    .requires(src -> src.hasPermissionLevel(2))
                    .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 512))
                            .executes(ctx -> {
                                ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                int r = IntegerArgumentType.getInteger(ctx, "radius");
                                List<BuildPartEntity> list = p.getServerWorld()
                                        .getEntitiesByClass(BuildPartEntity.class, p.getBoundingBox().expand(r), e -> true);
                                for (Entity e : list) e.discard();
                                int n = list.size();
                                ctx.getSource().sendFeedback(() -> Text.translatable("msg.treasurebuilder.cleared", n), false);
                                return n;
                            })));
        });
    }
}
