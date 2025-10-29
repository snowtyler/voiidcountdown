# Removes Tag

execute as @e[type=minecraft:wither,tag=DarkWither,tag=justSummoned] at @s run tag @s add theWither
execute as @e[type=minecraft:wither,tag=DarkWither,tag=justSummoned] at @s run data merge entity @s {Invul:220}
execute as @e[type=minecraft:wither,tag=DarkWither,tag=justSummoned] at @s run tag @s remove justSummoned

# Debug: set a higher max health if the debug flag is enabled
execute as @e[type=minecraft:wither,tag=DarkWither,tag=theWither] if data storage wither:options {debug_max_health:Enabled} run data merge entity @s {attributes:[{id:"minecraft:max_health",base:100.0}],Health:100.0f}



# Keeps count of how many withers are alive
execute as @s run scoreboard players add witherCount witherCount 1

execute if score witherCount witherCount matches 1 run function wither:wither/fight_start

