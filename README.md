# ZeroWeapons

Custom swords, axes and bows with abilities for Paper 1.21.11.

## Weapons

| Weapon | Type | Ability |
| --- | --- | --- |
| Dash Sword | sword | shift + right click: dash where you're looking |
| Vortex Sword | sword | 15% of hits: a black and purple spiral strike, +1 heart (through armour) |
| Nullblade | sword | 3 hits in a row: their custom weapon abilities stop for 2s |
| Overdrive | sword | each hit in a row: +4% speed (max 5), resets on a miss or new target |
| Dreadcleaver | axe | every 3rd hit: big knockback + slowness II for 1.5s |
| Lifesteal Axe | axe | every 2nd crit: steal 1.5 hearts |
| Cob Axe | axe | hit a cobweb to pop it, shift + right click clears webs in 4 blocks |
| Voidchopper | axe | on hit: no golden apples for 3s |
| Splitshot | bow | full draw: arrow splits into 3 mid-flight, each hits harder |
| Flaming Bow | bow | sets them on fire, extra burn goes through fire resistance |
| Curve Shot | bow | arrows hit harder and curve to the closest player |

Every number (chance, damage, cooldown, range...) is in `weapons.yml`.

## Visuals

Every ability has its own effect, built from particles and display entities (no resource pack):

- **Dash Sword** - a violet tear and wind burst, a thick streak with afterimage outlines, a landing ring
- **Vortex Sword** - a jagged purple bolt from the sky, a black ground sigil, crying-obsidian spikes, a twin spiral, an implosion
- **Nullblade** - grey combo pips, then a seal: chain rings orbiting the target and a NULLED tag over their head, shattering like glass when it ends
- **Overdrive** - a cyan shock ring per stack, an electric wake while boosted, speed lines at max, a fizzle when it resets
- **Dreadcleaver** - the axe smoulders before the heavy hit, then a red cleave arc, a shockwave, cracks and chunks of the floor thrown up
- **Lifesteal Axe** - blood drips on crits, then blood streams through the air into you and pulses as hearts
- **Cob Axe** - punched webs get an X slash; the clear sends a sweep out, threads to every web, and they burst into silk
- **Voidchopper** - a void mouth in their chest, and a golden apple behind a barrier sign hanging over their head for the whole lock
- **Splitshot** - a pink ribbon trail, a starburst where it splits into three
- **Flaming Bow** - a fire ribbon, a fire ring and flame pillar on hit, flames curling round them while they burn (soul-blue when it beats fire resistance)
- **Curve Shot** - a green ribbon, a lock-on ping and a turning reticle on the target, a burst on hit

All animations run from one shared task that stops when idle, are capped, and clean up their
display entities on shutdown. `visuals` in config.yml can turn them off, halve the particles,
or skip display entities.

## Commands

- `/weapons give <weapon> [player]`
- `/weapons list`
- `/weapons reload`

Permission: `zeroweapons.admin` (op by default).

## Config

`config.yml`:

- `disabled-worlds` - worlds where the weapons do nothing (e.g. a lobby)
- `safe-region` - a WorldGuard region name that counts the same (optional, needs WorldGuard)

## Build

```
mvn clean package
```


