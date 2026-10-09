package com.treasurebuilder;

import com.treasurebuilder.tool.PartToolItem;
import com.treasurebuilder.tool.PartToolItem.Kind;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    private ModItems() {}

    public static final List<Item> ALL = new ArrayList<>();

    public static final Item BUILD  = tool("build_tool", Kind.BUILD);
    public static final Item DELETE = tool("delete_tool", Kind.DELETE);
    public static final Item SCALE  = tool("scale_tool", Kind.SCALE);
    public static final Item ROTATE = tool("rotate_tool", Kind.ROTATE);
    public static final Item ANCHOR = tool("anchor_tool", Kind.ANCHOR);
    public static final Item GLASS  = tool("glass_tool", Kind.GLASS);

    public static final ItemGroup GROUP = Registry.register(
            Registries.ITEM_GROUP,
            TreasureBuilder.id("tools"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(BUILD))
                    .displayName(Text.translatable("itemGroup.treasurebuilder.tools"))
                    .entries((ctx, entries) -> {
                        for (Item i : ALL) entries.add(i);
                    })
                    .build());

    private static Item tool(String name, Kind kind) {
        Item item = Registry.register(Registries.ITEM, TreasureBuilder.id(name),
                new PartToolItem(new Item.Settings().maxCount(1), kind));
        ALL.add(item);
        return item;
    }

    public static void register() {}
}
