package com.keplersharvest.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

/**
 * Procedurally generated placeholder art.
 *
 * <p>Everything the game draws is a tinted rectangle, disc, ring or diamond built at start-up from
 * colours declared in the content files. That means the project ships no binary artwork at all and
 * cannot accidentally carry anyone else's assets. See {@code docs/ASSETS.md} for how to swap in real
 * sprites.
 */
public final class Placeholders implements Disposable {

    /** Pixels per tile. The simulation works in tile units; this is the only scale factor. */
    public static final int TILE = 32;

    private final Texture pixel;
    private final Texture disc;
    private final Texture ring;
    private final Texture diamond;
    private final Texture grain;
    private final Map<String, Color> colourCache = new HashMap<>();

    public Placeholders() {
        this.pixel = solid(1, 1);
        this.disc = disc(TILE);
        this.ring = ring(TILE);
        this.diamond = diamond(TILE);
        this.grain = grain(TILE);
    }

    /** Parses and caches an {@code RRGGBB} colour from content. */
    public Color colour(String hex) {
        return colourCache.computeIfAbsent(hex, key -> {
            try {
                return Color.valueOf(key.length() == 6 ? key + "ff" : key);
            } catch (RuntimeException e) {
                return Color.MAGENTA.cpy();
            }
        });
    }

    public Texture pixel() {
        return pixel;
    }

    public Texture disc() {
        return disc;
    }

    public Texture ring() {
        return ring;
    }

    public Texture diamond() {
        return diamond;
    }

    /** A faint speckle overlay that stops large flat areas looking like a solid block. */
    public Texture grain() {
        return grain;
    }

    public void rect(SpriteBatch batch, Color colour, float x, float y, float width, float height) {
        batch.setColor(colour);
        batch.draw(pixel, x, y, width, height);
    }

    public void rect(SpriteBatch batch, Color colour, float alpha, float x, float y, float width, float height) {
        batch.setColor(colour.r, colour.g, colour.b, alpha);
        batch.draw(pixel, x, y, width, height);
    }

    /** Draws a hollow rectangle of the given line thickness. */
    public void outline(SpriteBatch batch, Color colour, float x, float y, float width, float height, float weight) {
        batch.setColor(colour);
        batch.draw(pixel, x, y, width, weight);
        batch.draw(pixel, x, y + height - weight, width, weight);
        batch.draw(pixel, x, y, weight, height);
        batch.draw(pixel, x + width - weight, y, weight, height);
    }

    public void sprite(SpriteBatch batch, Texture texture, Color colour, float x, float y, float size) {
        batch.setColor(colour);
        batch.draw(texture, x, y, size, size);
    }

    private static Texture solid(int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private static Texture disc(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setColor(Color.WHITE);
        pixmap.fillCircle(size / 2, size / 2, size / 2 - 1);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private static Texture ring(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setColor(Color.WHITE);
        pixmap.fillCircle(size / 2, size / 2, size / 2 - 1);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fillCircle(size / 2, size / 2, size / 2 - 4);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private static Texture diamond(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setColor(Color.WHITE);
        int half = size / 2;
        for (int y = 0; y < size; y++) {
            int spread = half - Math.abs(y - half);
            pixmap.drawLine(half - spread, y, half + spread, y);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    /** Deterministic speckle so the same tile always looks the same between runs. */
    private static Texture grain(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        java.util.Random random = new java.util.Random(0x5EED);
        for (int i = 0; i < size * 3; i++) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            pixmap.setColor(1f, 1f, 1f, 0.05f + random.nextFloat() * 0.08f);
            pixmap.drawPixel(x, y);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        pixel.dispose();
        disc.dispose();
        ring.dispose();
        diamond.dispose();
        grain.dispose();
    }
}
