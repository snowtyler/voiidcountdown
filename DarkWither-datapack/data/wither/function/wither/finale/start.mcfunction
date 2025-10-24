# Kick off the finale cinematic once the Dark Wither dies
scoreboard players set finale finale_timer 0

# Stabilise players for the sequence and brighten the scene
execute as @a run effect give @s minecraft:slowness 10 5 true
execute as @a run effect give @s minecraft:resistance 10 255 true
execute as @a run effect give @s minecraft:night_vision 10 0 true
execute as @a run effect give @s minecraft:conduit_power 10 1 true

execute at @e[type=item,tag=nether_star,limit=1,sort=nearest] run playsound minecraft:entity.ender_dragon.death master @a ~ ~ ~ 0.6 1.2

schedule function wither:wither/finale/fade_loop 1t
schedule function wither:wither/finale/text1 80t
