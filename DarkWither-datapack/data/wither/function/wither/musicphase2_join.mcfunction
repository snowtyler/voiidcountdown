# Plays Phase 2 music once to players who haven't heard it yet during Phase 2
# Reschedules itself at a low cadence while the DarkWither exists in Phase 2

# For nearby players who haven't heard Phase 2 yet, play once and mark heard (only if Phase 2 is active)
execute if entity @e[type=minecraft:wither,tag=DarkWither,tag=Phase2,limit=1] at @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] as @a[distance=..400] unless score @s dw_music_p2_heard matches 1 at @s run playsound darkwither:events.phase2 record @s ~ ~ ~ 1 1
execute if entity @e[type=minecraft:wither,tag=DarkWither,tag=Phase2,limit=1] at @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] as @a[distance=..400] unless score @s dw_music_p2_heard matches 1 run scoreboard players set @s dw_music_p2_heard 1

# Keep checking periodically while the wither is alive in Phase 2
execute if entity @e[type=wither,tag=DarkWither,tag=Phase2] run schedule function wither:wither/musicphase2_join 2s replace
