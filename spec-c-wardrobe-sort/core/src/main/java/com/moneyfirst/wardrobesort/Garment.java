package com.moneyfirst.wardrobesort;

import java.util.Objects;

/**
 * A single wearable item. Garments are rendered procedurally (no external art
 * assets): {@link #shapeId()} selects a simple silhouette (rectangle, circle,
 * triangle, ...) drawn with libGDX's {@code ShapeRenderer}/{@code Pixmap} in
 * the flat color given by {@link #r()}/{@link #g()}/{@link #b()}/{@link #a()}.
 *
 * <p>{@link #pack()} identifies which content pack a garment belongs to:
 * {@code "base"} for the always-available garments {@link RoundGenerator}
 * draws timed rounds from, or a {@link WardrobeIapCatalog} product id
 * (e.g. {@code "ws_pack_streetwear"}) for a cosmetic garment that only
 * becomes available in freeplay/custom-outfit mode once purchased. Cosmetic
 * garments are never placed into a timed round's target or tray, so owning
 * them can never change round difficulty, timing, or scoring.
 */
public final class Garment {

    private final String id;
    private final GarmentSlot slot;
    private final float r;
    private final float g;
    private final float b;
    private final float a;
    private final int shapeId;
    private final String pack;

    public Garment(String id, GarmentSlot slot, float r, float g, float b, float a, int shapeId, String pack) {
        this.id = Objects.requireNonNull(id, "id");
        this.slot = Objects.requireNonNull(slot, "slot");
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.shapeId = shapeId;
        this.pack = Objects.requireNonNull(pack, "pack");
    }

    public String id() {
        return id;
    }

    public GarmentSlot slot() {
        return slot;
    }

    public float r() {
        return r;
    }

    public float g() {
        return g;
    }

    public float b() {
        return b;
    }

    public float a() {
        return a;
    }

    public int shapeId() {
        return shapeId;
    }

    public String pack() {
        return pack;
    }

    public boolean isCosmeticPackGarment() {
        return !"base".equals(pack);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Garment)) return false;
        Garment garment = (Garment) o;
        return id.equals(garment.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Garment{id='" + id + "', slot=" + slot + ", pack='" + pack + "'}";
    }
}
