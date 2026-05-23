package mangomilk.truetime;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlWriter;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TrueTimeConfig
{
    static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ALLOW_BACKWARD_TIME_FOR_OPS;
    public static final ModConfigSpec.BooleanValue LOG_CORRECTIONS;
    public static final ModConfigSpec.BooleanValue EXPORT_PLACEHOLDER_FILE;
    public static final ModConfigSpec.ConfigValue<String> PLACEHOLDER_FILE_PATH;
    public static final ModConfigSpec.BooleanValue ANNOUNCE_DAY_CHANGES;
    public static final ModConfigSpec.ConfigValue<String> ANNOUNCEMENT_MODE;
    public static final ModConfigSpec.ConfigValue<String> DAY_CHANGE_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> DAY_CHANGE_TITLE;
    public static final ModConfigSpec.ConfigValue<String> DAY_CHANGE_SUBTITLE;
    public static final ModConfigSpec.ConfigValue<String> DAY_CHANGE_ACTION_BAR;
    public static final ModConfigSpec.IntValue TITLE_FADE_IN_TICKS;
    public static final ModConfigSpec.IntValue TITLE_STAY_TICKS;
    public static final ModConfigSpec.IntValue TITLE_FADE_OUT_TICKS;

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

        ANNOUNCE_DAY_CHANGES = builder
                .comment("Announce preserved Overworld day changes to players.")
                .define("announceDayChanges", false);

        ANNOUNCEMENT_MODE = builder
                .comment("Where day-change announcements appear. Valid values: chat, actionbar, title, chat_actionbar, chat_title, all.")
                .define("announcementMode", "chat");

        DAY_CHANGE_MESSAGE = builder
                .comment("Chat message used for day-change broadcasts. Available tokens: {day}, {time}, {raw_time}.")
                .define("dayChangeMessage", "TrueTime: Day {day} has begun.");

        DAY_CHANGE_TITLE = builder
                .comment("Title text shown when announcementMode includes title. Available tokens: {day}, {time}, {raw_time}.")
                .define("dayChangeTitle", "Day {day}");

        DAY_CHANGE_SUBTITLE = builder
                .comment("Subtitle text shown when announcementMode includes title. Leave empty to hide it.")
                .define("dayChangeSubtitle", "A new day has begun.");

        DAY_CHANGE_ACTION_BAR = builder
                .comment("Action bar text shown when announcementMode includes actionbar. Available tokens: {day}, {time}, {raw_time}.")
                .define("dayChangeActionBar", "Day {day}");

        TITLE_FADE_IN_TICKS = builder
                .comment("Title fade-in duration in ticks.")
                .defineInRange("titleFadeInTicks", 10, 0, 200);

        TITLE_STAY_TICKS = builder
                .comment("Title stay duration in ticks.")
                .defineInRange("titleStayTicks", 60, 1, 400);

        TITLE_FADE_OUT_TICKS = builder
                .comment("Title fade-out duration in ticks.")
                .defineInRange("titleFadeOutTicks", 20, 0, 200);

        SPEC = builder.build();
    }

    private TrueTimeConfig()
    {
    }

    public static void reload(MinecraftServer server)
    {
        ModConfig config = ModConfigs.getModConfigs(TrueTime.MOD_ID).stream()
                .filter(modConfig -> modConfig.getType() == ModConfig.Type.SERVER)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("TrueTime server config is not registered."));

        Path path = config.getFullPath();
        if (path == null)
        {
            path = server.getServerDirectory().resolve("serverconfig").resolve(config.getFileName());
        }

        try
        {
            Files.createDirectories(path.getParent());
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Could not create config directory for " + path + ".", exception);
        }

        try (CommentedFileConfig fileConfig = CommentedFileConfig.of(path))
        {
            fileConfig.load();

            CommentedConfig loadedConfig = CommentedConfig.copy(fileConfig);
            boolean corrected = !SPEC.isCorrect(loadedConfig);
            if (corrected)
            {
                SPEC.correct(loadedConfig);
            }

            IConfigSpec.ILoadedConfig reloadedConfig = createLoadedConfig(loadedConfig, path, config);
            SPEC.acceptConfig(reloadedConfig);
            SPEC.afterReload();

            if (corrected)
            {
                reloadedConfig.save();
            }
        }
    }

    private static IConfigSpec.ILoadedConfig createLoadedConfig(CommentedConfig config, Path path, ModConfig modConfig)
    {
        try
        {
            Class<?> loadedConfigClass = Class.forName("net.neoforged.fml.config.LoadedConfig");
            Constructor<?> constructor = loadedConfigClass.getDeclaredConstructor(CommentedConfig.class, Path.class, ModConfig.class);
            constructor.setAccessible(true);
            return (IConfigSpec.ILoadedConfig) constructor.newInstance(config, path, modConfig);
        }
        catch (ReflectiveOperationException exception)
        {
            throw new IllegalStateException("Could not create NeoForge loaded config wrapper.", exception);
        }
    }
}
