package com.bettercontent.realisticores.compat;

import com.bettercontent.realisticores.RealisticOresMod;
import com.bettercontent.realisticores.registry.ModBlocks;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/** Optional, reflection-only bridge: the mod never requires Excavated Variants to load. */
public final class ExcavatedVariantSupport {
    private static final String MODIFIED_ORE_CLASS = "dev.lukebemish.excavatedvariants.impl.ModifiedOreBlock";
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    private static volatile Map<String, Variant> variantsByCycle;

    private ExcavatedVariantSupport() {
    }

    public static @Nullable Variant identify(Item item) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
        Block block = itemId == null ? null : ForgeRegistries.BLOCKS.getValue(itemId);
        if (block == null || !isModifiedOre(block)) {
            return null;
        }
        try {
            Object ore = publicField(block, "ore");
            Object stone = publicField(block, "stone");
            String family = (String) publicField(ore, "id");
            ResourceLocation substrateId = (ResourceLocation) publicField(stone, "blockId");
            boolean known = ModBlocks.oreDefinitions().stream().anyMatch(definition -> definition.id().equals(family));
            if (!known) {
                if (isOwnedOre(ore)) {
                    warnOnce(block, "unknown deposit family " + family);
                }
                return null;
            }
            Block substrate = ForgeRegistries.BLOCKS.getValue(substrateId);
            if (substrate == null) {
                warnOnce(block, "unknown substrate " + substrateId);
                return null;
            }
            return new Variant(family, substrate, block);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            warnOnce(block, "unreadable variant metadata");
            return null;
        }
    }

    public static @Nullable Variant find(String family, Item substrate) {
        Map<String, Variant> variants = variantsByCycle;
        if (variants == null) {
            synchronized (ExcavatedVariantSupport.class) {
                variants = variantsByCycle;
                if (variants == null) {
                    Map<String, Variant> discovered = new LinkedHashMap<>();
                    for (Block block : ForgeRegistries.BLOCKS.getValues()) {
                        Variant variant = identify(block.asItem());
                        if (variant != null) {
                            discovered.put(key(variant.family(), variant.substrate().asItem()), variant);
                        }
                    }
                    variants = Map.copyOf(discovered);
                    variantsByCycle = variants;
                }
            }
        }
        return variants.get(key(family, substrate));
    }

    private static String key(String family, Item substrate) {
        return family + ":" + ForgeRegistries.ITEMS.getKey(substrate);
    }

    private static boolean isModifiedOre(Block block) {
        Class<?> type = block.getClass();
        while (type != null) {
            if (type.getName().equals(MODIFIED_ORE_CLASS)) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static Object publicField(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getField(name);
        return field.get(owner);
    }

    private static boolean isOwnedOre(Object ore) throws ReflectiveOperationException {
        Object blockIds = publicField(ore, "blockId");
        if (!(blockIds instanceof Iterable<?> ids)) return false;
        for (Object id : ids) {
            if (id instanceof ResourceLocation location
                    && location.getNamespace().equals(RealisticOresMod.MOD_ID)) {
                return true;
            }
        }
        return false;
    }

    private static void warnOnce(Block block, String reason) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        String key = id + ":" + reason;
        if (WARNED.add(key)) {
            RealisticOresMod.LOGGER.warn("Skipping Excavated Variants ore {}: {}", id, reason);
        }
    }

    public record Variant(String family, Block substrate, Block hostedOre) {
    }
}
