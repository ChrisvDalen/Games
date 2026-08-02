package com.keplersharvest.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.keplersharvest.farming.CropDefinition;
import com.keplersharvest.farming.FarmLand;
import com.keplersharvest.farming.FarmPlot;
import com.keplersharvest.farming.SoilState;
import com.keplersharvest.game.GameSession;
import com.keplersharvest.game.InteractionTarget;
import com.keplersharvest.npc.Colonist;
import com.keplersharvest.time.TimeOfDay;
import com.keplersharvest.world.GridPoint;
import com.keplersharvest.world.MapObject;
import com.keplersharvest.world.TileSet;
import com.keplersharvest.world.WorldMap;
import com.keplersharvest.world.WorldPosition;

import java.util.Optional;

/**
 * Draws the current map, its contents and the player.
 *
 * <p>Reads game state and never writes it. Colours come from the tileset and the content files, so
 * a new crop or module is visible without touching this class.
 */
public final class WorldRenderer implements Disposable {

    private static final int TILE = Placeholders.TILE;
    /** How dark full night gets. High enough to stay readable, low enough to feel like night. */
    private static final float NIGHT_LIGHT = 0.55f;

    private final Placeholders art;
    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final Color tint = new Color();
    private final boolean ownsArt;

    public WorldRenderer(Placeholders art) {
        this(art, false);
    }

    public WorldRenderer(Placeholders art, boolean ownsArt) {
        this.art = art;
        this.ownsArt = ownsArt;
        // Matches the interface canvas so world pixels and font pixels share one grid.
        this.viewport = new ExtendViewport(20 * TILE, 11.25f * TILE, camera);
    }

    public Viewport viewport() {
        return viewport;
    }

    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    public void render(GameSession session, Optional<InteractionTarget> target, float animationTime) {
        WorldMap map = session.currentMap();
        TileSet tileSet = session.content().maps().tileSet();
        followPlayer(session, map);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        Color ambient = ambientTint(session.clock().now(), map.ambientColour());
        batch.begin();
        drawTiles(map, tileSet, ambient);
        drawFarmland(session, ambient);
        drawObjects(session, map, ambient);
        drawColonists(session, ambient);
        drawPlayer(session, ambient, animationTime);
        target.ifPresent(hit -> drawIndicator(hit, animationTime));
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Centres on the player, then clamps so the view never runs off the edge of a small map. */
    private void followPlayer(GameSession session, WorldMap map) {
        WorldPosition position = session.player().position();
        float halfWidth = camera.viewportWidth / 2f;
        float halfHeight = camera.viewportHeight / 2f;
        float mapWidth = map.width() * TILE;
        float mapHeight = map.height() * TILE;
        float x = position.x() * TILE;
        float y = position.y() * TILE;
        camera.position.x = mapWidth <= camera.viewportWidth
                ? mapWidth / 2f
                : MathUtils.clamp(x, halfWidth, mapWidth - halfWidth);
        camera.position.y = mapHeight <= camera.viewportHeight
                ? mapHeight / 2f
                : MathUtils.clamp(y, halfHeight, mapHeight - halfHeight);
        camera.update();
    }

    /** Darkens everything towards the map's ambient colour as the day turns over. */
    private Color ambientTint(TimeOfDay now, String ambientColour) {
        float minute = now.minuteOfDay();
        // Dawn is well under way by the 06:00 wake-up, so the player never starts the day in murk.
        float daylight;
        if (minute < 240) {
            daylight = NIGHT_LIGHT;
        } else if (minute < 400) {
            daylight = MathUtils.lerp(NIGHT_LIGHT, 1f, (minute - 240f) / 160f);
        } else if (minute < 1020) {
            daylight = 1f;
        } else if (minute < 1260) {
            daylight = MathUtils.lerp(1f, NIGHT_LIGHT, (minute - 1020f) / 240f);
        } else {
            daylight = NIGHT_LIGHT;
        }
        Color night = art.colour(ambientColour);
        tint.set(
                MathUtils.lerp(night.r + 0.25f, 1f, daylight),
                MathUtils.lerp(night.g + 0.28f, 1f, daylight),
                MathUtils.lerp(night.b + 0.38f, 1f, daylight),
                1f);
        return tint;
    }

    private void drawTiles(WorldMap map, TileSet tileSet, Color ambient) {
        int minX = Math.max(0, (int) ((camera.position.x - camera.viewportWidth / 2f) / TILE) - 1);
        int maxX = Math.min(map.width() - 1, (int) ((camera.position.x + camera.viewportWidth / 2f) / TILE) + 1);
        int minY = Math.max(0, (int) ((camera.position.y - camera.viewportHeight / 2f) / TILE) - 1);
        int maxY = Math.min(map.height() - 1, (int) ((camera.position.y + camera.viewportHeight / 2f) / TILE) + 1);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                drawTile(tileSet, map.groundTile(x, y), ambient, x, y);
                int overlay = map.overlayTile(x, y);
                if (overlay != 0) {
                    drawTile(tileSet, overlay, ambient, x, y);
                }
                if (map.blocked(x, y)) {
                    // A darker cap edge reads as height without needing real art.
                    batch.setColor(0f, 0f, 0f, 0.22f);
                    batch.draw(art.pixel(), x * TILE, y * TILE, TILE, 4f);
                }
            }
        }
    }

    private void drawTile(TileSet tileSet, int gid, Color ambient, int x, int y) {
        Color base = art.colour(tileSet.colourFor(gid, "1a1d22"));
        batch.setColor(base.r * ambient.r, base.g * ambient.g, base.b * ambient.b, 1f);
        batch.draw(art.pixel(), x * TILE, y * TILE, TILE, TILE);
        batch.setColor(ambient.r, ambient.g, ambient.b, 0.5f);
        batch.draw(art.grain(), x * TILE, y * TILE, TILE, TILE);
    }

    private void drawFarmland(GameSession session, Color ambient) {
        FarmLand land = session.farmLand(session.player().mapId());
        for (FarmPlot plot : land.plots()) {
            float x = plot.position().x() * TILE;
            float y = plot.position().y() * TILE;
            if (plot.soil() == SoilState.TILLED) {
                Color soil = plot.watered() ? art.colour("5c3a22") : art.colour("7a5236");
                batch.setColor(soil.r * ambient.r, soil.g * ambient.g, soil.b * ambient.b, 1f);
                batch.draw(art.pixel(), x + 2, y + 2, TILE - 4, TILE - 4);
                batch.setColor(0f, 0f, 0f, 0.25f);
                for (int furrow = 1; furrow <= 3; furrow++) {
                    batch.draw(art.pixel(), x + 4, y + furrow * 7f, TILE - 8, 1.5f);
                }
            }
            plot.cropId().flatMap(id -> session.content().crop(id))
                    .ifPresent(crop -> drawCrop(crop, plot, ambient, x, y));
        }
    }

    private void drawCrop(CropDefinition crop, FarmPlot plot, Color ambient, float x, float y) {
        if (plot.withered()) {
            Color dead = art.colour("6a5a45");
            art.sprite(batch, art.diamond(), new Color(dead.r * ambient.r, dead.g * ambient.g, dead.b * ambient.b, 1f),
                    x + TILE * 0.3f, y + TILE * 0.25f, TILE * 0.4f);
            return;
        }
        int stage = crop.stageFor(plot.grownDays());
        float progress = (stage + 1f) / crop.stageCount();
        boolean mature = crop.matureAt(plot.grownDays());
        float size = TILE * (0.24f + 0.4f * progress);
        Color colour = art.colour(crop.colour());
        Color shaded = new Color(colour.r * ambient.r, colour.g * ambient.g, colour.b * ambient.b, 1f);

        // Stem
        batch.setColor(0.32f * ambient.r, 0.5f * ambient.g, 0.34f * ambient.b, 1f);
        batch.draw(art.pixel(), x + TILE / 2f - 1.5f, y + 5, 3f, size * 0.8f);
        // Head: a disc while growing, a ring once it is ready to pick.
        art.sprite(batch, mature ? art.ring() : art.disc(), shaded,
                x + TILE / 2f - size / 2f, y + 4 + size * 0.55f, size);
    }

    private void drawObjects(GameSession session, WorldMap map, Color ambient) {
        for (MapObject object : map.objects()) {
            float x = object.position().x() * TILE;
            float y = object.position().y() * TILE;
            switch (object.kind()) {
                case MODULE -> drawModule(session, object, ambient, x, y);
                case RESOURCE_NODE -> drawResourceNode(session, map, object, ambient, x, y);
                case CREW_LOG -> {
                    if (!session.journal().hasLog(object.requireProperty("log"))) {
                        Color colour = art.colour("d8e070");
                        art.sprite(batch, art.diamond(),
                                new Color(colour.r, colour.g, colour.b, 0.95f),
                                x + TILE * 0.25f, y + TILE * 0.25f, TILE * 0.5f);
                    }
                }
                case CRAFTING_STATION -> {
                    shade(ambient, art.colour("7a6a55"));
                    batch.draw(art.pixel(), x + 2, y + 2, TILE - 4, TILE - 10);
                    shade(ambient, art.colour("b09a72"));
                    batch.draw(art.pixel(), x + 1, y + TILE - 12, TILE - 2, 8);
                }
                case BED -> {
                    shade(ambient, art.colour("8f6f8f"));
                    batch.draw(art.pixel(), x + 3, y + 4, TILE - 6, TILE - 10);
                    shade(ambient, art.colour("d0c0d8"));
                    batch.draw(art.pixel(), x + 5, y + TILE - 12, TILE - 10, 6);
                }
                case PORTAL -> {
                    batch.setColor(0.45f, 0.85f, 0.95f, 0.35f + 0.1f * MathUtils.sin(session.clock().day()));
                    batch.draw(art.pixel(), x + 2, y + 2, TILE - 4, TILE - 4);
                    art.outline(batch, new Color(0.6f, 0.95f, 1f, 0.8f), x + 2, y + 2, TILE - 4, TILE - 4, 2f);
                }
                case SIGN -> {
                    shade(ambient, art.colour("9a8f7a"));
                    batch.draw(art.pixel(), x + TILE * 0.35f, y + 4, TILE * 0.3f, TILE * 0.6f);
                }
                case LANDMARK -> {
                    batch.setColor(1f, 1f, 1f, 0.10f);
                    batch.draw(art.disc(), x - TILE * 0.5f, y - TILE * 0.5f, TILE * 2f, TILE * 2f);
                }
                default -> {
                }
            }
        }
    }

    private void drawModule(GameSession session, MapObject object, Color ambient, float x, float y) {
        String moduleId = object.requireProperty("module");
        session.colony().module(moduleId).ifPresent(module -> {
            Color base = art.colour(module.definition().colour());
            float condition = module.progress();
            // Broken modules render desaturated and dark; they brighten as stages complete.
            float mix = 0.35f + 0.65f * condition;
            shade(ambient, new Color(base.r * mix, base.g * mix, base.b * mix, 1f));
            batch.draw(art.pixel(), x + 2, y + 2, TILE - 4, TILE - 4);
            art.outline(batch, new Color(0.06f, 0.08f, 0.1f, 1f), x + 2, y + 2, TILE - 4, TILE - 4, 2f);

            // Status pips: one per repair stage.
            int stages = module.definition().stageCount();
            float pipWidth = (TILE - 12f) / stages;
            for (int i = 0; i < stages; i++) {
                boolean done = i < module.completedStages();
                batch.setColor(done ? 0.45f : 0.75f, done ? 0.95f : 0.35f, done ? 0.55f : 0.3f, 1f);
                batch.draw(art.pixel(), x + 6 + i * pipWidth, y + 5, pipWidth - 2f, 3f);
            }
            if (module.online()) {
                batch.setColor(0.6f, 1f, 0.75f, 0.18f);
                batch.draw(art.disc(), x - 8, y - 8, TILE + 16, TILE + 16);
            }
        });
    }

    private void drawResourceNode(GameSession session, WorldMap map, MapObject object, Color ambient,
                                  float x, float y) {
        String key = com.keplersharvest.world.WorldObjectState.key(map.id(), object.id());
        int respawnDays = object.intProperty("respawnDays", 0);
        boolean available = session.worldObjects().isAvailable(key, session.clock().day(), respawnDays);
        String itemId = object.requireProperty("item");
        Color colour = session.content().item(itemId)
                .map(item -> art.colour(item.colour()))
                .orElse(Color.GRAY);
        if (available) {
            shade(ambient, colour);
            art.sprite(batch, art.diamond(), batch.getColor().cpy(),
                    x + TILE * 0.15f, y + TILE * 0.15f, TILE * 0.7f);
        } else {
            batch.setColor(colour.r * 0.3f, colour.g * 0.3f, colour.b * 0.3f, 0.6f);
            batch.draw(art.pixel(), x + TILE * 0.35f, y + TILE * 0.25f, TILE * 0.3f, TILE * 0.15f);
        }
    }

    private void drawColonists(GameSession session, Color ambient) {
        for (Colonist colonist : session.colonistsHere()) {
            float x = colonist.position().x() * TILE;
            float y = colonist.position().y() * TILE;
            Color colour = art.colour(colonist.definition().colour());
            shade(ambient, new Color(colour.r * 0.5f, colour.g * 0.5f, colour.b * 0.5f, 1f));
            batch.draw(art.pixel(), x + TILE * 0.28f, y + 4, TILE * 0.44f, TILE * 0.45f);
            art.sprite(batch, art.disc(), new Color(colour.r * ambient.r, colour.g * ambient.g,
                    colour.b * ambient.b, 1f), x + TILE * 0.26f, y + TILE * 0.42f, TILE * 0.48f);
        }
    }

    private void drawPlayer(GameSession session, Color ambient, float animationTime) {
        WorldPosition position = session.player().position();
        float x = position.x() * TILE;
        float y = position.y() * TILE;
        float bob = session.player().moving() ? MathUtils.sin(animationTime * 12f) * 1.5f : 0f;

        batch.setColor(0f, 0f, 0f, 0.28f);
        batch.draw(art.disc(), x - TILE * 0.26f, y - TILE * 0.34f, TILE * 0.52f, TILE * 0.22f);

        Color suit = art.colour("d8dde4");
        shade(ambient, new Color(suit.r * 0.62f, suit.g * 0.64f, suit.b * 0.7f, 1f));
        batch.draw(art.pixel(), x - TILE * 0.22f, y - TILE * 0.30f + bob, TILE * 0.44f, TILE * 0.46f);
        art.sprite(batch, art.disc(), new Color(suit.r * ambient.r, suit.g * ambient.g, suit.b * ambient.b, 1f),
                x - TILE * 0.24f, y + TILE * 0.10f + bob, TILE * 0.48f);

        // Visor points the way the player is facing, which doubles as the interaction hint.
        float visorX = x - TILE * 0.10f + session.player().facing().dx() * TILE * 0.12f;
        float visorY = y + TILE * 0.22f + session.player().facing().dy() * TILE * 0.08f + bob;
        batch.setColor(0.25f, 0.75f, 0.85f, 0.95f);
        batch.draw(art.pixel(), visorX, visorY, TILE * 0.2f, TILE * 0.1f);
    }

    private void drawIndicator(InteractionTarget target, float animationTime) {
        GridPoint tile = target.tile();
        float pulse = 0.45f + 0.25f * MathUtils.sin(animationTime * 5f);
        art.outline(batch, new Color(1f, 0.95f, 0.6f, pulse),
                tile.x() * TILE + 1, tile.y() * TILE + 1, TILE - 2, TILE - 2, 2f);
    }

    private void shade(Color ambient, Color colour) {
        batch.setColor(colour.r * ambient.r, colour.g * ambient.g, colour.b * ambient.b, 1f);
    }

    /** World-space position of a tile centre, for UI that points at the world. */
    public com.badlogic.gdx.math.Vector3 project(WorldPosition position) {
        return camera.project(new com.badlogic.gdx.math.Vector3(position.x() * TILE, position.y() * TILE, 0f),
                viewport.getScreenX(), viewport.getScreenY(),
                viewport.getScreenWidth(), viewport.getScreenHeight());
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (ownsArt) {
            art.dispose();
        }
    }
}
