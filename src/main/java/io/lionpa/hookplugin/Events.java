package io.lionpa.hookplugin;

import io.lionpa.hookplugin.events.HookHitBlock;
import io.lionpa.hookplugin.events.ThrowHookEvent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
    public static final NamespacedKey DAMAGE_MULTIPLIER = new NamespacedKey(HookPlugin.getPlugin(),"damage_multiplier");

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
        Trident hook = createHook(player, trident);

        ThrowHookEvent event = new ThrowHookEvent(player,hook);
        event.callEvent();
        if (event.isCancelled()){
            hook.remove();
        }
    }

    private static Trident createHook(Player player, Trident trident){
        Trident hook = player.getWorld().spawn(player.getEyeLocation(), Trident.class);
        hook.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        hook.setDamage(0);
        hook.setGlint(true);
        hook.setVelocity(trident.getVelocity().multiply(0.8f));

        hook.getPersistentDataContainer().set(ENTITY_KEY,PersistentDataType.STRING, "hook");
        hook.getPersistentDataContainer().set(OWNER_KEY,PersistentDataType.STRING, player.getName());
        hook.getPersistentDataContainer().set(DAMAGE_MULTIPLIER,PersistentDataType.FLOAT,1.5f);

        return hook;
    }

    @EventHandler
    private static void hookHit(ProjectileHitEvent e){
        Projectile hook = e.getEntity();

        PersistentDataContainer data = hook.getPersistentDataContainer();

        if (!data.has(ENTITY_KEY)) return;
        if (!Objects.equals(data.get(ENTITY_KEY, PersistentDataType.STRING), "hook")) return;

        Player player = Bukkit.getPlayer(data.get(OWNER_KEY, PersistentDataType.STRING)); // Игрок который кинул хук

        if (e.getHitEntity() == player) { // Если игрок по себе попал
            e.setCancelled(true);
            return;
        }

        if (e.getHitEntity() != null){
            if (!(e.getHitEntity() instanceof LivingEntity entity)) return;
            hookEntity(player,entity,hook); // Хукает моба к себе
            hook.remove();
            return;
        }

        if (e.getHitBlock() == null){
            hook.remove();
            return;
        }

        startPlayerHooking(player,hook); // Хукается к блоку
    }
    private static void startPlayerHooking(Player player, Projectile hook){
        HookHitBlock event = new HookHitBlock(player, (Trident) hook);
        event.callEvent();

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (hook.isDead()){
                    cancel();
                    return;
                }

                if (canPickUpHook(player,hook)) {
                    player.setVelocity(player.getVelocity().multiply(0.9f));
                    hook.remove();
                    cancel();
                    return;
                }

                Vector velocity = setUpVelocity(hook,player,0.06f,false);

                spawnChains(player,hook.getLocation());

                playSound(player,velocity);
            }
        }.runTaskTimer(HookPlugin.getPlugin(),0,1);

        killTimer(hook,4,task,true);
    }

    private static void hookEntity(Player player, LivingEntity entity, Projectile hook){
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead()) {
                    cancel();
                }

                if (canPickUpHook(player,entity)) {
                    entity.setVelocity(entity.getVelocity().multiply(0.3f));
                    float damage_multiplier = hook.getPersistentDataContainer().get(DAMAGE_MULTIPLIER,PersistentDataType.FLOAT);
                    entity.getPersistentDataContainer().set(DAMAGE_MULTIPLIER,PersistentDataType.FLOAT,damage_multiplier);
                    cancel();
                }

                Vector velocity = setUpVelocity(player,entity,0.25f,true);

                spawnChains(player,entity.getEyeLocation());

                playSound(player,velocity);
            }
        }.runTaskTimer(HookPlugin.getPlugin(),0,1);

        killTimer(entity,8,task,false);
    }

    // Проверка может ли игрок поднять хук
    private static boolean canPickUpHook(Player player, Entity entity){
        return player.getLocation().getNearbyEntitiesByType(entity.getType().getEntityClass(),2,2,2).contains(entity);
    }

    // Устанавливает ускорение
    private static Vector setUpVelocity(Entity entity1, Entity entity2, float speed, boolean hookingEntity){
        Vector velocity = entity1.getLocation().toVector().subtract(entity2.getLocation().toVector()).multiply(speed);

        Vector entityVelocity;
        if (hookingEntity)
            entityVelocity = velocity; // Хук ентити
        else
            entityVelocity = velocity.add(entity2.getVelocity().multiply(0.95f)); // Хук к блоку

        limitVelocity(entityVelocity);

        entity2.setVelocity(entityVelocity);

        return entityVelocity;
    }

    private static final Quaternionf q = new Quaternionf().rotateLocalX((float) Math.toRadians(90));
    // Создает цепи
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
    // Звук
    private static void playSound(Player player, Vector velocity){
        if (Bukkit.getCurrentTick() % (1f / velocity.length()) < 0.5f)
            player.getWorld().playSound(player, Sound.BLOCK_CHAIN_STEP,SoundCategory.MASTER,0.3f, 0.3f);
    }
    // Таймер хука
    private static void killTimer(Entity entity, int time, BukkitTask task, boolean kill){
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!entity.isDead()) {
                    task.cancel();
                    if (kill) entity.remove();
                }
            }
        }.runTaskLater(HookPlugin.getPlugin(),time * 20L);
    }

    private static final float X_LIMIT = 1.4f;
    private static final float Y_LIMIT = 1.2f;
    private static final float Z_LIMIT = 1.4f;
    // Ограничевает скорость
    private static void limitVelocity(Vector velocity){
        if (velocity.getX() > X_LIMIT) velocity.setX(X_LIMIT);
        if (velocity.getX() < -X_LIMIT) velocity.setX(-X_LIMIT);

        if (velocity.getX() > Y_LIMIT) velocity.setY(Y_LIMIT);
        if (velocity.getX() < -Y_LIMIT) velocity.setY(-Y_LIMIT);

        if (velocity.getX() > Z_LIMIT) velocity.setZ(Z_LIMIT);
        if (velocity.getX() < -Z_LIMIT) velocity.setZ(-Z_LIMIT);
    }

    @EventHandler
    public static void damage(EntityDamageByEntityEvent e){
        if (!e.getEntity().getPersistentDataContainer().has(DAMAGE_MULTIPLIER)) return;
        e.setDamage(e.getDamage() * e.getEntity().getPersistentDataContainer().get(DAMAGE_MULTIPLIER,PersistentDataType.FLOAT));
        e.getEntity().getPersistentDataContainer().remove(DAMAGE_MULTIPLIER);
    }
}
