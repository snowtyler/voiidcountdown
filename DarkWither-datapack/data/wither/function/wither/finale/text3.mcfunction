# Final beat before the warp
title @a times 30 100 50
title @a title {"text":"Home calls you back.","color":"white"}
execute at @e[type=item,tag=nether_star,limit=1,sort=nearest] run playsound minecraft:block.note_block.chime master @a ~ ~ ~ 0.7 1.6

schedule function wither:wither/finale/warp 120t
