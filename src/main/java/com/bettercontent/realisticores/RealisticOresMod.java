package com.bettercontent.realisticores;

import com.bettercontent.realisticores.registry.ModBlocks;
import com.bettercontent.realisticores.client.ClientSetup;
import com.bettercontent.realisticores.registry.ModFeatures;
import com.bettercontent.realisticores.registry.ModItems;
import com.bettercontent.realisticores.worldgen.DisabledFeatureBiomeModifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RealisticOresMod.MOD_ID)
public final class RealisticOresMod {
    public static final String MOD_ID = "realistic_ores";

    public RealisticOresMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModFeatures.register(modBus);
        DisabledFeatureBiomeModifier.register(modBus);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modBus.addListener(ClientSetup::onClientSetup));
        modBus.addListener(this::addCreativeTabContents);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            ModItems.getAllBlockItems().forEach(event::accept);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            ModItems.getAllOreChunkItems().forEach(event::accept);
            ModItems.getAllCrushedOreItems().forEach(event::accept);
        }
    }

}
