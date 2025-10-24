# Conclude the finale and return everyone to the overworld spawn
execute at @e[type=item,tag=nether_star,limit=1,sort=nearest] run playsound minecraft:entity.enderman.teleport master @a ~ ~ ~ 1 1.2

# Clear lingering visual effects
execute as @a run title @s clear
execute as @a run effect clear @s minecraft:slowness
execute as @a run effect clear @s minecraft:resistance
execute as @a run effect clear @s minecraft:night_vision
execute as @a run effect clear @s minecraft:conduit_power

# Send everyone back to the overworld spawn platform (adjust coords as needed)
execute as @a in minecraft:overworld positioned 0 120 0 run teleport @s ~ ~ ~

scoreboard players reset finale finale_timer
