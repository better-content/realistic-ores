package io.github.realisticores;

import io.github.realisticores.registry.ModBlocks;
import io.github.realisticores.registry.ModFeatures;
import io.github.realisticores.registry.ModItems;
import io.github.realisticores.worldgen.DisabledFeatureBiomeModifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RealisticOresMod.MOD_ID)
public final class RealisticOresMod {
    public static final String MOD_ID = "realisticores";

    public RealisticOresMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModFeatures.register(modBus);
        DisabledFeatureBiomeModifier.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            ModItems.getAllBlockItems().forEach(event::accept);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            ModItems.getAllCrushedOreItems().forEach(event::accept);
        }
    }
}
