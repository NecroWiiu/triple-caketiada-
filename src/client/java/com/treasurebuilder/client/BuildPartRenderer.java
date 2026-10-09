package com.treasurebuilder.client;

import com.treasurebuilder.entity.BuildPartEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

/** Dibuja el bloque con escala por píxel, rotación libre y opacidad. Usa solo la API vanilla de render. */
public class BuildPartRenderer extends EntityRenderer<BuildPartEntity> {
    public BuildPartRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(BuildPartEntity e, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        Vector3f s = e.getSizePx();
        float alpha = e.getAlpha();

        matrices.push();
        matrices.multiply(e.getRot());
        matrices.scale(s.x / 16f, s.y / 16f, s.z / 16f);
        matrices.translate(-0.5f, -0.5f, -0.5f);

        VertexConsumerProvider provider;
        if (alpha >= 0.999f) {
            provider = vertexConsumers;
        } else {
            provider = layer -> new AlphaConsumer(
                    vertexConsumers.getBuffer(TexturedRenderLayers.getEntityTranslucentCull()), alpha);
        }

        MinecraftClient.getInstance().getBlockRenderManager()
                .renderBlockAsEntity(e.getBlockState(), matrices, provider, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }

    @Override
    public Identifier getTexture(BuildPartEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
}
