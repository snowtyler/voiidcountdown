execute at @e[type=minecraft:wither,tag=WitherGuardian,limit=1,sort=nearest] as @e[type=minecraft:player,distance=..200] at @s run playsound wither_guardian:events.phase1 music @a ~ ~ ~ 1 1

execute if entity @e[type=wither,tag=WitherGuardian] run schedule function wither:wither/musicphase1 139s