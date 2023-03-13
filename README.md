# OmegaHubPlugin
OmegaHub's main plugin

very bad code, but it works, any contributions are welcome.

### TODO
* [ ] Move all of this crap hardcoded stuff into the config file
* [ ] Make the config file actually work
* [ ] Make it so DataHandler isn't so unneficient (it was coded in a hurry).
* [ ] Make it so the plugin doesn't have to be reloaded every time the config file is changed.
* [ ] Move /gr to a menu.
* [ ] Argon2 is kind of slow, maybe use a different hashing algorithm or find a way to reduce load time.

## Features

#### Discord
* `..chat <text...>` Send a message to the in-game chat
* `..players` a list of the online players
* `..info` gives some info (preview of minimap, number of waves, name of the map)
* `..infores` amount of resources collected
* `..gameover` ends a game
* `..maps` *custom* maps on the server

#### In-game
* `/d <text...>` Send a message to discord.
* `/gr [player] [reason...]` Alert admins on discord if someone is griefing (5 minute cool down)
* `/js [code...]` Send a random dude trying to execute js on your server to 2R2T (/js sandbox)

### Administrative Commands
#### Discord
* `..kick <player>` kick a player
* `..ban <player>` ban a player
* `..modchat <text...>` send a message as a administrator

#### In-game
* `/ping` check the ping of all players (Thanks to ModLib)

### Building the Jar

`gradlew jar` / `./gradlew jar`

Output jar should be in `build/libs`.
