package com.cuadrospizarras.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import com.cuadrospizarras.MapColors;

/** Utilidades puras (sin API de Minecraft): buscar imágenes, leerlas y convertirlas a colores de mapa. */
public final class ImageTools {
	private static final String[] FOLDERS = {"screenshots", "cuadros"};
	private static final int MAX_FILES = 100;

	/** Imagen ya leída: píxeles ARGB y tamaño. */
	public record Loaded(int[] argb, int width, int height) {
	}

	private ImageTools() {
	}

	/** Imágenes de las carpetas "screenshots" y "cuadros", de más reciente a más antigua. */
	public static List<Path> listImages(Path gameDir) {
		List<Path> result = new ArrayList<>();

		for (String folder : FOLDERS) {
			Path dir = gameDir.resolve(folder);

			if (!Files.isDirectory(dir)) {
				continue;
			}

			try (Stream<Path> stream = Files.list(dir)) {
				stream.filter(Files::isRegularFile).filter(ImageTools::isImage).forEach(result::add);
			} catch (IOException ignored) {
				// carpeta ilegible: se ignora
			}
		}

		result.sort((a, b) -> Long.compare(modified(b), modified(a)));
		return result.size() > MAX_FILES ? new ArrayList<>(result.subList(0, MAX_FILES)) : result;
	}

	private static boolean isImage(Path path) {
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
				|| name.endsWith(".bmp") || name.endsWith(".gif");
	}

	private static long modified(Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException e) {
			return 0L;
		}
	}

	public static Loaded load(Path path) throws IOException {
		BufferedImage image = ImageIO.read(path.toFile());

		if (image == null) {
			throw new IOException("Formato de imagen no soportado");
		}

		int w = image.getWidth();
		int h = image.getHeight();
		return new Loaded(image.getRGB(0, 0, w, h, null, 0, w), w, h);
	}

	/**
	 * Escala la imagen a 128x128 y la convierte a la paleta de mapas.
	 *
	 * @param cover  true = rellenar recortando los bordes; false = encajar entera con barras negras
	 * @param dither true = tramado Floyd–Steinberg (más detalle en fotos)
	 */
	public static byte[] toMapColors(Loaded img, boolean cover, boolean dither) {
		final int size = MapColors.SIZE;
		final int w = img.width();
		final int h = img.height();
		final int[] argb = img.argb();

		double scale = cover
				? Math.max((double) size / w, (double) size / h)
				: Math.min((double) size / w, (double) size / h);
		double offX = (size - w * scale) / 2.0;
		double offY = (size - h * scale) / 2.0;

		float[] rgb = new float[size * size * 3];
		boolean[] bar = new boolean[size * size];

		// 1) Reducir a 128x128 promediando los píxeles de origen de cada píxel de destino
		for (int ty = 0; ty < size; ty++) {
			double sy0 = (ty - offY) / scale;
			double sy1 = (ty + 1 - offY) / scale;

			for (int tx = 0; tx < size; tx++) {
				int idx = ty * size + tx;
				double sx0 = (tx - offX) / scale;
				double sx1 = (tx + 1 - offX) / scale;

				if (sx1 <= 0 || sx0 >= w || sy1 <= 0 || sy0 >= h) {
					bar[idx] = true;
					continue;
				}

				int x0 = clamp((int) Math.floor(sx0), 0, w - 1);
				int x1 = clamp((int) Math.ceil(sx1), x0 + 1, w);
				int y0 = clamp((int) Math.floor(sy0), 0, h - 1);
				int y1 = clamp((int) Math.ceil(sy1), y0 + 1, h);

				long sumR = 0;
				long sumG = 0;
				long sumB = 0;
				int count = 0;

				for (int y = y0; y < y1; y++) {
					for (int x = x0; x < x1; x++) {
						int p = argb[y * w + x];
						int a = p >>> 24;
						int r = (p >> 16) & 0xFF;
						int g = (p >> 8) & 0xFF;
						int b = p & 0xFF;

						if (a < 255) { // transparencia sobre fondo blanco
							r = (r * a + 255 * (255 - a)) / 255;
							g = (g * a + 255 * (255 - a)) / 255;
							b = (b * a + 255 * (255 - a)) / 255;
						}

						sumR += r;
						sumG += g;
						sumB += b;
						count++;
					}
				}

				rgb[idx * 3] = sumR / (float) count;
				rgb[idx * 3 + 1] = sumG / (float) count;
				rgb[idx * 3 + 2] = sumB / (float) count;
			}
		}

		// 2) Convertir cada píxel al color de mapa más cercano (con tramado opcional)
		byte[] out = new byte[size * size];
		float[] errCur = new float[(size + 2) * 3];
		float[] errNext = new float[(size + 2) * 3];

		for (int y = 0; y < size; y++) {
			Arrays.fill(errNext, 0f);

			for (int x = 0; x < size; x++) {
				int idx = y * size + x;

				if (bar[idx]) {
					out[idx] = (byte) MapColors.BLACK;
					continue;
				}

				int e = (x + 1) * 3;
				float r = clamp255(rgb[idx * 3] + (dither ? errCur[e] : 0f));
				float g = clamp255(rgb[idx * 3 + 1] + (dither ? errCur[e + 1] : 0f));
				float b = clamp255(rgb[idx * 3 + 2] + (dither ? errCur[e + 2] : 0f));

				int pi = MapColors.nearestIndex(r, g, b);
				out[idx] = (byte) MapColors.PALETTE_IDS[pi];

				if (dither) {
					float er = r - MapColors.PALETTE_R[pi];
					float eg = g - MapColors.PALETTE_G[pi];
					float eb = b - MapColors.PALETTE_B[pi];
					spread(errCur, e + 3, er, eg, eb, 7f / 16f); // derecha
					spread(errNext, e - 3, er, eg, eb, 3f / 16f); // abajo-izquierda
					spread(errNext, e, er, eg, eb, 5f / 16f); // abajo
					spread(errNext, e + 3, er, eg, eb, 1f / 16f); // abajo-derecha
				}
			}

			float[] tmp = errCur;
			errCur = errNext;
			errNext = tmp;
		}

		return out;
	}

	private static void spread(float[] err, int at, float er, float eg, float eb, float weight) {
		err[at] += er * weight;
		err[at + 1] += eg * weight;
		err[at + 2] += eb * weight;
	}

	private static int clamp(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}

	private static float clamp255(float v) {
		return Math.max(0f, Math.min(255f, v));
	}
}
