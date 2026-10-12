package com.cuadrospizarras;

import java.util.function.Consumer;

/**
 * Puente entre el código común (los ítems) y el código solo-cliente (las pantallas).
 * El cliente registra aquí la función que abre cada pantalla.
 */
public final class ClientHooks {
	private static Consumer<ScreenItem.Kind> opener = kind -> { };

	private ClientHooks() {
	}

	public static void setOpener(Consumer<ScreenItem.Kind> newOpener) {
		opener = newOpener;
	}

	public static void open(ScreenItem.Kind kind) {
		opener.accept(kind);
	}
}
