# Marks the nearest Wither as a Wither Guardian so the datapack controls it.
execute as @e[type=minecraft:wither,sort=nearest,limit=1] run tag @s add WitherGuardian
execute as @e[type=minecraft:wither,tag=WitherGuardian,sort=nearest,limit=1] run tag @s remove theWither
