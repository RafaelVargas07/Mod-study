package com.cuadrospizarras.client;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.cuadrospizarras.MapColors;
import com.cuadrospizarras.SaveCanvasPayload;

/** Pizarra: lienzo de 128x128 para dibujar con el ratón con los colores de los mapas. */
public class DrawScreen extends Screen {
	private enum Tool {
		PEN,
		ERASER,
		FILL
	}

	private static final int SIZE = MapColors.SIZE;
	private static final int SWATCH = 10;
	private static final int COLUMNS = 6;
	private static final int SHADE_BOX = 14;
	private static final int[] SHADE_ORDER = {3, 0, 1, 2}; // de más oscuro a más claro
	private static final int RIGHT_WIDTH = 104;
	private static final int MAX_UNDO = 30;

	private final byte[] canvas = new byte[MapColors.PIXELS];
	private final Deque<byte[]> undoStack = new ArrayDeque<>();

	private Tool tool = Tool.PEN;
	private int color = 29 * 4 + 2; // negro
	private int brush = 1;

	private boolean drawing;
	private boolean erasingStroke;
	private int lastX;
	private int lastY;

	// Distribución (se recalcula en init)
	private int px = 3;
	private int canvasX;
	private int canvasY;
	private int paletteX;
	private int paletteY;
	private int rightX;

	private Button penButton;
	private Button eraserButton;
	private Button fillButton;

	public DrawScreen() {
		super(Component.translatable("gui.cuadrospizarras.board.title"));
		Arrays.fill(this.canvas, (byte) MapColors.WHITE);
	}

	@Override
	protected void init() {
		int paletteWidth = COLUMNS * SWATCH;
		int availableW = this.width - paletteWidth - RIGHT_WIDTH - 40;
		int availableH = this.height - 40;
		this.px = Math.max(1, Math.min(availableW / SIZE, availableH / SIZE));

		int canvasSize = SIZE * this.px;
		int totalW = paletteWidth + 10 + canvasSize + 10 + RIGHT_WIDTH;
		int startX = Math.max(4, (this.width - totalW) / 2);

		this.paletteX = startX;
		this.canvasX = startX + paletteWidth + 10;
		this.rightX = this.canvasX + canvasSize + 10;
		this.canvasY = Math.max(16, (this.height - canvasSize) / 2);
		this.paletteY = this.canvasY;

		int y = this.canvasY;
		int step = 22;

		this.penButton = this.addRenderableWidget(Button.builder(
				Component.translatable("gui.cuadrospizarras.pen"), button -> this.tool = Tool.PEN)
				.bounds(this.rightX, y, RIGHT_WIDTH, 20).build());
		y += step;
		this.eraserButton = this.addRenderableWidget(Button.builder(
				Component.translatable("gui.cuadrospizarras.eraser"), button -> this.tool = Tool.ERASER)
				.bounds(this.rightX, y, RIGHT_WIDTH, 20).build());
		y += step;
		this.fillButton = this.addRenderableWidget(Button.builder(
				Component.translatable("gui.cuadrospizarras.fill"), button -> this.tool = Tool.FILL)
				.bounds(this.rightX, y, RIGHT_WIDTH, 20).build());
		y += step;

		this.addRenderableWidget(Button.builder(Component.literal("-"),
				button -> this.brush = Math.max(1, this.brush - 1))
				.bounds(this.rightX, y, 50, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("+"),
				button -> this.brush = Math.min(8, this.brush + 1))
				.bounds(this.rightX + 54, y, 50, 20).build());
		y += step + 12;

		this.addRenderableWidget(Button.builder(
				Component.translatable("gui.cuadrospizarras.undo"), button -> this.undo())
				.bounds(this.rightX, y, RIGHT_WIDTH, 20).build());
		y += step;
		this.addRenderableWidget(Button.builder(
				Component.translatable("gui.cuadrospizarras.clear"), button -> {
					this.pushUndo();
					Arrays.fill(this.canvas, (byte) MapColors.WHITE);
				})
				.bounds(this.rightX, y, RIGHT_WIDTH, 20).build());
		y += step + 6;

		this.addRenderableWidget(Button.builder(
				Component.translatable("gui.cuadrospizarras.save"), button -> this.save())
				.bounds(this.rightX, y, RIGHT_WIDTH, 20).build());
		y += step;
		this.addRenderableWidget(Button.builder(
				Component.translatable("gui.cuadrospizarras.close"), button -> this.onClose())
				.bounds(this.rightX, y, RIGHT_WIDTH, 20).build());
	}

	private void save() {
		String name = Component.translatable("gui.cuadrospizarras.default_drawing").getString();
		ClientPlayNetworking.send(new SaveCanvasPayload(name, this.canvas.clone()));
		this.onClose();
	}

	// ---------- Edición del lienzo ----------

	private void pushUndo() {
		this.undoStack.push(this.canvas.clone());

		while (this.undoStack.size() > MAX_UNDO) {
			this.undoStack.removeLast();
		}
	}

	private void undo() {
		if (!this.undoStack.isEmpty()) {
			System.arraycopy(this.undoStack.pop(), 0, this.canvas, 0, this.canvas.length);
		}
	}

	private void stamp(int cx, int cy, byte value) {
		int r = this.brush - 1;

		for (int dy = -r; dy <= r; dy++) {
			for (int dx = -r; dx <= r; dx++) {
				if (dx * dx + dy * dy > r * r + r) {
					continue; // esquinas fuera: pincel redondeado
				}

				int x = cx + dx;
				int y = cy + dy;

				if (x >= 0 && x < SIZE && y >= 0 && y < SIZE) {
					this.canvas[y * SIZE + x] = value;
				}
			}
		}
	}

	/** Línea de Bresenham para que el trazo no deje huecos al mover el ratón rápido. */
	private void line(int x0, int y0, int x1, int y1, byte value) {
		int dx = Math.abs(x1 - x0);
		int dy = -Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx + dy;

		while (true) {
			this.stamp(x0, y0, value);

			if (x0 == x1 && y0 == y1) {
				break;
			}

			int e2 = 2 * err;

			if (e2 >= dy) {
				err += dy;
				x0 += sx;
			}

			if (e2 <= dx) {
				err += dx;
				y0 += sy;
			}
		}
	}

	private void floodFill(int startX, int startY, byte value) {
		byte target = this.canvas[startY * SIZE + startX];

		if (target == value) {
			return;
		}

		Deque<Integer> pending = new ArrayDeque<>();
		pending.push(startY * SIZE + startX);

		while (!pending.isEmpty()) {
			int i = pending.pop();

			if (this.canvas[i] != target) {
				continue;
			}

			this.canvas[i] = value;
			int x = i % SIZE;
			int y = i / SIZE;

			if (x > 0) {
				pending.push(i - 1);
			}
			if (x < SIZE - 1) {
				pending.push(i + 1);
			}
			if (y > 0) {
				pending.push(i - SIZE);
			}
			if (y < SIZE - 1) {
				pending.push(i + SIZE);
			}
		}
	}

	private int cellX(double mouseX) {
		return (int) Math.floor((mouseX - this.canvasX) / this.px);
	}

	private int cellY(double mouseY) {
		return (int) Math.floor((mouseY - this.canvasY) / this.px);
	}

	private static boolean inside(int cx, int cy) {
		return cx >= 0 && cx < SIZE && cy >= 0 && cy < SIZE;
	}

	// ---------- Entrada ----------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}

		double mx = event.x();
		double my = event.y();
		int button = event.button();

		// Paleta de colores
		if (button == 0 && this.clickPalette(mx, my)) {
			return true;
		}

		// Lienzo: clic izquierdo dibuja, clic derecho borra
		int cx = this.cellX(mx);
		int cy = this.cellY(my);

		if ((button == 0 || button == 1) && inside(cx, cy)) {
			this.pushUndo();
			boolean erase = button == 1 || this.tool == Tool.ERASER;
			byte value = (byte) (erase ? MapColors.WHITE : this.color);

			if (this.tool == Tool.FILL) {
				this.floodFill(cx, cy, value);
			} else {
				this.drawing = true;
				this.erasingStroke = erase;
				this.lastX = cx;
				this.lastY = cy;
				this.stamp(cx, cy, value);
			}

			return true;
		}

		return false;
	}

	private boolean clickPalette(double mx, double my) {
		int col = (int) Math.floor((mx - this.paletteX) / SWATCH);
		int row = (int) Math.floor((my - this.paletteY) / SWATCH);
		int baseCount = MapColors.BASE_COUNT - 1;
		int rows = (baseCount + COLUMNS - 1) / COLUMNS;

		if (col >= 0 && col < COLUMNS && row >= 0 && row < rows) {
			int index = row * COLUMNS + col;

			if (index < baseCount) {
				this.color = (index + 1) * 4 + (this.color & 3);

				if (this.tool == Tool.ERASER) {
					this.tool = Tool.PEN;
				}
			}

			return true;
		}

		// Selector de brillo
		int shadeY = this.paletteY + rows * SWATCH + 8;

		if (my >= shadeY && my < shadeY + SHADE_BOX) {
			int slot = (int) Math.floor((mx - this.paletteX) / SHADE_BOX);

			if (slot >= 0 && slot < 4) {
				this.color = (this.color & ~3) | SHADE_ORDER[slot];
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.drawing) {
			int cx = this.cellX(event.x());
			int cy = this.cellY(event.y());
			byte value = (byte) (this.erasingStroke || this.tool == Tool.ERASER ? MapColors.WHITE : this.color);
			this.line(this.lastX, this.lastY, cx, cy, value);
			this.lastX = cx;
			this.lastY = cy;
			return true;
		}

		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		this.drawing = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY > 0) {
			this.brush = Math.min(8, this.brush + 1);
			return true;
		}

		if (scrollY < 0) {
			this.brush = Math.max(1, this.brush - 1);
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_Z) {
			this.undo();
			return true;
		}

		return super.keyPressed(event);
	}

	// ---------- Dibujo de la interfaz ----------

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		int canvasSize = SIZE * this.px;

		graphics.text(this.font, this.title, this.canvasX, this.canvasY - 12, 0xFFFFFFFF, true);
		graphics.fill(this.canvasX - 2, this.canvasY - 2, this.canvasX + canvasSize + 2, this.canvasY + canvasSize + 2, 0xFF000000);
		CanvasView.draw(graphics, this.canvas, this.canvasX, this.canvasY, this.px);

		this.drawPalette(graphics);
		this.drawToolHighlight(graphics);

		// Vista previa del pincel bajo el ratón
		int cx = this.cellX(mouseX);
		int cy = this.cellY(mouseY);

		if (inside(cx, cy)) {
			int r = this.brush - 1;
			graphics.outline(this.canvasX + (cx - r) * this.px, this.canvasY + (cy - r) * this.px,
					(2 * r + 1) * this.px, (2 * r + 1) * this.px, 0xFFFFFFFF);
		}

		graphics.text(this.font, Component.translatable("gui.cuadrospizarras.brush", this.brush),
				this.rightX, this.canvasY + 3 * 22 + 26, 0xFFFFFFFF, true);
		graphics.text(this.font, Component.translatable("gui.cuadrospizarras.board.hint"),
				this.canvasX, this.canvasY + canvasSize + 6, 0xFFAAAAAA, true);
	}

	private void drawPalette(GuiGraphicsExtractor graphics) {
		int baseCount = MapColors.BASE_COUNT - 1;
		int rows = (baseCount + COLUMNS - 1) / COLUMNS;
		int selectedBase = MapColors.baseOf(this.color);

		for (int i = 0; i < baseCount; i++) {
			int x = this.paletteX + (i % COLUMNS) * SWATCH;
			int y = this.paletteY + (i / COLUMNS) * SWATCH;
			graphics.fill(x, y, x + SWATCH, y + SWATCH, MapColors.argbOf((i + 1) * 4 + 2));

			if (i + 1 == selectedBase) {
				graphics.outline(x - 1, y - 1, SWATCH + 2, SWATCH + 2, 0xFFFFFFFF);
				graphics.outline(x, y, SWATCH, SWATCH, 0xFF000000);
			}
		}

		int shadeY = this.paletteY + rows * SWATCH + 8;

		for (int slot = 0; slot < 4; slot++) {
			int x = this.paletteX + slot * SHADE_BOX;
			int packed = selectedBase * 4 + SHADE_ORDER[slot];
			graphics.fill(x, shadeY, x + SHADE_BOX - 1, shadeY + SHADE_BOX, MapColors.argbOf(packed));

			if (SHADE_ORDER[slot] == (this.color & 3)) {
				graphics.outline(x - 1, shadeY - 1, SHADE_BOX + 1, SHADE_BOX + 2, 0xFFFFFFFF);
			}
		}

		// Color actual
		int currentY = shadeY + SHADE_BOX + 8;
		graphics.fill(this.paletteX - 1, currentY - 1, this.paletteX + 4 * SHADE_BOX + 1, currentY + 15, 0xFF000000);
		graphics.fill(this.paletteX, currentY, this.paletteX + 4 * SHADE_BOX, currentY + 14, MapColors.argbOf(this.color));
	}

	private void drawToolHighlight(GuiGraphicsExtractor graphics) {
		Button selected = switch (this.tool) {
			case PEN -> this.penButton;
			case ERASER -> this.eraserButton;
			case FILL -> this.fillButton;
		};

		graphics.outline(selected.getX() - 1, selected.getY() - 1,
				selected.getWidth() + 2, selected.getHeight() + 2, 0xFFFFFF55);
	}
}
