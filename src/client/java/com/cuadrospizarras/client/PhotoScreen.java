package com.cuadrospizarras.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

import com.cuadrospizarras.CuadrosPizarras;
import com.cuadrospizarras.MapColors;
import com.cuadrospizarras.SaveCanvasPayload;

/**
 * Cámara: elige una captura de pantalla (carpeta "screenshots") o cualquier imagen
 * de la carpeta "cuadros", la convierte a colores de mapa y la guarda en un cuadro.
 */
public class PhotoScreen extends Screen {
	private static final int RIGHT_WIDTH = 120;

	private final List<Path> files;
	private final byte[] canvas = new byte[MapColors.PIXELS];

	private int index;
	private boolean cover = true;
	private boolean dither = true;
	private boolean hasImage;
	private String error = "";
	private ImageTools.Loaded loaded;
	private int loadedIndex = -1;

	private int px = 3;
	private int canvasX;
	private int canvasY;
	private int rightX;

	private Button fitButton;
	private Button ditherButton;
	private Button saveButton;

	public PhotoScreen() {
		super(Component.translatable("gui.cuadrospizarras.camera.title"));
		this.files = ImageTools.listImages(FabricLoader.getInstance().getGameDir());
		this.rebuild();
	}

	@Override
	protected void init() {
		int availableW = this.width - RIGHT_WIDTH - 40;
		int availableH = this.height - 50;
		this.px = Math.max(1, Math.min(availableW / MapColors.SIZE, availableH / MapColors.SIZE));

		int canvasSize = MapColors.SIZE * this.px;
		int totalW = canvasSize + 12 + RIGHT_WIDTH;
		this.canvasX = Math.max(4, (this.width - totalW) / 2);
		this.canvasY = Math.max(18, (this.height - canvasSize) / 2);
		this.rightX = this.canvasX + canvasSize + 12;

		int y = this.canvasY;
		int step = 22;

		this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.go(-1))
				.bounds(this.rightX, y, 58, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.go(1))
				.bounds(this.rightX + 62, y, 58, 20).build());
		y += step + 14;

		this.fitButton = this.addRenderableWidget(Button.builder(this.fitLabel(), button -> {
			this.cover = !this.cover;
			this.fitButton.setMessage(this.fitLabel());
			this.rebuild();
		}).bounds(this.rightX, y, RIGHT_WIDTH, 20).build());
		y += step;

		this.ditherButton = this.addRenderableWidget(Button.builder(this.ditherLabel(), button -> {
			this.dither = !this.dither;
			this.ditherButton.setMessage(this.ditherLabel());
			this.rebuild();
		}).bounds(this.rightX, y, RIGHT_WIDTH, 20).build());
		y += step + 14;

		this.saveButton = this.addRenderableWidget(Button.builder(
				Component.translatable("gui.cuadrospizarras.save"), button -> this.save())
				.bounds(this.rightX, y, RIGHT_WIDTH, 20).build());
		this.saveButton.active = this.hasImage;
		y += step;

		this.addRenderableWidget(Button.builder(
				Component.translatable("gui.cuadrospizarras.close"), button -> this.onClose())
				.bounds(this.rightX, y, RIGHT_WIDTH, 20).build());
	}

	private Component fitLabel() {
		return Component.translatable(this.cover ? "gui.cuadrospizarras.fit.cover" : "gui.cuadrospizarras.fit.contain");
	}

	private Component ditherLabel() {
		return Component.translatable(this.dither ? "gui.cuadrospizarras.dither.on" : "gui.cuadrospizarras.dither.off");
	}

	private void go(int delta) {
		if (this.files.isEmpty()) {
			return;
		}

		this.index = Math.floorMod(this.index + delta, this.files.size());
		this.rebuild();
	}

	/** Lee la imagen actual (si hace falta) y recalcula la vista previa. */
	private void rebuild() {
		this.hasImage = false;
		this.error = "";

		if (!this.files.isEmpty()) {
			try {
				if (this.loadedIndex != this.index || this.loaded == null) {
					this.loaded = ImageTools.load(this.files.get(this.index));
					this.loadedIndex = this.index;
				}

				byte[] converted = ImageTools.toMapColors(this.loaded, this.cover, this.dither);
				System.arraycopy(converted, 0, this.canvas, 0, this.canvas.length);
				this.hasImage = true;
			} catch (IOException | RuntimeException | OutOfMemoryError e) {
				this.loaded = null;
				this.loadedIndex = -1;
				this.error = "No se pudo leer la imagen";
				CuadrosPizarras.LOGGER.warn("No se pudo leer {}: {}", this.files.get(this.index), e.toString());
			}
		}

		if (!this.hasImage) {
			java.util.Arrays.fill(this.canvas, (byte) 0);
		}

		if (this.saveButton != null) {
			this.saveButton.active = this.hasImage;
		}
	}

	private void save() {
		if (!this.hasImage) {
			return;
		}

		String name = this.files.get(this.index).getFileName().toString();
		int dot = name.lastIndexOf('.');
		if (dot > 0) {
			name = name.substring(0, dot);
		}
		if (name.length() > SaveCanvasPayload.MAX_NAME) {
			name = name.substring(0, SaveCanvasPayload.MAX_NAME);
		}

		ClientPlayNetworking.send(new SaveCanvasPayload(name, this.canvas.clone()));
		this.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		int canvasSize = MapColors.SIZE * this.px;

		graphics.text(this.font, this.title, this.canvasX, this.canvasY - 12, 0xFFFFFFFF, true);
		graphics.fill(this.canvasX - 2, this.canvasY - 2, this.canvasX + canvasSize + 2, this.canvasY + canvasSize + 2, 0xFF000000);

		if (this.hasImage) {
			CanvasView.draw(graphics, this.canvas, this.canvasX, this.canvasY, this.px);
		} else {
			graphics.fill(this.canvasX, this.canvasY, this.canvasX + canvasSize, this.canvasY + canvasSize, 0xFF202020);
			Component message = this.files.isEmpty()
					? Component.translatable("gui.cuadrospizarras.camera.empty")
					: Component.literal(this.error);
			graphics.text(this.font, message, this.canvasX + 6, this.canvasY + 6, 0xFFFF7777, true);
		}

		if (!this.files.isEmpty()) {
			String name = this.files.get(this.index).getFileName().toString();
			if (name.length() > 22) {
				name = name.substring(0, 19) + "...";
			}

			graphics.text(this.font, (this.index + 1) + " / " + this.files.size(),
					this.rightX, this.canvasY + 26, 0xFFFFFFFF, true);
			graphics.text(this.font, name, this.rightX, this.canvasY + 38, 0xFFAAAAAA, true);
		}

		graphics.text(this.font, Component.translatable("gui.cuadrospizarras.camera.hint"),
				this.canvasX, this.canvasY + canvasSize + 6, 0xFFAAAAAA, true);
	}
}
