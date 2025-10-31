# Teleport wither back to spawn coordinates with visual/audio effects

# Set cooldown to prevent infinite loop (100 ticks = 5 seconds)
scoreboard players set @s leashCooldown 100

# Visual effects before teleport
particle minecraft:portal ~ ~1 ~ 0.5 1 0.5 1 100 normal
playsound minecraft:entity.enderman.teleport hostile @a ~ ~ ~ 2 0.8

# Get spawn coordinates into scoreboard
execute store result score #spawn_x witherCheck run data get storage wither:spawn_pos x
execute store result score #spawn_y witherCheck run data get storage wither:spawn_pos y
execute store result score #spawn_z witherCheck run data get storage wither:spawn_pos z

# Teleport using summon/kill workaround - actually just use spreadplayers or direct tp
# The simplest approach: use execute positioned
execute positioned 0.0 0.0 0.0 run function wither:wither/leash_restore_pos

# Visual effects after teleport
execute at @s run particle minecraft:portal ~ ~1 ~ 0.5 1 0.5 1 100 normal
execute at @s run playsound minecraft:entity.enderman.teleport hostile @a ~ ~ ~ 2 1.2
