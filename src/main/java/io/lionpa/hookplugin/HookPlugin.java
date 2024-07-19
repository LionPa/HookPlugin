package io.lionpa.hookplugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class HookPlugin extends JavaPlugin {
    private static Plugin plugin;
    @Override
    public void onEnable() {
        plugin = this;
        Items.init();
        Recipes.init();
        Bukkit.getPluginManager().registerEvents(new Events(),this);

        // Удаляет цепи хука
        for (World world : Bukkit.getWorlds()){
            for (Entity entity : world.getEntitiesByClasses(BlockDisplay.class)){
                if (!entity.getPersistentDataContainer().has(Events.ENTITY_KEY)) continue;
                if (Objects.equals(entity.getPersistentDataContainer().get(Events.ENTITY_KEY, PersistentDataType.STRING), "visual")){
                    entity.remove();
                }
            }
        }
    }

    @Override
    public void onDisable() {

    }

    public static Plugin getPlugin() {
        return plugin;
    }
}
