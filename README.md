# Treasure Builder (Fabric 1.21.1)

Herramientas de construcción inspiradas en *Build a Boat for Treasure*, sin el mapa.
Compatible con **Fabulously Optimized** (solo usa la API de render vanilla y Fabric API).

## Herramientas (pestaña creativa "Treasure Builder", o `/tbkit`)

| Herramienta | Clic derecho | Clic izquierdo | Agacharse (Shift) |
|---|---|---|---|
| **Construcción** | Coloca la pieza seleccionada | Copia el bloque apuntado | — |
| **Borrado** | Borra la pieza | Borra la pieza | Borra toda la estructura conectada |
| **Escala** | Agranda la cara apuntada | Reduce la cara apuntada | Todos los ejes a la vez |
| **Llana (rotar)** | Gira + | Gira − | — |
| **Anclaje** | Ancla / desancla (física) | Ancla / desancla | Afecta a toda la estructura conectada |
| **Transparencia** | Más transparente | Menos transparente | Restablece a opaco |

Teclas: **B** abre el menú de bloques (buscador + cuadrícula) · **V** cambia el paso de la herramienta en mano
(escala: 1, 2, 4, 8, 16, 32 px · rotación: 1, 5, 15, 45, 90° · opacidad: 5, 10, 25 %).

- **Escala por píxel**: cada clic cambia 1 px de juego (1/16 de bloque), no un bloque entero. Máximo 256 px por eje.
- **Rotación**: gira alrededor del eje de la cara que tocas, en coordenadas del mundo.
- **Física**: las piezas desancladas caen, chocan con el mundo y con otras piezas, se apilan y se pueden empujar caminando.
  Las ancladas no consumen CPU y las que están quietas "duermen".
- Comandos (op): `/tbkit` (te da las herramientas), `/tbclear <radio>` (borra piezas cercanas).

Límite conocido: la colisión usa una caja AABB que envuelve la pieza rotada (no hay vuelcos ni rotación por física).

## Compilar en GitHub (sin instalar nada)

1. Crea un repositorio en GitHub y sube **todo el contenido de esta carpeta** (incluida `.github`).
2. Ve a la pestaña **Actions** → workflow **build** (se ejecuta solo en cada push).
3. Cuando termine, abre la ejecución y baja el `.jar` de **Artifacts** (`treasure-builder`).
4. Para publicar una release: crea un tag `v1.0.0` y el `.jar` aparece en **Releases**.

Instálalo en `mods/` junto a Fabric Loader 0.16+ y Fabric API para 1.21.1 (ya viene en Fabulously Optimized).

## Compilar en tu PC

Java 21 + Gradle 8.10+: `gradle build` → el jar queda en `build/libs/`.
(Si Fabric API no se resuelve, actualiza `fabric_version` en `gradle.properties` con la que indique https://fabricmc.net/develop)
