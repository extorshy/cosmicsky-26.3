package com.cosmicsky;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Draws the whole custom night sky. This is the ONLY file that touches Minecraft's rendering API,
 * so if a future/odd version breaks, this (and SkyRenderingMixin) is where to look.
 */
public final class SkyDrawer {
    private static final Identifier SUN = Identifier.ofVanilla("textures/environment/sun.png");
    private static final Identifier MOON = Identifier.ofVanilla("textures/environment/moon_phases.png");
    private static final Identifier GLOW = tex("glow");
    private static final Identifier SATURN = tex("saturn");
    private static final Identifier JUPITER = tex("jupiter");
    private static final Identifier MARS = tex("mars");

    private static final int STAR_COUNT = 1100;
    private static final Vector3f[] STAR_DIR = new Vector3f[STAR_COUNT];
    private static final Vector3f[] STAR_U = new Vector3f[STAR_COUNT];
    private static final Vector3f[] STAR_V = new Vector3f[STAR_COUNT];
    private static final float[] STAR_SIZE = new float[STAR_COUNT];
    private static final float[] STAR_PHASE = new float[STAR_COUNT];

    private static final Random RNG = new Random();
    private static final List<Meteor> METEORS = new ArrayList<>();
    private static long lastNs;

    static {
        Random r = new Random(10842L);
        for (int i = 0; i < STAR_COUNT; i++) {
            float z = r.nextFloat() * 2f - 1f;
            float a = r.nextFloat() * MathHelper.TAU;
            float rr = (float) Math.sqrt(1f - z * z);
            Vector3f d = new Vector3f(rr * MathHelper.cos(a), z, rr * MathHelper.sin(a));
            Vector3f[] b = basis(d);
            STAR_DIR[i] = d;
            STAR_U[i] = b[0];
            STAR_V[i] = b[1];
            STAR_SIZE[i] = 0.5f + r.nextFloat() * r.nextFloat() * 1.6f;
            STAR_PHASE[i] = r.nextFloat() * MathHelper.TAU;
        }
    }

    private SkyDrawer() {}

    private static Identifier tex(String name) {
        return Identifier.of(CosmicSky.MOD_ID, "textures/environment/" + name + ".png");
    }

    /** Draw everything. Matrices are expected to be in vanilla's sky space (before the day/night rotation). */
    public static void draw(MatrixStack ms, VertexConsumerProvider vcp, ClientWorld world) {
        CosmicConfig c = CosmicSky.CONFIG;

        long time = world.getTimeOfDay();
        float angle = skyAngle(time);
        float night = 1f - MathHelper.clamp(MathHelper.cos(angle * MathHelper.TAU) * 2f + 0.25f, 0f, 1f);

        long nowNs = System.nanoTime();
        float dt = lastNs == 0 ? 0f : Math.min((nowNs - lastNs) / 1.0e9f, 0.1f);
        lastNs = nowNs;
        float t = (float) ((nowNs / 1.0e9) % 1000.0);

        Matrix4f base = new Matrix4f(ms.peek().getPositionMatrix());

        ms.push();
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90f));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle * 360f));
        Matrix4f m = ms.peek().getPositionMatrix();

        // ---- stars ----
        if (c.starsEnabled && night > 0.01f) {
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getCelestial(GLOW));
            for (int i = 0; i < STAR_COUNT; i++) {
                float twinkle = 0.7f + 0.3f * MathHelper.sin(t * 1.7f + STAR_PHASE[i]);
                sprite(vc, m, STAR_DIR[i], STAR_U[i], STAR_V[i], STAR_SIZE[i],
                        argb(night * twinkle, c.starsColor), 0, 0, 1, 1);
            }
        }

        // ---- planets (fixed spots on the star sphere) ----
        if (c.planetsEnabled && night > 0.01f) {
            planet(vcp, m, SATURN, new Vector3f(0.55f, 0.62f, 0.55f), 16f, argb(night, c.saturnColor));
            planet(vcp, m, JUPITER, new Vector3f(-0.70f, 0.45f, 0.55f), 8f, argb(night, c.jupiterColor));
            planet(vcp, m, MARS, new Vector3f(0.20f, 0.35f, -0.90f), 4.5f, argb(night, c.marsColor));
        }

        // ---- sun ----
        if (c.sunEnabled) {
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getCelestial(SUN));
            Vector3f d = new Vector3f(0, 1, 0);
            Vector3f[] b = basis(d);
            sprite(vc, m, d, b[0], b[1], 30f, argb(1f, c.sunColor), 0, 0, 1, 1);
        }

        // ---- moon (8-phase atlas, 4x2) ----
        if (c.moonEnabled) {
            int phase = (int) (((time / 24000L) % 8L + 8L) % 8L);
            float u0 = (phase % 4) / 4f, u1 = u0 + 0.25f;
            float v0 = (phase / 4) / 2f, v1 = v0 + 0.5f;
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getCelestial(MOON));
            Vector3f d = new Vector3f(0, -1, 0);
            Vector3f[] b = basis(d);
            sprite(vc, m, d, b[0], b[1], 20f, argb(1f, c.moonColor), u0, v0, u1, v1);
        }

        ms.pop();

        // ---- shooting stars (world-aligned, so they always streak above the horizon) ----
        if (c.shootingEnabled) {
            if (night > 0.4f && METEORS.size() < 6
                    && RNG.nextFloat() < dt * c.shootingPerMinute / 60f) {
                spawnMeteor();
            }
        }
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getCelestial(GLOW));
        for (Iterator<Meteor> it = METEORS.iterator(); it.hasNext(); ) {
            Meteor mt = it.next();
            mt.age += dt;
            if (mt.age >= mt.life || mt.pos.y < 0.02f) { it.remove(); continue; }

            mt.pos.add(mt.dir.x * mt.speed * dt, mt.dir.y * mt.speed * dt, mt.dir.z * mt.speed * dt).normalize();
            float dot = mt.dir.dot(mt.pos);
            mt.dir.sub(mt.pos.x * dot, mt.pos.y * dot, mt.pos.z * dot).normalize();

            if (!c.shootingEnabled) continue;
            float fade = MathHelper.sin(MathHelper.PI * (mt.age / mt.life));
            int trail = 16;
            for (int k = 0; k < trail; k++) {
                float f = 1f - k / (float) trail;
                Vector3f p = new Vector3f(mt.pos).sub(
                        mt.dir.x * mt.speed * 0.03f * k,
                        mt.dir.y * mt.speed * 0.03f * k,
                        mt.dir.z * mt.speed * 0.03f * k).normalize();
                Vector3f[] b = basis(p);
                sprite(vc, base, p, b[0], b[1], 0.4f + 1.4f * f,
                        argb(fade * night * (float) Math.pow(f, 1.5), c.shootingColor), 0, 0, 1, 1);
            }
        }
    }

    private static void planet(VertexConsumerProvider vcp, Matrix4f m, Identifier texture, Vector3f dir, float size, int color) {
        dir.normalize();
        Vector3f[] b = basis(dir);
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getCelestial(texture));
        sprite(vc, m, dir, b[0], b[1], size, color, 0, 0, 1, 1);
    }

    private static void spawnMeteor() {
        Meteor mt = new Meteor();
        float az = RNG.nextFloat() * MathHelper.TAU;
        float el = 0.35f + RNG.nextFloat() * 0.9f;
        mt.pos.set(MathHelper.cos(el) * MathHelper.cos(az), MathHelper.sin(el), MathHelper.cos(el) * MathHelper.sin(az));
        Vector3f r = new Vector3f(RNG.nextFloat() - .5f, RNG.nextFloat() - .5f, RNG.nextFloat() - .5f);
        float dot = r.dot(mt.pos);
        r.sub(mt.pos.x * dot, mt.pos.y * dot, mt.pos.z * dot);
        if (r.y > 0) r.negate();
        mt.dir.set(r).normalize();
        mt.speed = 0.7f + RNG.nextFloat() * 0.6f;
        mt.life = 0.9f + RNG.nextFloat() * 0.8f;
        METEORS.add(mt);
    }

    // ---------- helpers ----------

    private static float skyAngle(long timeOfDay) {
        double f = timeOfDay / 24000.0 - 0.25;
        f = f - Math.floor(f);
        double d = 0.5 - Math.cos(f * Math.PI) / 2.0;
        return (float) ((f * 2.0 + d) / 3.0);
    }

    private static int argb(float a, int rgb) {
        int alpha = (int) (MathHelper.clamp(a, 0f, 1f) * 255f);
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private static Vector3f[] basis(Vector3f d) {
        Vector3f ref = Math.abs(d.y) > 0.99f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = d.cross(ref, new Vector3f()).normalize();
        Vector3f v = d.cross(u, new Vector3f()).normalize();
        return new Vector3f[]{u, v};
    }

    /** A camera-facing square placed on the sky sphere (radius 100) in direction d. */
    private static void sprite(VertexConsumer vc, Matrix4f m, Vector3f d, Vector3f u, Vector3f v,
                               float s, int argb, float u0, float v0, float u1, float v1) {
        corner(vc, m, d, u, v, -s, -s, argb, u0, v0);
        corner(vc, m, d, u, v, -s, s, argb, u0, v1);
        corner(vc, m, d, u, v, s, s, argb, u1, v1);
        corner(vc, m, d, u, v, s, -s, argb, u1, v0);
    }

    private static void corner(VertexConsumer vc, Matrix4f m, Vector3f d, Vector3f u, Vector3f v,
                               float a, float b, int argb, float tu, float tv) {
        float R = 100f;
        vc.vertex(m,
                d.x * R + u.x * a + v.x * b,
                d.y * R + u.y * a + v.y * b,
                d.z * R + u.z * a + v.z * b)
          .texture(tu, tv)
          .color(argb);
    }

    private static final class Meteor {
        final Vector3f pos = new Vector3f();
        final Vector3f dir = new Vector3f();
        float age, life, speed;
    }
}
