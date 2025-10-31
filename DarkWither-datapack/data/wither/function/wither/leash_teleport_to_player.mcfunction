# Teleport wither near the closest player instead of spawn

# Set cooldown to prevent infinite loop (100 ticks = 5 seconds)
scoreboard players set @s leashCooldown 200

# Visual effects before teleport
particle minecraft:portal ~ ~1 ~ 0.5 1 0.5 1 100 normal
playsound minecraft:entity.enderman.teleport hostile @a ~ ~ ~ 2 0.8

# Check if random target player is in the void (Y < -60), if so use spawn coordinates instead
execute store result score #player_y_check witherCheck run data get entity @r Pos[1]
execute if score #player_y_check witherCheck matches ..-60 store result score #spawn_x witherCheck run data get storage wither:spawn_pos x
execute if score #player_y_check witherCheck matches ..-60 store result score #spawn_y witherCheck run data get storage wither:spawn_pos y
execute if score #player_y_check witherCheck matches ..-60 store result score #spawn_z witherCheck run data get storage wither:spawn_pos z
execute if score #player_y_check witherCheck matches ..-60 positioned 0.0 0.0 0.0 run function wither:wither/leash_restore_pos

# Teleport to position near a random player (10 blocks away in the air) if they're not in the void
execute if score #player_y_check witherCheck matches -59.. at @r run tp @s ~ ~10 ~

# Visual effects after teleport
execute at @s run particle minecraft:portal ~ ~1 ~ 0.5 1 0.5 1 100 normal
execute at @s run playsound minecraft:entity.enderman.teleport hostile @a ~ ~ ~ 2 1.2
