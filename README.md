# Gacha S Mods
 
Fixes all design issues with s-modding, 100% guaranteed. Ü

## Core Features
- Creates two dummy 0 cost hullmods:
  1. {Random S-Mod}: When built into the ship, adds a random hullmod as an s-mod. No effect otherwise.
  2. {Remove S-Mod}: When built into the ship, removes a random number of s-mods (default up to 3; configurable). Story points are not refunded. No effect otherwise.
- Blocks building in all other hullmods by default (configurable). So if you want s-mods, they have to be randomized.
- Three possible selection modes:
  1. Classic: Hullmods are selected with uniform probability of selection.
  2. Proportional: Hullmods are selected with weight = 1/OP, with an adjustable increase for free/hidden hullmods.
  3. True Gacha: Hullmods are split into 4 categories (d-mods, cheap mods, standard mods, hidden mods), with each category being allocated an adjustable weight.
- Customization options available in data/settings/json.

## Other gameplay changes
- S-modding will grant a random amount of bonus XP so you can't predict what hullmod you're going to get from that.
- I added a hullmod category so you can find the hullmod easier (it defaults to sitting at the bottom). it's kinda ugly but what can you do.
- Expect a mild amount of jank as required to workaround various UI limitations. I did my best, I swear.

## Options (found in settings.json)
- No Save Scumming: ships will roll the same s-mods and bonus xp every time until you've confirmed your choice.
### Disable/Enable Features
- Disable Random S-Mods: hides the Random S-Mod hullmod and allows regular s-modding.
- Disable Remove S-Mods: as god intended.
- Allow Standard S-Mods: allows regular s-modding but does not affect the two hullmods added by the mod.
### Random S-Mod Options
- Selection Mode: choose between the three modes described above
- True Random Mode: will randomly choose (almost) any non-hiddenEverywhere hullmod for s-modding. (Maybe you get Targeting Supercomputer, maybe you get Axial Rotation, thems the works.)
- Only Known Hullmods: will only roll hullmods known by the player (off by default because then why would you learn Shield Shunt/High Scatter Amp
- Only Not Hidden Hullmods: will only roll hullmods that can be learned by the player (the usual modular ones)
- Only Applicable Hullmods: will only roll hullmods that are normally applicable on the ship (so no Safety Overrides for capital ships)
- Blacklisted Hullmods: Want to not roll specific hullmods? Add their hullmod id here.
### Selection Mode Options
- Adjust the rarity of free and hidden hullmods in Proportional Mode.
- Adjust the weights for the different categories in True Gacha Mode, and what counts as a "cheap" hullmod.
### Remove S-Mod Options
- Min Removed S-Mods: Sets the minimum number of s-mods that can be randomly removed whenever Remove S-Mods is built in (set it to 0, coward)
- Max Removed S-Mods: Sets the maximum number of s-mods that can be randomly removed whenever Remove S-Mods is built in
