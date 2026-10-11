package dev.zyron.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.zyron.client.ZyronClient;
import dev.zyron.client.feature.EspSource;
import dev.zyron.client.feature.Feature;
import dev.zyron.client.feature.FeatureManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Draws EspBox lists from every enabled EspSource feature. Follows the "extraction then drawing" pattern from
 * the Fabric 26.1.2 docs: boxes are gathered at END_EXTRACTION, then drawn after translucent terrain.
 * Boxes are drawn as plain filled quads; outlines are 12 thin box edges, so one pipeline handles everything.
 */
public final class EspRenderer {
    private static final RenderPipeline WALLS_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(ZyronClient.MOD_ID, "pipeline/esp_through_walls"))
                    .withDepthStencilState(Optional.empty())
                    .build());
    private static final RenderPipeline DEPTH_PIPELINE = RenderPipelines.DEBUG_FILLED_BOX;

    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private static volatile List<EspBox> boxes = List.of();
    private static MappableRingBuffer vertexBuffer;

    private EspRenderer() {
    }

    public static void init(FeatureManager features) {
        LevelRenderEvents.END_EXTRACTION.register((LevelExtractionContext context) -> {
            List<EspBox> out = new ArrayList<>();
            if (Minecraft.getInstance().level != null) {
                for (Feature f : features.all()) {
                    if (f.isEnabled() && f instanceof EspSource source) source.collectBoxes(out);
                }
            }
            boxes = out;
        });
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(EspRenderer::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> close());
    }

    // ------------------------------------------------------------------ drawing phase

    private static void render(LevelRenderContext context) {
        List<EspBox> list = boxes;
        if (list.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        Matrix4fc pose = context.poseStack().last().pose();

        // Depth-tested boxes first, then through-wall boxes on top.
        drawBatch(client, pose, camera, list, false, DEPTH_PIPELINE);
        drawBatch(client, pose, camera, list, true, WALLS_PIPELINE);
    }

    private static void drawBatch(Minecraft client, Matrix4fc pose, Vec3 camera, List<EspBox> list,
                                  boolean throughWalls, RenderPipeline pipeline) {
        BufferBuilder builder = null;
        for (EspBox box : list) {
            if (box.throughWalls() != throughWalls) continue;
            if (builder == null) {
                builder = new BufferBuilder(ALLOCATOR, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
            }
            emit(builder, pose, camera, box);
        }
        if (builder == null) return;

        MeshData built = builder.buildOrThrow();
        MeshData.DrawState drawState = built.drawState();
        VertexFormat format = drawState.format();
        GpuBuffer vertices = upload(drawState, format, built);
        draw(client, pipeline, built, drawState, vertices);
        vertexBuffer.rotate();
    }

    /** Coordinates are written relative to the camera (subtracted in double precision) to avoid float jitter far from spawn. */
    private static void emit(BufferBuilder buf, Matrix4fc pose, Vec3 cam, EspBox b) {
        float x0 = (float) (b.minX() - cam.x), y0 = (float) (b.minY() - cam.y), z0 = (float) (b.minZ() - cam.z);
        float x1 = (float) (b.maxX() - cam.x), y1 = (float) (b.maxY() - cam.y), z1 = (float) (b.maxZ() - cam.z);
        int argb = b.argb();
        float r = ((argb >> 16) & 255) / 255f, g = ((argb >> 8) & 255) / 255f, bl = (argb & 255) / 255f;
        float a = ((argb >>> 24) & 255) / 255f;

        if (b.fill()) {
            filledBox(buf, pose, x0, y0, z0, x1, y1, z1, r, g, bl, a);
        }
        if (b.outline()) {
            float t = b.thickness();
            // 4 edges along each axis.
            for (int i = 0; i < 4; i++) {
                float y = (i & 1) == 0 ? y0 : y1 - t;
                float z = (i & 2) == 0 ? z0 : z1 - t;
                filledBox(buf, pose, x0, y, z, x1, y + t, z + t, r, g, bl, 1f);

                float x = (i & 1) == 0 ? x0 : x1 - t;
                filledBox(buf, pose, x, y0, z, x + t, y1, z + t, r, g, bl, 1f);

                float zz = (i & 1) == 0 ? z0 : z1 - t;
                float xx = (i & 2) == 0 ? x0 : x1 - t;
                filledBox(buf, pose, xx, y0, zz, xx + t, y1, zz + t, r, g, bl, 1f);
            }
        }
    }

    private static void filledBox(BufferBuilder buffer, Matrix4fc m,
                                  float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                  float red, float green, float blue, float alpha) {
        // Front
        buffer.addVertex(m, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        // Back
        buffer.addVertex(m, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        // Left
        buffer.addVertex(m, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, minX, maxY, minZ).setColor(red, green, blue, alpha);
        // Right
        buffer.addVertex(m, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        // Top
        buffer.addVertex(m, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, minX, maxY, minZ).setColor(red, green, blue, alpha);
        // Bottom
        buffer.addVertex(m, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(m, minX, minY, maxZ).setColor(red, green, blue, alpha);
    }

    private static GpuBuffer upload(MeshData.DrawState drawState, VertexFormat format, MeshData built) {
        int size = drawState.vertexCount() * format.getVertexSize();

        if (vertexBuffer == null || vertexBuffer.size() < size) {
            if (vertexBuffer != null) vertexBuffer.close();
            vertexBuffer = new MappableRingBuffer(() -> ZyronClient.MOD_ID + " esp vertices",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, size);
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView view = encoder.mapBuffer(
                vertexBuffer.currentBuffer().slice(0, built.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(built.vertexBuffer(), view.data());
        }
        return vertexBuffer.currentBuffer();
    }

    private static void draw(Minecraft client, RenderPipeline pipeline, MeshData built,
                             MeshData.DrawState drawState, GpuBuffer vertices) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            built.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(built.indexBuffer());
            indexType = built.drawState().indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = shapeIndexBuffer.getBuffer(drawState.indexCount());
            indexType = shapeIndexBuffer.type();
        }

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> ZyronClient.MOD_ID + " esp",
                client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
                client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indices, indexType);
            pass.drawIndexed(0, 0, drawState.indexCount(), 1);
        }

        built.close();
    }

    public static void close() {
        ALLOCATOR.close();
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
