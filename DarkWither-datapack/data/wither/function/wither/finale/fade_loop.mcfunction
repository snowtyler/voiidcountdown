# Drive the particle-based white fade and stop once the timer expires
scoreboard players add finale finale_timer 1

execute as @a at @s run particle minecraft:dust_color_transition{from_color:[0.75,0.75,0.75],to_color:[1.0,1.0,1.0],scale:3.2} ~ ~1 ~ 1.2 1.4 1.2 0.02 80 force
execute as @a at @s run particle minecraft:cloud ~ ~1 ~ 0.6 0.8 0.6 0.02 25 force

execute if score finale finale_timer matches ..80 run schedule function wither:wither/finale/fade_loop 1t
