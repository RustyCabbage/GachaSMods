# Gacha S Mods
 
Fixes all design issues with s-modding, 100% guaranteed. Ü

## Okay fine, a more concrete explanation:
- Creates two dummy 0 cost hullmods:
  1. {Random S-Mod}: When built into the ship, adds a random hullmod as an s-mod. No effect otherwise.
  2. {Remove S-Mod}: When built into the ship, removes a random number of s-mods (up to 3). Story points are not refunded. No effect otherwise.
- Blocks building in all other hullmods by default. So if you want s-mods, they have to be randomized.

### Other gameplay changes
- S-modding will grant a random amount of bonus XP so you can't predict what hullmod you're going to get from that.
- I added a hullmod category so you can find the hullmod easier (it defaults to sitting at the bottom). it's kinda ugly but what can you do.
- Expect a mild amount of jank as required to workaround various UI limitations. I did my best, I swear.

## Options (found in settings.json)
- True Random Mode: will randomly choose (almost) any non-hiddenEverywhere hullmod for s-modding. (Maybe you get Targeting Supercomputer, maybe you get Axial Rotation, thems the works.)
- No Save Scumming: ships will roll the same s-mods and bonus xp every time.
- Only Known Hullmods: will only roll hullmods known by the player (off by default because then why would you learn Shield Shunt/High Scatter Amp
- Only Not Hidden Hullmods: will only roll hullmods that can be learned by the player (the usual modular ones)
- Only Applicable Hullmods: will only roll hullmods that are normally applicable on the ship (so no Safety Overrides for capital ships)
