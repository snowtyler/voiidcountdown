execute as @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] at @s run function wither:wither/unstuck1

execute if entity @e[type=wither,tag=DarkWither] run schedule function wither:wither/unstuck 2s
