package mangomilk.truetime;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class TrueTimeAnnouncements
{
    private TrueTimeAnnouncements()
    {
    }

    public static void announceDayChange(MinecraftServer server, long preservedDay, long dayTime)
    {
        if (!TrueTimeConfig.ANNOUNCE_DAY_CHANGES.get())
        {
            return;
        }

        String message = TrueTimeConfig.DAY_CHANGE_MESSAGE.get()
                .replace("{day}", Long.toString(preservedDay))
                .replace("{time}", Long.toString(TrueTimeTimekeeper.timeOfDay(dayTime)))
                .replace("{raw_time}", Long.toString(dayTime));

        String mode = TrueTimeConfig.ANNOUNCEMENT_MODE.get().toLowerCase(Locale.ROOT);

        if (mode.equals("chat") || mode.equals("chat_actionbar") || mode.equals("chat_title") || mode.equals("all"))
        {
            server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        }

        if (mode.equals("actionbar") || mode.equals("chat_actionbar") || mode.equals("all"))
        {
            broadcastActionBar(server, format(TrueTimeConfig.DAY_CHANGE_ACTION_BAR.get(), preservedDay, dayTime));
        }

        if (mode.equals("title") || mode.equals("chat_title") || mode.equals("all"))
        {
            broadcastTitle(
                    server,
                    format(TrueTimeConfig.DAY_CHANGE_TITLE.get(), preservedDay, dayTime),
                    format(TrueTimeConfig.DAY_CHANGE_SUBTITLE.get(), preservedDay, dayTime)
            );
        }
    }

    private static void broadcastActionBar(MinecraftServer server, String message)
    {
        Component component = Component.literal(message);

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            player.connection.send(new ClientboundSetActionBarTextPacket(component));
        }
    }

    private static void broadcastTitle(MinecraftServer server, String title, String subtitle)
    {
        ClientboundSetTitlesAnimationPacket timings = new ClientboundSetTitlesAnimationPacket(
                TrueTimeConfig.TITLE_FADE_IN_TICKS.get(),
                TrueTimeConfig.TITLE_STAY_TICKS.get(),
                TrueTimeConfig.TITLE_FADE_OUT_TICKS.get()
        );

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            player.connection.send(timings);
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));

            if (!subtitle.isBlank())
            {
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
            }
        }
    }

    private static String format(String template, long preservedDay, long dayTime)
    {
        return template
                .replace("{day}", Long.toString(preservedDay))
                .replace("{time}", Long.toString(TrueTimeTimekeeper.timeOfDay(dayTime)))
                .replace("{raw_time}", Long.toString(dayTime));
    }
}
