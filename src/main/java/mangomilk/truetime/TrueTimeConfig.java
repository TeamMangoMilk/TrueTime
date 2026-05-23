package mangomilk.truetime;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TrueTimeConfig
{
    static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ALLOW_BACKWARD_TIME_FOR_OPS;
    public static final ModConfigSpec.BooleanValue LOG_CORRECTIONS;

    static
    {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        ALLOW_BACKWARD_TIME_FOR_OPS = builder
                .comment("Allow dedicated TrueTime operator commands to move Overworld time backwards.")
                .define("allowBackwardTimeForOps", false);

        LOG_CORRECTIONS = builder
                .comment("Log whenever TrueTime converts a backward Overworld time change into the next future occurrence.")
                .define("logCorrections", true);

        SPEC = builder.build();
    }

    private TrueTimeConfig()
    {
    }
}
