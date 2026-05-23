package mangomilk.truetime;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class TrueTimeTimekeeper
{
    public static final long TICKS_PER_DAY = 24000L;

    private static final Logger LOGGER = LogManager.getLogger(TrueTimeTimekeeper.class);

    private static long correctionsThisSession;
    private static long lastCorrectionFrom = -1L;
    private static long lastCorrectionTo = -1L;

    private TrueTimeTimekeeper()
    {
    }

    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD))
        {
            return;
        }

        TrueTimeSavedData data = TrueTimeSavedData.get(level);
        long currentDayTime = Math.max(0L, level.getDayTime());

        if (!data.isInitialised())
        {
            data.setFromDayTime(currentDayTime);
            return;
        }

        long previousDayTime = data.getLastKnownDayTime();

        if (currentDayTime < previousDayTime)
        {
            long correctedDayTime = nextForwardOccurrence(previousDayTime, timeOfDay(currentDayTime));
            level.setDayTime(correctedDayTime);
            data.setFromDayTime(correctedDayTime);

            correctionsThisSession++;
            lastCorrectionFrom = currentDayTime;
            lastCorrectionTo = correctedDayTime;

            if (TrueTimeConfig.LOG_CORRECTIONS.get())
            {
                LOGGER.info(
                        "Corrected backward Overworld time change from {} to {}.",
                        currentDayTime,
                        correctedDayTime
                );
            }

            return;
        }

        if (currentDayTime != previousDayTime || data.getPreservedDay() != dayOf(currentDayTime))
        {
            data.setFromDayTime(currentDayTime);
        }
    }

    public static long nextForwardOccurrence(long currentDayTime, long targetTimeOfDay)
    {
        long normalisedTarget = timeOfDay(targetTimeOfDay);
        long currentDay = dayOf(currentDayTime);
        long candidate = composeDayTime(currentDay, normalisedTarget);
        return candidate >= currentDayTime ? candidate : candidate + TICKS_PER_DAY;
    }

    public static long composeDayTime(long day, long timeOfDay)
    {
        return Math.max(0L, day) * TICKS_PER_DAY + timeOfDay(timeOfDay);
    }

    public static long dayOf(long dayTime)
    {
        return Math.floorDiv(Math.max(0L, dayTime), TICKS_PER_DAY);
    }

    public static long timeOfDay(long dayTime)
    {
        return Math.floorMod(dayTime, TICKS_PER_DAY);
    }

    public static long getCorrectionsThisSession()
    {
        return correctionsThisSession;
    }

    public static long getLastCorrectionFrom()
    {
        return lastCorrectionFrom;
    }

    public static long getLastCorrectionTo()
    {
        return lastCorrectionTo;
    }
}
