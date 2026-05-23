package mangomilk.truetime;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.event.plugin.TabLoadEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class TrueTimeTabIntegration
{
    private static final Logger LOGGER = LogManager.getLogger(TrueTimeTabIntegration.class);

    private static boolean registered;
    private static boolean listeningForReloads;
    private static boolean apiNotReadyLogged;

    private TrueTimeTabIntegration()
    {
    }

    public static void onServerStarted(ServerStartedEvent event)
    {
        tryRegister();
    }

    public static void tryRegister()
    {
        if (!ModList.get().isLoaded("tab") || registered)
        {
            return;
        }

        register();
    }

    private static void register()
    {
        try
        {
            registerPlaceholder();
            registerReloadListener();
        }
        catch (IllegalStateException exception)
        {
            if (!apiNotReadyLogged)
            {
                LOGGER.info("TAB was detected, waiting for its API to finish loading before registering %truetime_day%.");
                apiNotReadyLogged = true;
            }
        }
        catch (Throwable throwable)
        {
            LOGGER.warn("TAB was detected, but TrueTime could not register its TAB placeholder.", throwable);
        }
    }

    private static void registerPlaceholder()
    {
        if (registered)
        {
            return;
        }

        TabAPI.getInstance().getPlaceholderManager().registerServerPlaceholder(
                "%truetime_day%",
                1000,
                () -> Long.toString(TrueTimeTimekeeper.getCurrentPreservedDay())
        );

        registered = true;
        apiNotReadyLogged = false;
        LOGGER.info("Registered TAB placeholder %truetime_day%.");
    }

    private static void registerReloadListener()
    {
        if (listeningForReloads)
        {
            return;
        }

        TabAPI.getInstance().getEventBus().register(TabLoadEvent.class, event ->
        {
            registered = false;
            registerPlaceholder();
        });

        listeningForReloads = true;
    }
}
