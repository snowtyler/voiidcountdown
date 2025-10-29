# Check if wither is too far from all players and teleport to nearest player

# Decrease cooldown timer
execute as @e[type=minecraft:wither,tag=DarkWither,limit=1] if score @s leashCooldown matches 1.. run scoreboard players remove @s leashCooldown 1

# Only check if cooldown is 0 - teleport to player instead of spawn
execute as @e[type=minecraft:wither,tag=DarkWither,limit=1] if score @s leashCooldown matches 0 at @s unless entity @a[distance=..100] run function wither:wither/leash_teleport_to_player

# Check Y-axis leash (20 blocks from random player's Y coordinate)
execute as @e[type=minecraft:wither,tag=DarkWither,limit=1] if score @s leashCooldown matches 0 store result score #current_y witherCheck run data get entity @s Pos[1]
execute as @e[type=minecraft:wither,tag=DarkWither,limit=1] if score @s leashCooldown matches 0 at @s store result score #player_y witherCheck run data get entity @r Pos[1]
execute as @e[type=minecraft:wither,tag=DarkWither,limit=1] if score @s leashCooldown matches 0 run scoreboard players operation #y_diff witherCheck = #current_y witherCheck
execute as @e[type=minecraft:wither,tag=DarkWither,limit=1] if score @s leashCooldown matches 0 run scoreboard players operation #y_diff witherCheck -= #player_y witherCheck
execute as @e[type=minecraft:wither,tag=DarkWither,limit=1] if score @s leashCooldown matches 0 if score #y_diff witherCheck matches 41.. run function wither:wither/leash_teleport_to_player
execute as @e[type=minecraft:wither,tag=DarkWither,limit=1] if score @s leashCooldown matches 0 if score #y_diff witherCheck matches ..-41 run function wither:wither/leash_teleport_to_player

# Absolute void protection - teleport if Y < -60
execute as @e[type=minecraft:wither,tag=DarkWither,limit=1] if score @s leashCooldown matches 0 if score #current_y witherCheck matches ..-60 run function wither:wither/leash_teleport_to_player

# Check if wither is stuck inside solid blocks (only check center position to avoid false positives)
execute as @e[type=minecraft:wither,tag=DarkWither,limit=1] if score @s leashCooldown matches 0 at @s unless block ~ ~1 ~ #minecraft:wither_immune unless block ~ ~1 ~ #minecraft:replaceable run function wither:wither/leash_teleport_to_player
