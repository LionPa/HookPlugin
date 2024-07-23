package io.lionpa.hookplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.regex.Pattern;

public class UpdateCheck implements Listener {
    private static String OLD_VERSION_MESSAGE, UPDATE_MESSAGE, URL, NEW_UPDATE_URL;
    private static TextColor NOT_UP_TO_DATE_MESSAGE_COLOR, OLD_VERSION_COLOR, NEW_VERSION_COLOR, UPDATE_MESSAGE_COLOR, NEW_VERSION_URL_COLOR;
    private static TextReplacementConfig CURRENT_VERSION_REPLACEMENT, NEW_VERSION_REPLACEMENT;

    private static boolean UpToDate;
    private static String latestVersion;

    private static void initVars(Plugin plugin){
        YamlConfiguration config = getResource(plugin,"versionData.yml");
        OLD_VERSION_MESSAGE = (String) config.get("old_version_message");
        UPDATE_MESSAGE = (String) config.get("update_message");

        URL = (String) config.get("version_url");
        NEW_UPDATE_URL = (String) config.get("new_version_url");

        NOT_UP_TO_DATE_MESSAGE_COLOR = TextColor.fromHexString((String) config.get("not_up_to_date_message_color"));
        OLD_VERSION_COLOR = TextColor.fromHexString((String) config.get("old_version_color"));
        NEW_VERSION_COLOR = TextColor.fromHexString((String) config.get("new_version_color"));
        UPDATE_MESSAGE_COLOR = TextColor.fromHexString((String) config.get("update_message_color"));
        NEW_VERSION_URL_COLOR = TextColor.fromHexString((String) config.get("new_version_url_color"));
    }

    private static YamlConfiguration getResource(Plugin plugin, String resource){
        return YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource(resource)));
    }

    public static void init(Plugin plugin) {
        initVars(plugin);

        Bukkit.getPluginManager().registerEvents(new UpdateCheck(), plugin);

        connectToGithub(plugin);

        CURRENT_VERSION_REPLACEMENT = TextReplacementConfig.builder()
                .match(Pattern.compile("CURRENT"))
                .replacement(Component.text(plugin.getPluginMeta().getVersion()).color(OLD_VERSION_COLOR))
                .build();
        NEW_VERSION_REPLACEMENT = TextReplacementConfig.builder()
                .match(Pattern.compile("NEW"))
                .replacement(Component.text(latestVersion).color(NEW_VERSION_COLOR))
                .build();

        sendMessagesToOpsPlayers();
    }
    private static void connectToGithub(Plugin plugin){
        try {
            URLConnection openConnection = new URL(URL).openConnection();
            openConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
            Scanner scan = new Scanner((new InputStreamReader(openConnection.getInputStream())));

            latestVersion = scan.nextLine();
            UpToDate = plugin.getPluginMeta().getVersion().equals(latestVersion);
        } catch (Exception ignored) {}
    }

    private static void sendMessagesToOpsPlayers(){
        for (Player player : Bukkit.getOnlinePlayers()){
            if (!player.isOp()) continue;
            if (isUpToDate()) continue;
            sendUpdateMessage(player);
        }
    }

    @EventHandler
    public static void playerJoined(PlayerJoinEvent e){
        if (isUpToDate()) return;
        if (!e.getPlayer().isOp()) return;

        sendUpdateMessage(e.getPlayer());
    }

    private static void sendUpdateMessage(Player player){
        Component message = Component.text(OLD_VERSION_MESSAGE)
                .color(NOT_UP_TO_DATE_MESSAGE_COLOR)
                .replaceText(CURRENT_VERSION_REPLACEMENT)
                .replaceText(NEW_VERSION_REPLACEMENT);
        Component update = Component.text(UPDATE_MESSAGE)
                .append(Component.text("HERE")
                        .color(NEW_VERSION_URL_COLOR)
                        .clickEvent(ClickEvent.openUrl(NEW_UPDATE_URL)))
                .color(UPDATE_MESSAGE_COLOR);

        player.sendMessage(message);
        player.sendMessage(update);
    }

    public static boolean isUpToDate() {
        return UpToDate;
    }
}
