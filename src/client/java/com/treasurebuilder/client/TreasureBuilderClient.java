package com.treasurebuilder.client;

import com.treasurebuilder.ModEntities;
import com.treasurebuilder.net.Payloads;
import com.treasurebuilder.tool.PartToolItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class TreasureBuilderClient implements ClientModInitializer {
    /** Bloque seleccionado (el servidor es la fuente de verdad y lo sincroniza). */
    public static Identifier selected = Identifier.of("minecraft", "stone");

    private static KeyBinding menuKey;
    private static KeyBinding cycleKey;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.PART, BuildPartRenderer::new);

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.treasurebuilder.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.treasurebuilder"));
        cycleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.treasurebuilder.cycle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.treasurebuilder"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new BlockMenuScreen());
                }
            }
            while (cycleKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    ClientPlayNetworking.send(new Payloads.Cycle());
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(Payloads.Sync.ID, (payload, ctx) -> selected = payload.block());

        // Mini HUD: bloque elegido, solo con la herramienta de construcción en la mano
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) return;
            Item held = mc.player.getMainHandStack().getItem();
            if (!(held instanceof PartToolItem tool) || tool.kind != PartToolItem.Kind.BUILD) return;

            Block b = Registries.BLOCK.get(selected);
            ctx.drawItem(new ItemStack(b), 8, 8);
            ctx.drawTextWithShadow(mc.textRenderer, b.getName(), 28, 8, 0xFFFFFF);
            ctx.drawTextWithShadow(mc.textRenderer, Text.translatable("hud.treasurebuilder.hint"), 28, 18, 0xAAAAAA);
        });
    }
}
