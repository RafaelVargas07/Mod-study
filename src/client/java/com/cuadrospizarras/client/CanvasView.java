package com.cuadrospizarras.client;

import java.util.Arrays;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import com.cuadrospizarras.MapColors;

/** Dibuja en pantalla un lienzo de 128x128 colores de mapa. */
public final class CanvasView {
	private CanvasView() {
	}

	/**
	 * @param px tamaño en píxeles de pantalla de cada celda del lienzo
	 */
	public static void draw(GuiGraphicsExtractor graphics, byte[] canvas, int x0, int y0, int px) {
		final int size = MapColors.SIZE;
		int row = 0;

		while (row < size) {
			// Agrupa filas consecutivas idénticas en una sola banda (menos rectángulos que dibujar)
			int bandEnd = row + 1;
			while (bandEnd < size && Arrays.equals(
					canvas, row * size, row * size + size,
					canvas, bandEnd * size, bandEnd * size + size)) {
				bandEnd++;
			}

			int x = 0;
			while (x < size) {
				byte value = canvas[row * size + x];
				int end = x + 1;
				while (end < size && canvas[row * size + end] == value) {
					end++;
				}

				graphics.fill(x0 + x * px, y0 + row * px, x0 + end * px, y0 + bandEnd * px,
						MapColors.argbOf(value & 0xFF));
				x = end;
			}

			row = bandEnd;
		}
	}
}
