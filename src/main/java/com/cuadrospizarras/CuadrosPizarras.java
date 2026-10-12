package com.cuadrospizarras;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class CuadrosPizarras implements ModInitializer {
	public static final String MOD_ID = "cuadrospizarras";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModItems.initialize();

		// El cliente envía el dibujo / la captura ya convertido a colores de mapa.
		PayloadTypeRegistry.serverboundPlay().register(SaveCanvasPayload.TYPE, SaveCanvasPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SaveCanvasPayload.TYPE,
				(payload, context) -> CanvasService.save(context.player(), payload));

		LOGGER.info("Cuadros y Pizarras cargado.");
	}
}
