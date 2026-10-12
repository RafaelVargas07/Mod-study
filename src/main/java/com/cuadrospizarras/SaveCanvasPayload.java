package com.cuadrospizarras;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Cliente → servidor: una imagen de 128x128 ya convertida a colores de mapa
 * (un byte por píxel = id de color * 4 + brillo) y un nombre para el cuadro.
 */
public record SaveCanvasPayload(String name, byte[] colors) implements CustomPacketPayload {
	public static final int MAX_NAME = 40;

	public static final CustomPacketPayload.Type<SaveCanvasPayload> TYPE =
			new CustomPacketPayload.Type<>(CuadrosPizarras.id("save_canvas"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SaveCanvasPayload> CODEC =
			CustomPacketPayload.codec(SaveCanvasPayload::write, SaveCanvasPayload::new);

	public SaveCanvasPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readUtf(MAX_NAME), buf.readByteArray(MapColors.PIXELS));
	}

	private void write(RegistryFriendlyByteBuf buf) {
		String safeName = this.name.length() > MAX_NAME ? this.name.substring(0, MAX_NAME) : this.name;
		buf.writeUtf(safeName, MAX_NAME);
		buf.writeByteArray(this.colors);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
