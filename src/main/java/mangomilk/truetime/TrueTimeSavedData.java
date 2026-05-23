package mangomilk.truetime;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class TrueTimeSavedData extends SavedData
{
    private static final String DATA_NAME = TrueTime.MOD_ID;
    private static final String INITIALISED = "Initialised";
    private static final String PRESERVED_DAY = "PreservedDay";
    private static final String LAST_KNOWN_DAY_TIME = "LastKnownDayTime";

    private static final SavedData.Factory<TrueTimeSavedData> FACTORY = new SavedData.Factory<>(TrueTimeSavedData::new, TrueTimeSavedData::load
    );

    private long preservedDay;
    private long lastKnownDayTime;
    private boolean initialised;

    public static TrueTimeSavedData get(Level level)
    {
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static TrueTimeSavedData load(CompoundTag tag, HolderLookup.Provider registries)
    {
        TrueTimeSavedData data = new TrueTimeSavedData();
        data.initialised = tag.getBoolean(INITIALISED);
        data.preservedDay = Math.max(0L, tag.getLong(PRESERVED_DAY));
        data.lastKnownDayTime = Math.max(0L, tag.getLong(LAST_KNOWN_DAY_TIME));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
    {
        tag.putBoolean(INITIALISED, initialised);
        tag.putLong(PRESERVED_DAY, preservedDay);
        tag.putLong(LAST_KNOWN_DAY_TIME, lastKnownDayTime);
        return tag;
    }

    public boolean isInitialised()
    {
        return initialised;
    }

    public long getPreservedDay()
    {
        return preservedDay;
    }

    public long getLastKnownDayTime()
    {
        return lastKnownDayTime;
    }

    public void setFromDayTime(long dayTime)
    {
        this.initialised = true;
        this.lastKnownDayTime = Math.max(0L, dayTime);
        this.preservedDay = Math.max(0L, Math.floorDiv(this.lastKnownDayTime, TrueTimeTimekeeper.TICKS_PER_DAY));
        setDirty();
    }

    public void setPreservedTime(long day, long timeOfDay)
    {
        this.initialised = true;
        this.preservedDay = Math.max(0L, day);
        this.lastKnownDayTime = TrueTimeTimekeeper.composeDayTime(this.preservedDay, timeOfDay);
        setDirty();
    }
}
