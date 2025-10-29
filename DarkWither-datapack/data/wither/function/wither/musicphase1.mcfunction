execute unless entity @e[type=minecraft:wither,tag=DarkWither,tag=Phase2,limit=1] at @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] as @e[type=minecraft:player,distance=..400] at @s run playsound darkwither:events.phase1 record @a ~ ~ ~ 1 1

# Mark players in range as having heard Phase 1 music
execute unless entity @e[type=minecraft:wither,tag=DarkWither,tag=Phase2,limit=1] at @e[type=minecraft:wither,tag=DarkWither,limit=1,sort=nearest] as @a[distance=..400] run scoreboard players set @s dw_music_p1_heard 1

# Ensure only one loop is queued at a time to prevent overlapping music
execute if entity @e[type=wither,tag=DarkWither,tag=!Phase2] run schedule function wither:wither/musicphase1 117s replace

# Start a lightweight join-immediate check that plays Phase 1 music once to newly joined/entering players
execute if entity @e[type=wither,tag=DarkWither,tag=!Phase2] run schedule function wither:wither/musicphase1_join 2s replace