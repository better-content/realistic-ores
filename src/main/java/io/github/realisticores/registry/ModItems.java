package io.github.realisticores.registry;

import io.github.realisticores.RealisticOresMod;
import io.github.realisticores.ore.OreDefinition;
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
    private static final Map<String, RegistryObject<Item>> ORE_CHUNK_ITEMS_BY_ID = new LinkedHashMap<>();
    private static final Map<String, RegistryObject<Item>> CRUSHED_ORE_ITEMS_BY_ID = new LinkedHashMap<>();
    private static boolean initialized;

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        if (!initialized) {
            initialized = true;
            for (OreDefinition definition : ModBlocks.oreDefinitions()) {
                String oreChunkItemId = definition.oreChunkItemId();
                ORE_CHUNK_ITEMS_BY_ID.put(oreChunkItemId, ITEMS.register(
                        oreChunkItemId,
                        () -> new Item(new Item.Properties())));
                String crushedItemId = definition.crushedItemId();
                CRUSHED_ORE_ITEMS_BY_ID.put(crushedItemId, ITEMS.register(
                        crushedItemId,
                        () -> new Item(new Item.Properties())));
            }
            for (Map.Entry<String, RegistryObject<net.minecraft.world.level.block.Block>> entry : ModBlocks.surfaceSampleEntries()) {
                BLOCK_ITEMS_BY_ID.put(entry.getKey(), ITEMS.register(
                        entry.getKey(),
                        () -> new BlockItem(entry.getValue().get(), new Item.Properties())));
            }
            for (Map.Entry<String, RegistryObject<net.minecraft.world.level.block.Block>> entry : ModBlocks.blockEntries()) {
                BLOCK_ITEMS_BY_ID.put(entry.getKey(), ITEMS.register(entry.getKey(), () -> new BlockItem(entry.getValue().get(), new Item.Properties())));
            }
            BLOCK_ITEMS_BY_ID.put("oil_seep", ITEMS.register(
                    "oil_seep",
                    () -> new BlockItem(ModBlocks.OIL_SEEP.get(), new Item.Properties())));
        }
        ITEMS.register(modBus);
    }

    public static Collection<Item> getAllBlockItems() {
        return BLOCK_ITEMS_BY_ID.values().stream().map(RegistryObject::get).toList();
    }

    public static Collection<Item> getAllCrushedOreItems() {
        return CRUSHED_ORE_ITEMS_BY_ID.values().stream().map(RegistryObject::get).toList();
    }

    public static Collection<Item> getAllOreChunkItems() {
        return ORE_CHUNK_ITEMS_BY_ID.values().stream().map(RegistryObject::get).toList();
    }
}
