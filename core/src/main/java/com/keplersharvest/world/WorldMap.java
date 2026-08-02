package com.keplersharvest.world;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A loaded tile map: terrain, collision, farmable ground and placed objects.
 *
 * <p>Coordinates are y-up with the origin at the bottom-left tile, matching the renderer. The
 * loader converts from Tiled's y-down layout so map files stay editable in Tiled.
 */
public final class WorldMap {

    private final String id;
    private final String displayName;
    private final int width;
    private final int height;
    private final int tileSize;
    private final int[] ground;
    private final int[] overlay;
    private final boolean[] solid;
    private final boolean[] farmable;
    private final List<MapObject> objects;
    private final Map<GridPoint, MapObject> objectsByPosition;
    private final Map<String, GridPoint> spawnPoints;
    private final String ambientColour;

    WorldMap(String id,
             String displayName,
             int width,
             int height,
             int tileSize,
             int[] ground,
             int[] overlay,
             boolean[] solid,
             boolean[] farmable,
             List<MapObject> objects,
             String ambientColour) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.ground = ground;
        this.overlay = overlay;
        this.solid = solid;
        this.farmable = farmable;
        this.objects = List.copyOf(objects);
        this.ambientColour = ambientColour;

        Map<GridPoint, MapObject> byPosition = new LinkedHashMap<>();
        Map<String, GridPoint> spawns = new LinkedHashMap<>();
        for (MapObject object : this.objects) {
            byPosition.putIfAbsent(object.position(), object);
            if (object.kind() == MapObjectKind.SPAWN) {
                spawns.put(object.id(), object.position());
            }
        }
        this.objectsByPosition = Map.copyOf(byPosition);
        this.spawnPoints = Map.copyOf(spawns);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int tileSize() {
        return tileSize;
    }

    public String ambientColour() {
        return ambientColour;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public boolean inBounds(GridPoint point) {
        return inBounds(point.x(), point.y());
    }

    /** Ground tile index, or 0 for nothing. */
    public int groundTile(int x, int y) {
        return inBounds(x, y) ? ground[index(x, y)] : 0;
    }

    /** Decorative tile drawn above the ground, or 0. */
    public int overlayTile(int x, int y) {
        return inBounds(x, y) ? overlay[index(x, y)] : 0;
    }

    /** Out-of-bounds counts as blocked, which keeps the player inside the map. */
    public boolean blocked(int x, int y) {
        return !inBounds(x, y) || solid[index(x, y)];
    }

    public boolean blocked(GridPoint point) {
        return blocked(point.x(), point.y());
    }

    public boolean farmable(GridPoint point) {
        return inBounds(point) && farmable[index(point.x(), point.y())];
    }

    public Collection<MapObject> objects() {
        return objects;
    }

    public List<MapObject> objectsOfKind(MapObjectKind kind) {
        List<MapObject> matches = new ArrayList<>();
        for (MapObject object : objects) {
            if (object.kind() == kind) {
                matches.add(object);
            }
        }
        return List.copyOf(matches);
    }

    public Optional<MapObject> objectAt(GridPoint point) {
        return Optional.ofNullable(objectsByPosition.get(point));
    }

    public Optional<MapObject> object(String objectId) {
        return objects.stream().filter(o -> o.id().equals(objectId)).findFirst();
    }

    public Optional<GridPoint> spawn(String name) {
        return Optional.ofNullable(spawnPoints.get(name));
    }

    /** Falls back to any spawn, then to the map centre, so a map is never unenterable. */
    public GridPoint spawnOrDefault(String name) {
        return spawn(name)
                .or(() -> spawnPoints.values().stream().findFirst())
                .orElseGet(() -> new GridPoint(width / 2, height / 2));
    }

    private int index(int x, int y) {
        return y * width + x;
    }
}
