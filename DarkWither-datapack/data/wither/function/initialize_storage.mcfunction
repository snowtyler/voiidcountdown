# Set initial storage values true = enabled, false = disabled
data modify storage wither:options toggletrophy set value Enabled
data modify storage wither:options toggledestruction set value Enabled
data modify storage wither:options togglehoming set value Enabled
data modify storage wither:options togglecharge set value Enabled
data modify storage wither:options toggledash set value Enabled
data modify storage wither:options toggleboomtp set value Enabled
data modify storage wither:options togglespawns set value Enabled
data modify storage wither:options toggleweather set value Enabled

data modify storage wither:options toggleweather set value Enabled

# Animations 0 = enabled, 1 = disabled, 2 = bedrock
data modify storage wither:options toggleanimation set value Default

# Debug options
# When Enabled, the wither's max health will be set to the debug value on spawn
data modify storage wither:options debug_max_health set value Disabled

# Marks storage as initialized if this is set to 2 it'll just re-apply default settings [changes on updates that require data to be modified]
# Use if you want to persist with the default settings
data modify storage wither:options initialized set value 2
