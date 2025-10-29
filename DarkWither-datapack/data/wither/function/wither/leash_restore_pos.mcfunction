# Teleport using execute store result
# This moves the wither to the coordinates stored in the scoreboard

execute positioned 0.0 0.0 0.0 if score #spawn_x witherCheck matches -2147483648..2147483647 if score #spawn_y witherCheck matches -2147483648..2147483647 if score #spawn_z witherCheck matches -2147483648..2147483647 run summon marker ~ ~ ~ {Tags:["wither_tp_marker"]}

execute as @e[type=marker,tag=wither_tp_marker,limit=1] store result entity @s Pos[0] double 1 run scoreboard players get #spawn_x witherCheck
execute as @e[type=marker,tag=wither_tp_marker,limit=1] store result entity @s Pos[1] double 1 run scoreboard players get #spawn_y witherCheck
execute as @e[type=marker,tag=wither_tp_marker,limit=1] store result entity @s Pos[2] double 1 run scoreboard players get #spawn_z witherCheck

tp @s @e[type=marker,tag=wither_tp_marker,limit=1]
kill @e[type=marker,tag=wither_tp_marker]
