package io.lionpa.hookplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Events implements Listener {
    public static final NamespacedKey ENTITY_KEY = new NamespacedKey(HookPlugin.getPlugin(),"entity");
    public static final NamespacedKey OWNER_KEY = new NamespacedKey(HookPlugin.getPlugin(),"owner");
    @EventHandler
    public static void hookUsed(ProjectileLaunchEvent e){
        if (!(e.getEntity().getShooter() instanceof Player player)) return;
        if (!(e.getEntity() instanceof Trident trident)) return;

        PersistentDataContainer data = trident.getItemStack().getItemMeta().getPersistentDataContainer();

        if (!data.has(Items.ITEM_KEY)){return;}
        if (!Objects.equals(data.get(Items.ITEM_KEY, PersistentDataType.STRING), "hook")) return;

        e.setCancelled(true);
        spawnHook(player,trident);
    }
    private static void spawnHook(Player player, Trident trident){
        Trident hook = player.getWorld().spawn(player.getEyeLocation(), Trident.class);
        hook.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        hook.setDamage(0);
        hook.setGlint(true);
        hook.setVelocity(trident.getVelocity().multiply(0.8f));

        hook.getPersistentDataContainer().set(ENTITY_KEY,PersistentDataType.STRING, "hook");
        hook.getPersistentDataContainer().set(OWNER_KEY,PersistentDataType.STRING, player.getName());
    }
    @EventHandler
    private static void hookHit(ProjectileHitEvent e){
        PersistentDataContainer data = e.getEntity().getPersistentDataContainer();
        if (!data.has(ENTITY_KEY)) return;
        if (!Objects.equals(data.get(ENTITY_KEY, PersistentDataType.STRING), "hook")) return;

        Projectile hook = e.getEntity();

        Player player = Bukkit.getPlayer(data.get(OWNER_KEY, PersistentDataType.STRING));

        if (e.getHitEntity() != null){
            e.getHitEntity().setVelocity(player.getLocation().toVector().subtract(e.getHitEntity().getLocation().toVector()).multiply(0.2));
        }
        if (e.getHitBlock() == null){
            e.getEntity().remove();
            return;
        }
        startPlayerHooking(player,hook);
    }
    private static void startPlayerHooking(Player player, Projectile hook){
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.getLocation().getNearbyEntitiesByType(Trident.class,2,2,2).contains(hook)) {
                    hook.remove();
                    cancel();
                }

                Vector velocity = hook.getLocation().toVector().subtract(player.getLocation().toVector()).multiply(0.04f);

                Vector playerVelocity = player.getVelocity().add(velocity);
                limitVelocity(playerVelocity);

                player.setVelocity(playerVelocity);

                spawnChains(player,hook.getLocation());
            }
        }.runTaskTimer(HookPlugin.getPlugin(),0,1);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!hook.isDead()) {
                    task.cancel();
                    hook.remove();
                }
            }
        }.runTaskLater(HookPlugin.getPlugin(),4 * 20);
    }
    private static final float X_LIMIT = 1.6f;
    private static final float Y_LIMIT = 1.6f;
    private static final float Z_LIMIT = 1.6f;
    private static void limitVelocity(Vector velocity){
        if (velocity.getX() > X_LIMIT) velocity.setX(X_LIMIT);
        if (velocity.getX() < -X_LIMIT) velocity.setX(-X_LIMIT);

        if (velocity.getX() > Y_LIMIT) velocity.setY(Y_LIMIT);
        if (velocity.getX() < -Y_LIMIT) velocity.setY(-Y_LIMIT);

        if (velocity.getX() > Z_LIMIT) velocity.setZ(Z_LIMIT);
        if (velocity.getX() < -Z_LIMIT) velocity.setZ(-Z_LIMIT);
    }
    private static final Quaternionf q = new Quaternionf().rotateLocalX((float) Math.toRadians(90));

    private static void spawnChains(Player player, Location hook){
        hook.add(0,1,0);
        double distance = player.getLocation().distance(hook);
        List<BlockDisplay> chains = new ArrayList<>();

        Vector rotation = hook.toVector().subtract(player.getEyeLocation().toVector());

        for (int i = 0; i < distance; i++){
            float progress = (float) (i/distance);
            Location spawn = lerpVector(player.getEyeLocation().toVector(),hook.toVector(),progress).toLocation(player.getWorld());
            spawn.setDirection(rotation);
            BlockDisplay display = player.getWorld().spawn(spawn, BlockDisplay.class);
            display.getPersistentDataContainer().set(ENTITY_KEY,PersistentDataType.STRING,"visual");

            display.setBlock(Material.CHAIN.createBlockData());

            Transformation t = display.getTransformation();
            t.getTranslation().set(-0.5,-0.5f,0);
            t.getLeftRotation().set(q);
            display.setTransformation(t);
            chains.add(display);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                chains.forEach(Entity::remove);
            }
        }.runTaskLater(HookPlugin.getPlugin(),2);
    }
    private static Vector lerpVector(Vector a, Vector b, float progress){
        double x = (b.getX() - a.getX()) * progress + a.getX();
        double y = (b.getY() - a.getY()) * progress + a.getY();
        double z = (b.getZ() - a.getZ()) * progress + a.getZ();

        return a.setX(x).setY(y).setZ(z);
    }
    private static float normal(float angle){
        angle = angle % 360;

        angle = (angle + 360) % 360;

        if (angle > 180)
            angle -= 360;
        return angle;
    }
    private static float[] getRotation(Player player, float x, float y, float z){
        double dX = x - player.getEyeLocation().x();
        double dY = player.getEyeLocation().y() - y;
        double dZ = z - player.getEyeLocation().z();

        float lookYaw = normal((float) (Math.toDegrees(Math.atan2(dZ, dX)) - 90));
        float lookPitch = (float) Math.toDegrees(Math.atan2(dY, Math.sqrt(dX * dX + dZ * dZ)));
        return new float[]{lookPitch, lookYaw};
    }
}
