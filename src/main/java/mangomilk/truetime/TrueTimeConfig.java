package mangomilk.truetime;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TrueTimeConfig
{
    static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ALLOW_BACKWARD_TIME_FOR_OPS;
    public static final ModConfigSpec.BooleanValue LOG_CORRECTIONS;
    public static final ModConfigSpec.BooleanValue EXPORT_PLACEHOLDER_FILE;
    public static final ModConfigSpec.ConfigValue<String> PLACEHOLDER_FILE_PATH;

    static
    {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        ALLOW_BACKWARD_TIME_FOR_OPS = builder
                .comment("Allow dedicated TrueTime operator commands to move Overworld time backwards.")
                .define("allowBackwardTimeForOps", false);

        LOG_CORRECTIONS = builder
                .comment("Log whenever TrueTime converts a backward Overworld time change into the next future occurrence.")
                .define("logCorrections", true);

        EXPORT_PLACEHOLDER_FILE = builder
                .comment("Write the preserved TrueTime day counter to a plain text file for PlaceholderAPI/TAB bridges.")
                .define("exportPlaceholderFile", true);

        PLACEHOLDER_FILE_PATH = builder
                .comment("Path to the placeholder export file, relative to the server root unless absolute.")
                .define("placeholderFilePath", "truetime/day.txt");

        SPEC = builder.build();
    }

    private TrueTimeConfig()
    {
    }
}
