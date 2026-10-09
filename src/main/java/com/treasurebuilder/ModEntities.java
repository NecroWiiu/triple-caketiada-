package com.treasurebuilder;

import com.treasurebuilder.entity.BuildPartEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
    private ModEntities() {}

    public static final EntityType<BuildPartEntity> PART = Registry.register(
            Registries.ENTITY_TYPE,
            TreasureBuilder.id("part"),
            EntityType.Builder.<BuildPartEntity>create(BuildPartEntity::new, SpawnGroup.MISC)
                    .dimensions(1.0f, 1.0f)
                    .maxTrackingRange(10)
                    .trackingTickInterval(2)
                    .build("part"));

    /** Fuerza la carga de la clase (y con ello el registro). */
    public static void register() {}
}
