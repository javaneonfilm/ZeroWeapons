package dev.cuda.zeroweapons.weapons.utils;

import java.util.Comparator;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public final class TargetUtils {

    /**
     * How far off the crosshair a player can be and still count as aimed at.
     *
     * The ray is a line; a player is not. Without a little padding, "looking at them" means
     * threading a mathematical line through a moving hitbox at twenty blocks, which reads as
     * the ability being broken rather than as the player having missed.
     */
    private static final double AIM_TOLERANCE = 0.7;

    private TargetUtils() {}

    /**
     * Nearest other player within {@code range}.
     *
     * Spectators and creative-mode staff are skipped so an admin flying over a fight can't
     * accidentally eat a stun or a root.
     */
    public static Player nearest(Player source, double range) {
        return source.getNearbyEntities(range, range, range).stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .filter(other -> !other.equals(source))
            .filter(TargetUtils::targetable)
            .min(Comparator.comparingDouble(other -> other.getLocation().distanceSquared(source.getLocation())))
            .orElse(null);
    }

    /**
     * The player under the crosshair, out to {@code range}.
     *
     * <p>Blocks are traced in the same pass as entities, so the nearest hit wins and a wall
     * between the two of you means no target — the alternative is shooting through terrain,
     * which turns any ranged ability into a way to hit people who can't hit back. Passable
     * blocks (grass, torches, banners) are ignored, because being saved by a fern is worse
     * than either outcome.
     */
    public static Player looking(Player source, double range) {
        RayTraceResult result = source.getWorld().rayTrace(
            source.getEyeLocation(),
            source.getEyeLocation().getDirection(),
            range,
            FluidCollisionMode.NEVER,
            true,
            AIM_TOLERANCE,
            entity -> entity instanceof Player other && !other.equals(source) && targetable(other));

        return result != null && result.getHitEntity() instanceof Player hit ? hit : null;
    }

    /** Survival and adventure only — staff watching a fight are not part of it. */
    public static boolean targetable(Entity entity) {
        if (!(entity instanceof Player player)) return false;
        return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
    }
}
