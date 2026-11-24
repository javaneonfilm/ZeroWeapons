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


