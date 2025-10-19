# Play Ender Dragon music when fight starts
# Only play for players within 500 blocks of the dragon's position.
# We run this function 'at' the dragon, so distance is relative to the dragon.

execute if entity @a[distance=..500] run playsound prophecy:prophecy.ripple music @a[distance=..500] ~ ~ ~ 1 1
