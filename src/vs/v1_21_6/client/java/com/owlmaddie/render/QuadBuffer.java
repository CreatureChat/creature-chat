// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.owlmaddie.utils.TextureLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * Wrapper for Tessellator/BufferBuilder. Since the API changes between different versions of Minecraft,
 * this wrapper helps standardize the rendering/drawing calls. This is modified for Minecraft 1.21.6.
 */
@Environment(EnvType.CLIENT)
public final class QuadBuffer {
    public static final QuadBuffer INSTANCE = new QuadBuffer();
    private BufferBuilder buf;

    // 🔧 STATIC CUSTOM PIPELINE SETUP (only once)
    private static final RenderPipeline.Snippet QUAD_SNIPPET = RenderPipeline.builder()
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, Mode.QUADS)
            .buildSnippet();

    private static final RenderPipeline QUAD_PIPELINE = RenderPipeline.builder(QUAD_SNIPPET)
            .withLocation("creaturepals/quad") // arbitrary ID
            .withDepthBias(3.0f, 3.0f) // mimic polygonOffset
            .withBlend(BlendFunction.TRANSLUCENT) // vanilla style alpha blending
            .build();

    private QuadBuffer() {}

    // begin
    public QuadBuffer begin() {
        return begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
    }

    public QuadBuffer begin(Mode mode) {
        return begin(mode, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
    }

    public QuadBuffer begin(Mode mode, VertexFormat fmt) {
        buf = Tesselator.getInstance().begin(mode, fmt);
        return this;
    }

    /** Add a vertex and set a default normal (pointing up) to satisfy the format */
    public QuadBuffer vertex(float x, float y, float z) {
        buf.addVertex(x, y, z);
        return this;
    }

    // vertex helpers
    public QuadBuffer vertex(Matrix4f mat, float x, float y, float z) {
        buf.addVertex(mat, x, y, z);
        return this;
    }

    public QuadBuffer color(int r, int g, int b, int a) {
        buf.setColor(r, g, b, a);
        return this;
    }

    public QuadBuffer texture(float u, float v) {
        buf.setUv(u, v);
        return this;
    }

    public QuadBuffer overlay(int packed) {
        buf.setOverlay(packed);
        return this;
    }

    public QuadBuffer light(int packed) {
        buf.setLight(packed);
        return this;
    }

    public void draw() {
        if (buf == null) return;

        try (MeshData meshData = buf.buildOrThrow()) {
            buf = null; // always clear reference

            GpuTextureView gpuTex = TextureLoader.lastTextureView;
            if (gpuTex == null) return;

            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            var vb = DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP
                    .uploadImmediateVertexBuffer(meshData.vertexBuffer());

            var ib = RenderSystem
                    .getSequentialBuffer(meshData.drawState().mode())
                    .getBuffer(meshData.drawState().indexCount());
            var it = RenderSystem
                    .getSequentialBuffer(meshData.drawState().mode())
                    .type();

            RenderTarget fb = Minecraft.getInstance().getMainRenderTarget();
            // label supplier for debugging
            Supplier<String> passName = () -> "quad-pass";

            try (RenderPass pass = encoder.createRenderPass(
                    passName,
                    fb.getColorTextureView(), OptionalInt.empty(),
                    fb.getDepthTextureView(), OptionalDouble.empty()
            )) {
                pass.bindSampler("Sampler0", gpuTex);
                pass.setVertexBuffer(0, vb);
                pass.setIndexBuffer(ib, it);
                pass.setPipeline(QUAD_PIPELINE);
                pass.drawIndexed(0, 0,  meshData.drawState().indexCount(), 1);
            }

        } catch (IllegalStateException e) {
            // BufferBuilder was empty — clean up safely
            buf = null;
        }
    }
}