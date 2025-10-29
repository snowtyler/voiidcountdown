execute at @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] as @e[type=minecraft:player,distance=..400] at @s run playsound darkwither:events.phase2 record @a ~ ~ ~ 1 1

# Mark players in range as having heard Phase 2 music
execute at @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] as @a[distance=..400] run scoreboard players set @s dw_music_p2_heard 1

# Ensure only one loop is queued at a time to prevent overlapping music
execute if entity @e[type=wither,tag=DarkWither] run schedule function wither:wither/musicphase2 226s replace

# Start a lightweight join-immediate check that plays Phase 2 music once to newly joined/entering players
execute if entity @e[type=wither,tag=DarkWither] run schedule function wither:wither/musicphase2_join 2s replace