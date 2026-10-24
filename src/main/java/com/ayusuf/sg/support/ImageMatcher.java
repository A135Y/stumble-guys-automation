package com.ayusuf.sg.support;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;

/** Grayscale template matching (normalised cross-correlation). Good enough for fixed-size UI buttons. */
public final class ImageMatcher {

    public record Match(double x, double y, double score) {
    }

    private static final int STEP = 2;

    private ImageMatcher() {
    }

    /** Best match centre in haystack pixels, if the score reaches {@code threshold} (0..1). */
    public static Optional<Match> find(BufferedImage haystack, BufferedImage needle, double threshold) {
        Match m = best(haystack, needle);
        return m.score() >= threshold ? Optional.of(m) : Optional.empty();
    }

    /** Best match whatever its score; score is -1 if nothing could be compared. */
    public static Match best(BufferedImage haystack, BufferedImage needle) {
        float[][] n = gray(needle, STEP, 0, 0);
        int nh = n.length, nw = n[0].length;
        double nMean = mean(n, 0, 0, nw, nh);
        double nVar = 0;
        for (float[] row : n) for (float v : row) nVar += (v - nMean) * (v - nMean);
        if (nVar == 0) return new Match(0, 0, -1);

        // try every alignment of the downsampling grid, otherwise a template cut at an odd pixel scores low
        Match best = new Match(0, 0, -1);
        for (int py = 0; py < STEP; py++) {
            for (int px = 0; px < STEP; px++) {
                float[][] h = gray(haystack, STEP, px, py);
                if (nh > h.length || nw > h[0].length) continue;
                for (int y = 0; y + nh <= h.length; y++) {
                    for (int x = 0; x + nw <= h[0].length; x++) {
                        double score = ncc(h, x, y, n, nMean, nVar);
                        if (score > best.score()) {
                            best = new Match(x * STEP + px + nw * STEP / 2.0, y * STEP + py + nh * STEP / 2.0, score);
                        }
                    }
                }
            }
        }
        return best;
    }

    private static double ncc(float[][] h, int x, int y, float[][] n, double nMean, double nVar) {
        int nh = n.length, nw = n[0].length;
        double hMean = mean(h, x, y, nw, nh);
        double cov = 0, hVar = 0;
        for (int j = 0; j < nh; j++) {
            for (int i = 0; i < nw; i++) {
                double hv = h[y + j][x + i] - hMean;
                cov += hv * (n[j][i] - nMean);
                hVar += hv * hv;
            }
        }
        return hVar == 0 ? -1 : cov / Math.sqrt(hVar * nVar);
    }

    public static BufferedImage scaleToWidth(BufferedImage src, int width) {
        if (src.getWidth() == width) return src;
        int height = Math.round(src.getHeight() * (width / (float) src.getWidth()));
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    public static BufferedImage load(String resource) {
        try (InputStream in = ImageMatcher.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalArgumentException("Missing image " + resource);
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Downsamples by averaging step x step blocks, so a 1px shift between captures barely changes the result. */
    private static float[][] gray(BufferedImage img, int step, int offsetX, int offsetY) {
        int w = (img.getWidth() - offsetX) / step, h = (img.getHeight() - offsetY) / step;
        float[][] g = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float sum = 0;
                for (int j = 0; j < step; j++) {
                    for (int i = 0; i < step; i++) {
                        int rgb = img.getRGB(offsetX + x * step + i, offsetY + y * step + j);
                        sum += 0.299f * ((rgb >> 16) & 0xff) + 0.587f * ((rgb >> 8) & 0xff) + 0.114f * (rgb & 0xff);
                    }
                }
                g[y][x] = sum / (step * step);
            }
        }
        return g;
    }

    private static double mean(float[][] a, int x, int y, int w, int h) {
        double sum = 0;
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) sum += a[y + j][x + i];
        return sum / (w * h);
    }
}
