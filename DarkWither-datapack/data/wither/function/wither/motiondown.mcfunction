# Helper function to set motion down
execute as @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] run data merge entity @s {Motion:[0.0,-8.0,0.0]}