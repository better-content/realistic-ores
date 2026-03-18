package io.github.realisticores.registry;

import io.github.realisticores.RealisticOresMod;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RealisticOresMod.MOD_ID);
    private static final Map<String, RegistryObject<Item>> BLOCK_ITEMS_BY_ID = new LinkedHashMap<>();
    private static boolean initialized;

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        if (!initialized) {
            initialized = true;
            for (Map.Entry<String, RegistryObject<net.minecraft.world.level.block.Block>> entry : ModBlocks.blockEntries()) {
                BLOCK_ITEMS_BY_ID.put(entry.getKey(), ITEMS.register(entry.getKey(), () -> new BlockItem(entry.getValue().get(), new Item.Properties())));
            }
        }
        ITEMS.register(modBus);
    }

    public static Collection<Item> getAllBlockItems() {
        return BLOCK_ITEMS_BY_ID.values().stream().map(RegistryObject::get).toList();
    }
}
