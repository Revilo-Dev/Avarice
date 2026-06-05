package com.revilo.gatesofavarice.dungeon;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

final class DungeonRunStorage extends SavedData {

    private static final String DATA_NAME = "gatesofavarice_dungeon_runs";
    private static final String STATE_KEY = "state";
    private static final Factory<DungeonRunStorage> FACTORY = new Factory<>(DungeonRunStorage::new, DungeonRunStorage::load);

    private CompoundTag state = new CompoundTag();

    static DungeonRunStorage get(ServerLevel level) {
        ServerLevel dataLevel = level.getServer().overworld() != null ? level.getServer().overworld() : level;
        return dataLevel.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static DungeonRunStorage load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonRunStorage storage = new DungeonRunStorage();
        if (tag.contains(STATE_KEY, Tag.TAG_COMPOUND)) {
            storage.state = tag.getCompound(STATE_KEY).copy();
        }
        return storage;
    }

    CompoundTag state() {
        return this.state.copy();
    }

    void setState(CompoundTag state) {
        this.state = state.copy();
        this.setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(STATE_KEY, this.state.copy());
        return tag;
    }
}
