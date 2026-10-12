package com.cuadrospizarras.client;

import net.minecraft.client.Minecraft;

import net.fabricmc.api.ClientModInitializer;

import com.cuadrospizarras.ClientHooks;
import com.cuadrospizarras.ScreenItem;

public class CuadrosPizarrasClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Evita que ImageIO intente inicializar el sistema de ventanas de AWT junto a GLFW.
		System.setProperty("java.awt.headless", "true");

		ClientHooks.setOpener(kind -> {
			Minecraft minecraft = Minecraft.getInstance();

			if (kind == ScreenItem.Kind.BOARD) {
				minecraft.gui.setScreen(new DrawScreen());
			} else {
				minecraft.gui.setScreen(new PhotoScreen());
			}
		});
	}
}
