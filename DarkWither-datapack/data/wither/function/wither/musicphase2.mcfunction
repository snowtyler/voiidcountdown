execute at @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] as @e[type=minecraft:player,distance=..200] at @s run playsound darkwither:events.phase2 record @a ~ ~ ~ 1 1

execute if entity @e[type=wither,tag=DarkWither] run schedule function wither:wither/musicphase2 226s