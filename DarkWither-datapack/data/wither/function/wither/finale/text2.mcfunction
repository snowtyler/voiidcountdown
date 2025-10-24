# Second beat of the finale narration
title @a times 30 90 40
title @a title {"text":"Light floods the crater.","color":"white"}
execute at @e[type=item,tag=nether_star,limit=1,sort=nearest] run playsound minecraft:block.amethyst_block.chime master @a ~ ~ ~ 0.6 1.9

schedule function wither:wither/finale/text3 100t
