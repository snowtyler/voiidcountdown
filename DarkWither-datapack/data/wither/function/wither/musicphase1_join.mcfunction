# Plays Phase 1 music once to players who haven't heard it yet during Phase 1
# Reschedules itself at a low cadence while the DarkWither exists and Phase 1 is active

# For nearby players who haven't heard Phase 1 yet, play once and mark heard
execute unless entity @e[type=minecraft:wither,tag=DarkWither,tag=Phase2,limit=1] at @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] as @a[distance=..400] unless score @s dw_music_p1_heard matches 1 at @s run playsound darkwither:events.phase1 record @s ~ ~ ~ 1 1
execute unless entity @e[type=minecraft:wither,tag=DarkWither,tag=Phase2,limit=1] at @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] as @a[distance=..400] unless score @s dw_music_p1_heard matches 1 run scoreboard players set @s dw_music_p1_heard 1

# Keep checking periodically while the wither is alive (midpoint clears this schedule)
execute if entity @e[type=wither,tag=DarkWither,tag=!Phase2] run schedule function wither:wither/musicphase1_join 2s replace
