package io.github.realisticores.client;

import io.github.realisticores.registry.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientSetup {
    private ClientSetup() {
    }

    @SuppressWarnings("deprecation")
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModBlocks.surfaceSampleEntries().forEach(entry ->
                    ItemBlockRenderTypes.setRenderLayer(entry.getValue().get(), RenderType.cutout()));
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.OIL_SEEP.get(), RenderType.cutout());
        });
    }
}
