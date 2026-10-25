package com.ayusuf.sg.support;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Grayscale template matching (normalised cross-correlation). Good enough for fixed-size UI buttons. */
public final class ImageMatcher {

    public record Match(double x, double y, double score) {
    }

    private static final int STEP = 2;
    private static final int COARSE = 4;
    private static final int CANDIDATES = 5;
    private static final int WINDOW = 3;

    private ImageMatcher() {
    }

    /** Best match centre in haystack pixels, if the score reaches {@code threshold} (0..1). */
    public static Optional<Match> find(BufferedImage haystack, BufferedImage needle, double threshold) {
        Match m = best(haystack, needle);
        return m.score() >= threshold ? Optional.of(m) : Optional.empty();
    }

    /**
     * Best match whatever its score; score is -1 if nothing could be compared.
     * Coarse-to-fine: candidates from a quarter-resolution scan, then the exact match only around them
     * (several times faster than {@link #bestExhaustive}, which it falls back to for tiny templates).
     */
    public static Match best(BufferedImage haystack, BufferedImage needle) {
        float[][] hFull = grayFull(haystack), nFull = grayFull(needle);
        float[][] n4 = downsample(nFull, COARSE, 0, 0);
        if (n4.length < 4 || n4[0].length < 4) return exhaustive(hFull, nFull);
        Stats ns4 = Stats.of(n4);
        if (ns4.var == 0) return exhaustive(hFull, nFull);

        // coarse: top candidates (top-left corner in full-res pixels), kept apart from each other
        List<double[]> candidates = new ArrayList<>();
        for (int py = 0; py < COARSE; py += STEP) {
            for (int px = 0; px < COARSE; px += STEP) {
                float[][] h = downsample(hFull, COARSE, px, py);
                if (n4.length > h.length || n4[0].length > h[0].length) continue;
                for (int y = 0; y + n4.length <= h.length; y++) {
                    for (int x = 0; x + n4[0].length <= h[0].length; x++) {
                        keepTop(candidates, ncc(h, x, y, n4, ns4.mean, ns4.var), x * COARSE + px, y * COARSE + py);
                    }
                }
            }
        }

        // fine: full STEP-resolution match in a small window around each candidate
        float[][] n = downsample(nFull, STEP, 0, 0);
        Stats ns = Stats.of(n);
        Match best = new Match(0, 0, -1);
        for (int py = 0; py < STEP; py++) {
            for (int px = 0; px < STEP; px++) {
                float[][] h = downsample(hFull, STEP, px, py);
                for (double[] c : candidates) {
                    int cx = (int) ((c[1] - px) / STEP), cy = (int) ((c[2] - py) / STEP);
                    for (int y = Math.max(0, cy - WINDOW); y <= Math.min(h.length - n.length, cy + WINDOW); y++) {
                        for (int x = Math.max(0, cx - WINDOW); x <= Math.min(h[0].length - n[0].length, cx + WINDOW); x++) {
                            double score = ncc(h, x, y, n, ns.mean, ns.var);
                            if (score > best.score()) best = centre(x, y, px, py, n, score);
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Scans every position at STEP resolution and every grid alignment. Slow; reference for {@link #best}. */
    public static Match bestExhaustive(BufferedImage haystack, BufferedImage needle) {
        return exhaustive(grayFull(haystack), grayFull(needle));
    }

    private static Match exhaustive(float[][] hFull, float[][] nFull) {
        float[][] n = downsample(nFull, STEP, 0, 0);
        Stats ns = Stats.of(n);
        if (ns.var == 0) return new Match(0, 0, -1);
        // try every alignment of the downsampling grid, otherwise a template cut at an odd pixel scores low
        Match best = new Match(0, 0, -1);
        for (int py = 0; py < STEP; py++) {
            for (int px = 0; px < STEP; px++) {
                float[][] h = downsample(hFull, STEP, px, py);
                if (n.length > h.length || n[0].length > h[0].length) continue;
                for (int y = 0; y + n.length <= h.length; y++) {
                    for (int x = 0; x + n[0].length <= h[0].length; x++) {
                        double score = ncc(h, x, y, n, ns.mean, ns.var);
                        if (score > best.score()) best = centre(x, y, px, py, n, score);
                    }
                }
            }
        }
        return best;
    }

    private static Match centre(int x, int y, int px, int py, float[][] n, double score) {
        return new Match(x * STEP + px + n[0].length * STEP / 2.0, y * STEP + py + n.length * STEP / 2.0, score);
    }

    private static void keepTop(List<double[]> top, double score, int x, int y) {
        if (top.size() == CANDIDATES && score <= top.getLast()[0]) return;
        for (int k = 0; k < top.size(); k++) {
            double[] c = top.get(k);
            if (Math.abs(c[1] - x) <= COARSE * 2 && Math.abs(c[2] - y) <= COARSE * 2) {
                if (score > c[0]) {
                    top.remove(k);
                    break;
                }
                return;
            }
        }
        top.add(new double[]{score, x, y});
        top.sort((a, b) -> Double.compare(b[0], a[0]));
        if (top.size() > CANDIDATES) top.removeLast();
    }

    private record Stats(double mean, double var) {
        static Stats of(float[][] a) {
            double sum = 0;
            int count = 0;
            for (float[] row : a) for (float v : row) { sum += v; count++; }
            double mean = sum / count, var = 0;
            for (float[] row : a) for (float v : row) var += (v - mean) * (v - mean);
            return new Stats(mean, var);
        }
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

    /** Mean absolute grey-level difference (0-255) between two same-size images, sampled every 4th pixel. */
    public static double meanDifference(BufferedImage a, BufferedImage b) {
        int w = Math.min(a.getWidth(), b.getWidth()), h = Math.min(a.getHeight(), b.getHeight());
        long sum = 0, n = 0;
        for (int y = 0; y < h; y += 4) {
            for (int x = 0; x < w; x += 4) {
                sum += Math.abs(grey(a.getRGB(x, y)) - grey(b.getRGB(x, y)));
                n++;
            }
        }
        return (double) sum / n;
    }

    private static int grey(int rgb) {
        return (((rgb >> 16) & 0xff) * 299 + ((rgb >> 8) & 0xff) * 587 + (rgb & 0xff) * 114) / 1000;
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

    private static float[][] grayFull(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] rgb = img.getRGB(0, 0, w, h, null, 0, w);
        float[][] g = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = rgb[y * w + x];
                g[y][x] = 0.299f * ((p >> 16) & 0xff) + 0.587f * ((p >> 8) & 0xff) + 0.114f * (p & 0xff);
            }
        }
        return g;
    }

    /** Averages step x step blocks starting at an offset, so a 1px shift between captures barely changes the result. */
    private static float[][] downsample(float[][] g, int step, int offsetX, int offsetY) {
        int w = (g[0].length - offsetX) / step, h = (g.length - offsetY) / step;
        float[][] out = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float sum = 0;
                for (int j = 0; j < step; j++) {
                    for (int i = 0; i < step; i++) sum += g[offsetY + y * step + j][offsetX + x * step + i];
                }
                out[y][x] = sum / (step * step);
            }
        }
        return out;
    }

    private static double mean(float[][] a, int x, int y, int w, int h) {
        double sum = 0;
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) sum += a[y + j][x + i];
        return sum / (w * h);
    }
}
