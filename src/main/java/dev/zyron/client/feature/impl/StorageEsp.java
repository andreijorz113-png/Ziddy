package dev.zyron.client.feature.impl;

import dev.zyron.client.feature.Category;
import dev.zyron.client.feature.EspSource;
import dev.zyron.client.feature.Feature;
import dev.zyron.client.render.EspBox;
import dev.zyron.client.setting.BoolSetting;
import dev.zyron.client.setting.ColorSetting;
import dev.zyron.client.setting.ModeSetting;
import dev.zyron.client.setting.NumberSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Highlights storage blocks in loaded chunks. Every block type has its own on/off toggle and color;
 * the alpha channel of each color is the fill opacity (the outline is always solid).
 */
public class StorageEsp extends Feature implements EspSource {
    private record Kind(String name, Predicate<BlockEntity> matcher, BoolSetting enabled, ColorSetting color) {
    }

    private record Hit(AABB box, Kind kind) {
    }

    private static final int MAX_HITS = 1500;

    private final ModeSetting style = add(new ModeSetting("Style", "Both", "Outline", "Filled", "Both"));
    private final BoolSetting throughWalls = add(new BoolSetting("Through walls", true));
    private final NumberSetting range = add(new NumberSetting("Range (blocks)", 96, 8, 256, 4));
    private final NumberSetting thickness = add(new NumberSetting("Outline thickness", 0.03, 0.01, 0.10, 0.005));
    private final NumberSetting interval = add(new NumberSetting("Scan interval (ticks)", 10, 2, 40, 1));

    private final List<Kind> kinds = new ArrayList<>();
    private volatile List<Hit> hits = List.of();
    private int ticks;

    public StorageEsp() {
        super("Storage ESP", "Highlights chests, ender chests, shulker boxes, barrels and other storage blocks, with a color per type.",
                Category.RENDER);

        // Trapped chests extend ChestBlockEntity, so the plain chest matcher excludes them.
        kind("Chests", be -> be instanceof ChestBlockEntity && !(be instanceof TrappedChestBlockEntity), 0x50FFA21F, true);
        kind("Trapped chests", be -> be instanceof TrappedChestBlockEntity, 0x50FF3B30, true);
        kind("Ender chests", be -> be instanceof EnderChestBlockEntity, 0x50B455FF, true);
        kind("Shulker boxes", be -> be instanceof ShulkerBoxBlockEntity, 0x50FF55FF, true);
        kind("Barrels", be -> be instanceof BarrelBlockEntity, 0x50C98B4B, true);
        kind("Hoppers", be -> be instanceof HopperBlockEntity, 0x50A0A0A0, false);
        kind("Dispensers / droppers", be -> be instanceof DispenserBlockEntity, 0x508AA4FF, false);
        kind("Furnaces", be -> be instanceof AbstractFurnaceBlockEntity, 0x50FF7A45, false);
        kind("Brewing stands", be -> be instanceof BrewingStandBlockEntity, 0x5055FFAA, false);
    }

    private void kind(String name, Predicate<BlockEntity> matcher, int argb, boolean defaultOn) {
        BoolSetting enabled = add(new BoolSetting(name, defaultOn));
        ColorSetting color = add(new ColorSetting(name + " color", argb));
        color.visibleWhen(enabled::get);
        kinds.add(new Kind(name, matcher, enabled, color));
    }

    @Override
    protected void onEnable() {
        ticks = 1000; // scan on the very next tick
    }

    @Override
    protected void onDisable() {
        hits = List.of();
    }

    @Override
    public void onTick() {
        if (++ticks < interval.getInt()) return;
        ticks = 0;
        scan();
    }

    private Kind match(BlockEntity be) {
        for (Kind k : kinds) {
            if (k.enabled().get() && k.matcher().test(be)) return k;
        }
        return null;
    }

    private void scan() {
        var level = mc.level;
        var player = mc.player;
        if (level == null || player == null) {
            hits = List.of();
            return;
        }

        BlockPos origin = player.blockPosition();
        double maxSq = range.get() * range.get();
        int chunkRadius = (range.getInt() >> 4) + 1;
        int pcx = origin.getX() >> 4;
        int pcz = origin.getZ() >> 4;

        List<Hit> out = new ArrayList<>();
        scan:
        for (int cx = pcx - chunkRadius; cx <= pcx + chunkRadius; cx++) {
            for (int cz = pcz - chunkRadius; cz <= pcz + chunkRadius; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
                    BlockPos pos = be.getBlockPos();
                    if (pos.distSqr(origin) > maxSq) continue;
                    Kind kind = match(be);
                    if (kind == null) continue;

                    VoxelShape shape = be.getBlockState().getShape(level, pos);
                    AABB box = (shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos)).inflate(0.002);
                    out.add(new Hit(box, kind));
                    if (out.size() >= MAX_HITS) break scan;
                }
            }
        }
        hits = out;
    }

    @Override
    public void collectBoxes(List<EspBox> out) {
        boolean fill = !style.is("Outline");
        boolean outline = !style.is("Filled");
        float t = thickness.getFloat();
        boolean walls = throughWalls.get();
        for (Hit h : hits) {
            if (!h.kind().enabled().get()) continue;
            AABB b = h.box();
            out.add(new EspBox(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ,
                    h.kind().color().get(), fill, outline, t, walls));
        }
    }
}
