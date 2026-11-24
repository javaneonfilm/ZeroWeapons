package dev.cuda.zeroweapons.fx;

import dev.cuda.zeroweapons.core.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

/**
 * Every weapon's ability visual. Each method is fire-and-forget: it draws on the spot or starts
 * an {@link Fx.Anim} and returns. Nothing here changes gameplay.
 */
public final class WeaponFx {

    private static final Random R = new Random();

    // Colours, matched to each weapon's gradient.
    private static final int LAVENDER = 0xD9A6FF, VIOLET = 0x8C00F6;
    private static final int ABYSS = 0x140014, AMETHYST = 0xA020F0, ORCHID = 0xD07CFF;
    private static final int NULL_HI = 0xE3E8EF, NULL_LO = 0x6B7280;
    private static final int CYAN = 0x6BFFF2, AZURE = 0x0091FF;
    private static final int DREAD_HI = 0xFF6B6B, DREAD_LO = 0x7A0010;
    private static final int BLOOD = 0xE0002B, BLOOD_HI = 0xFF9EB0, BLOOD_DARK = 0x8A0012;
    private static final int SILK = 0xF2F2F2;
    private static final int VOID_HI = 0x7A9CFF, VOID_LO = 0x1B2A80, VOID_BLACK = 0x05061A;
    private static final int PINK_HI = 0xFFB3F0, PINK = 0xFF3DA8;
    private static final int EMBER = 0xFFD36B, FIRE = 0xFF3D00, SOUL = 0x3FD0FF;
    private static final int LIME = 0xC4FF7A, JADE = 0x00A86B;

    private WeaponFx() {}

    private static Location chest(LivingEntity e) {
        return e.getLocation().add(0, e.getHeight() * 0.55, 0);
    }

    private static Location head(LivingEntity e) {
        return e.getLocation().add(0, e.getHeight() + 0.55, 0);
    }

    private static boolean gone(LivingEntity e) {
        return e == null || !e.isValid() || e.isDead() || (e instanceof Player p && !p.isOnline());
    }

    /** A continuous ribbon over the distance an arrow covered this tick (it moves ~3 blocks a tick). */
    private static void ribbon(AbstractArrow arrow, double step, java.util.function.Consumer<Location> draw) {
        Location at = arrow.getLocation();
        Vector v = arrow.getVelocity();
        if (v.lengthSquared() > 25) v = v.normalize().multiply(5);
        Fx.line(at.clone().subtract(v), at, step, draw);
    }

    private static Vector flat(Vector v) {
        Vector f = v.clone().setY(0);
        return f.lengthSquared() < 1.0E-4 ? new Vector(0, 0, 1) : f.normalize();
    }

    // ── Dash Sword ──────────────────────────────────────────────────────────

    /**
     * A violet tear rips open where you were with a burst of wind, a thick streak follows you
     * with fading afterimage outlines of your body, and a ring slams the ground where you land.
     */
    public static void dash(Player player, Vector dir) {
        if (!Fx.on()) return;
        Location origin = player.getLocation().add(0, 1.0, 0);
        Vector d = dir.clone().normalize();
        Particle.DustTransition streak = Fx.fade(LAVENDER, VIOLET, 1.7f);
        Particle.DustTransition ghost = Fx.fade(LAVENDER, VIOLET, 0.9f);
        Fx.flash(origin, LAVENDER);
        Fx.p(origin, Particle.GUST, 1, 0, 0);
        Fx.play(new Fx.Anim() {
            Location last = origin.clone();

            @Override
            public boolean tick(int age) {
                if (age <= 3) {
                    // The tear: rings blowing outward around where the dash started.
                    double r = 0.6 + age * 0.5;
                    Fx.ring(origin, d, r, 30, age * 0.4, Fx.fadeAt(Fx.fade(LAVENDER, VIOLET, 1.6f)));
                    if (age <= 1) Fx.ring(origin, d, r * 0.55, 14, 0.2, Fx.at(Particle.END_ROD));
                }
                if (gone(player)) return false;
                Location now = player.getLocation().add(0, 1.0, 0);
                if (age <= 10 && now.getWorld().equals(this.last.getWorld()) && now.distanceSquared(this.last) < 64) {
                    // Thick streak between last tick and this one: a core line and two thinner rails.
                    Fx.line(this.last, now, 0.16, Fx.fadeAt(streak));
                    Fx.line(this.last.clone().add(0, -0.45, 0), now.clone().add(0, -0.45, 0), 0.3, Fx.fadeAt(ghost));
                    Fx.line(this.last.clone().add(0, 0.45, 0), now.clone().add(0, 0.45, 0), 0.3, Fx.fadeAt(ghost));
                    Fx.p(now, Particle.END_ROD, 2, 0.2, 0.01);
                    Fx.p(now.clone().add(0, -0.6, 0), Particle.SCULK_SOUL, 1, 0.2, 0.01);
                    // Afterimage: an outline of the body left behind every other tick.
                    if (age % 2 == 0) {
                        Fx.ring(this.last.clone().add(0, -0.1, 0), d.clone().setY(0).lengthSquared() < 1.0E-4 ? new Vector(0, 0, 1) : d.clone().setY(0),
                            0.55, 16, 0, at -> {
                                // Squash the circle into a tall oval the shape of a body.
                                Location c = this.last.clone().add(0, -0.1, 0);
                                Vector off = at.toVector().subtract(c.toVector());
                                Location p = c.clone().add(off.getX() * 0.8, off.getY() * 1.7, off.getZ() * 0.8);
                                Fx.dot(p.getWorld(), p.getX(), p.getY(), p.getZ(), Particle.DUST_COLOR_TRANSITION, ghost);
                            });
                    }
                    this.last = now;
                }
                if (age > 3 && (player.isOnGround() || age >= 12)) {
                    Location feet = player.getLocation().add(0, 0.1, 0);
                    Fx.flatRing(feet, 1.1, 20, 0, Fx.fadeAt(Fx.fade(LAVENDER, VIOLET, 1.2f)));
                    Fx.flatRing(feet, 0.6, 12, 0.3, Fx.dustAt(Fx.dust(VIOLET, 1.0f)));
                    Fx.p(feet, Particle.CLOUD, 6, 0.3, 0.02);
                    return false;
                }
                return age < 14;
            }
        });
    }

    // ── Vortex Sword ────────────────────────────────────────────────────────

    /**
     * A jagged purple bolt from the sky, a black sigil on the ground, crying-obsidian spikes
     * bursting up around them, a twin spiral, then everything implodes.
     */
    public static void vortex(LivingEntity target) {
        if (!Fx.on()) return;
        World world = target.getWorld();
        Location base = target.getLocation().add(0, 0.05, 0);
        Particle.DustOptions black = Fx.dust(ABYSS, 1.7f);
        Particle.DustOptions purple = Fx.dust(AMETHYST, 1.2f);
        Particle.DustOptions orchid = Fx.dust(ORCHID, 0.9f);

        // The bolt: a jagged line from high above down into their chest, drawn in one frame.
        Location top = base.clone().add(R.nextGaussian() * 0.6, 9.0, R.nextGaussian() * 0.6);
        Location bottom = chest(target);
        Location prev = top;
        int segments = 8;
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            Location node = top.clone().add(bottom.clone().subtract(top).toVector().multiply(t));
            if (i < segments) node.add(R.nextGaussian() * 0.45, 0, R.nextGaussian() * 0.45);
            Fx.line(prev, node, 0.14, Fx.dustAt(i % 2 == 0 ? black : purple));
            Fx.line(prev, node, 0.5, Fx.dustAt(orchid));
            prev = node;
        }
        Fx.flash(bottom, AMETHYST);
        world.playSound(base, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 0.6f);

        Fx.play(new Fx.Anim() {
            final List<BlockDisplay> shards = new ArrayList<>();
            final float[] tilt = new float[6];

            @Override
            public boolean tick(int age) {
                // Ground sigil: two counter-turning rings and six spokes.
                if (age <= 8) {
                    Fx.flatRing(base, 1.7, 30, age * 0.25, Fx.dustAt(age % 2 == 0 ? black : purple));
                    Fx.flatRing(base, 1.15, 20, -age * 0.35, Fx.dustAt(purple));
                    if (age == 0) {
                        for (int i = 0; i < 6; i++) {
                            double a = Math.PI * 2 * i / 6;
                            Location in = base.clone().add(Math.cos(a) * 0.5, 0, Math.sin(a) * 0.5);
                            Location out = base.clone().add(Math.cos(a) * 1.7, 0, Math.sin(a) * 1.7);
                            Fx.line(in, out, 0.18, Fx.dustAt(black));
                        }
                    }
                }
                // Spikes erupt on tick 1, stand, then sink.
                if (age == 1) this.spawnShards();
                if (age == 2) {
                    for (int i = 0; i < this.shards.size(); i++) {
                        Fx.morph(this.shards.get(i), shard(this.tilt[i], 1.0f), 3);
                    }
                    world.playSound(base, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 0.5f);
                }
                if (age == 10) {
                    for (int i = 0; i < this.shards.size(); i++) {
                        Fx.morph(this.shards.get(i), shard(this.tilt[i], 0.0f), 5);
                    }
                }
                // Twin spiral coming down onto them.
                if (age <= 5 && !gone(target)) {
                    Location at = target.getLocation();
                    double topY = 4.0 * (1.0 - age / 6.0), lowY = 4.0 * (1.0 - (age + 1) / 6.0);
                    for (double y = topY; y >= lowY; y -= 0.12) {
                        double a = y * 3.2, r = 0.25 + 0.15 * y;
                        for (int s = 0; s < 2; s++) {
                            double aa = a + s * Math.PI;
                            Fx.dot(world, at.getX() + Math.cos(aa) * r, at.getY() + y, at.getZ() + Math.sin(aa) * r,
                                Particle.DUST, s == 0 ? black : purple);
                        }
                    }
                }
                // Implosion.
                if (age == 7) {
                    Location c = gone(target) ? base.clone().add(0, 1, 0) : chest(target);
                    Fx.p(c, Particle.REVERSE_PORTAL, 40, 0.6, 0.12);
                    Fx.ring(c, new Vector(0, 1, 0), 0.9, 18, 0, l -> world.spawnParticle(Particle.SQUID_INK,
                        l, 0, l.getX() - c.getX(), 0.05, l.getZ() - c.getZ(), 0.25));
                    Fx.p(base, Particle.DUST, 20, 0.7, 0, purple);
                }
                return age < 16;
            }

            private void spawnShards() {
                for (int i = 0; i < 6; i++) {
                    double a = Math.PI * 2 * i / 6 + 0.5;
                    Location at = base.clone().add(Math.cos(a) * 1.35, -0.05, Math.sin(a) * 1.35);
                    at.setYaw((float) Math.toDegrees(-a) + 90f);
                    at.setPitch(0f);
                    this.tilt[i] = 0.25f + R.nextFloat() * 0.25f;
                    BlockData data = (i % 2 == 0 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN).createBlockData();
                    BlockDisplay d = Fx.block(at, data, shard(this.tilt[i], 0.0f));
                    if (d == null) continue;
                    Fx.glow(d);
                    this.shards.add(d);
                }
            }

            @Override
            public void end() {
                this.shards.forEach(Fx::kill);
            }
        });
    }

    /** A thin spike leaning outward; {@code height} 0 is flat in the ground, 1 is full size. */
    private static org.bukkit.util.Transformation shard(float tilt, float height) {
        Quaternionf rot = new Quaternionf().rotateX(-tilt);
        float h = Math.max(0.01f, height * 1.25f);
        return Fx.tf(-0.11f, -0.05f, -0.11f, rot, 0.22f, h, 0.22f);
    }

    // ── Nullblade ───────────────────────────────────────────────────────────

    /** Combo pips: one grey mark circling them per hit landed so far. */
    public static void nullMark(LivingEntity target, int hits) {
        if (!Fx.on() || hits <= 0) return;
        Location c = chest(target).add(0, 0.5, 0);
        Fx.flatRing(c, 0.55, hits, R.nextDouble() * Math.PI, at -> {
            Fx.p(at, Particle.DUST, 3, 0.03, 0, Fx.dust(NULL_HI, 1.3f));
            Fx.dot(at.getWorld(), at.getX(), at.getY(), at.getZ(), Particle.ENCHANTED_HIT, null);
        });
    }

    /**
     * The seal: a pale flash, two chain rings that lock around them and keep orbiting for the
     * whole lock, a "nulled" tag over their head, and a glass shatter when it wears off.
     */
    public static void nullLock(Player target, int ticks) {
        if (!Fx.on()) return;
        World world = target.getWorld();
        Location c0 = chest(target);
        Fx.flash(c0, NULL_HI);
        Fx.p(c0, Particle.WHITE_ASH, 30, 0.5, 0.05);
        Fx.ring(c0, new Vector(0, 1, 0), 1.3, 26, 0, Fx.dustAt(Fx.dust(NULL_HI, 1.4f)));
        Particle.DustOptions hi = Fx.dust(NULL_HI, 1.0f);
        Particle.DustOptions lo = Fx.dust(NULL_LO, 1.0f);
        Fx.play(new Fx.Anim() {
            TextDisplay tag;

            @Override
            public boolean tick(int age) {
                if (gone(target)) return false;
                Location c = chest(target);
                if (age == 0 && Fx.displaysOn()) {
                    this.tag = Fx.text(head(target), Msg.parse("<bold><gradient:#E3E8EF:#6B7280>ɴᴜʟʟᴇᴅ</gradient></bold>"),
                        Fx.tf(0, 0, 0, 0.1f));
                }
                if (age == 1) Fx.morph(this.tag, Fx.tf(0, 0, 0, 1.15f), 3);
                if (age == ticks - 4) Fx.morph(this.tag, Fx.tf(0, 0.3f, 0, 0.0f), 4);
                if (this.tag != null && this.tag.isValid()) this.tag.teleport(head(target));
                // Two tilted chain rings, drawn a few links per tick so they read as spinning.
                double spin = age * 0.42;
                Vector n1 = new Vector(Math.sin(spin * 0.5) * 0.6, 1, Math.cos(spin * 0.5) * 0.6);
                Vector n2 = new Vector(-Math.cos(spin * 0.5) * 0.6, 1, Math.sin(spin * 0.5) * 0.6);
                Fx.ring(c, n1, 0.8, 5, spin, Fx.dustAt(hi));
                Fx.ring(c, n2, 0.8, 5, -spin, Fx.dustAt(lo));
                if (age % 5 == 0) Fx.flatRing(target.getLocation().add(0, 0.05, 0), 0.7, 14, spin, Fx.dustAt(lo));
                return age < ticks;
            }

            @Override
            public void end() {
                Fx.kill(this.tag);
                if (gone(target)) return;
                Location c = chest(target);
                Fx.p(c, Particle.BLOCK, 24, 0.35, 0.1, Material.LIGHT_GRAY_STAINED_GLASS.createBlockData());
                Fx.p(c, Particle.WHITE_ASH, 12, 0.4, 0.03);
                world.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.4f);
            }
        });
    }

    // ── Overdrive ───────────────────────────────────────────────────────────

    /** Each stack: a cyan shock ring off your feet. The last stack adds a full-body ring and a crackle. */
    public static void overdriveStack(Player player, int stacks, int max) {
        if (!Fx.on()) return;
        Location feet = player.getLocation().add(0, 0.08, 0);
        Fx.p(player.getLocation().add(0, 0.5, 0), Particle.ELECTRIC_SPARK, 4 + stacks * 3, 0.3, 0.15);
        Fx.play(new Fx.Anim() {
            @Override
            public boolean tick(int age) {
                double r = 0.3 + age * 0.28;
                Fx.flatRing(feet, r, 14 + stacks * 2, age * 0.5, Fx.fadeAt(Fx.fade(CYAN, AZURE, 0.9f)));
                return age < 4;
            }
        });
        if (stacks >= max) {
            Location c = chest(player);
            Vector side = flat(player.getLocation().getDirection());
            Fx.ring(c, side, 1.1, 30, 0, Fx.fadeAt(Fx.fade(CYAN, AZURE, 1.2f)));
            Fx.ring(c, new Vector(0, 1, 0), 1.1, 30, 0, Fx.dustAt(Fx.dust(CYAN, 1.0f)));
            Fx.p(c, Particle.END_ROD, 14, 0.2, 0.18);
            Fx.flash(c, CYAN);
            player.getWorld().playSound(c, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.9f);
        }
    }

    /** While boosted: an electric wake behind your feet, speed lines at max. Called every 2 ticks. */
    public static void overdriveTrail(Player player, int stacks, int max) {
        if (!Fx.on()) return;
        Location feet = player.getLocation().add(0, 0.15, 0);
        Fx.p(feet, Particle.DUST_COLOR_TRANSITION, 1 + stacks, 0.18, 0, Fx.fade(CYAN, AZURE, 0.8f));
        if (stacks >= 3) Fx.p(feet, Particle.ELECTRIC_SPARK, stacks - 2, 0.2, 0.05);
        Vector v = player.getVelocity().setY(0);
        if (stacks >= max && v.lengthSquared() > 0.003) {
            Vector back = v.normalize().multiply(-0.9);
            Vector right = new Vector(-back.getZ(), 0, back.getX()).normalize().multiply(0.35);
            for (int s = -1; s <= 1; s += 2) {
                Location from = player.getLocation().add(0, 1.1, 0).add(right.clone().multiply(s));
                Fx.line(from, from.clone().add(back), 0.18, Fx.dustAt(Fx.dust(CYAN, 0.6f)));
            }
        }
    }

    /** The streak ended: the charge fizzles out. */
    public static void overdriveFizzle(Player player) {
        if (!Fx.on()) return;
        Location c = player.getLocation().add(0, 0.8, 0);
        Fx.p(c, Particle.DUST_COLOR_TRANSITION, 10, 0.35, 0, Fx.fade(CYAN, 0x3A3A3A, 1.0f));
        Fx.p(c, Particle.SMOKE, 6, 0.25, 0.02);
        player.playSound(c, Sound.BLOCK_FIRE_EXTINGUISH, 0.25f, 1.8f);
    }

    // ── Dreadcleaver ────────────────────────────────────────────────────────

    /** The hit before the heavy one: the axe head smoulders. */
    public static void dreadCharge(Player attacker) {
        if (!Fx.on()) return;
        Location eye = attacker.getEyeLocation();
        Vector fwd = eye.getDirection().normalize();
        Vector right = new Vector(-fwd.getZ(), 0, fwd.getX()).normalize();
        Location hand = eye.clone().add(fwd.multiply(0.5)).add(right.multiply(0.45)).add(0, -0.45, 0);
        Fx.p(hand, Particle.DUST_COLOR_TRANSITION, 8, 0.12, 0, Fx.fade(DREAD_HI, DREAD_LO, 1.0f));
        Fx.p(hand, Particle.SMALL_FLAME, 3, 0.08, 0.01);
        attacker.playSound(hand, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.4f, 0.6f);
    }

    /**
     * A blood-red cleave arc, a shockwave rolling out over the ground, cracks, and chunks of the
     * floor thrown up around them that crumble back down.
     */
    public static void dreadSlam(LivingEntity victim, Player attacker) {
        if (!Fx.on()) return;
        World world = victim.getWorld();
        Location base = victim.getLocation().add(0, 0.1, 0);
        Block below = victim.getLocation().add(0, -0.2, 0).getBlock();
        BlockData ground = below.getType().isSolid() ? below.getBlockData() : Material.DEEPSLATE.createBlockData();
        Vector fwd = flat(victim.getLocation().toVector().subtract(attacker.getLocation().toVector()));

        Fx.flash(chest(victim), DREAD_HI);
        Fx.p(base.clone().add(0, 0.3, 0), Particle.BLOCK, 40, 0.7, 0.2, ground);
        // Cracks: seven jagged lines out from their feet.
        Particle.DustOptions crack = Fx.dust(DREAD_LO, 1.0f);
        for (int i = 0; i < 7; i++) {
            double a = Math.PI * 2 * i / 7 + R.nextDouble() * 0.4;
            Location p0 = base.clone();
            for (int s = 1; s <= 3; s++) {
                double r = s * 0.75;
                double aa = a + (R.nextDouble() - 0.5) * 0.35;
                Location p1 = base.clone().add(Math.cos(aa) * r, 0, Math.sin(aa) * r);
                Fx.line(p0, p1, 0.16, Fx.dustAt(crack));
                p0 = p1;
            }
        }

        Fx.play(new Fx.Anim() {
            final List<BlockDisplay> chunks = new ArrayList<>();
            final List<Quaternionf> spins = new ArrayList<>();
            final Display.Brightness light = new Display.Brightness(
                base.getBlock().getLightFromBlocks(), base.getBlock().getLightFromSky());

            @Override
            public boolean tick(int age) {
                // The cleave: a wide arc over their head, drawn top to bottom in three frames.
                if (age <= 2 && !gone(victim)) {
                    Location c = chest(victim).subtract(fwd.clone().multiply(0.4));
                    Vector up = new Vector(0, 1, 0);
                    double from = Math.toRadians(110 - age * 50), to = Math.toRadians(110 - (age + 1) * 50);
                    for (double a = from; a >= to; a -= 0.09) {
                        Location p = c.clone().add(up.clone().multiply(Math.sin(a) * 1.4)).add(fwd.clone().multiply(Math.cos(a) * 1.4));
                        Fx.dot(world, p.getX(), p.getY(), p.getZ(), Particle.DUST_COLOR_TRANSITION, Fx.fade(DREAD_HI, DREAD_LO, 1.6f));
                    }
                }
                // Shockwave.
                if (age <= 7) {
                    double r = 0.5 + age * 0.5;
                    Fx.flatRing(base, r, 26 + age * 3, age * 0.2, Fx.fadeAt(Fx.fade(DREAD_HI, DREAD_LO, 1.5f)));
                    if (age % 2 == 0) Fx.flatRing(base, r, 10, 0, l -> world.spawnParticle(Particle.BLOCK, l, 1, 0.05, 0.05, 0.05, 0, ground));
                }
                // Thrown-up floor chunks.
                if (age == 1) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI * 2 * i / 8 + R.nextDouble() * 0.3;
                        Location at = base.clone().add(Math.cos(a) * 1.35, -0.35, Math.sin(a) * 1.35);
                        float s = 0.35f + R.nextFloat() * 0.2f;
                        BlockDisplay d = Fx.block(at, ground, Fx.tf(-s / 2, 0, -s / 2, new Quaternionf(), s, s, s));
                        if (d == null) continue;
                        // Lit like the air above the floor, not the inside of it (which is pitch black).
                        d.setBrightness(this.light);
                        this.chunks.add(d);
                        this.spins.add(new Quaternionf().rotateXYZ(R.nextFloat() * 1.6f, R.nextFloat() * 1.6f, R.nextFloat() * 1.6f));
                    }
                }
                if (age == 2) {
                    for (int i = 0; i < this.chunks.size(); i++) {
                        BlockDisplay d = this.chunks.get(i);
                        float s = d.getTransformation().getScale().x();
                        Fx.morph(d, Fx.tf(-s / 2, 0.9f + R.nextFloat() * 0.5f, -s / 2, this.spins.get(i), s, s, s), 5);
                    }
                }
                if (age == 8) {
                    for (int i = 0; i < this.chunks.size(); i++) {
                        Fx.morph(this.chunks.get(i), Fx.tf(0, -0.4f, 0, this.spins.get(i).rotateY(1.2f), 0.02f, 0.02f, 0.02f), 7);
                    }
                }
                if (age == 14) {
                    for (BlockDisplay d : this.chunks) Fx.p(d.getLocation().add(0, 0.3, 0), Particle.BLOCK, 4, 0.15, 0, ground);
                }
                return age < 16;
            }

            @Override
            public void end() {
                this.chunks.forEach(Fx::kill);
            }
        });
    }

    // ── Lifesteal Axe ───────────────────────────────────────────────────────

    /** A crit that didn't steal yet: blood drips. */
    public static void leechDrip(LivingEntity victim) {
        if (!Fx.on()) return;
        Location c = chest(victim);
        Fx.p(c, Particle.FALLING_DUST, 6, 0.25, 0, Material.REDSTONE_BLOCK.createBlockData());
        Fx.p(c, Particle.DUST, 5, 0.2, 0, Fx.dust(BLOOD_DARK, 1.0f));
    }

    /**
     * Blood bursts out of them and streams through the air into you in an arc, then pulses
     * around you as hearts.
     */
    public static void leechSteal(LivingEntity victim, Player attacker) {
        if (!Fx.on()) return;
        World world = victim.getWorld();
        Location v0 = chest(victim);
        Fx.p(v0, Particle.DAMAGE_INDICATOR, 6, 0.3, 0.1);
        Fx.p(v0, Particle.FALLING_DUST, 10, 0.35, 0, Material.REDSTONE_BLOCK.createBlockData());
        Vector toward = chest(attacker).toVector().subtract(v0.toVector());
        Fx.ring(v0, toward, 0.75, 22, 0, Fx.fadeAt(Fx.fade(BLOOD_HI, BLOOD, 1.3f)));
        world.playSound(v0, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, 0.8f, 0.6f);

        Fx.play(new Fx.Anim() {
            @Override
            public boolean tick(int age) {
                if (gone(attacker)) return false;
                Location a = chest(attacker);
                Location v = gone(victim) ? v0 : chest(victim);
                if (age <= 7) {
                    // Streams: trail particles that fly into you, plus one fat orb riding an arc.
                    Fx.trail(v, a, age % 2 == 0 ? BLOOD : BLOOD_HI, 8, 3, 0.3);
                    double s = age / 8.0;
                    Location mid = v.clone().add(a).multiply(0.5).add(0, 1.3, 0);
                    Location orb = bezier(v, mid, a, s);
                    Fx.p(orb, Particle.DUST, 4, 0.06, 0, Fx.dust(BLOOD, 1.8f));
                    Fx.p(orb, Particle.DUST, 2, 0.12, 0, Fx.dust(BLOOD_HI, 0.8f));
                }
                if (age == 8) {
                    Fx.p(a.clone().add(0, 0.5, 0), Particle.HEART, 4, 0.35, 0);
                    Fx.ring(a, new Vector(0, 1, 0), 0.9, 20, 0, Fx.fadeAt(Fx.fade(BLOOD_HI, BLOOD, 1.2f)));
                    world.playSound(a, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 1.6f);
                }
                if (age >= 8 && age <= 14) {
                    // Rising pulse round you.
                    double ang = age * 0.9;
                    double y = (age - 8) * 0.25;
                    Location feet = attacker.getLocation();
                    for (int k = 0; k < 2; k++) {
                        double aa = ang + k * Math.PI;
                        Fx.dot(world, feet.getX() + Math.cos(aa) * 0.6, feet.getY() + y, feet.getZ() + Math.sin(aa) * 0.6,
                            Particle.DUST, Fx.dust(BLOOD, 1.1f));
                    }
                }
                return age < 15;
            }
        });
    }

    private static Location bezier(Location a, Location b, Location c, double t) {
        double u = 1 - t;
        return new Location(a.getWorld(),
            u * u * a.getX() + 2 * u * t * b.getX() + t * t * c.getX(),
            u * u * a.getY() + 2 * u * t * b.getY() + t * t * c.getY(),
            u * u * a.getZ() + 2 * u * t * b.getZ() + t * t * c.getZ());
    }

    // ── Cob Axe ─────────────────────────────────────────────────────────────

    /** A punched web: an X of white slashes over it and the web bursting into threads. */
    public static void cobSnip(Block web, Player player) {
        if (!Fx.on()) return;
        Location c = web.getLocation().add(0.5, 0.5, 0.5);
        Vector toPlayer = player.getEyeLocation().toVector().subtract(c.toVector());
        Vector[] uv = Fx.basis(toPlayer);
        Location face = c.clone().add(toPlayer.normalize().multiply(0.45));
        Particle.DustOptions silk = Fx.dust(SILK, 0.7f);
        for (int s = -1; s <= 1; s += 2) {
            Vector diag = uv[0].clone().add(uv[1].clone().multiply(s)).multiply(0.45);
            Fx.line(face.clone().subtract(diag), face.clone().add(diag), 0.07, Fx.dustAt(silk));
        }
        Fx.p(c, Particle.BLOCK, 16, 0.3, 0, Material.COBWEB.createBlockData());
        Fx.p(c, Particle.WHITE_ASH, 10, 0.35, 0.02);
        Fx.dot(c.getWorld(), face.getX(), face.getY(), face.getZ(), Particle.SWEEP_ATTACK, null);
    }

    /**
     * Shift clear: a spinning white sweep rolls out from you, a thread shoots to every web it
     * reaches and snaps, and the webs burst into drifting silk.
     */
    public static void cobSweep(Player player, double radius, List<Location> webs) {
        if (!Fx.on()) return;
        World world = player.getWorld();
        Location centre = player.getLocation().add(0, 1.0, 0);
        List<Location> some = webs.size() > 16 ? webs.subList(0, 16) : webs;
        Fx.play(new Fx.Anim() {
            @Override
            public boolean tick(int age) {
                if (age <= 4) {
                    double r = 1.0 + (radius - 1.0) * age / 4.0;
                    if (age % 2 == 0) Fx.flatRing(centre.clone().add(0, -0.2, 0), r, 5, age * 0.7, Fx.at(Particle.SWEEP_ATTACK));
                    Fx.flatRing(centre, r, 18, -age * 0.4, Fx.dustAt(Fx.dust(SILK, 0.8f)));
                }
                if (age == 1) {
                    for (Location w : some) {
                        Location c = w.clone().add(0.5, 0.5, 0.5);
                        Fx.line(centre, c, 0.28, Fx.dustAt(Fx.dust(SILK, 0.5f)));
                    }
                }
                if (age == 3) {
                    for (Location w : some) {
                        Location c = w.clone().add(0.5, 0.5, 0.5);
                        Fx.p(c, Particle.BLOCK, 6, 0.3, 0, Material.COBWEB.createBlockData());
                        Fx.p(c, Particle.CLOUD, 2, 0.2, 0.02);
                    }
                    world.playSound(centre, Sound.BLOCK_WOOL_BREAK, 1.0f, 1.3f);
                }
                if (age >= 3 && age <= 12 && age % 3 == 0) {
                    for (Location w : some) Fx.p(w.clone().add(0.5, 0.4, 0.5), Particle.WHITE_ASH, 3, 0.4, 0.01);
                }
                return age < 12;
            }
        });
    }

    /** A web growing back: a soft white shimmer. */
    public static void cobRegrow(Location web) {
        if (!Fx.on()) return;
        Location c = web.clone().add(0.5, 0.5, 0.5);
        Fx.p(c, Particle.END_ROD, 3, 0.3, 0.005);
        Fx.p(c, Particle.WHITE_ASH, 6, 0.35, 0.01);
    }

    // ── Voidchopper ─────────────────────────────────────────────────────────

    /**
     * A void mouth opens in their chest and swallows light; a golden apple hangs over their head,
     * spinning behind a forbidden sign, for exactly as long as gaps are locked.
     */
    public static void voidLock(Player target, Player attacker, int ticks) {
        if (!Fx.on()) return;
        World world = target.getWorld();
        Location c0 = chest(target);
        Vector facing = attacker.getEyeLocation().toVector().subtract(c0.toVector());
        int[] shades = {VOID_HI, 0x4A63D6, VOID_LO, VOID_BLACK};
        for (int i = 0; i < shades.length; i++) {
            Fx.ring(c0, facing, 0.95 - i * 0.22, 26 - i * 5, i * 0.4, Fx.dustAt(Fx.dust(shades[i], 1.3f)));
        }
        Fx.p(c0, Particle.PORTAL, 50, 0.1, 1.2);
        Fx.p(c0, Particle.REVERSE_PORTAL, 20, 0.6, 0.05);
        Fx.p(c0, Particle.SQUID_INK, 6, 0.2, 0.05);

        Fx.play(new Fx.Anim() {
            ItemDisplay apple;
            ItemDisplay ban;
            float spin = 0f;

            @Override
            public boolean tick(int age) {
                if (gone(target)) return false;
                Location h = head(target).add(0, 0.25, 0);
                if (age == 0 && Fx.displaysOn()) {
                    this.apple = Fx.item(h, new ItemStack(Material.GOLDEN_APPLE), Fx.tf(0, 0, 0, 0.01f));
                    this.ban = Fx.item(h, new ItemStack(Material.BARRIER), Fx.tf(0, 0, 0, 0.01f));
                    if (this.apple != null) {
                        this.apple.setBillboard(Display.Billboard.FIXED);
                        Fx.glow(this.apple);
                    }
                    if (this.ban != null) {
                        this.ban.setBillboard(Display.Billboard.CENTER);
                        Fx.glow(this.ban);
                    }
                }
                if (age == 1) {
                    Fx.morph(this.apple, Fx.tf(0, 0, 0, 0.6f), 4);
                    Fx.morph(this.ban, Fx.tf(0, 0, 0.02f, 0.55f), 4);
                }
                if (age >= 5 && age % 5 == 0 && age < ticks - 5) {
                    this.spin += (float) (Math.PI / 2);
                    Fx.morph(this.apple, Fx.tf(0, 0, 0, new Quaternionf().rotateY(this.spin), 0.6f, 0.6f, 0.6f), 5);
                }
                if (age == ticks - 4) {
                    Fx.morph(this.apple, Fx.tf(0, 0, 0, 0.0f), 4);
                    Fx.morph(this.ban, Fx.tf(0, 0, 0, 0.0f), 4);
                }
                if (this.apple != null && this.apple.isValid()) this.apple.teleport(h);
                if (this.ban != null && this.ban.isValid()) this.ban.teleport(h);
                // Two dark motes orbiting the apple, void dripping off it.
                double a = age * 0.35;
                for (int k = 0; k < 2; k++) {
                    double aa = a + k * Math.PI;
                    Fx.dot(world, h.getX() + Math.cos(aa) * 0.5, h.getY() + Math.sin(a * 0.7) * 0.12, h.getZ() + Math.sin(aa) * 0.5,
                        Particle.DUST, Fx.dust(k == 0 ? VOID_HI : VOID_LO, 0.9f));
                }
                if (age % 3 == 0) Fx.p(h.clone().add(0, -0.3, 0), Particle.REVERSE_PORTAL, 1, 0.15, 0.01);
                return age < ticks;
            }

            @Override
            public void end() {
                Fx.kill(this.apple);
                Fx.kill(this.ban);
                if (!gone(target)) {
                    Location h = head(target).add(0, 0.25, 0);
                    Fx.p(h, Particle.REVERSE_PORTAL, 14, 0.25, 0.05);
                    world.playSound(h, Sound.BLOCK_BEACON_DEACTIVATE, 0.4f, 1.8f);
                }
            }
        });
    }

    // ── Splitshot ───────────────────────────────────────────────────────────

    /** A pink streak behind an arrow from this bow, until it lands. */
    public static void splitTrail(AbstractArrow arrow, boolean bright) {
        if (!Fx.on()) return;
        Fx.play(new Fx.Anim() {
            @Override
            public boolean tick(int age) {
                // The bow fires the event before the arrow is in the world, so tick 0 can't check it.
                if (age > 0 && !arrow.isValid()) return false;
                if (arrow.isInBlock() || arrow.isOnGround()) {
                    Fx.p(arrow.getLocation(), Particle.DUST, 8, 0.15, 0, Fx.dust(PINK, 1.0f));
                    return false;
                }
                Location at = arrow.getLocation();
                ribbon(arrow, bright ? 0.2 : 0.3, Fx.fadeAt(Fx.fade(PINK_HI, PINK, bright ? 1.3f : 1.0f)));
                if (bright && age % 2 == 0) Fx.p(at, Particle.END_ROD, 1, 0.02, 0.0);
                return age < 80;
            }
        });
    }

    /** The split: a pink starburst, a ring around the arrow's path, and three rays fanning out. */
    public static void splitFlash(Location at, Vector velocity, List<Vector> dirs) {
        if (!Fx.on()) return;
        Fx.flash(at, PINK_HI);
        Fx.ring(at, velocity, 0.7, 24, 0, Fx.fadeAt(Fx.fade(PINK_HI, PINK, 1.3f)));
        Fx.ring(at, velocity, 0.35, 12, 0.25, Fx.dustAt(Fx.dust(0xFFFFFF, 0.8f)));
        Fx.p(at, Particle.FIREWORK, 12, 0.05, 0.12);
        for (Vector d : dirs) {
            Location end = at.clone().add(d.clone().normalize().multiply(1.8));
            Fx.line(at, end, 0.1, Fx.fadeAt(Fx.fade(PINK_HI, PINK, 1.0f)));
        }
        at.getWorld().playSound(at, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.8f);
    }

    // ── Flaming Bow ─────────────────────────────────────────────────────────

    /** Fire and smoke shed off the arrow; it bursts when it lands. */
    public static void flameTrail(AbstractArrow arrow) {
        if (!Fx.on()) return;
        Fx.play(new Fx.Anim() {
            @Override
            public boolean tick(int age) {
                // The bow fires the event before the arrow is in the world, so tick 0 can't check it.
                if (age > 0 && !arrow.isValid()) return false;
                Location at = arrow.getLocation();
                if (arrow.isInBlock() || arrow.isOnGround()) {
                    Fx.p(at, Particle.FLAME, 10, 0.15, 0.04);
                    Fx.p(at, Particle.LAVA, 2, 0.1, 0);
                    return false;
                }
                ribbon(arrow, 0.3, l -> l.getWorld().spawnParticle(Particle.FLAME, l, 1, 0.03, 0.03, 0.03, 0.005));
                ribbon(arrow, 0.6, Fx.fadeAt(Fx.fade(EMBER, FIRE, 1.1f)));
                if (age % 2 == 0) Fx.p(at, Particle.SMOKE, 1, 0.05, 0.01);
                if (age % 5 == 0) Fx.p(at, Particle.DUST, 2, 0.06, 0, Fx.dust(EMBER, 1.2f));
                return age < 80;
            }
        });
    }

    /**
     * The hit: a fire ring blasts out at their feet and a three-strand pillar of flame wraps
     * up their body. Against fire resistance it burns soul-blue, so everyone sees it went through.
     */
    public static void flameIgnite(LivingEntity victim, boolean hellfire) {
        if (!Fx.on()) return;
        World world = victim.getWorld();
        Particle flame = hellfire ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
        int hot = hellfire ? SOUL : FIRE;
        Location c = chest(victim);
        Fx.flash(c, hellfire ? SOUL : EMBER);
        Fx.p(c, Particle.LAVA, 6, 0.3, 0);
        if (hellfire) world.playSound(c, Sound.PARTICLE_SOUL_ESCAPE, 1.2f, 0.8f);
        Fx.play(new Fx.Anim() {
            @Override
            public boolean tick(int age) {
                if (gone(victim)) return false;
                Location feet = victim.getLocation().add(0, 0.1, 0);
                if (age <= 4) {
                    double r = 0.4 + age * 0.3;
                    Fx.flatRing(feet, r, 18 + age * 2, age * 0.3, l -> world.spawnParticle(flame, l, 1, 0, 0.02, 0, 0.01));
                    Fx.flatRing(feet, r + 0.1, 12, 0, Fx.fadeAt(Fx.fade(EMBER, hot, 1.2f)));
                }
                if (age <= 8) {
                    double y0 = age * 0.3, y1 = (age + 1) * 0.3;
                    for (double y = y0; y < y1; y += 0.1) {
                        for (int s = 0; s < 3; s++) {
                            double a = y * 4.0 + s * Math.PI * 2 / 3;
                            double r = 0.6 - y * 0.08;
                            world.spawnParticle(flame, feet.getX() + Math.cos(a) * r, feet.getY() + y,
                                feet.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0.0);
                        }
                    }
                }
                return age < 9;
            }
        });
    }

    /** While they burn: flames curling round them, embers lifting off. Stops when {@code burning} says so. */
    public static void flameBurn(LivingEntity victim, boolean hellfire, BooleanSupplier burning) {
        if (!Fx.on()) return;
        World world = victim.getWorld();
        Particle flame = hellfire ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
        Fx.play(new Fx.Anim() {
            @Override
            public boolean tick(int age) {
                if (gone(victim) || !burning.getAsBoolean()) return false;
                Location feet = victim.getLocation();
                double a = age * 0.55;
                double y = 0.2 + (age % 12) * 0.14;
                for (int s = 0; s < 2; s++) {
                    double aa = a + s * Math.PI;
                    world.spawnParticle(flame, feet.getX() + Math.cos(aa) * 0.5, feet.getY() + y,
                        feet.getZ() + Math.sin(aa) * 0.5, 1, 0, 0, 0, 0.0);
                }
                if (age % 6 == 0) Fx.p(feet.clone().add(0, 1.0, 0), Particle.DUST, 2, 0.3, 0, Fx.dust(hellfire ? SOUL : EMBER, 0.9f));
                if (age % 20 == 10) Fx.p(feet.clone().add(0, 0.8, 0), Particle.LAVA, 1, 0.2, 0);
                return age < 20 * 30;
            }

            @Override
            public void end() {
                if (gone(victim)) return;
                Location c = chest(victim);
                Fx.p(c, Particle.SMOKE, 12, 0.3, 0.03);
                Fx.p(c, Particle.WHITE_SMOKE, 5, 0.25, 0.02);
                world.playSound(c, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.2f);
            }
        });
    }

    // ── Curve Shot ──────────────────────────────────────────────────────────

    /** A homing arrow's tick: a green trail, brighter while locked, and a turning reticle on its target. */
    public static void curveTick(AbstractArrow arrow, LivingEntity target, int age) {
        if (!Fx.on()) return;
        Location at = arrow.getLocation();
        ribbon(arrow, target != null ? 0.18 : 0.3, Fx.fadeAt(Fx.fade(LIME, JADE, target != null ? 1.3f : 0.9f)));
        if (target == null || age % 2 != 0) return;
        // Reticle: four brackets turning around their chest, facing the arrow.
        Location c = chest(target);
        Vector facing = at.toVector().subtract(c.toVector());
        Particle.DustOptions mark = Fx.dust(LIME, 0.9f);
        double phase = age * 0.25;
        for (int b = 0; b < 4; b++) {
            double start = phase + b * Math.PI / 2;
            Fx.ring(c, facing, 0.85, 1, start, Fx.dustAt(mark));
            Fx.ring(c, facing, 0.85, 1, start + 0.22, Fx.dustAt(mark));
            Fx.ring(c, facing, 0.85, 1, start + 0.44, Fx.dustAt(mark));
        }
    }

    /** The moment it locks: a ping ring on them, and a lock-on beep for the shooter. */
    public static void curveLock(LivingEntity target, Player shooter) {
        if (!Fx.on()) return;
        Location c = chest(target);
        Fx.ring(c, new Vector(0, 1, 0), 1.1, 24, 0, Fx.fadeAt(Fx.fade(LIME, JADE, 1.1f)));
        if (shooter != null && shooter.isOnline()) shooter.playSound(shooter.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.7f, 1.9f);
    }

    /** It hit: a green burst and a quick double ring. */
    public static void curveHit(LivingEntity victim) {
        if (!Fx.on()) return;
        Location c = chest(victim);
        Fx.p(c, Particle.HAPPY_VILLAGER, 8, 0.35, 0);
        Fx.ring(c, new Vector(0, 1, 0), 0.6, 16, 0, Fx.dustAt(Fx.dust(LIME, 1.2f)));
        Fx.ring(c, new Vector(0, 1, 0), 1.0, 22, 0.2, Fx.dustAt(Fx.dust(JADE, 1.0f)));
        victim.getWorld().playSound(c, Sound.ENTITY_ARROW_HIT_PLAYER, 0.6f, 1.5f);
    }
}
