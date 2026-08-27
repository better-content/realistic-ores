package com.bettercontent.realisticores.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

/** Optional Threads bridge for a sample and the matching deposit family it identifies. */
public final class ThreadsBridge {
    private static final String ROOT = "RealisticOresThreadEpisodes";

    private ThreadsBridge() {}

    public static void sampleRead(ServerPlayer player, String family) {
        String token = player.getUUID() + ":deposit:" + family + ":" + player.server.getTickCount();
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag episodes = persisted.getCompound(ROOT);
        episodes.putString(family, token);
        persisted.put(ROOT, episodes);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        emit(player, "deposit_read", family, token);
    }

    public static void depositExtracted(ServerPlayer player, String family) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag episodes = persisted.getCompound(ROOT);
        String token = episodes.getString(family);
        if (token.isBlank() || token.length() > 128) return;
        emit(player, "deposit_extract", family, token);
        episodes.remove(family);
        persisted.put(ROOT, episodes);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private static void emit(ServerPlayer player, String type, String value, String correlation) {
        try {
            Class<?> api = Class.forName("com.bettercontent.threads.api.ThreadSignals");
            Method method = api.getMethod("emit", ServerPlayer.class, String.class, String.class, String.class);
            method.invoke(null, player, type, value, correlation);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
