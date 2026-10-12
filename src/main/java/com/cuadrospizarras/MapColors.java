package com.cuadrospizarras;

/**
 * Paleta de colores de los mapas de Minecraft, independiente de la API del juego.
 *
 * Cada píxel de un mapa es un byte: (idBase * 4 + brillo). Hay 61 colores base
 * (el 0 es transparente) y 4 brillos. Esta tabla replica los colores vanilla
 * para poder previsualizar y convertir imágenes en el cliente.
 */
public final class MapColors {
	public static final int SIZE = 128;
	public static final int PIXELS = SIZE * SIZE;
	public static final int BASE_COUNT = 62; // ids 0..61
	public static final int WHITE = 8 * 4 + 2; // SNOW, brillo máximo => 255,255,255
	public static final int BLACK = 29 * 4 + 2; // BLACK, brillo máximo => 25,25,25

	// RGB de cada color base de mapa (índice = id base)
	private static final int[] BASE_RGB = {
		0x000000, // 0  NONE (transparente)
		0x7FB238, 0xF7E9A3, 0xC7C7C7, 0xFF0000, 0xA0A0FF, 0xA7A7A7, 0x007C00, 0xFFFFFF, // 1-8
		0xA4A8B8, 0x976D4D, 0x707070, 0x4040FF, 0x8F7748, 0xFFFCF5, 0xD87F33, 0xB24CD8, // 9-16
		0x6699D8, 0xE5E533, 0x7FCC19, 0xF27FA5, 0x4C4C4C, 0x999999, 0x4C7F99, 0x7F3FB2, // 17-24
		0x334CB2, 0x664C33, 0x667F33, 0x993333, 0x191919, 0xFAEE4D, 0x5CDBD5, 0x4A80FF, // 25-32
		0x00D93A, 0x815631, 0x700200, 0xD1B1A1, 0x9F5224, 0x95576C, 0x706C8A, 0xBA8524, // 33-40
		0x677535, 0xA04D4E, 0x392923, 0x876B62, 0x575C5C, 0x7A4958, 0x4C3E5C, 0x4C3223, // 41-48
		0x4C522A, 0x8E3C2E, 0x251610, 0xBD3031, 0x943F61, 0x5C191D, 0x167E86, 0x3A8E8C, // 49-56
		0x562C3E, 0x14B485, 0x646464, 0xD8AF93, 0x7FA796 // 57-61
	};

	// Multiplicador de brillo según los 2 bits bajos del byte de color
	private static final int[] SHADE = {180, 220, 255, 135};

	/** Colores utilizables (ids 4..247), excluyendo el transparente. */
	public static final int[] PALETTE_IDS = new int[(BASE_COUNT - 1) * 4];
	public static final int[] PALETTE_R = new int[PALETTE_IDS.length];
	public static final int[] PALETTE_G = new int[PALETTE_IDS.length];
	public static final int[] PALETTE_B = new int[PALETTE_IDS.length];

	static {
		int n = 0;
		for (int base = 1; base < BASE_COUNT; base++) {
			for (int shade = 0; shade < 4; shade++) {
				int packed = base * 4 + shade;
				int rgb = rgbOf(packed);
				PALETTE_IDS[n] = packed;
				PALETTE_R[n] = (rgb >> 16) & 0xFF;
				PALETTE_G[n] = (rgb >> 8) & 0xFF;
				PALETTE_B[n] = rgb & 0xFF;
				n++;
			}
		}
	}

	private MapColors() {
	}

	/** RGB (sin alfa) de un byte de color de mapa. */
	public static int rgbOf(int packed) {
		int base = (packed & 0xFF) >> 2;
		if (base <= 0 || base >= BASE_COUNT) {
			return 0;
		}

		int rgb = BASE_RGB[base];
		int m = SHADE[packed & 3];
		int r = ((rgb >> 16) & 0xFF) * m / 255;
		int g = ((rgb >> 8) & 0xFF) * m / 255;
		int b = (rgb & 0xFF) * m / 255;
		return (r << 16) | (g << 8) | b;
	}

	/** Color ARGB opaco para dibujar en pantalla (el "transparente" se ve gris oscuro). */
	public static int argbOf(int packed) {
		if (((packed & 0xFF) >> 2) == 0) {
			return 0xFF303030;
		}
		return 0xFF000000 | rgbOf(packed);
	}

	/** Id base (1..61) de un color de paleta; útil para las muestras de color. */
	public static int baseOf(int packed) {
		return (packed & 0xFF) >> 2;
	}

	/**
	 * Un cliente modificado podría enviar bytes fuera de la paleta, lo que haría
	 * fallar el renderizado de mapas a otros jugadores. Se corrigen aquí.
	 */
	public static void sanitize(byte[] colors) {
		for (int i = 0; i < colors.length; i++) {
			int base = (colors[i] & 0xFF) >> 2;
			if (base >= BASE_COUNT) {
				colors[i] = 0;
			}
		}
	}

	/** Índice de la paleta más cercano a un color (distancia "redmean", más fiel al ojo). */
	public static int nearestIndex(float r, float g, float b) {
		int best = 0;
		float bestDist = Float.MAX_VALUE;

		for (int i = 0; i < PALETTE_IDS.length; i++) {
			float dr = r - PALETTE_R[i];
			float dg = g - PALETTE_G[i];
			float db = b - PALETTE_B[i];
			float rm = (r + PALETTE_R[i]) * 0.5f;
			float dist = (2f + rm / 256f) * dr * dr + 4f * dg * dg + (2f + (255f - rm) / 256f) * db * db;

			if (dist < bestDist) {
				bestDist = dist;
				best = i;
			}
		}

		return best;
	}
}
