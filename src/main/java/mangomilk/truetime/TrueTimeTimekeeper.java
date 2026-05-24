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
    private static long currentPreservedDay;
    private static boolean startupValidationLogged;

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

        TrueTimeTabIntegration.tryRegister();

        if (!data.isInitialised())
        {
            data.setFromDayTime(currentDayTime);
            updateCurrentPreservedDay(data.getPreservedDay());
            TrueTimePlaceholderExport.export(level.getServer(), data.getPreservedDay());
            logStartupValidation(level, data);
            return;
        }

        if (!startupValidationLogged)
        {
            updateCurrentPreservedDay(data.getPreservedDay());
            TrueTimePlaceholderExport.exportIfChanged(level.getServer(), data.getPreservedDay());
            logStartupValidation(level, data);
        }

        long previousDayTime = data.getLastKnownDayTime();
        long previousPreservedDay = data.getPreservedDay();

        if (currentDayTime < previousDayTime)
        {
            long correctedDayTime = nextForwardOccurrence(previousDayTime, timeOfDay(currentDayTime));
            level.setDayTime(correctedDayTime);
            data.setFromDayTime(correctedDayTime);
            updateCurrentPreservedDay(data.getPreservedDay());

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

            TrueTimePlaceholderExport.exportIfChanged(level.getServer(), data.getPreservedDay());
            if (data.getPreservedDay() != previousPreservedDay)
            {
                TrueTimeAnnouncements.announceDayChange(level.getServer(), data.getPreservedDay(), correctedDayTime);
            }
            return;
        }

        if (currentDayTime != previousDayTime || data.getPreservedDay() != dayOf(currentDayTime))
        {
            data.setFromDayTime(currentDayTime);
            updateCurrentPreservedDay(data.getPreservedDay());

            if (data.getPreservedDay() != previousPreservedDay)
            {
                TrueTimePlaceholderExport.export(level.getServer(), data.getPreservedDay());
                TrueTimeAnnouncements.announceDayChange(level.getServer(), data.getPreservedDay(), currentDayTime);
            }
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

    public static long getCurrentPreservedDay()
    {
        return currentPreservedDay;
    }

    public static void updateCurrentPreservedDay(long preservedDay)
    {
        currentPreservedDay = Math.max(0L, preservedDay);
    }

    private static void logStartupValidation(ServerLevel level, TrueTimeSavedData data)
    {
        LOGGER.info(
                "TrueTime tracking Overworld day {} at raw time {}. TAB placeholder registered: {}. Placeholder export enabled: {}.",
                data.getPreservedDay(),
                level.getDayTime(),
                TrueTimeTabIntegration.isRegistered(),
                TrueTimeConfig.EXPORT_PLACEHOLDER_FILE.get()
        );

        startupValidationLogged = true;
    }
}
