# Second attempt (one-time) to ensure ascension if the first check raced with other merges
execute as @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] if data storage wither:options {toggleanimation:Default} run data merge entity @s {NoAI:0b,NoGravity:0b}
execute as @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] if data storage wither:options {toggleanimation:Default} unless data entity @s {active_effects:[{id:"minecraft:levitation"}]} run effect give @s minecraft:levitation 7 3 true
