package mangomilk.truetime;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TrueTime.MOD_ID)
public class TrueTime
{
    public static final String MOD_ID = "truetime";
    private static final Logger LOGGER = LogManager.getLogger(TrueTime.class);

    public TrueTime(IEventBus modEventBus, ModContainer modContainer)
    {
        modContainer.registerConfig(ModConfig.Type.SERVER, TrueTimeConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(TrueTimeCommands::register);
        NeoForge.EVENT_BUS.addListener(TrueTimeTimekeeper::onLevelTick);
        NeoForge.EVENT_BUS.addListener(TrueTimeTabIntegration::onServerStarted);

        LOGGER.info("TrueTime initialised.");
    }
}
