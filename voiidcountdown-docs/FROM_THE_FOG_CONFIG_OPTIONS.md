# Datapack Configuration Options

This file lists the datapack's configuration options (alphabetically), a short description for each (sourced from the English localization), and any enum/allowed values that appear in the UI strings.

Runtime storage: `storage lunareclipse.watching:config_options` (JSON; field `options`).
Defaults are defined in `data/lunareclipse.watching/function/config/defaults.mcfunction` and the in-game UI is implemented under `data/lunareclipse.watching/function/config/**`.

Note: many `*_chance` and frequency options accept the value `"default"` which uses the datapack's internal probability table. Use the in-game config UI or `/function lunareclipse.watching:config/initialize_defaults` to reset defaults.

## Options (alphabetical)

Below are the canonical option keys and concise descriptions derived from `assets/lunareclipse.watching/lang/en_us.json`.

- advancements
  - Display name: "Advancements"
  - Description: Toggle whether datapack-specific advancements are enabled. Disabling may clear progress.
  - Default: "false"

- biome_boards
  - Display name: "Biome Boards"
  - Description: When enabled, the sign type Herobrine places adapts to surrounding wood/biome.
  - Default: "true"

- burning_base
  - Display name: "Burning Base"
  - Description: When enabled, Herobrine will attempt to burn your base when you leave the area (periodic checks).
  - Default: "false"

- burning_base_chance
  - Display name: "Burning Base Chance"
  - Description: Changes the chance Herobrine succeeds at burning a base.
  - Allowed values: Common / Default / Rare / Scarce
  - Default: "default"

- caution_caption
  - Display name: "Caution Caption"
  - Description: When a shrine is activated Herobrine will slip a spooky subtitle message to you.
  - Default: "true"

- chilled_candles
  - Display name: "Chilled Candles"
  - Description: Herobrine may douse the flames on candles when you're not nearby (periodic checks).
  - Default: "true"

- chilled_candles_chance
  - Display name: "Chilled Candles Chance"
  - Description: Changes the chance Herobrine will douse candles.
  - Allowed values: Common / Default / Rare / Scarce
  - Default: "default"

- creeping_sighting
  - Display name: "Creeping Sightings"
  - Description: Spawns Herobrine standing behind you, waiting for you to turn around.
  - Default: "false"

- creeping_vanishing_delay
  - Display name: "Creeping Vanishing Delay"
  - Description: How long Herobrine lingers after being caught creeping.
  - Allowed values: 0s / 0.5s / 1s / 2s
  - Default: "0.1"

- crimson_curse
  - Display name: "Crimson Curse"
  - Description: Replaces certain light sources (e.g., torches → redstone torches) when executed (periodic checks).
  - Default: "true"

- crimson_curse_chance
  - Display name: "Crimson Curse Chance"
  - Description: Changes the chance Herobrine will perform the Crimson Curse.
  - Allowed values: Common / Default / Rare / Scarce
  - Default: "default"

- disappearing_torches
  - Display name: "Disappearing Torches"
  - Description: Herobrine may break your torches when you leave the area (periodic checks).
  - Default: "true"

- disappearing_torches_chance
  - Display name: "Disappearing Torches Chance"
  - Description: Changes the chance Herobrine will successfully break torches.
  - Allowed values: Common / Default / Rare / Scarce
  - Default: "default"

- dreadful_donation
  - Display name: "Dreadful Donation"
  - Description: Herobrine may leave strange items in your chests (periodic checks).
  - Default: "true"

- dreadful_donation_chance
  - Display name: "Dreadful Donation Chance"
  - Description: Changes the chance Herobrine will leave donations.
  - Allowed values: Common / Default / Rare / Scarce
  - Default: "default"

- dwelling_shadow
  - Display name: "Dwelling Shadow"
  - Description: If Herobrine spawns in darkness (dwelling), he will refuse to disappear while in dark areas.
  - Default: "true"

- dwelling_sighting
  - Display name: "Dwelling Sightings"
  - Description: Spawns Herobrine in dark corners of caves, watching from out of view.
  - Default: "true"

- dwelling_vanishing_delay
  - Display name: "Dwelling Vanishing Delay"
  - Description: How long Herobrine lingers after being caught dwelling.
  - Allowed values: 0s / 0.5s / 1s / 2s
  - Default: "0.5"

- eerie_entrance
  - Display name: "Eerie Entrance"
  - Description: Toggles whether a join message is shown at the end of the haunting delay.
  - Default: "false"

- emissive_layers
  - Display name: "Emissive Layers"
  - Description: Toggles glowing/emissive layers on parts of Herobrine skins.
  - Default: "true"

- fearful_footsteps
  - Display name: "Fearful Footsteps"
  - Description: You may hear disembodied footsteps around you (periodic checks).
  - Default: "true"

- fearful_footsteps_chance
  - Display name: "Fearful Footsteps Chance"
  - Description: Changes the chance you will hear disembodied footsteps.
  - Allowed values: Common / Default / Rare / Scarce
  - Default: "default"

- flickering_flashlight
  - Display name: "Flickering Flashlight"
  - Description: When enabled, Herobrine can make your flashlight flicker (EPILEPSY WARNING in UI).
  - Default: "false"

- from_the_pants
  - Display name: "From The Pants"
  - Description: Enables an easter-egg mechanic (nonsensical/quirky behavior described in UI).
  - Default: "false"

- ghost_doors
  - Display name: "Ghost Doors"
  - Description: Herobrine will sometimes open and close doors when you're not looking.
  - Default: "true"

- ghost_doors_chance
  - Display name: "Ghost Doors Chance"
  - Description: Changes the chance Herobrine will successfully open/close doors.
  - Allowed values: Common / Default / Rare / Scarce
  - Default: "default"

- ghost_miner
  - Display name: "Ghost Miner"
  - Description: Herobrine may dig tunnels around you both underground and on the surface (periodic checks).
  - Default: "true"

- ghost_miner_chance
  - Display name: "Ghost Miner Chance"
  - Description: Changes the chance Herobrine will dig tunnels.
  - Allowed values: Common / Default / Rare / Scarce
  - Default: "default"

- give_him_control
  - Display name: "Give Him Control." (alt: "I Have Control.")
  - Description: A toggle that hands control of the config to Herobrine (or switches UI mode). Shorthand for debug/special behavior.
  - Default: "false"

- haunted_heirlooms
  - Display name: "Haunted Heirlooms"
  - Description: When enabled, Herobrine may leave custom paintings and music discs as donations.
  - Default: "false"

- haunted_herd
  - Display name: "Haunted Herd"
  - Description: Herobrine may possess passive mobs to help him blend in.
  - Default: "true"

- haunted_herd_chance
  - Display name: "Haunted Herd Chance"
  - Description: Changes the chance Herobrine will possess a passive mob.
  - Allowed values: Common / Default / Rare / Scarce
  - Default: "default"

- haunting_delay
  - Display name: "Haunting Delay"
  - Description: Number of in-game days before major hauntings (e.g., shrine timing) begin.
  - Default: 3

- herobrine_skin
  - Display name: "Herobrine Variants / Skin Library"
  - Description: A skin library object controlling which Herobrine variants are available/selected. Contains lists and a selected value; many predefined skin values exist (e.g., Default, Classic, Distorted, etc.).
  - Default: list includes "default" (storage ensures at least ["default"]) 

- hoebrine_returns
  - Display name: "Hoebrine Returns"
  - Description: Toggles an optional resourcepack-specific feature (UI wording: "Enable the fucking resourcepack.").
  - Default: "false"

- leafless_grove
  - Display name: "Leafless Groves"
  - Description: Herobrine will remove leaves from forests you've visited (periodic checks).
  - Default: "true"

- leafless_grove_chance
  - Display name: "Leafless Groves Chance"
  - Description: Changes the chance Herobrine will remove leaves.
  - Allowed values: Common / Default / Rare / Scarce
  - Default: "default"

- letter
  - Display name: "Glowstone Letters"
  - Description: When enabled, Herobrine will place large glowstone letters in plains/deserts.
  - Allowed values: Off / Default / Common / Rare
  - Default: "off"

- lurking_language
  - Display name: "Lurking Language"
  - Description: Language used for signs/donations placed by Herobrine (e.g., English, Swedish).
  - Allowed values (examples): English / Swedish
  - Default: "local" (English)

- lurking_sighting
  - Display name: "Lurking Sightings"
  - Description: Spawns Herobrine far away, watching from a distance.
  - Default: "true"

- lurking_vanishing_delay
  - Display name: "Lurking Vanishing Delay"
  - Description: How long Herobrine lingers after being caught lurking.
  - Allowed values: 0s / 0.5s / 1s / 2s
  - Default: "1"

- malicious_malfunction
  - Display name: "Malicious Malfunction"
  - Description: Aggressive malfunction that may crash the game on jumpscare (warning: not for weaker machines).
  - Default: "false"

- mossy_pyramid
  - Display name: "Mossy Pyramids"
  - Description: Toggles placement of mossy pyramids in forests.
  - Allowed values: Off / Default / Common / Rare
  - Default: "default"

- nametag
  - Display name: "Nametag"
  - Description: Controls whether Herobrine has a nametag and how it appears.
  - Allowed values: Off / Cryptic / Dynamic
  - Default: "off"

- nightmare_sighting
  - Display name: "Nightmare Sightings"
  - Description: Appears at the foot of your bed when you sleep (nightmare mechanic).
  - Default: "true"

- no_sleep
  - Display name: "No Sleep"
  - Description: Prevents normal sleeping when Herobrine is nearby (does not affect peaceful difficulty).
  - Default: "true"

- rekindling_shrine
  - Display name: "Rekindling Shrine"
  - Description: Toggles whether Herobrine will relight doused shrines if you leave them.
  - Default: "true"

- sand_pyramid
  - Display name: "Sand Pyramids"
  - Description: Toggles placement of sand pyramids in deserts/beaches.
  - Allowed values: Off / Default / Common / Rare
  - Default: "default"

- shrine_strike
  - Display name: "Shrine Strike"
  - Description: Toggles whether lightning strikes when activating a Herobrine shrine.
  - Default: "false"

- shrine_surprise
  - Display name: "Shrine Surprise"
  - Description: Toggles whether Herobrine will appear on top of a shrine when lit.
  - Default: "false"

- shrouded_specter
  - Display name: "Shrouded Specter"
  - Description: Controls which particles appear when Herobrine disappears.
  - Allowed values: Off / Poof / Portal / Smoke
  - Default: "off"

- sighting_frequency
  - Display name: "Sighting Frequency"
  - Description: Adjusts how often Herobrine appears. Note: spawn rate doubles at night.
  - Allowed values: Common / Default / Rare / Scarce
  - Default: "default"

- sighting_noise
  - Display name: "Sighting Noise"
  - Description: Toggles a noise played on sighting (50% chance when enabled).
  - Allowed values: Off / Cave / Ghast
  - Default: "cave"

- sighting_sense
  - Display name: "Sighting Sense"
  - Description: Toggles whether a tamed wolf will sense Herobrine's presence.
  - Default: "true"

- sighting_sense_chance
  - Display name: "Sighting Sense Chance"
  - Description: Changes the chance wolves will notice Herobrine.
  - Allowed values: Common / Default / Rare / Scarce
  - Default: "default"

- sighting_window
  - Display name: "Sighting Window"
  - Description: Time window where Herobrine may appear.
  - Allowed values: Always / Day / Night / Rain
  - Default: "always"

- sinister_signs
  - Display name: "Sinister Signs"
  - Description: Toggles whether Herobrine will place ominous signs with messages.
  - Default: "true"

- sinister_signs_chance
  - Display name: "Sinister Signs Chance"
  - Description: Changes how often Herobrine places signs during events.
  - Allowed values: Common / Default / Rare / Scarce
  - Default: "default"

- sneaky_strike
  - Display name: "Sneaky Strike"
  - Description: Herobrine may attempt to hit you if you take too long to notice him during certain sightings.
  - Default: "true"

- sneaky_strike_chance
  - Display name: "Sneaky Strike Chance"
  - Description: Changes the chance Herobrine will hit you when you delay noticing him.
  - Allowed values: Common / Default / Rare / Scarce
  - Default: "default"

- soul_shift
  - Display name: "Soul Shift"
  - Description: Toggles Herobrine replacing lanterns during Crimson Curse execution.
  - Default: "false"

- stalking_sighting
  - Display name: "Stalking Sightings"
  - Description: Spawns Herobrine stalking you from a distance.
  - Default: "true"

- stalking_vanishing_delay
  - Display name: "Stalking Vanishing Delay"
  - Description: How long Herobrine lingers after being caught stalking.
  - Allowed values: 0s / 0.5s / 1s / 2s
  - Default: "0.5"

- sudden_scare
  - Display name: "Sudden Scare"
  - Description: Enables jumpscares when you collide with Herobrine.
  - Default: "true"

- suspenseful_sighting
  - Display name: "Suspenseful Sighting"
  - Description: Toggles whether a suspense strike sound is played when Herobrine is spotted.
  - Allowed values: Off / Default / Dynamic
  - Default: "off"

- twisted_tapestries
  - Display name: "Twisted Tapestries"
  - Description: Herobrine may replace paintings with haunted versions (periodic checks).
  - Default: "false"

- twisted_tapestries_chance
  - Display name: "Twisted Tapestries Chance"
  - Description: Changes the chance Herobrine will haunt paintings.
  - Allowed values: Common / Default / Rare / Scarce
  - Default: "default"

- vanilla_sightings
  - Display name: "Vanilla Sightings"
  - Description: Allows players without the resourcepack to see Herobrine (overrides skins).
  - Default: "false"

- window_watcher
  - Display name: "Window Watcher"
  - Description: Toggles whether Herobrine continues watching you when spotted through a window.
  - Default: "true"

- wooden_cross
  - Display name: "Wooden Crosses"
  - Description: Toggles placement of wooden crosses in forests.
  - Allowed values: Off / Default / Common / Rare
  - Default: "off"

---

## Example: change a config option at runtime

You can change options at runtime by modifying the datapack's storage directly or by reproducing what the config UI does. Use the in-game chat/command line.

- Direct storage modification (advanced): set the `activity_multiplier` to 2

  /data modify storage lunareclipse.watching:config_options options.activity_multiplier set value 2

  Notes: string options require quoted values. Example - set `sighting_window` to `night`:

  /data modify storage lunareclipse.watching:config_options options.sighting_window set value "night"

- Reproduce the UI handler (recommended for boolean/option updates): the option click handlers perform the equivalent `data modify` calls. To set `sneaky_strike` to false as the current player:

  /execute as @s run data modify storage lunareclipse.watching:config_options options.sneaky_strike set value "false"

After editing storage directly, the datapack may pick up changes immediately; if it doesn't, reopen the in-game config UI or run `/function lunareclipse.watching:config/initialize_defaults` for reinitialization steps when appropriate.

### Get current value(s)

You can inspect current config values with `/data get storage`. Examples:

- Get a single option value (shows the stored value for `sneaky_strike`):

  /data get storage lunareclipse.watching:config_options options.sneaky_strike

- Get a nested object field (herobrine_skin selected value):

  /data get storage lunareclipse.watching:config_options options.herobrine_skin.selected

- Get the entire options object (returns JSON for all options):

  /data get storage lunareclipse.watching:config_options options

The command prints the stored JSON/value to chat; for larger objects it may be truncated in chat depending on your client. Use targeted paths (e.g., `options.some_option`) when possible.

### Reset all settings to defaults

You can restore the datapack's canonical defaults by running the initializer function. This will overwrite the runtime `options` storage with the values defined in `data/lunareclipse.watching/function/config/defaults.mcfunction`.

- Reset everything to the datapack defaults:

  /function lunareclipse.watching:config/initialize_defaults

  Notes: this function is also called on reload. It requires operator/console permissions.

- Reset a single option to its default using the helper (advanced):

  /function lunareclipse.watching:config/set_default {option:"<option_key>",value:"\"<value>\""}

  Example — set `sighting_frequency` back to the datapack's default value:

  /function lunareclipse.watching:config/set_default {option:"sighting_frequency",value:"\"default\""}

  Example — set a numeric default (no extra quotes required):

  /function lunareclipse.watching:config/set_default {option:"haunting_delay",value:3}

Be careful: these commands will overwrite any existing configuration. Reopen the in-game config UI or run the initializer again if UI elements do not immediately reflect the changes.