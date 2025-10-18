Java Edition
For text component format used in versions before Java Edition 1.21.5, see Text component format/Before Java Edition 1.21.5.
Text components are written in SNBT, for example {text: "Hello world"}. They are used for rich-text formatting in written books, signs, custom names and the /tellraw, /title, /bossbar, /scoreboard and /team commands.

The format is made up of text components. There is a single root component, which can have child components, which can have their own children and so on. Components can also have formatting and interactivity added to them, which is inherited by their children.

A component can be a [String] string, [NBT List / JSON Array] list or a [NBT Compound / JSON Object] object. Strings and lists are both shorthand for longer object structures, as described below.

[String] A string containing plain text to display directly. This is the same as an object that only has a [String] text tag. For example, "A" and {text: "A"} are equivalent.
[NBT List / JSON Array] A list of components. Same as having all components after the first one appended to the first's [NBT List / JSON Array] extra list. For example, ["A", "B", "C"] is equivalent to {text: "A", extra: ["B", "C"]}. Note that because the later components are actually children of the first one, any formatting applied to the first component is inherited by the later ones. For example, [{text: "A", color: "red"}, "B", "C"] will display all three letters with red text.
[NBT Compound / JSON Object] A text component object. All non-content tags are optional.
Content
[String] type: Optional. Specifies the content type. One of "text", "translatable", "score", "selector", "keybind", or "nbt".
If [String] type is not present, has an invalid value, or if the required tags for the specified type are not present, the type is determined automatically by checking the object for the following tags: [String] text, [String] translate, [NBT Compound / JSON Object] score, [String] selector, [String] keybind, and finally [String] nbt. If multiple are present, whichever one comes first in that list is used.
Values specific to each content type are described below.
Children
[NBT List / JSON Array] extra: A list of additional components to be displayed after this one.
A child text component. Child text components inherit all formatting and interactivity from the parent component, unless they explicitly override them.
Formatting
[String] color: Optional. Changes the color to render the content in the text component object and its child objects. If not present, the parent color will be used instead. The color is specified as a color code or as a color name.
"#<hex>", where <hex> is a 6-digit hexadecimal color, changes the color to #<hex>
"black" changes the color to 
 #000000
"dark_blue" changes the color to 
 #0000AA
"dark_green" changes the color to 
 #00AA00
"dark_aqua" changes the color to 
 #00AAAA
"dark_red" changes the color to 
 #AA0000
"dark_purple" changes the color to 
 #AA00AA
"gold" changes the color to 
 #FFAA00
"gray" changes the color to 
 #AAAAAA
"dark_gray" changes the color to 
 #555555
"blue" changes the color to 
 #5555FF
"green" changes the color to 
 #55FF55
"aqua" changes the color to 
 #55FFFF
"red" changes the color to 
 #FF5555
"light_purple" changes the color to 
 #FF55FF
"yellow" changes the color to 
 #FFFF55
"white" changes the color to 
 #FFFFFF
[String] font: Optional. The resource location of the font for this component in the resource pack within assets/<namespace>/font. Defaults to "minecraft:default".
[Boolean] bold: Optional. Whether to render the content in bold.
[Boolean] italic: Optional. Whether to render the content in italics. Note that text that is italicized by default, such as custom item names, can be unitalicized by setting this to false.
[Boolean] underlined: Optional. Whether to underline the content.
[Boolean] strikethrough: Optional. Whether to strikethrough the content.
[Boolean] obfuscated: Optional. Whether to render the content obfuscated.
[Int] shadow_color: The color and opacity of the shadow. If omitted, the shadow is 25% the brightness of the text color and 100% the opacity[verify]. Color codes are the ARGB hex code of the color converted to a decimal number, or can be calculated from the opacity, red, green and blue components using this formula:
Alpha<<24 + Red<<16 + Green<<8 + Blue
[NBT List / JSON Array] shadow_color: Another format. A list containing 4 floats corresponding to red, green, blue, and opacity values as a fraction (ranged 0 to 1, inclusive) that is automatically converted to the int format.
Interactivity
[String] insertion: Optional. When the text is shift-clicked by a player, this string is inserted in their chat input. It does not overwrite any existing text the player was writing. This only works in chat messages.
[NBT Compound / JSON Object] click_event: Optional. Allows for events to occur when the player clicks on text. See § Click events.
[NBT Compound / JSON Object] hover_event: Optional. Allows for a tooltip to be displayed when the player hovers their mouse over text. See § Hover events.
Due to the [NBT List / JSON Array] extra tag, the above format may be recursively nested to produce complex and functional text strings. However, a text component doesn't have to be complicated at all: virtually all properties are optional and may be left out.

Content types
Text components can display several types of content. These tags should be included directly into the text component object.

Plain Text
Displays plain text.

[NBT Compound / JSON Object] The text component.
[String] type: Optional. Set to "text".
[String] text: A string containing plain text to display directly.
Translated Text
Displays a translated piece of text from the currently selected language. This uses the client's selected language, so if players with their games set to different languages are logged into the same server, each will see the component in their own language.

Translations are defined in language files in resource packs, including the built-in resource pack.

Translations can contain slots for text that is not known ahead of time, such as player names. When displaying the translated text, slots will be filled from a provided list of text components. The slots are defined in the language file, and generally take the form %s (displays the next component in the list), or %3$s (displays the third component in the list; replace 3 with whichever index is desired).[note 1] For example, the built-in English language file contains the translation "chat.type.advancement.task": "%s has made the advancement %s",.

[NBT Compound / JSON Object] The text component.
[String] type: Optional. Set to "translatable".
[String] translate: A translation identifier, corresponding to the identifiers found in loaded language files. Displayed as the corresponding text in the player's selected language. If no corresponding translation can be found, the identifier itself is used as the translated text.
[String] fallback: Optional. If no corresponding translation can be found, this is used as the translated text. Ignored if [String] translate is not present.
[NBT List / JSON Array] with: Optional. A list of text components to be inserted into slots in the translation text. Ignored if [String] translate is not present.
A text component. If no component is provided for a slot, the slot is displayed as no text.
Scoreboard Value
Displays a score from the scoreboard.

[]Requires component resolution.
This component is resolved into a [String] text component containing the scoreboard value.
[NBT Compound / JSON Object] The text component.
[String] type: Optional. Set to "score".
[NBT Compound / JSON Object] score: Displays a score holder's current score in an objective. Displays nothing if the given score holder or the given objective do not exist, or if the score holder is not tracked in the objective.
[String] name: The name of the score holder whose score should be displayed. This can be a selector like @p or an explicit name. If the text is a selector, the selector must be guaranteed to never select more than one entity, possibly by adding limit=1. If the text is "*", it shows the reader's own score (for example, /tellraw @a {score: {name: "*", objective: "obj"}} shows every online player their own score in the "obj" objective).[note 2]
[String] objective: The internal name of the objective to display the player's score in.
Entity Names
Displays the name of one or more entities found by a selector.

If exactly one entity is found, the entity's name is displayed by itself. If more are found, their names are displayed in the form "Name1, Name2, Name3", with gray commas. If none are found, the component is displayed as no text.

Hovering over a name shows a tooltip with the name, type, and UUID of the target. Clicking a player's name suggests a command to whisper to that player. Shift-clicking a player's name inserts that name into chat. Shift-clicking a non-player entity's name inserts its UUID into chat.

[]Requires component resolution.
If the selector finds a single entity,
If the entity is a player, the component is resolved into a [String] text component containing their name.
If it is an entity with a custom name, it is resolved into the text component of the custom name. In all vanilla survival scenarios (name tag, anvil) this will be a [String] text component.
If it is a non-player entity with no custom name, it is resolved into a [String] translate component containing the translation key for the entity type's name.
The resolved component also has an [String] insertion tag, a [NBT Compound / JSON Object] hover_event tag with the show_entity action, and a [NBT Compound / JSON Object] click_event tag with the suggest_command action (if the entity is a player) added to it to provide the functionality described above. If any of these tags are already present on the original component being resolved, the tag on the original component will be used.
If more than one entity is found by the selector, the component is resolved into an empty [String] text component, with an [NBT List / JSON Array] extra list containing the individual entity name components (each resolved as in the single entity case) separated by copies of the [Undefined] separator component (or its default, if not present).
If no entities are found by the selector, the component is resolved into an empty [String] text component.
[NBT Compound / JSON Object] The text component.
[String] type: Optional. Set to "selector".
[String] selector: A string containing a selector.
[NBT Compound / JSON Object] separator: Optional, defaults to {color: "gray", text: ", "}. A text component. Used as the separator between different names, if the component selects multiple entities.
Keybind
Displays the name of the button that is currently bound to a certain configurable control. This uses the client's own control scheme, so if players with different control schemes are logged into the same server, each will see their own keybind.

[NBT Compound / JSON Object] The text component.
[String] type: Optional. Set to "keybind".
[String] keybind: A keybind identifier, to be displayed as the name of the button that is currently bound to that action. For example, {keybind: "key.inventory"} displays "e" if the player is using the default control scheme.
NBT Values
Displays NBT values from entities, block entities, or command storage.

NBT strings display their contents. Other NBT values are displayed as SNBT, with no spacing between symbols. If [Boolean] interpret is set to true, the game will instead attempt to parse and display that text as its own text component. That only works on compounds and lists which contain tags with the proper key names. If [Boolean] interpret is true and parsing fails, the component is displayed as no text, or if it directs to a string, shows the string itself. If more than one NBT value is found, either by selecting multiple entities or by using a multi-value path, they are displayed in order, with the [Undefined] separator value between them.

[]Requires component resolution.
If [Boolean] interpret is false, the component is resolved into a [String] text component containing the display text.
If multiple values are selected, each value is resolved into an individual [String] text component, and all values after the first will be added to the first's [NBT List / JSON Array] extra list, separated by copies of the [Undefined] separator component.
If [Boolean] interpret is true, the component is resolved into the parsed text component. For any non-content tags that are present on both the parsed text component and the component being resolved, the tag on the component being resolved will be used.
If multiple values are selected, all values after the first will be added to the first's [NBT List / JSON Array] extra list, separated by copies of the [Undefined] separator component. This means that all values after the first will inherit the first value's formatting tags, if any.
[NBT Compound / JSON Object] The text component.
[String] type: Optional. Set to "nbt".
[String] source: Optional. Allowed values are "block", "entity", and "storage", corresponding to the source of the NBT data.
[String] nbt: The NBT path used for looking up NBT values from an entity, block entity, or storage. Requires one of [String] block, [String] entity, or [String] storage. Having more than one is allowed, but only one is used.[note 3]
[Boolean] interpret: Optional, defaults to false. If true, the game attempts to parse the text of each NBT value as a text component. Ignored if [String] nbt is not present.
[NBT Compound / JSON Object] separator: Optional, defaults to {text: ", "}. A text component. Used as the separator between different tags, if the component selects multiple tags.
[String] block: A string specifying the coordinates of the block entity from which the NBT value is obtained. The coordinates can be absolute, relative, or local. Ignored if [String] nbt is not present.
[String] entity: A string specifying the target selector for the entity or entities from which the NBT value is obtained. Ignored if [String] nbt is not present.
[String] storage: A string specifying the resource location of the command storage from which the NBT value is obtained. Ignored if [String] nbt is not present.
Object
Displays objects as single sprites in component messages. Sprites are rendered as 8x8 pixels squares. This will ignore bold and italic styles!

Atlas Object Type
When specifying [String] object as "atlas" it will displays a single sprite from a texture atlas as a character.

[NBT Compound / JSON Object] The text component.
[String] type: Optional. Defaults to "object" if object-specific keys are detected.
[String] object: "atlas" (Optional as it defaults to "atlas" if not specified)
[String] atlas: Optional. The name of texture atlas. Defaults to "minecraft:blocks".
[String] sprite: The sprite name (for example: "item/emerald").
Player Object Type
When specifying [String] object as "player" it will displays the head of a player as a character.

[NBT Compound / JSON Object] The text component.
[String] type: Optional. Defaults to "object" if object-specific keys are detected.
[String] object: "player"
[NBT Compound / JSON Object] player: The minecraft:profile data component to display the player head for.
[String] name: The name of a player profile, i.e. its username. If this is the only tag provided, it resolves into the other ones below. Optional.
[Int Array] id: The UUID of the owner. Used to update the other tags when the chunk loads or the holder logs in, in case the owner's name has changed. Optional.
[String] texture: Namespaced path to the skin texture relative to the textures folder. Optional. If specified overrides the resolved skin or provided properties.
[String] cape: Namespaced path to the cape texture relative to the textures folder. Optional. If specified overrides the resolved skin or provided properties.
[String] model: The model to use. Either "wide" or "slim". Optional. If specified overrides the resolved skin or provided properties.
[NBT List / JSON Array] properties: A list of properties. Optional.
[NBT Compound / JSON Object]: A single property.
[String] name: The name of the property. Can be textures.
[String] value: The texture data json, encoded in base64.
[String] signature: Optional. Mojang's signature of the value, encoded in base64.
[Boolean] hat: Whether to display the hat layer. (Defaults to false)
Component resolution
Certain text content types ([NBT Compound / JSON Object] score, [String] selector, and [String] nbt) do not work in all contexts. These content types need to be resolved, which involves retrieving the appropriate data from the world, rendering it into "simple" text components, and replacing the "advanced" text component with that. This resolution can be done by signs, by written books when they are first opened, by boss bar names, by text displays, and by commands such as /tellraw and /title. It can also be done by the item modifers set_name and set_lore, but only if their [String] entity tag is set. Custom item names, custom entity names and scoreboard objective names cannot by themselves resolve these components,[1] nor can dialogs.[2]

Additionally, resolution fixes a single value in place. Therefore, these content types are not dynamic, and don't update to reflect changes in their environment, while "simple" components usually do.

Click events
Click events control what happens when the player clicks on the text. Can be one of the following:

open_url
Opens the specified URL in the user's default web browser.

[NBT Compound / JSON Object] click_event
[String] action: open_url
[String] url: The URL to open.
open_file
Opens the specified file on the user's computer. This is used in messages automatically generated by the game (e.g., on taking a screenshot) and cannot be sent by servers for security reasons.

[NBT Compound / JSON Object] click_event
[String] action: open_file
[String] path: The file to open.
run_command
Runs the specified command. This runs as if the player typed the specified command in chat and pressed enter. However, this can only be used to run commands that do not send chat messages directly (like /say, /tell, and /teammsg). Since they are being run from chat, the player must have the required permissions.

On signs, the command is run by the server at the sign's location, with the player who used the sign as the command executor (that is, the entity selected by @s). Since they are run by the server, sign commands have the same permission level as a command block instead of using the player's permission level, and are not restricted by chat length limits.

[NBT Compound / JSON Object] click_event
[String] action: run_command
[String] command: The command to run. Does not need to be prefixed with a / slash.
suggest_command
Opens chat and fills in the specified text or command. If a chat message was already being composed, it is overwritten.This does not work in books.[3]

[NBT Compound / JSON Object] click_event
[String] action: suggest_command
[String] command: The command to fill in chat. Also works with normal texts.
change_page
Can only be used in written books. Changes to the specified page if that page exists.

[NBT Compound / JSON Object] click_event
[String] action: change_page
[Int] page: The page to change to.
copy_to_clipboard
Copies the specified text to the clipboard.

[NBT Compound / JSON Object] click_event
[String] action: copy_to_clipboard
[String] value: The text to copy.
show_dialog
Opens the specified dialog.

[NBT Compound / JSON Object] click_event
[String] action: show_dialog
[String][NBT Compound / JSON Object] dialog: One dialog (an [String] ID, or a new [NBT Compound / JSON Object] dialog definition) to display.
custom
Sends a custom event to the server; has no effect on vanilla servers.

[NBT Compound / JSON Object] click_event
[String] action: custom
[String] id: Any ID to identify the event.
[String] payload: Optional payload of the event.
Hover events
Hover events control what happens when the player hovers over the text. Can be one of the following:

show_text
Shows a text component.

[NBT Compound / JSON Object] hover_event
[String] action: show_text
[String][NBT List / JSON Array][NBT Compound / JSON Object] value: Another text component. Can be any valid text component type: string, list, or object. Note that [NBT Compound / JSON Object] click_event and [NBT Compound / JSON Object] hover_event do not function within the tooltip.
show_item
Shows the tooltip of an item as if it was being hovering over it in an inventory.

[NBT Compound / JSON Object] hover_event
[String] action: show_item
[String] id: The item's resource location. Defaults to minecraft:air if invalid.
[Int] count: Optional. Size of the item stack. This typically does not change the content tooltip.
[NBT Compound / JSON Object] components: Optional. Additional information about the item. See Data component format.
show_entity
Shows an entity's name, type, and UUID. Used by [String] selector.

[NBT Compound / JSON Object] hover_event
[String] action: show_entity
[NBT Compound / JSON Object] name: Optional. Hidden if not present. A text that is displayed as the name of the entity.
[String] id: A string containing the type of the entity, as a resource location.
[String][Int Array][NBT List / JSON Array] uuid: The UUID of the entity. Either:
A string representing the UUID in the hyphenated hexadecimal format. Must be a valid UUID.
A list of four numbers representing the UUID in int-array or list format. e.g. [I;1,1,1,1] or [1,1,1,1].
Bedrock Edition
Unlike Java Edition, text components in Bedrock Edition are written in JSON.

[NBT Compound / JSON Object] The root tag.
[NBT List / JSON Array] rawtext: A list contains all text object.
[NBT Compound / JSON Object] To be valid, an object must contain one content tag: [String] text, [String] translate, [NBT Compound / JSON Object] score, or [String] selector. Having more than one is allowed, but only one is used.[note 4]
Content: Plain Text
[String] text: A string containing plain text to display directly.
Content: Translated Text
[String] translate: A translation identifier, to be displayed as the corresponding text in the player's selected language. If no corresponding translation can be found, the identifier itself is used as the translation text. This identifier is the same as the identifiers found in lang files from assets or resource packs.
[NBT List / JSON Array] with: Optional. A list of text component arguments to be inserted into slots in the translation text. Ignored if [String] translate is not present.
Translations can contain slots for text that is not known ahead of time, such as player names. These slots are defined in the translation text itself, not in the text component, and generally take the form %%1 (displays the first argument; replace 1 with whichever index is desired). If no argument is provided for a slot, the slot is not displayed.
Content: Scoreboard Value (requires resolution)
[NBT Compound / JSON Object] score: Displays a score holder's current score in an objective. Displays nothing if the given score holder or the given objective do not exist, or if the score holder is not tracked in the objective.
[String] name: The name of the score holder whose score should be displayed. This can be a selector like @p or an explicit name. If the text is "*", it shows the reader's own score (for example, /tellraw @a { "rawtext" : [ { "score" : { "name" : "*" , "objective" : "obj"} } ] } shows every online player their own score in the "obj" objective).[note 5]
[String] objective: The internal name of the objective to display the player's score in.
Content: Entity Names (requires resolution)
[String] selector: A string containing a selector. Displayed as the name of the player or entity found by the selector. If more than one player or entity is found by the selector, their names are displayed in either the form "Name1 and Name2" or the form "Name1, Name2, Name3, and Name4". Hovering over a name shows a tooltip with the name, type, and UUID of the target. Clicking a player's name suggests a command to whisper to that player. Shift-clicking a player's name inserts that name into chat. Shift-clicking a non-player entity's name inserts its UUID into chat.
Basic raw text example
/tellraw @a { "rawtext" : [ { "text" : "Hello world" } ] }
This sends a message to all players saying "Hello World" in English only. See the Translate action to see how to send localized texts.

Appending
Raw text takes in an array of text objects. Each object in the list is added to the previous object. For example, /tellraw @a { "rawtext" : [ { "text":"Hello" }, { "text" : " World" } ] } outputs the same "Hello World" as the first example. Appending text can be useful to combine 2 different localized texts, or apply different colors to each word etc.

Breaking lines
You can go down a line by using "\n". For example,

/tellraw @a { "rawtext" : [ { "text" : "Hello\nNext line" } ] }

Translate
The translate object allows creators to provide localized text to users. If translate is specified along with text, translate overrides the text object. The string to provide to translate is the name of the string in the language files. For example, in Vanilla Minecraft "commands.op.success" is the string that displays when /op is used on a player successfully.

/tellraw @a { "rawtext": [ { "translate" : "commands.op.success" } ] }

This outputs "Opped %s" to all players. Note that because of text being ignored with translate specified, the following example outputs the same text:

/tellraw @a { "rawtext" : [ { "text":"Hello World", "translate":"commands.op.success" } ] }

With
In the translate example above, it outputs "Opped %s". To have a name or other text show up instead of %s, "with" needs to be specified as well. Note that "with" only works with "translate" and also requires an array [] instead of curly brackets {} .

/tellraw @a { "rawtext": [ { "translate" : "commands.op.success", "with": [ "Steve" ] } ] }

If you want to use a translated text inside the "with" component, instead of an array it needs to be another rawtext component (which consists of an array of text components). The following example outputs "Opped Apple".

/tellraw @a { "rawtext": [ { "translate" : "commands.op.success", "with": { "rawtext": [ { "translate" : "item.apple.name" } ] } } ] }

%%s
"translate" and "%s" can be used without needing a corresponding string in the localization files. For example:

/tellraw @a { "rawtext": [ { "translate" : "Hello %%s", "with": [ "Steve" ] } ] }

This outputs "Hello Steve" to all players.

Multiple %s
%%s can be used multiple times. They are filled in, in the order specified

/tellraw @a { "rawtext": [ { "translate" : "Hello %%s and %%s", "with": [ "Steve", "Alex" ] } ] }

Outputs: "Hello Steve and Alex"

You can again use a rawtext component to replace the plain string array, like so

/tellraw @a { "rawtext": [ { "translate" : "Hello %%s and %%s", "with": { "rawtext" : [ { "text" : "Steve" }, { "translate" : "item.apple.name" } ] } } ] }

Outputs: "Hello Steve and Apple"

Ordering with %%#
The order to fill in %%s can be changed by instead specifying it with %%#, replacing # with an actual number. For example, to swap the position of Steve and Alex in the above example, instead run the following:

/tellraw @a { "rawtext" : [ {"translate" : "Hello %%2 and %%1", "with": [ "Steve", "Alex"] } ] }

Outputs: "Hello Alex and Steve"

Formatting
String formatting is still possible, but not using the text component format used in Java Edition. Instead, formatting codes are used to change text color and style.