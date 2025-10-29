execute as @e[type=wither_skull,tag=deathtag] at @r run playsound minecraft:entity.experience_orb.pickup player @e[type=player,distance=..50] ~ ~ ~ 0.3 1.2

execute as @e[type=wither_skull,tag=deathtag] at @s run tp @s ~ ~ ~ facing entity @r eyes
execute as @e[type=wither_skull,tag=deathtag] at @s run tp @s ~ ~ ~ ~180 ~


schedule function wither:wither/enraged/skulls/rotation 10t