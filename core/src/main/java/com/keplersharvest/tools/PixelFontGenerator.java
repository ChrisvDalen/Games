package com.keplersharvest.tools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Turns {@link PixelGlyphs} into the font files the game loads.
 *
 * <p>Writes {@code assets/ui/pixel-font.png} and {@code assets/ui/pixel-font.fnt} in AngelCode
 * format. Both are build output generated from glyph data committed in this repository, so the game
 * ships a crisp bitmap font without depending on any third-party typeface.
 *
 * <p>Run with {@code ./gradlew generatePixelFont} after editing a glyph.
 */
public final class PixelFontGenerator {

    private static final int GLYPH_WIDTH = 5;
    private static final int GLYPH_HEIGHT = 8;
    /** One pixel of transparent padding so neighbouring glyphs cannot bleed into each other. */
    private static final int CELL_WIDTH = GLYPH_WIDTH + 1;
    private static final int CELL_HEIGHT = GLYPH_HEIGHT + 1;
    private static final int COLUMNS = 16;

    /** Rows 0-6 sit above the baseline; row 7 is the descender row. */
    private static final int BASELINE = 7;
    private static final int LINE_HEIGHT = 10;
    /** Monospace: a fixed advance suits the chunky look and makes layout arithmetic exact. */
    private static final int ADVANCE = 6;

    private PixelFontGenerator() {
    }

    public static void main(String[] args) throws IOException {
        write(Path.of(args.length > 0 ? args[0] : "assets"));
    }

    static void write(Path assets) throws IOException {
        String[] glyphs = PixelGlyphs.TABLE;
        int rows = (int) Math.ceil(glyphs.length / (double) COLUMNS);
        int atlasWidth = COLUMNS * CELL_WIDTH;
        int atlasHeight = rows * CELL_HEIGHT;

        BufferedImage image = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB);
        StringBuilder chars = new StringBuilder();

        for (int index = 0; index < glyphs.length; index++) {
            String glyph = glyphs[index];
            if (glyph.length() != GLYPH_WIDTH * GLYPH_HEIGHT) {
                throw new IllegalStateException("Glyph '" + (char) (PixelGlyphs.FIRST_CHAR + index)
                        + "' has " + glyph.length() + " pixels, expected " + GLYPH_WIDTH * GLYPH_HEIGHT);
            }
            int originX = (index % COLUMNS) * CELL_WIDTH;
            int originY = (index / COLUMNS) * CELL_HEIGHT;
            for (int y = 0; y < GLYPH_HEIGHT; y++) {
                for (int x = 0; x < GLYPH_WIDTH; x++) {
                    boolean ink = glyph.charAt(y * GLYPH_WIDTH + x) == '#';
                    image.setRGB(originX + x, originY + y, ink ? 0xFFFFFFFF : 0x00000000);
                }
            }
            chars.append("char id=").append(PixelGlyphs.FIRST_CHAR + index)
                    .append(" x=").append(originX)
                    .append(" y=").append(originY)
                    .append(" width=").append(GLYPH_WIDTH)
                    .append(" height=").append(GLYPH_HEIGHT)
                    .append(" xoffset=0 yoffset=0 xadvance=").append(ADVANCE)
                    .append(" page=0 chnl=15\n");
        }

        Path uiDirectory = assets.resolve("ui");
        Files.createDirectories(uiDirectory);
        ImageIO.write(image, "png", uiDirectory.resolve("pixel-font.png").toFile());

        String descriptor = "info face=\"Meridian Pixel\" size=" + GLYPH_HEIGHT
                + " bold=0 italic=0 charset=\"\" unicode=1 stretchH=100 smooth=0 aa=1"
                + " padding=0,0,0,0 spacing=1,1\n"
                + "common lineHeight=" + LINE_HEIGHT + " base=" + BASELINE
                + " scaleW=" + atlasWidth + " scaleH=" + atlasHeight + " pages=1 packed=0\n"
                + "page id=0 file=\"pixel-font.png\"\n"
                + "chars count=" + glyphs.length + "\n"
                + chars;
        Files.writeString(uiDirectory.resolve("pixel-font.fnt"), descriptor, StandardCharsets.UTF_8);

        System.out.println("Wrote " + uiDirectory.resolve("pixel-font.png") + " and pixel-font.fnt ("
                + glyphs.length + " glyphs, " + atlasWidth + "x" + atlasHeight + ")");
    }
}
