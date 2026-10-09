package com.treasurebuilder.tool;

import com.treasurebuilder.ModEntities;
import com.treasurebuilder.entity.BuildPartEntity;
import com.treasurebuilder.net.Payloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Toda la lógica de las herramientas, siempre en el servidor.
 * positive = clic derecho (añadir / agrandar / alternar), false = clic izquierdo (quitar / reducir).
 */
public final class ToolActions {
    private ToolActions() {}

    public static final double REACH = 8.0;
    private static final int MAX_STRUCTURE = 512;

    /** Lo que apunta el jugador: una pieza (part != null) o un bloque del mundo. */
    public record Target(BuildPartEntity part, Direction face, Vec3d hit, BlockPos blockPos, Direction blockSide) {}

    // ------------------------------------------------------------------ entrada

    public static void run(ServerPlayerEntity p, Item item, PartToolItem.Kind kind, boolean positive) {
        if (p.isSpectator()) return;
        if (p.getItemCooldownManager().isCoolingDown(item)) return;
        int cooldown = switch (kind) {
            case ANCHOR -> 10;
            case BUILD -> 4;
            case GLASS -> 2;
            default -> 0;
        };
        if (cooldown > 0) p.getItemCooldownManager().set(item, cooldown);

        PlayerData data = PlayerData.get(p);
        Target t = find(p);

        switch (kind) {
            case BUILD -> {
                if (positive) place(p, t, data);
                else pick(p, t, data);
            }
            case DELETE -> delete(p, t);
            case SCALE -> scale(p, t, data, positive);
            case ROTATE -> rotate(p, t, data, positive);
            case ANCHOR -> anchor(p, t);
            case GLASS -> glass(p, t, data, positive);
        }
    }

    /** Tecla V: cambia el paso de la herramienta que se lleva en la mano. */
    public static void cycleStep(ServerPlayerEntity p) {
        Item item = p.getMainHandStack().getItem();
        if (!(item instanceof PartToolItem tool)) return;
        PlayerData d = PlayerData.get(p);
        switch (tool.kind) {
            case SCALE -> {
                d.scaleIdx = (d.scaleIdx + 1) % PlayerData.SCALE_STEPS.length;
                msg(p, "msg.treasurebuilder.step.scale", PlayerData.SCALE_STEPS[d.scaleIdx]);
            }
            case ROTATE -> {
                d.rotIdx = (d.rotIdx + 1) % PlayerData.ROT_STEPS.length;
                msg(p, "msg.treasurebuilder.step.rot", PlayerData.ROT_STEPS[d.rotIdx]);
            }
            case GLASS -> {
                d.alphaIdx = (d.alphaIdx + 1) % PlayerData.ALPHA_STEPS.length;
                msg(p, "msg.treasurebuilder.step.alpha", PlayerData.ALPHA_STEPS[d.alphaIdx]);
            }
            default -> {}
        }
    }

    // ------------------------------------------------------------------ puntería

    public static Target find(PlayerEntity p) {
        World w = p.getWorld();
        Vec3d eye = p.getEyePos();
        Vec3d end = eye.add(p.getRotationVec(1.0f).multiply(REACH));

        BlockHitResult bhr = w.raycast(new RaycastContext(eye, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, p));
        double bestSq = bhr.getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE : eye.squaredDistanceTo(bhr.getPos());

        BuildPartEntity best = null;
        Vec3d bestPos = null;
        for (BuildPartEntity e : w.getEntitiesByClass(BuildPartEntity.class, new Box(eye, end).expand(1.0), x -> true)) {
            Optional<Vec3d> r = e.getBoundingBox().raycast(eye, end);
            if (r.isPresent()) {
                double d = eye.squaredDistanceTo(r.get());
                if (d < bestSq) {
                    bestSq = d;
                    best = e;
                    bestPos = r.get();
                }
            }
        }
        if (best != null) {
            return new Target(best, faceOf(best.getBoundingBox(), bestPos), bestPos, null, null);
        }
        if (bhr.getType() == HitResult.Type.MISS) return null;
        return new Target(null, null, bhr.getPos(), bhr.getBlockPos(), bhr.getSide());
    }

    private static Direction faceOf(Box b, Vec3d h) {
        double[] d = {
                Math.abs(h.y - b.minY), Math.abs(h.y - b.maxY),
                Math.abs(h.z - b.minZ), Math.abs(h.z - b.maxZ),
                Math.abs(h.x - b.minX), Math.abs(h.x - b.maxX)};
        Direction[] dirs = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        int best = 0;
        for (int i = 1; i < 6; i++) if (d[i] < d[best]) best = i;
        return dirs[best];
    }

    // ------------------------------------------------------------ herramientas

    private static void place(ServerPlayerEntity p, Target t, PlayerData data) {
        World w = p.getWorld();
        Vec3d c;
        if (t == null) {
            Vec3d q = p.getEyePos().add(p.getRotationVec(1.0f).multiply(4.5));
            c = new Vec3d(Math.floor(q.x) + 0.5, Math.floor(q.y) + 0.5, Math.floor(q.z) + 0.5);
        } else if (t.part() != null) {
            Box b = t.part().getBoundingBox();
            Direction f = t.face();
            Vec3d h = t.hit();
            double cx = f.getAxis() == Direction.Axis.X ? (f.getOffsetX() > 0 ? b.maxX + 0.5 : b.minX - 0.5) : Math.floor(h.x) + 0.5;
            double cy = f.getAxis() == Direction.Axis.Y ? (f.getOffsetY() > 0 ? b.maxY + 0.5 : b.minY - 0.5) : Math.floor(h.y) + 0.5;
            double cz = f.getAxis() == Direction.Axis.Z ? (f.getOffsetZ() > 0 ? b.maxZ + 0.5 : b.minZ - 0.5) : Math.floor(h.z) + 0.5;
            c = new Vec3d(cx, cy, cz);
        } else {
            c = Vec3d.ofCenter(t.blockPos().offset(t.blockSide()));
        }

        // Evita duplicar una pieza idéntica en el mismo sitio (clic mantenido)
        Box probe = new Box(c.x - 0.05, c.y - 0.05, c.z - 0.05, c.x + 0.05, c.y + 0.05, c.z + 0.05);
        for (BuildPartEntity o : w.getEntitiesByClass(BuildPartEntity.class, probe, x -> true)) {
            Vector3f os = o.getSizePx();
            if (o.getPos().squaredDistanceTo(c) < 0.0025 && os.x == 16f && os.y == 16f && os.z == 16f) return;
        }

        BlockState state = data.selected.getDefaultState();
        BuildPartEntity e = new BuildPartEntity(ModEntities.PART, w);
        e.setBlockState(state);
        e.setPosition(c.x, c.y, c.z);
        w.spawnEntity(e);
        w.playSound(null, BlockPos.ofFloored(c), state.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    private static void pick(ServerPlayerEntity p, Target t, PlayerData data) {
        if (t == null) return;
        Block b = t.part() != null
                ? t.part().getBlockState().getBlock()
                : p.getWorld().getBlockState(t.blockPos()).getBlock();
        if (b.asItem() == Items.AIR) return;
        data.selected = b;
        ServerPlayNetworking.send(p, new Payloads.Sync(Registries.BLOCK.getId(b)));
        p.sendMessage(Text.translatable("msg.treasurebuilder.selected", b.getName()), true);
    }

    private static void delete(ServerPlayerEntity p, Target t) {
        if (t == null || t.part() == null) {
            msg(p, "msg.treasurebuilder.noPart");
            return;
        }
        List<BuildPartEntity> list = p.isSneaking() ? connected(t.part()) : List.of(t.part());
        BlockState s = t.part().getBlockState();
        p.getWorld().playSound(null, t.part().getBlockPos(), s.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
        for (BuildPartEntity e : list) e.discard();
    }

    private static void scale(ServerPlayerEntity p, Target t, PlayerData data, boolean positive) {
        if (t == null || t.part() == null) {
            msg(p, "msg.treasurebuilder.noPart");
            return;
        }
        BuildPartEntity part = t.part();
        int step = PlayerData.SCALE_STEPS[data.scaleIdx];
        float d = positive ? step : -step;
        Vector3f s = part.getSizePx();
        float[] a = {s.x, s.y, s.z};

        if (p.isSneaking()) {
            // Todos los ejes a la vez, manteniendo el centro
            for (int i = 0; i < 3; i++) a[i] = clamp(a[i] + d);
            part.setSizePx(a[0], a[1], a[2]);
        } else {
            // Eje local más alineado con la cara apuntada; la cara opuesta se queda quieta
            Direction f = t.face();
            Vector3f n = new Vector3f(f.getOffsetX(), f.getOffsetY(), f.getOffsetZ());
            new Quaternionf(part.getRot()).conjugate().transform(n);
            float[] na = {n.x, n.y, n.z};
            int ax = 0;
            for (int i = 1; i < 3; i++) if (Math.abs(na[i]) > Math.abs(na[ax])) ax = i;
            float sign = na[ax] >= 0 ? 1f : -1f;

            float nv = clamp(a[ax] + d);
            float real = nv - a[ax];
            if (real == 0f) {
                msg(p, "msg.treasurebuilder.size", (int) a[0], (int) a[1], (int) a[2]);
                return;
            }
            a[ax] = nv;
            float[] sh = new float[3];
            sh[ax] = sign * real / 32f; // medio incremento, en bloques
            Vector3f shift = new Vector3f(sh[0], sh[1], sh[2]);
            part.getRot().transform(shift);

            part.setSizePx(a[0], a[1], a[2]);
            part.setPosition(part.getX() + shift.x, part.getY() + shift.y, part.getZ() + shift.z);
        }
        msg(p, "msg.treasurebuilder.size", (int) a[0], (int) a[1], (int) a[2]);
    }

    private static void rotate(ServerPlayerEntity p, Target t, PlayerData data, boolean positive) {
        if (t == null || t.part() == null) {
            msg(p, "msg.treasurebuilder.noPart");
            return;
        }
        int deg = PlayerData.ROT_STEPS[data.rotIdx] * (positive ? 1 : -1);
        Direction f = t.face();
        // Giro alrededor del eje de la cara tocada, en coordenadas del mundo
        Quaternionf q = new Quaternionf()
                .rotationAxis((float) Math.toRadians(deg), f.getOffsetX(), f.getOffsetY(), f.getOffsetZ())
                .mul(t.part().getRot())
                .normalize();
        t.part().setRot(q);
        msg(p, "msg.treasurebuilder.rot", deg);
    }

    private static void anchor(ServerPlayerEntity p, Target t) {
        if (t == null || t.part() == null) {
            msg(p, "msg.treasurebuilder.noPart");
            return;
        }
        boolean anchoredNow = !t.part().isAnchored();
        List<BuildPartEntity> list = p.isSneaking() ? connected(t.part()) : List.of(t.part());
        for (BuildPartEntity e : list) e.setAnchored(anchoredNow);
        msg(p, anchoredNow ? "msg.treasurebuilder.anchored" : "msg.treasurebuilder.free");
    }

    private static void glass(ServerPlayerEntity p, Target t, PlayerData data, boolean positive) {
        if (t == null || t.part() == null) {
            msg(p, "msg.treasurebuilder.noPart");
            return;
        }
        float step = PlayerData.ALPHA_STEPS[data.alphaIdx] / 100f;
        float a = t.part().getAlpha();
        float na = p.isSneaking() ? 1f : a + (positive ? -step : step);
        na = Math.round(Math.max(0.1f, Math.min(1f, na)) * 100f) / 100f;
        t.part().setAlpha(na);
        msg(p, "msg.treasurebuilder.alpha", Math.round(na * 100f));
    }

    // ------------------------------------------------------------------ utilidades

    /** Piezas que se tocan entre sí (estructura conectada), hasta MAX_STRUCTURE. */
    public static List<BuildPartEntity> connected(BuildPartEntity start) {
        World w = start.getWorld();
        Set<BuildPartEntity> seen = new LinkedHashSet<>();
        Deque<BuildPartEntity> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && seen.size() < MAX_STRUCTURE) {
            BuildPartEntity cur = queue.poll();
            for (BuildPartEntity o : w.getEntitiesByClass(BuildPartEntity.class, cur.getBoundingBox().expand(0.05), x -> true)) {
                if (seen.size() >= MAX_STRUCTURE) break;
                if (seen.add(o)) queue.add(o);
            }
        }
        return new ArrayList<>(seen);
    }

    private static float clamp(float v) {
        return Math.max(BuildPartEntity.MIN_PX, Math.min(BuildPartEntity.MAX_PX, v));
    }

    private static void msg(ServerPlayerEntity p, String key, Object... args) {
        p.sendMessage(Text.translatable(key, args), true);
    }
}
