
# Checks for alive wither every tick
## Detect players who just joined (no persistent entity tag across reconnects)
## Reset their per-phase heard flags so join-immediate playback can trigger
execute as @a[tag=!dw_known] run scoreboard players set @s dw_music_p1_heard 0
execute as @a[tag=!dw_known] run scoreboard players set @s dw_music_p2_heard 0
execute as @a[tag=!dw_known] run tag @s add dw_known

execute as @e[type=minecraft:wither,tag=DarkWither,tag=!theWither,limit=1] at @s run tag @s add justSummoned
execute as @e[type=minecraft:wither,tag=DarkWither,tag=justSummoned,limit=1] at @s run function wither:wither/fight_once

# Check if wither strayed too far from players
function wither:wither/leash_check

function wither:wither/skull
execute as @e[type=minecraft:wither_skull,tag=buff,limit=1,tag=buff] run kill @s 

# CHECKS HEALTH EVERY TICK
execute at @p as @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] store result score @s checkHealth run data get entity @s Health
execute at @p as @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=furthest] store result score @s checkHealth run data get entity @s Health
execute as @e[type=wither,tag=DarkWither,limit=1,sort=nearest] store result score @s Health run data get entity @s Health


# WITHER CHECK
execute unless entity @e[type=minecraft:wither,tag=DarkWither] run schedule clear wither:wither/charge/chargepre
execute unless entity @e[type=minecraft:wither,tag=DarkWither] run schedule clear wither:wither/homing/homingpre
execute unless entity @e[type=minecraft:wither,tag=DarkWither] run schedule clear wither:wither/spawn


# WITHER HEALTH NEVER ABOVE HALF AFTER HITTING IT

execute as @e[type=wither,tag=DarkWither,tag=!Wither] if score @s Health matches 298.. run data modify entity @s Health set value 298.0f
execute as @e[type=wither,tag=DarkWither,tag=!Dash] if score @s Health matches 100.. run data modify entity @s Health set value 98.0f
