package io.lionpa.hookplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.awt.*;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class UpdateCheck implements Listener {
    private static final String NEW_VERSION_MESSAGE = "HookPlugin has new version! Current version: {CURRENT}, New version: {NEW}";
    private static final String UPDATE_MESSAGE = "You can download new version ";

    private static final String URL = "https://raw.githubusercontent.com/LionPa/HookPlugin/main/version.txt";
    private static final String UPDATE_URL = "https://github.com/LionPa/HookPlugin/releases";

    private static boolean UpToDate;
    private static String lastVersion;
    private static Plugin plug;

    public static void init(Plugin plugin) {
        plug = plugin;
        Bukkit.getPluginManager().registerEvents(new UpdateCheck(), plugin);

        try {
            URLConnection openConnection = new URL(URL).openConnection();
            openConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
            Scanner scan = new Scanner((new InputStreamReader(openConnection.getInputStream())));

            lastVersion = scan.nextLine();
            UpToDate = plugin.getPluginMeta().getVersion().equals(lastVersion);
            System.out.println(lastVersion + " " + plugin.getPluginMeta().getVersion());

        } catch (Exception ignored) {
        }
    }

    public static boolean isIsUpToDate() {
        return UpToDate;
    }

    @EventHandler
    public static void playerJoined(PlayerJoinEvent e){
        if (isIsUpToDate()) return;
        if (!e.getPlayer().isOp()) return;
        Component message = Component.text(NEW_VERSION_MESSAGE
                .replace("{CURRENT}", plug.getPluginMeta().getVersion())
                .replace("{NEW}", lastVersion)).color(TextColor.color(255,20,20));

        Component update = Component.text(UPDATE_MESSAGE).append(Component.text("HERE").color(TextColor.color(255, 203, 19)).clickEvent(ClickEvent.openUrl(UPDATE_URL))).color(TextColor.color(255,20,20));

        e.getPlayer().sendMessage(message);
        e.getPlayer().sendMessage(update);
    }
}
