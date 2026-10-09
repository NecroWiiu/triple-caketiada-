package com.treasurebuilder.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Una "pieza" de construcción: un bloque con tamaño en PÍXELES (1/16 de bloque) por eje,
 * rotación libre (cuaternión), opacidad y modo anclado/libre (física).
 * La posición de la entidad es el CENTRO de la pieza.
 *
 * Ligera: mientras está anclada no hace tick; en reposo duerme y solo comprueba cada 5 ticks.
 */
public class BuildPartEntity extends Entity {
    public static final float MIN_PX = 1f;
    public static final float MAX_PX = 256f;

    private static final TrackedData<BlockState> STATE =
            DataTracker.registerData(BuildPartEntity.class, TrackedDataHandlerRegistry.BLOCK_STATE);
    private static final TrackedData<Vector3f> SIZE =
            DataTracker.registerData(BuildPartEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Quaternionf> ROT =
            DataTracker.registerData(BuildPartEntity.class, TrackedDataHandlerRegistry.QUATERNIONF);
    private static final TrackedData<Float> ALPHA =
            DataTracker.registerData(BuildPartEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> ANCHORED =
            DataTracker.registerData(BuildPartEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private int restTicks;

    public BuildPartEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(STATE, Blocks.STONE.getDefaultState());
        builder.add(SIZE, new Vector3f(16f, 16f, 16f));
        builder.add(ROT, new Quaternionf());
        builder.add(ALPHA, 1.0f);
        builder.add(ANCHORED, true);
    }

    // ------------------------------------------------------------------ datos

    public BlockState getBlockState() { return getDataTracker().get(STATE); }
    public void setBlockState(BlockState s) { getDataTracker().set(STATE, s); }

    /** Tamaño en píxeles (X, Y, Z locales). Devuelve una copia. */
    public Vector3f getSizePx() { return new Vector3f(getDataTracker().get(SIZE)); }

    public void setSizePx(float x, float y, float z) {
        getDataTracker().set(SIZE, new Vector3f(clampPx(x), clampPx(y), clampPx(z)));
    }

    public Quaternionf getRot() { return new Quaternionf(getDataTracker().get(ROT)); }
    public void setRot(Quaternionf q) { getDataTracker().set(ROT, new Quaternionf(q)); }

    public float getAlpha() { return getDataTracker().get(ALPHA); }
    public void setAlpha(float a) { getDataTracker().set(ALPHA, Math.max(0.1f, Math.min(1f, a))); }

    public boolean isAnchored() { return getDataTracker().get(ANCHORED); }

    public void setAnchored(boolean anchored) {
        getDataTracker().set(ANCHORED, anchored);
        restTicks = 0;
        setVelocity(Vec3d.ZERO);
    }

    private static float clampPx(float v) { return Math.max(MIN_PX, Math.min(MAX_PX, v)); }

    // ------------------------------------------------------------ colisión

    /** Caja AABB que envuelve a la pieza rotada, centrada en la posición de la entidad. */
    @Override
    protected Box calculateBoundingBox() {
        DataTracker dt = this.getDataTracker();
        if (dt == null) return super.calculateBoundingBox();
        Vector3f s = dt.get(SIZE);
        Matrix3f m = new Matrix3f().set(dt.get(ROT));
        double hx = (Math.abs(m.m00) * s.x + Math.abs(m.m10) * s.y + Math.abs(m.m20) * s.z) / 32.0;
        double hy = (Math.abs(m.m01) * s.x + Math.abs(m.m11) * s.y + Math.abs(m.m21) * s.z) / 32.0;
        double hz = (Math.abs(m.m02) * s.x + Math.abs(m.m12) * s.y + Math.abs(m.m22) * s.z) / 32.0;
        return new Box(getX() - hx, getY() - hy, getZ() - hz, getX() + hx, getY() + hy, getZ() + hz);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (SIZE.equals(data) || ROT.equals(data)) {
            this.setBoundingBox(this.calculateBoundingBox());
        }
    }

    @Override public boolean canHit() { return true; }
    @Override public boolean isCollidable() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isAttackable() { return false; }

    @Override
    protected MoveEffect getMoveEffect() { return MoveEffect.NONE; }

    // -------------------------------------------------------------- física

    @Override
    public void tick() {
        if (isAnchored()) return; // piezas ancladas: coste cero

        World world = getWorld();
        if (!world.isClient && getY() < world.getBottomY() - 64) {
            discard();
            return;
        }
        // Dormir cuando está en reposo (se despierta si cambia el soporte o la empuja alguien)
        if (restTicks > 20 && (age % 5) != 0) return;

        if (!world.isClient) pushFromPlayers(world);

        setVelocity(getVelocity().add(0.0, -0.04, 0.0));
        move(MovementType.SELF, getVelocity());

        Vec3d v = getVelocity();
        if (isOnGround()) {
            v = new Vec3d(v.x * 0.6, 0.0, v.z * 0.6);
        } else {
            v = v.multiply(0.98);
        }
        setVelocity(v);

        if (isOnGround() && v.lengthSquared() < 1.0E-4) restTicks++;
        else restTicks = 0;
    }

    private void pushFromPlayers(World world) {
        Box zone = getBoundingBox().expand(0.12, 0.0, 0.12);
        Vector3f s = getDataTracker().get(SIZE);
        double mass = Math.max(1.0, (double) s.x * s.y * s.z / 4096.0);
        for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, zone, pl -> !pl.isSpectator())) {
            Vec3d d = getPos().subtract(p.getPos());
            double len = Math.hypot(d.x, d.z);
            if (len < 1.0E-4) continue;
            double f = 0.08 / mass;
            addVelocity(d.x / len * f, 0.0, d.z / len * f);
            restTicks = 0;
        }
    }

    // --------------------------------------------------------------- NBT

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putString("Block", Registries.BLOCK.getId(getBlockState().getBlock()).toString());
        Vector3f s = getDataTracker().get(SIZE);
        nbt.putFloat("SX", s.x);
        nbt.putFloat("SY", s.y);
        nbt.putFloat("SZ", s.z);
        Quaternionf q = getDataTracker().get(ROT);
        nbt.putFloat("QX", q.x);
        nbt.putFloat("QY", q.y);
        nbt.putFloat("QZ", q.z);
        nbt.putFloat("QW", q.w);
        nbt.putFloat("Alpha", getAlpha());
        nbt.putBoolean("Anchored", isAnchored());
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        Block block = Blocks.STONE;
        Identifier id = Identifier.tryParse(nbt.getString("Block"));
        if (id != null) {
            Block b = Registries.BLOCK.get(id);
            if (b != Blocks.AIR) block = b;
        }
        setBlockState(block.getDefaultState());
        if (nbt.contains("SX")) setSizePx(nbt.getFloat("SX"), nbt.getFloat("SY"), nbt.getFloat("SZ"));
        if (nbt.contains("QW")) {
            Quaternionf q = new Quaternionf(nbt.getFloat("QX"), nbt.getFloat("QY"), nbt.getFloat("QZ"), nbt.getFloat("QW"));
            q.normalize();
            setRot(q);
        }
        if (nbt.contains("Alpha")) setAlpha(nbt.getFloat("Alpha"));
        getDataTracker().set(ANCHORED, !nbt.contains("Anchored") || nbt.getBoolean("Anchored"));
    }
}
