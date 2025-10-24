# First beat of the finale narration
title @a times 30 80 40
title @a title {"text":"The darkness lifts...","color":"white"}
execute at @e[type=item,tag=nether_star,limit=1,sort=nearest] run playsound minecraft:block.amethyst_block.resonate master @a ~ ~ ~ 0.6 1.8

schedule function wither:wither/finale/text2 100t
