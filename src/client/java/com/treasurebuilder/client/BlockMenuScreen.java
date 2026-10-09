package com.treasurebuilder.client;

import com.treasurebuilder.net.Payloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Menú minimalista: buscador + cuadrícula de bloques. Clic = elegir. Rueda = desplazar. */
public class BlockMenuScreen extends Screen {
    private static final int COLS = 9;
    private static final int ROWS = 6;
    private static final int CELL = 20;

    private static List<Block> CACHE;

    private TextFieldWidget search;
    private List<Block> shown = List.of();
    private int scroll;
    private int px;
    private int py;

    public BlockMenuScreen() {
        super(Text.translatable("screen.treasurebuilder.blocks"));
    }

    /** Solo cubos completos y sólidos que se vean como un bloque normal. */
    private static List<Block> all() {
        if (CACHE == null) {
            List<Block> list = new ArrayList<>();
            for (Block b : Registries.BLOCK) {
                try {
                    if (b.asItem() == Items.AIR) continue;
                    BlockState s = b.getDefaultState();
                    if (s.getRenderType() != BlockRenderType.MODEL) continue;
                    if (s.hasBlockEntity()) continue;
                    if (!Block.isShapeFullCube(s.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN))) continue;
                    list.add(b);
                } catch (Exception ignored) {
                    // bloque raro: se omite
                }
            }
            list.sort(Comparator.comparing((Block b) -> b.getName().getString()));
            CACHE = list;
        }
        return CACHE;
    }

    @Override
    protected void init() {
        int w = COLS * CELL;
        px = (width - w) / 2;
        py = height / 2 - (ROWS * CELL) / 2 + 10;

        search = new TextFieldWidget(textRenderer, px, py - 24, w, 16, Text.empty());
        search.setPlaceholder(Text.translatable("screen.treasurebuilder.search"));
        search.setChangedListener(s -> refilter());
        addDrawableChild(search);
        setInitialFocus(search);
        refilter();
    }

    private void refilter() {
        String q = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        List<Block> out = new ArrayList<>();
        for (Block b : all()) {
            if (q.isEmpty()
                    || b.getName().getString().toLowerCase(Locale.ROOT).contains(q)
                    || Registries.BLOCK.getId(b).getPath().contains(q)) {
                out.add(b);
            }
        }
        shown = out;
        scroll = 0;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x80000000);
        int w = COLS * CELL;
        ctx.fill(px - 6, py - 32, px + w + 6, py + ROWS * CELL + 24, 0xC0101014);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        Identifier selectedId = TreasureBuilderClient.selected;
        Block hover = null;
        int start = scroll * COLS;
        for (int i = 0; i < COLS * ROWS; i++) {
            int idx = start + i;
            if (idx >= shown.size()) break;
            Block b = shown.get(idx);
            int x = px + (i % COLS) * CELL;
            int y = py + (i / COLS) * CELL;

            if (Registries.BLOCK.getId(b).equals(selectedId)) {
                ctx.fill(x, y, x + CELL, y + CELL, 0x66FFFFFF);
            }
            if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) {
                ctx.fill(x, y, x + CELL, y + CELL, 0x33FFFFFF);
                hover = b;
            }
            ctx.drawItem(new ItemStack(b), x + 2, y + 2);
        }

        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.treasurebuilder.hint"),
                width / 2, py + ROWS * CELL + 8, 0xAAAAAA);
        if (hover != null) ctx.drawTooltip(textRenderer, hover.getName(), mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalRows = (shown.size() + COLS - 1) / COLS;
        int maxScroll = Math.max(0, totalRows - ROWS);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(verticalAmount)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;

        int cx = (int) Math.floor((mouseX - px) / CELL);
        int cy = (int) Math.floor((mouseY - py) / CELL);
        if (cx < 0 || cx >= COLS || cy < 0 || cy >= ROWS) return false;

        int idx = scroll * COLS + cy * COLS + cx;
        if (idx < shown.size()) {
            Identifier id = Registries.BLOCK.getId(shown.get(idx));
            TreasureBuilderClient.selected = id;
            ClientPlayNetworking.send(new Payloads.Select(id));
            close();
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
