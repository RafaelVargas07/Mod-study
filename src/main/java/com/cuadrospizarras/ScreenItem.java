package com.cuadrospizarras;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Ítem que, al usarlo (clic derecho), abre una pantalla en el cliente. */
public class ScreenItem extends Item {
	public enum Kind {
		BOARD,
		CAMERA
	}

	private final Kind kind;

	public ScreenItem(Properties properties, Kind kind) {
		super(properties);
		this.kind = kind;
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		if (level.isClientSide()) {
			ClientHooks.open(this.kind);
		}

		return InteractionResult.SUCCESS;
	}
}
