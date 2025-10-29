# Ensure the Dark Wither ascends after spawn
# - Reapply levitation if missing
# - Clear any accidental NoAI/NoGravity

# Only when default animation is enabled and a DarkWither exists
execute as @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] if data storage wither:options {toggleanimation:Default} run data merge entity @s {NoAI:0b,NoGravity:0b}

# If levitation is missing (or very short), give it again for ~7s (140t) at amplifier 3
execute as @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] if data storage wither:options {toggleanimation:Default} unless data entity @s {active_effects:[{id:"minecraft:levitation"}]} run effect give @s minecraft:levitation 7 3 true

# If still missing on the next check (rare), try once more then stop
execute if entity @e[type=wither,tag=DarkWither] run schedule function wither:wither/fight_start1_levi_check2 3t replace
