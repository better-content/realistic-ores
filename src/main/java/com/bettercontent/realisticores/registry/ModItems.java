package com.bettercontent.realisticores.registry;

import com.bettercontent.realisticores.RealisticOresMod;
import com.bettercontent.realisticores.ore.OreDefinition;
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
    private static final Map<String, RegistryObject<Item>> CONCENTRATE_ITEMS_BY_ID = new LinkedHashMap<>();
    private static final Map<String, RegistryObject<Item>> GRINDING_BALL_ITEMS_BY_ID = new LinkedHashMap<>();
    private static final Map<String, RegistryObject<Item>> GEM_CHIP_ITEMS_BY_ID = new LinkedHashMap<>();
    private static final Map<String, RegistryObject<Item>> IMMEDIATE_UTILITY_ITEMS_BY_ID = new LinkedHashMap<>();
    private static final String[] CONCENTRATES = {
            "coal", "iron", "nickel", "copper", "sulfur", "gold", "tin", "quartz", "zinc",
            "lead", "cadmium", "silver", "aluminum", "titanium", "cobalt", "osmium", "diamond",
            "emerald", "amethyst", "uranium", "thorium", "redstone", "lapis", "soul_sand",
            "rock_salt", "sodium_chloride", "saltpeter"
    };
    private static final String[] GRINDING_BALLS = {
            "andesite", "iron", "brass", "steel", "nickel", "titanium", "blood_infused", "fluix"
    };
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
                        () -> definition.id().equals("coal_measures")
                                ? new FuelItem(new Item.Properties(), 800)
                                : new Item(new Item.Properties())));
                String crushedItemId = definition.crushedItemId();
                CRUSHED_ORE_ITEMS_BY_ID.put(crushedItemId, ITEMS.register(
                        crushedItemId,
                        () -> new Item(new Item.Properties())));
                String smallChunkItemId = definition.smallOreChunkItemId();
                RegistryObject<net.minecraft.world.level.block.Block> sample = ModBlocks.surfaceSampleEntries().stream()
                        .filter(entry -> entry.getKey().equals(definition.surfaceSampleBlockId()))
                        .findFirst()
                        .orElseThrow()
                        .getValue();
                BLOCK_ITEMS_BY_ID.put(smallChunkItemId, ITEMS.register(
                        smallChunkItemId,
                        () -> new BlockItem(sample.get(), new Item.Properties())));
            }
            for (Map.Entry<String, RegistryObject<net.minecraft.world.level.block.Block>> entry : ModBlocks.blockEntries()) {
                BLOCK_ITEMS_BY_ID.put(entry.getKey(), ITEMS.register(entry.getKey(), () -> new BlockItem(entry.getValue().get(), new Item.Properties())));
            }
            BLOCK_ITEMS_BY_ID.put("oil_seep", ITEMS.register(
                    "oil_seep",
                    () -> new BlockItem(ModBlocks.OIL_SEEP.get(), new Item.Properties())));
            for (String concentrate : CONCENTRATES) {
                String id = concentrate + "_concentrate";
                CONCENTRATE_ITEMS_BY_ID.put(id, ITEMS.register(id, () -> new Item(new Item.Properties())));
            }
            for (String medium : GRINDING_BALLS) {
                String id = medium + "_grinding_ball";
                GRINDING_BALL_ITEMS_BY_ID.put(id, ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(16))));
            }
            for (String gem : new String[] {"diamond", "emerald", "amethyst"}) {
                String id = gem + "_chip";
                GEM_CHIP_ITEMS_BY_ID.put(id, ITEMS.register(id, () -> new Item(new Item.Properties())));
            }
            IMMEDIATE_UTILITY_ITEMS_BY_ID.put(
                    "rock_salt",
                    ITEMS.register("rock_salt", () -> new Item(new Item.Properties())));
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

    public static Collection<Item> getAllProcessingItems() {
        return java.util.stream.Stream.of(
                        CONCENTRATE_ITEMS_BY_ID, GRINDING_BALL_ITEMS_BY_ID,
                        GEM_CHIP_ITEMS_BY_ID, IMMEDIATE_UTILITY_ITEMS_BY_ID)
                .flatMap(map -> map.values().stream())
                .map(RegistryObject::get)
                .toList();
    }

    private static final class FuelItem extends Item {
        private final int burnTime;

        private FuelItem(Properties properties, int burnTime) {
            super(properties);
            this.burnTime = burnTime;
        }

        @Override
        public int getBurnTime(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.crafting.RecipeType<?> type) {
            return burnTime;
        }
    }
}
