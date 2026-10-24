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
        float[][] h = gray(haystack, STEP);
        float[][] n = gray(needle, STEP);
        int nh = n.length, nw = n[0].length;
        if (nh > h.length || nw > h[0].length) return Optional.empty();

        double nMean = mean(n, 0, 0, nw, nh);
        double nVar = 0;
        for (float[] row : n) for (float v : row) nVar += (v - nMean) * (v - nMean);
        if (nVar == 0) return Optional.empty();

        double best = -1;
        int bx = 0, by = 0;
        for (int y = 0; y + nh <= h.length; y++) {
            for (int x = 0; x + nw <= h[0].length; x++) {
                double hMean = mean(h, x, y, nw, nh);
                double cov = 0, hVar = 0;
                for (int j = 0; j < nh; j++) {
                    for (int i = 0; i < nw; i++) {
                        double hv = h[y + j][x + i] - hMean;
                        cov += hv * (n[j][i] - nMean);
                        hVar += hv * hv;
                    }
                }
                if (hVar == 0) continue;
                double score = cov / Math.sqrt(hVar * nVar);
                if (score > best) {
                    best = score;
                    bx = x;
                    by = y;
                }
            }
        }
        if (best < threshold) return Optional.empty();
        return Optional.of(new Match((bx + nw / 2.0) * STEP, (by + nh / 2.0) * STEP, best));
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

    private static float[][] gray(BufferedImage img, int step) {
        int w = img.getWidth() / step, h = img.getHeight() / step;
        float[][] g = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x * step, y * step);
                g[y][x] = 0.299f * ((rgb >> 16) & 0xff) + 0.587f * ((rgb >> 8) & 0xff) + 0.114f * (rgb & 0xff);
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
