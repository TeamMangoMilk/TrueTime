package mangomilk.truetime;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TrueTime.MOD_ID)
public class TrueTime {
    public static final String MOD_ID = "truetime";
    private static final Logger LOGGER = LogManager.getLogger(TrueTime.class);

    public TrueTime(IEventBus modEventBus) {
        LOGGER.info("TrueTime initialised.");
    }
}
