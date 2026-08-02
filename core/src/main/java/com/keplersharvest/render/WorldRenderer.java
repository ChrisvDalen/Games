package com.keplersharvest.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import com.keplersharvest.world.Direction;
import com.keplersharvest.world.GridPoint;
import com.keplersharvest.world.MapObject;
import com.keplersharvest.world.TileSet;
import com.keplersharvest.world.WorldMap;
import com.keplersharvest.world.WorldObjectState;
import com.keplersharvest.world.WorldPosition;

import java.util.Optional;

/**
 * Draws the current map, its contents and the player from the baked sprite atlas.
 *
 * <p>Reads game state and never writes it. Which sprite to use is decided from content - a tile's
 * declared pattern, a crop's growth stage, a module's repair progress - so new content appears
 * without changes here.
 */
public final class WorldRenderer implements Disposable {

    private static final int TILE = Placeholders.TILE;
    /** How dark full night gets. High enough to stay readable, low enough to feel like night. */
    private static final float NIGHT_LIGHT = 0.55f;
    /** Steps per second of the two-frame walk cycle. */
    private static final float WALK_SPEED = 7f;

    private final Placeholders art;
    private final WorldSprites sprites;
    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final Color tint = new Color();

    public WorldRenderer(Placeholders art, WorldSprites sprites) {
        this.art = art;
        this.sprites = sprites;
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
        followPlayer(session, map);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        Color ambient = ambientTint(session.clock().now(), map.ambientColour());
        batch.begin();
        drawTiles(session, map, ambient);
        drawFarmland(session, ambient);
        drawObjects(session, map, ambient);
        drawColonists(session, ambient, animationTime);
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
        camera.position.x = mapWidth <= camera.viewportWidth
                ? mapWidth / 2f
                : MathUtils.clamp(position.x() * TILE, halfWidth, mapWidth - halfWidth);
        camera.position.y = mapHeight <= camera.viewportHeight
                ? mapHeight / 2f
                : MathUtils.clamp(position.y() * TILE, halfHeight, mapHeight - halfHeight);
        // Snapping to whole units keeps sprite pixels aligned with the screen grid while walking.
        camera.position.x = Math.round(camera.position.x);
        camera.position.y = Math.round(camera.position.y);
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

    private void drawTiles(GameSession session, WorldMap map, Color ambient) {
        TileSet tileSet = session.content().maps().tileSet();
        int minX = Math.max(0, (int) ((camera.position.x - camera.viewportWidth / 2f) / TILE) - 1);
        int maxX = Math.min(map.width() - 1, (int) ((camera.position.x + camera.viewportWidth / 2f) / TILE) + 1);
        int minY = Math.max(0, (int) ((camera.position.y - camera.viewportHeight / 2f) / TILE) - 1);
        int maxY = Math.min(map.height() - 1, (int) ((camera.position.y + camera.viewportHeight / 2f) / TILE) + 1);

        batch.setColor(ambient);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                drawTile(tileSet, map.groundTile(x, y), x, y);
                int overlay = map.overlayTile(x, y);
                if (overlay != 0) {
                    drawTile(tileSet, overlay, x, y);
                }
            }
        }
    }

    private void drawTile(TileSet tileSet, int gid, int x, int y) {
        String shape = sprites.terrainShape(tileSet.patternFor(gid), x, y);
        batch.draw(sprites.get(shape, tileSet.colourFor(gid, "1a1d22")), x * TILE, y * TILE, TILE, TILE);
    }

    private void drawFarmland(GameSession session, Color ambient) {
        FarmLand land = session.farmLand(session.player().mapId());
        for (FarmPlot plot : land.plots()) {
            float x = plot.position().x() * TILE;
            float y = plot.position().y() * TILE;
            if (plot.soil() == SoilState.TILLED) {
                batch.setColor(ambient);
                batch.draw(sprites.get("soil_tilled", plot.watered() ? "4a2d1a" : "7a5236"), x, y, TILE, TILE);
            }
            plot.cropId().flatMap(id -> session.content().crop(id))
                    .ifPresent(crop -> drawCrop(crop, plot, ambient, x, y));
        }
    }

    private void drawCrop(CropDefinition crop, FarmPlot plot, Color ambient, float x, float y) {
        batch.setColor(ambient);
        if (plot.withered()) {
            batch.draw(sprites.get("crop_dead", WorldSprites.DEAD_PLANT), x, y, TILE, TILE);
            return;
        }
        batch.draw(sprites.get(cropShape(crop, plot.grownDays()), crop.colour()), x, y, TILE, TILE);
    }

    /**
     * Picks a sprite for a crop's growth. The last shape is reserved for "ready to pick", so the
     * player can tell at a glance without counting days.
     */
    private String cropShape(CropDefinition crop, int grownDays) {
        if (crop.matureAt(grownDays)) {
            return "crop_3";
        }
        int stageCount = crop.stageCount();
        if (stageCount <= 1) {
            return "crop_0";
        }
        int stage = crop.stageFor(grownDays);
        return "crop_" + Math.min(2, Math.round(stage * 2f / (stageCount - 1)));
    }

    private void drawObjects(GameSession session, WorldMap map, Color ambient) {
        for (MapObject object : map.objects()) {
            float x = object.position().x() * TILE;
            float y = object.position().y() * TILE;
            batch.setColor(ambient);
            switch (object.kind()) {
                case MODULE -> drawModule(session, object, ambient, x, y);
                case RESOURCE_NODE -> drawResourceNode(session, map, object, x, y);
                case CREW_LOG -> {
                    if (!session.journal().hasLog(object.requireProperty("log"))) {
                        // Undiscovered slates pulse so they read as something to walk towards.
                        float glow = 0.82f + 0.18f * MathUtils.sin(session.clock().now().minuteOfDay());
                        batch.setColor(ambient.r * glow, ambient.g * glow, ambient.b, 1f);
                        batch.draw(sprites.get("slate", WorldSprites.SLATE, WorldSprites.SLATE_GLOW),
                                x, y, TILE, TILE);
                    }
                }
                case CRAFTING_STATION -> batch.draw(sprites.get("bench", WorldSprites.BENCH), x, y, TILE, TILE);
                case BED -> batch.draw(sprites.get("bed", WorldSprites.BED), x, y, TILE, TILE);
                case SIGN -> batch.draw(sprites.get("sign", WorldSprites.SIGN), x, y, TILE, TILE);
                case PORTAL -> {
                    batch.setColor(1f, 1f, 1f, 0.85f);
                    batch.draw(sprites.get("portal", WorldSprites.PORTAL, WorldSprites.PORTAL_GLOW),
                            x, y, TILE, TILE);
                }
                case LANDMARK -> {
                    batch.setColor(1f, 1f, 1f, 0.08f);
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
            String shape = module.online() ? "module_on" : "module_off";
            batch.setColor(ambient);
            batch.draw(sprites.get(shape, module.definition().colour()), x, y, TILE, TILE);

            // One pip per repair stage, so progress is legible without opening anything.
            int stages = module.definition().stageCount();
            float pipWidth = (TILE - 12f) / stages;
            for (int i = 0; i < stages; i++) {
                boolean done = i < module.completedStages();
                batch.setColor(done ? 0.45f : 0.78f, done ? 0.95f : 0.34f, done ? 0.55f : 0.28f, 1f);
                batch.draw(art.pixel(), x + 6 + i * pipWidth, y + 3, pipWidth - 2f, 2f);
            }
            if (module.online()) {
                batch.setColor(0.6f, 1f, 0.75f, 0.14f);
                batch.draw(art.disc(), x - 8, y - 8, TILE + 16, TILE + 16);
            }
        });
    }

    private void drawResourceNode(GameSession session, WorldMap map, MapObject object, float x, float y) {
        String key = WorldObjectState.key(map.id(), object.id());
        int respawnDays = object.intProperty("respawnDays", 0);
        boolean available = session.worldObjects().isAvailable(key, session.clock().day(), respawnDays);
        if (!available) {
            batch.draw(sprites.get("node_gone", WorldSprites.SPENT_NODE), x, y, TILE, TILE);
            return;
        }
        String colour = session.content().item(object.property("item", ""))
                .map(item -> item.colour())
                .orElse("cccccc");
        batch.draw(sprites.get(WorldSprites.nodeShape(object), colour), x, y, TILE, TILE);
    }

    private void drawColonists(GameSession session, Color ambient, float animationTime) {
        for (Colonist colonist : session.colonistsHere()) {
            float x = colonist.position().x() * TILE;
            float y = colonist.position().y() * TILE;
            // A slow sway so they read as alive while standing at their post.
            boolean second = MathUtils.sin(animationTime * 1.4f + colonist.id().hashCode()) > 0.7f;
            shadow(x + TILE / 2f, y + TILE * 0.18f);
            batch.setColor(ambient);
            batch.draw(sprites.get(second ? "colonist_b" : "colonist_a", colonist.definition().colour()),
                    x, y, TILE, TILE);
        }
    }

    private void drawPlayer(GameSession session, Color ambient, float animationTime) {
        WorldPosition position = session.player().position();
        Direction facing = session.player().facing();
        boolean second = session.player().moving()
                && (int) (animationTime * WALK_SPEED) % 2 == 1;

        String shape = switch (facing) {
            case UP -> second ? "player_up_b" : "player_up_a";
            case DOWN -> second ? "player_down_b" : "player_down_a";
            case LEFT, RIGHT -> second ? "player_side_b" : "player_side_a";
        };
        TextureRegion region = sprites.get(shape, WorldSprites.SUIT, WorldSprites.VISOR);

        // Whole-pixel placement, or the sprite shimmers as it moves.
        float x = Math.round(position.x() * TILE - TILE / 2f);
        float y = Math.round(position.y() * TILE - TILE / 2f);

        shadow(position.x() * TILE, position.y() * TILE - TILE * 0.30f);
        batch.setColor(ambient);
        if (facing == Direction.LEFT) {
            // The side sprite is drawn facing right; flip it rather than authoring a mirror.
            batch.draw(region, x + TILE, y, -TILE, TILE);
        } else {
            batch.draw(region, x, y, TILE, TILE);
        }
    }

    private void shadow(float centreX, float baseY) {
        batch.setColor(0f, 0f, 0f, 0.25f);
        batch.draw(art.disc(), centreX - TILE * 0.26f, baseY - TILE * 0.06f, TILE * 0.52f, TILE * 0.2f);
    }

    private void drawIndicator(InteractionTarget target, float animationTime) {
        GridPoint tile = target.tile();
        float pulse = 0.45f + 0.25f * MathUtils.sin(animationTime * 5f);
        art.outline(batch, new Color(1f, 0.95f, 0.6f, pulse),
                tile.x() * TILE + 1, tile.y() * TILE + 1, TILE - 2, TILE - 2, 2f);
    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
