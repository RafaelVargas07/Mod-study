package com.cuadrospizarras;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/** Lógica del servidor: convierte una imagen recibida en un mapa lleno listo para colgar en un marco. */
public final class CanvasService {
	// El mapa se crea lejos del mundo jugable. Así el juego nunca "repinta" el terreno encima
	// de la imagen cuando el jugador lo lleva en la mano (la imagen queda fija).
	private static final int FAR_AWAY = 29_000_000;

	private CanvasService() {
	}

	public static void save(ServerPlayer player, SaveCanvasPayload payload) {
		byte[] colors = payload.colors();

		if (colors.length != MapColors.PIXELS) {
			return;
		}

		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		boolean free = player.isCreative();

		if (!free && findEmptyMapSlot(player) < 0) {
			player.sendSystemMessage(Component.translatable("message.cuadrospizarras.need_map"));
			return;
		}

		MapColors.sanitize(colors);

		ItemStack stack = MapItem.create(level, FAR_AWAY, FAR_AWAY, (byte) 0, false, false);
		MapItemSavedData data = MapItem.getSavedData(stack, level);

		if (data == null) {
			CuadrosPizarras.LOGGER.warn("No se pudo crear el mapa para {}", player.getName().getString());
			return;
		}

		System.arraycopy(colors, 0, data.colors, 0, MapColors.PIXELS);
		data.setColorsDirty(0, 0); // marca el mapa como modificado para guardarlo en disco

		String name = payload.name().strip();
		if (!name.isEmpty()) {
			stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
		}

		if (!free) {
			int slot = findEmptyMapSlot(player);
			if (slot >= 0) {
				player.getInventory().getItem(slot).shrink(1);
			}
		}

		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}

		player.sendSystemMessage(Component.translatable("message.cuadrospizarras.saved"));
	}

	private static int findEmptyMapSlot(ServerPlayer player) {
		Inventory inventory = player.getInventory();

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(Items.MAP)) {
				return i;
			}
		}

		return -1;
	}
}
