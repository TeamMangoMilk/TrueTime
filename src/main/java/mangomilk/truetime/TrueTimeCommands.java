package mangomilk.truetime;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TrueTimeCommands
{
    private static final SimpleCommandExceptionType BACKWARD_TIME_DISABLED = new SimpleCommandExceptionType(Component.literal("TrueTime is configured to prevent operator commands from moving Overworld time backwards.")
    );
    private static final DynamicCommandExceptionType INVALID_TIME_OF_DAY = new DynamicCommandExceptionType(value -> Component.literal("Invalid time of day: " + value + ". Use 0-23999, day, noon, night, or midnight.")
    );

    private TrueTimeCommands()
    {
    }

    public static void register(RegisterCommandsEvent event)
    {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("truetime")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("info").executes(TrueTimeCommands::info))
                .then(Commands.literal("resetday").executes(context -> setDay(context, 0L)))
                .then(Commands.literal("setday")
                .then(Commands.argument("day", LongArgumentType.longArg(0L)).executes(context -> setDay(context, LongArgumentType.getLong(context, "day")))))
                .then(Commands.literal("adddays")
                .then(Commands.argument("days", LongArgumentType.longArg(0L)).executes(context -> addDays(context, LongArgumentType.getLong(context, "days")))))
                .then(Commands.literal("settime")
                .then(Commands.argument("day", LongArgumentType.longArg(0L)) 
                .then(Commands.argument("timeOfDay", StringArgumentType.word()).executes(TrueTimeCommands::setTime))))
                .then(Commands.literal("sync").executes(TrueTimeCommands::sync))
                .then(Commands.literal("reload").executes(TrueTimeCommands::reload)));
    }

    private static int info(CommandContext<CommandSourceStack> context)
    {
        ServerLevel overworld = context.getSource().getServer().overworld();
        TrueTimeSavedData data = TrueTimeSavedData.get(overworld);
        long rawDayTime = overworld.getDayTime();
        long timeOfDay = TrueTimeTimekeeper.timeOfDay(rawDayTime);
        String lastCorrection = TrueTimeTimekeeper.getLastCorrectionFrom() >= 0L ? TrueTimeTimekeeper.getLastCorrectionFrom() + " -> " + TrueTimeTimekeeper.getLastCorrectionTo() : "none";

        context.getSource().sendSuccess(() -> Component.literal(
                "TrueTime: day " + data.getPreservedDay()
                        + ", raw Overworld time " + rawDayTime
                        + ", time of day " + timeOfDay
                        + ", corrections this session " + TrueTimeTimekeeper.getCorrectionsThisSession()
                        + ", last correction " + lastCorrection + "."
        ), false);
        return (int) Math.min(Integer.MAX_VALUE, data.getPreservedDay());
    }

    private static int setDay(CommandContext<CommandSourceStack> context, long day) throws CommandSyntaxException
    {
        ServerLevel overworld = context.getSource().getServer().overworld();
        long targetDayTime = TrueTimeTimekeeper.composeDayTime(day, TrueTimeTimekeeper.timeOfDay(overworld.getDayTime()));
        applyOperatorTimeChange(context.getSource(), overworld, targetDayTime);
        context.getSource().sendSuccess(() -> Component.literal("TrueTime day counter set to " + day + "."), true);
        return (int) Math.min(Integer.MAX_VALUE, day);
    }

    private static int addDays(CommandContext<CommandSourceStack> context, long days) throws CommandSyntaxException
    {
        ServerLevel overworld = context.getSource().getServer().overworld();
        TrueTimeSavedData data = TrueTimeSavedData.get(overworld);
        long targetDay = data.getPreservedDay() + days;
        long targetDayTime = TrueTimeTimekeeper.composeDayTime(targetDay, TrueTimeTimekeeper.timeOfDay(overworld.getDayTime()));
        applyOperatorTimeChange(context.getSource(), overworld, targetDayTime);
        context.getSource().sendSuccess(() -> Component.literal("TrueTime day counter advanced to " + targetDay + "."), true);
        return (int) Math.min(Integer.MAX_VALUE, targetDay);
    }

    private static int setTime(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        long day = LongArgumentType.getLong(context, "day");
        long timeOfDay = parseTimeOfDay(StringArgumentType.getString(context, "timeOfDay"));
        ServerLevel overworld = context.getSource().getServer().overworld();
        long targetDayTime = TrueTimeTimekeeper.composeDayTime(day, timeOfDay);
        applyOperatorTimeChange(context.getSource(), overworld, targetDayTime);
        context.getSource().sendSuccess(() -> Component.literal("TrueTime set to day " + day + " at time " + TrueTimeTimekeeper.timeOfDay(targetDayTime) + "."), true);
        return (int) Math.min(Integer.MAX_VALUE, day);
    }

    private static int sync(CommandContext<CommandSourceStack> context)
    {
        ServerLevel overworld = context.getSource().getServer().overworld();
        TrueTimeSavedData data = TrueTimeSavedData.get(overworld);
        data.setFromDayTime(overworld.getDayTime());
        context.getSource().sendSuccess(() -> Component.literal("TrueTime synchronised to Overworld day " + data.getPreservedDay() + "."), true);
        return (int) Math.min(Integer.MAX_VALUE, data.getPreservedDay());
    }

    private static int reload(CommandContext<CommandSourceStack> context)
    {
        ServerLevel overworld = context.getSource().getServer().overworld();
        TrueTimeSavedData data = TrueTimeSavedData.get(overworld);
        TrueTimeTimekeeper.updateCurrentPreservedDay(data.getPreservedDay());
        TrueTimePlaceholderExport.export(context.getSource().getServer(), data.getPreservedDay());
        TrueTimeTabIntegration.tryRegister();
        context.getSource().sendSuccess(() -> Component.literal("TrueTime reloaded. TAB placeholder registered: " + TrueTimeTabIntegration.isRegistered() + "."), true);
        return TrueTimeTabIntegration.isRegistered() ? 1 : 0;
    }

    private static void applyOperatorTimeChange(CommandSourceStack source, ServerLevel overworld, long targetDayTime)
            throws CommandSyntaxException
    {
        long currentDayTime = overworld.getDayTime();
        if (targetDayTime < currentDayTime && !TrueTimeConfig.ALLOW_BACKWARD_TIME_FOR_OPS.get())
        {
            throw BACKWARD_TIME_DISABLED.create();
        }

        overworld.setDayTime(targetDayTime);
        TrueTimeSavedData.get(overworld).setFromDayTime(targetDayTime);
        TrueTimeTimekeeper.updateCurrentPreservedDay(TrueTimeTimekeeper.dayOf(targetDayTime));
        TrueTimePlaceholderExport.export(source.getServer(), TrueTimeTimekeeper.dayOf(targetDayTime));
        source.getServer().forceTimeSynchronization();
    }

    private static long parseTimeOfDay(String input) throws CommandSyntaxException
    {
        return switch (input.toLowerCase())
        {
            case "day" -> 1000L;
            case "noon" -> 6000L;
            case "night" -> 13000L;
            case "midnight" -> 18000L;
            default -> parseNumericTimeOfDay(input);
        };
    }

    private static long parseNumericTimeOfDay(String input) throws CommandSyntaxException
    {
        try
        {
            long value = Long.parseLong(input);
            if (value < 0L || value >= TrueTimeTimekeeper.TICKS_PER_DAY)
            {
                throw INVALID_TIME_OF_DAY.create(input);
            }
            return value;
        }
        catch (NumberFormatException exception)
        {
            throw INVALID_TIME_OF_DAY.create(input);
        }
    }
}
