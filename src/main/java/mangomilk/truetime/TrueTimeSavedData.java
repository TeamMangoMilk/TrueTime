package mangomilk.truetime;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TrueTimeSavedData extends SavedData
{
    private static final String DATA_NAME = TrueTime.MOD_ID;
    private static final String INITIALISED = "Initialised";
    private static final String PRESERVED_DAY = "PreservedDay";
    private static final String LAST_KNOWN_DAY_TIME = "LastKnownDayTime";
    private static final String SUPPRESSED_VISUAL_ANNOUNCEMENTS = "SuppressedVisualAnnouncements";
    private static final String PLAYER_UUID = "PlayerUuid";

    private static final SavedData.Factory<TrueTimeSavedData> FACTORY = new SavedData.Factory<>(TrueTimeSavedData::new, TrueTimeSavedData::load
    );

    private long preservedDay;
    private long lastKnownDayTime;
    private boolean initialised;
    private final Set<UUID> suppressedVisualAnnouncements = new HashSet<>();

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

        ListTag suppressedPlayers = tag.getList(SUPPRESSED_VISUAL_ANNOUNCEMENTS, Tag.TAG_COMPOUND);
        for (int index = 0; index < suppressedPlayers.size(); index++)
        {
            CompoundTag playerTag = suppressedPlayers.getCompound(index);
            if (playerTag.hasUUID(PLAYER_UUID))
            {
                data.suppressedVisualAnnouncements.add(playerTag.getUUID(PLAYER_UUID));
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
    {
        tag.putBoolean(INITIALISED, initialised);
        tag.putLong(PRESERVED_DAY, preservedDay);
        tag.putLong(LAST_KNOWN_DAY_TIME, lastKnownDayTime);

        ListTag suppressedPlayers = new ListTag();
        for (UUID playerId : suppressedVisualAnnouncements)
        {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID(PLAYER_UUID, playerId);
            suppressedPlayers.add(playerTag);
        }

        tag.put(SUPPRESSED_VISUAL_ANNOUNCEMENTS, suppressedPlayers);
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

    public boolean hasSuppressedVisualAnnouncements(UUID playerId)
    {
        return suppressedVisualAnnouncements.contains(playerId);
    }

    public void setVisualAnnouncementsSuppressed(UUID playerId, boolean suppressed)
    {
        boolean changed = suppressed ? suppressedVisualAnnouncements.add(playerId) : suppressedVisualAnnouncements.remove(playerId);
        if (changed)
        {
            setDirty();
        }
    }
}
