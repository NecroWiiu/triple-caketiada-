package com.treasurebuilder.client;

import net.minecraft.client.render.VertexConsumer;

/**
 * Envuelve un VertexConsumer y multiplica el alfa de cada vértice (para piezas transparentes).
 * Se implementan las dos variantes de vertex(...) a propósito (float/double) para ser robusto
 * ante cambios menores de mappings; por eso no llevan @Override.
 */
public final class AlphaConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float alpha;

    public AlphaConsumer(VertexConsumer delegate, float alpha) {
        this.delegate = delegate;
        this.alpha = alpha;
    }

    public VertexConsumer vertex(float x, float y, float z) {
        delegate.vertex(x, y, z);
        return this;
    }

    public VertexConsumer vertex(double x, double y, double z) {
        delegate.vertex((float) x, (float) y, (float) z);
        return this;
    }

    public VertexConsumer color(int red, int green, int blue, int a) {
        delegate.color(red, green, blue, (int) (a * alpha));
        return this;
    }

    public VertexConsumer texture(float u, float v) {
        delegate.texture(u, v);
        return this;
    }

    public VertexConsumer overlay(int u, int v) {
        delegate.overlay(u, v);
        return this;
    }

    public VertexConsumer light(int u, int v) {
        delegate.light(u, v);
        return this;
    }

    public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }
}
