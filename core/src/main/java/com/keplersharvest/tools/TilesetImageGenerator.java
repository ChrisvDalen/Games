package com.keplersharvest.tools;

import com.keplersharvest.configuration.FileResourceReader;
import com.keplersharvest.world.TiledJsonMapLoader;
import com.keplersharvest.world.TileSet;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Regenerates {@code assets/maps/meridian_tileset.png} from the colours declared in the
 * {@code .tsj} tileset.
 *
 * <p>The running game never loads this image - it draws the same colours procedurally. The PNG
 * exists purely so the {@code .tmj} maps open with visible tiles in Tiled. Run it with
 * {@code ./gradlew generateTilesetImage} after changing a tile colour.
 */
public final class TilesetImageGenerator {

    private static final int TILE = 32;
    private static final int COLUMNS = 4;

    private TilesetImageGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path assets = Path.of(args.length > 0 ? args[0] : "assets");
        FileResourceReader reader = new FileResourceReader(assets);
        TileSet tileSet = new TiledJsonMapLoader(reader).loadTileSet("maps/meridian_tileset.tsj");

        int rows = (int) Math.ceil(tileSet.tileCount() / (double) COLUMNS);
        BufferedImage image = new BufferedImage(COLUMNS * TILE, rows * TILE, BufferedImage.TYPE_INT_ARGB);

        for (int id = 0; id < tileSet.tileCount(); id++) {
            int originX = (id % COLUMNS) * TILE;
            int originY = (id / COLUMNS) * TILE;
            int rgb = 0xFF000000 | Integer.parseInt(tileSet.colourForLocal(id, "ff00ff"), 16);
            int edge = darken(rgb);
            for (int y = 0; y < TILE; y++) {
                for (int x = 0; x < TILE; x++) {
                    boolean border = x == 0 || y == 0 || x == TILE - 1 || y == TILE - 1;
                    image.setRGB(originX + x, originY + y, border ? edge : rgb);
                }
            }
        }

        Path output = assets.resolve("maps/meridian_tileset.png");
        ImageIO.write(image, "png", output.toFile());
        System.out.println("Wrote " + output + " (" + tileSet.tileCount() + " tiles)");
    }

    private static int darken(int argb) {
        int r = (int) (((argb >> 16) & 0xFF) * 0.72);
        int g = (int) (((argb >> 8) & 0xFF) * 0.72);
        int b = (int) ((argb & 0xFF) * 0.72);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
