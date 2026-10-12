package com.cuadrospizarras;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

public final class ModItems {
	public static final Item PIZARRA = register("pizarra",
			properties -> new ScreenItem(properties, ScreenItem.Kind.BOARD),
			new Item.Properties().stacksTo(1));

	public static final Item CAMARA = register("camara",
			properties -> new ScreenItem(properties, ScreenItem.Kind.CAMERA),
			new Item.Properties().stacksTo(1));

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties settings) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, CuadrosPizarras.id(name));
		Item item = factory.apply(settings.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(tab -> {
			tab.accept(PIZARRA);
			tab.accept(CAMARA);
		});
	}
}
