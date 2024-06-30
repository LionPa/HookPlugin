package io.lionpa.hookplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

public class Recipes {
    public static void init(){
        createHookRecipe();
    }
    private static void createHookRecipe(){
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(HookPlugin.getPlugin(),"hook_recipe"),Items.HOOK);
        recipe.shape(
                " ii",
                "rci",
                "sr ");
        recipe.setIngredient('i', Material.IRON_NUGGET);
        recipe.setIngredient('c', Material.CHAIN);
        recipe.setIngredient('r', Material.REDSTONE);
        recipe.setIngredient('s', Material.STRING);
        Bukkit.addRecipe(recipe);
    }
}
