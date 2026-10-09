package com.treasurebuilder.tool;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;
import java.util.Locale;

public class PartToolItem extends Item {
    public enum Kind { BUILD, DELETE, SCALE, ROTATE, ANCHOR, GLASS }

    public final Kind kind;

    public PartToolItem(Settings settings, Kind kind) {
        super(settings);
        this.kind = kind;
    }

    /** Clic derecho en el aire / sobre una pieza (los bloques los gestiona UseBlockCallback). */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            ToolActions.run(sp, this, kind, true);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.treasurebuilder." + kind.name().toLowerCase(Locale.ROOT))
                .formatted(Formatting.GRAY));
    }
}
