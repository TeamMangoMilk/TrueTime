package mangomilk.truetime;

import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TrueTimePlaceholderExport
{
    private static final Logger LOGGER = LogManager.getLogger(TrueTimePlaceholderExport.class);

    private static Long lastExportedDay;

    private TrueTimePlaceholderExport()
    {
    }

    public static void exportIfChanged(MinecraftServer server, long preservedDay)
    {
        if (lastExportedDay != null && lastExportedDay == preservedDay)
        {
            return;
        }

        export(server, preservedDay);
    }

    public static void export(MinecraftServer server, long preservedDay)
    {
        if (!TrueTimeConfig.EXPORT_PLACEHOLDER_FILE.get())
        {
            return;
        }

        Path path = resolveExportPath(server);

        try
        {
            Path parent = path.getParent();
            if (parent != null)
            {
                Files.createDirectories(parent);
            }

            Files.writeString(path, Long.toString(preservedDay), StandardCharsets.UTF_8);
            lastExportedDay = preservedDay;
        }
        catch (IOException exception)
        {
            LOGGER.warn("Failed to export TrueTime placeholder file to {}.", path, exception);
        }
    }

    private static Path resolveExportPath(MinecraftServer server)
    {
        Path configuredPath = Path.of(TrueTimeConfig.PLACEHOLDER_FILE_PATH.get());
        if (configuredPath.isAbsolute())
        {
            return configuredPath;
        }

        return server.getServerDirectory().resolve(configuredPath).normalize();
    }
}
