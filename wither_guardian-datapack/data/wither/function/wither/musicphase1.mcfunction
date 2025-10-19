execute at @e[type=minecraft:wither,tag=WitherGuardian,limit=1,sort=nearest] as @e[type=minecraft:player,distance=..200] at @s run playsound wither_guardian:phase1 record @a ~ ~ ~ 1 1

execute if entity @e[type=wither,tag=WitherGuardian] run schedule function wither:wither/musicphase1 139s