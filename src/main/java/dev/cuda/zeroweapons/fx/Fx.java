package dev.cuda.zeroweapons.fx;

import dev.cuda.zeroweapons.ZeroWeapons;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The effects engine every weapon visual runs on.
 *
 * <ul>
 *   <li>One repeating task drives every running animation and cancels itself when nothing
 *       is left, so an idle server pays nothing.</li>
 *   <li>At most {@link #MAX_ACTIVE} animations at once; past that new ones are skipped rather
 *       than queued, so a 40-player fight can't snowball.</li>
 *   <li>Display entities are non-persistent, tagged, tracked and removed when their animation
 *       ends, on shutdown, and (orphans from a crash) on enable.</li>
 *   <li>{@code visuals.quality} in config.yml scales every particle count (0.5 = half).</li>
 * </ul>
 *
 * Main thread only.
 */
public final class Fx {

    /** One running animation. {@code tick} gets the age (0, 1, 2...) and returns false when done. */
    public interface Anim {
        boolean tick(int age);

        /** Always called once when it stops, finished or not. Remove displays here. */
        default void end() {
        }
    }

    public static final String TAG = "zeroweapons_fx";
    private static final int MAX_ACTIVE = 192;
    private static final int MAX_DISPLAYS = 160;

    private static final class Running {
        final Anim anim;
        int age;

        Running(Anim anim) {
            this.anim = anim;
        }
    }

    private static final List<Running> RUNNING = new ArrayList<>();
    private static final Set<Display> DISPLAYS = new HashSet<>();
    private static BukkitTask task;
    private static ZeroWeapons plugin;
    private static boolean enabled = true;
    private static boolean displays = true;
    private static double quality = 1.0;

    private Fx() {}

    public static void init(ZeroWeapons owner) {
        plugin = owner;
        reload();
        // Displays are non-persistent, but a hard crash between spawn and save can still leave
        // one behind in a chunk that was saved mid-animation. Clear any in loaded worlds.
        for (World world : owner.getServer().getWorlds()) {
            for (Entity e : world.getEntitiesByClass(Display.class)) {
                if (e.getScoreboardTags().contains(TAG)) e.remove();
            }
        }
    }

    public static void reload() {
        if (plugin == null) return;
        enabled = plugin.getConfig().getBoolean("visuals.enabled", true);
        displays = plugin.getConfig().getBoolean("visuals.display-entities", true);
        quality = Math.max(0.0, Math.min(2.0, plugin.getConfig().getDouble("visuals.quality", 1.0)));
    }

    public static boolean on() {
        return enabled && plugin != null;
    }

    public static boolean displaysOn() {
        return on() && displays;
    }

    /** Starts an animation. Its first tick runs right away, so tick 0 lands on the same frame as the hit. */
    public static void play(Anim anim) {
        if (!on() || RUNNING.size() >= MAX_ACTIVE) return;
        Running r = new Running(anim);
        boolean more;
        try {
            more = anim.tick(0);
        } catch (RuntimeException e) {
            more = false;
        }
        if (!more) {
            safeEnd(anim);
            return;
        }
        r.age = 1;
        RUNNING.add(r);
        if (task == null) task = plugin.getServer().getScheduler().runTaskTimer(plugin, Fx::tickAll, 1L, 1L);
    }

    private static void tickAll() {
        Iterator<Running> it = RUNNING.iterator();
        while (it.hasNext()) {
            Running r = it.next();
            boolean more;
            try {
                more = r.anim.tick(r.age++);
            } catch (RuntimeException e) {
                more = false;
            }
            if (!more) {
                it.remove();
                safeEnd(r.anim);
            }
        }
        if (RUNNING.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
    }

    private static void safeEnd(Anim anim) {
        try {
            anim.end();
        } catch (RuntimeException ignored) {
            // A broken end() must not leave the rest of the list half-stopped.
        }
    }

    public static void shutdown() {
        for (Running r : new ArrayList<>(RUNNING)) safeEnd(r.anim);
        RUNNING.clear();
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Display d : DISPLAYS) {
            if (d.isValid()) d.remove();
        }
        DISPLAYS.clear();
    }

    public static int active() {
        return RUNNING.size();
    }

    // ── Particles ───────────────────────────────────────────────────────────

    /** {@code count} scaled by quality, at least 1 when the base count is above 0. */
    public static int n(int count) {
        if (count <= 0) return 0;
        return Math.max(1, (int) Math.round(count * quality));
    }

    public static Particle.DustOptions dust(int rgb, float size) {
        return new Particle.DustOptions(Color.fromRGB(rgb), size);
    }

    public static Particle.DustTransition fade(int from, int to, float size) {
        return new Particle.DustTransition(Color.fromRGB(from), Color.fromRGB(to), size);
    }

    public static void p(Location at, Particle particle, int count, double spread, double speed) {
        int c = n(count);
        if (c > 0) at.getWorld().spawnParticle(particle, at, c, spread, spread, spread, speed);
    }

    public static <T> void p(Location at, Particle particle, int count, double spread, double speed, T data) {
        int c = n(count);
        if (c > 0) at.getWorld().spawnParticle(particle, at, c, spread, spread, spread, speed, data);
    }

    /** One particle exactly at {@code x,y,z}, no spread. Used for drawn shapes. */
    public static <T> void dot(World world, double x, double y, double z, Particle particle, T data) {
        if (data == null) {
            world.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0);
        } else {
            world.spawnParticle(particle, x, y, z, 1, 0, 0, 0, 0, data);
        }
    }

    /** A particle that flies from {@code from} to {@code to} over {@code ticks}, in {@code rgb}. */
    public static void trail(Location from, Location to, int rgb, int ticks, int count, double spread) {
        int c = n(count);
        if (c <= 0) return;
        from.getWorld().spawnParticle(Particle.TRAIL, from, c, spread, spread, spread, 0,
            new Particle.Trail(to, Color.fromRGB(rgb), ticks));
    }

    /**
     * A tight bright pop in {@code rgb}. (Not vanilla FLASH: that draws a huge translucent box
     * that fills half the screen.)
     */
    public static void flash(Location at, int rgb) {
        p(at, Particle.END_ROD, 6, 0.05, 0.12);
        p(at, Particle.DUST, 10, 0.15, 0, dust(rgb, 1.8f));
    }

    // ── Shapes ──────────────────────────────────────────────────────────────

    /** Two unit vectors at right angles to {@code normal} and each other. */
    public static Vector[] basis(Vector normal) {
        Vector n = normal.clone();
        if (n.lengthSquared() < 1.0E-6) n = new Vector(0, 1, 0);
        n.normalize();
        Vector helper = Math.abs(n.getY()) < 0.95 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
        Vector u = n.getCrossProduct(helper).normalize();
        Vector v = n.getCrossProduct(u).normalize();
        return new Vector[] {u, v};
    }

    /** Points on a circle around {@code centre}, facing {@code normal}, starting at {@code phase} radians. */
    public static void ring(Location centre, Vector normal, double radius, int points, double phase, Consumer<Location> draw) {
        int count = n(points);
        Vector[] uv = basis(normal);
        for (int i = 0; i < count; i++) {
            double a = phase + Math.PI * 2 * i / count;
            double c = Math.cos(a) * radius;
            double s = Math.sin(a) * radius;
            draw.accept(centre.clone().add(uv[0].getX() * c + uv[1].getX() * s,
                uv[0].getY() * c + uv[1].getY() * s, uv[0].getZ() * c + uv[1].getZ() * s));
        }
    }

    /** Flat ring on the ground. */
    public static void flatRing(Location centre, double radius, int points, double phase, Consumer<Location> draw) {
        ring(centre, new Vector(0, 1, 0), radius, points, phase, draw);
    }

    /** Points every {@code step} blocks from {@code a} to {@code b}. */
    public static void line(Location a, Location b, double step, Consumer<Location> draw) {
        Vector d = b.toVector().subtract(a.toVector());
        double len = d.length();
        if (len < 1.0E-4) return;
        double s = step / Math.max(0.1, quality);
        d.multiply(1.0 / len);
        for (double t = 0; t <= len; t += s) draw.accept(a.clone().add(d.getX() * t, d.getY() * t, d.getZ() * t));
    }

    /** Draws {@code shape} with a coloured dust particle. */
    public static Consumer<Location> dustAt(Particle.DustOptions dust) {
        return at -> dot(at.getWorld(), at.getX(), at.getY(), at.getZ(), Particle.DUST, dust);
    }

    public static Consumer<Location> fadeAt(Particle.DustTransition dust) {
        return at -> dot(at.getWorld(), at.getX(), at.getY(), at.getZ(), Particle.DUST_COLOR_TRANSITION, dust);
    }

    public static Consumer<Location> at(Particle particle) {
        return at -> dot(at.getWorld(), at.getX(), at.getY(), at.getZ(), particle, null);
    }

    // ── Display entities ────────────────────────────────────────────────────

    private static boolean roomForDisplay() {
        DISPLAYS.removeIf(d -> !d.isValid());
        return displaysOn() && DISPLAYS.size() < MAX_DISPLAYS;
    }

    private static <D extends Display> D setup(D d, Transformation start) {
        d.setPersistent(false);
        d.addScoreboardTag(TAG);
        d.setViewRange(0.5f);
        d.setTransformation(start);
        d.setInterpolationDelay(0);
        d.setTeleportDuration(2);
        DISPLAYS.add(d);
        return d;
    }

    /** A block display, or null when displays are off or at the cap. */
    public static BlockDisplay block(Location at, BlockData data, Transformation start) {
        if (!roomForDisplay()) return null;
        return at.getWorld().spawn(at, BlockDisplay.class, d -> {
            d.setBlock(data);
            setup(d, start);
        });
    }

    public static ItemDisplay item(Location at, ItemStack stack, Transformation start) {
        if (!roomForDisplay()) return null;
        return at.getWorld().spawn(at, ItemDisplay.class, d -> {
            d.setItemStack(stack);
            setup(d, start);
        });
    }

    public static TextDisplay text(Location at, net.kyori.adventure.text.Component text, Transformation start) {
        if (!roomForDisplay()) return null;
        return at.getWorld().spawn(at, TextDisplay.class, d -> {
            d.text(text);
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setShadowed(true);
            setup(d, start);
        });
    }

    /** Moves a display to a new shape over {@code ticks} (client side, smooth). */
    public static void morph(Display d, Transformation to, int ticks) {
        if (d == null || !d.isValid()) return;
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(Math.max(0, ticks));
        d.setTransformation(to);
    }

    public static void kill(Display d) {
        if (d == null) return;
        DISPLAYS.remove(d);
        if (d.isValid()) d.remove();
    }

    public static Transformation tf(float tx, float ty, float tz, Quaternionf rot, float sx, float sy, float sz) {
        return new Transformation(new Vector3f(tx, ty, tz), rot, new Vector3f(sx, sy, sz), new Quaternionf());
    }

    public static Transformation tf(float tx, float ty, float tz, float scale) {
        return tf(tx, ty, tz, new Quaternionf(), scale, scale, scale);
    }

    /** Full brightness, so void and fire shapes glow in the dark. */
    public static void glow(Display d) {
        if (d != null) d.setBrightness(new Display.Brightness(15, 15));
    }
}
