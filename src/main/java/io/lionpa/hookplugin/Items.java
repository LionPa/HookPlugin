package io.lionpa.hookplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class Items {
    public static final NamespacedKey ITEM_KEY = new NamespacedKey(HookPlugin.getPlugin(),"item");
    public static ItemStack HOOK;

    public static void init(){
        createHook();
    }
    public static void createHook(){
        HOOK = new ItemStack(Material.TRIDENT);
        ItemMeta meta = HOOK.getItemMeta();
        meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING,"hook");
        meta.setDisplayName(ChatColor.RESET + "Крюк");
        meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS,ItemFlag.HIDE_ATTRIBUTES);
        HOOK.setItemMeta(meta);
    }
}
