# ChattORE

Because we want to have a chat system that actually wOREks for us.

## Messaging Commands

| Command                         | Permission                | Description                                              | Aliases                                            |
|---------------------------------|---------------------------|----------------------------------------------------------|----------------------------------------------------|
| `/ac <message>`                 | `chattore.helpop`         | Message ORE Staff                                        | No aliases                                         |
| `/confirmmessage`               | `chattore.confirmmessage` | Confirm a flagged chat message                           | No aliases                                         |
| `/mail mailbox`                 | `chattore.mail`           | Manage your mailbox                                      | `/mailbox\|/mail`                                  |
| `/mail send <player> <message>` | `chattore.mail`           | Send a mail message                                      | No aliases                                         |
| `/mail read <mail ID>`          | `chattore.mail`           | Read a mail message (Designed for usage with `/mailbox`) | No aliases                                         |
| `/message <player> <message>`   | `chattore.message`        | Send a message to a player                               | `/m\|/pm\|/msg\|/vmsg\|/vmessage\|/whisper\|/tell` |
| `/reply <message>`              | `chattore.message`        | Reply to a message                                       | `/playerprofile`                                   |

## Funcommands Commands

| Command                       | Permission             | Description                         | Aliases |
|-------------------------------|------------------------|-------------------------------------|---------|
| `/funcommands`                | `chattore.funcommands` | Display current Funcommands version | `fc`    |
| `/funcommands list`           | `chattore.funcommands` | Display list of all Funcommands     | `fc`    |
| `/funcommands info <command>` | `chattore.funcommands` | Display description of <command>    | `fc`    |

## Nickname Commands

| Command                               | Permission                  | Description                                             | Aliases    |
|---------------------------------------|-----------------------------|---------------------------------------------------------|------------|
| `/nick color <color>+`                | `chattore.nick`             | Set your nickname with at least one color (up to three) | No aliases |
| `/nick presets`                       | `chattore.nick.preset`      | View available presets                                  | No aliases |
| `/nick preset <preset>`               | `chattore.nick.preset`      | Apply a nickname preset                                 | No aliases |
| `/nick nick <player> <nickname>`      | `chattore.nick.others`      | Set a player's nickname                                 | No aliases |
| `/nick remove <player>`               | `chattore.nick.remove`      | Remove a player's nickname                              | No aliases |
| `/nick setgradient <player> <color>+` | `chattore.nick.setgradient` | Set a gradient for a user                               | No aliases |

## Profile Commands

| Command                              | Permission                      | Description                | Aliases          |
|--------------------------------------|---------------------------------|----------------------------|------------------|
| `/profile info <player>`             | `chattore.profile`              | View a player's profile    | `/playerprofile` |
| `/profile about <player>`            | `chattore.profile.about`        | Set your about             | `/playerprofile` |
| `/profile setabout <player> <about>` | `chattore.profile.about.others` | Set another player's about | `/playerprofile` |

## Bubble Commands

| Command                            | Permission               | Description                                            | Aliases                |
|------------------------------------|--------------------------|--------------------------------------------------------|------------------------|
| `/bubble create [players]`         | `chattore.bubble`        | Create ("blow") a bubble and invite players            | `/bb create\|/bb blow` |
| `/bubble invite <players>`         | `chattore.bubble`        | Invite players to your bubble (if it is private)       | `/bb invite`           |
| `/bubble join <player>`            | `chattore.bubble`        | Join a player's bubble                                 | `/bb join`             |
| `/bubble leave`                    | `chattore.bubble`        | Leave your current bubble                              | `/bb leave`            |
| `/bubble delete`                   | `chattore.bubble`        | Delete ("pop") your own bubble                         | `/bb delete\|/bb pop`  |
| `/bubble kick <player>`            | `chattore.bubble`        | Kick a player from your own bubble                     | `/bb kick`             |
| `/bubble setprivate <boolean>`     | `chattore.bubble`        | Set the visibility of your own bubble                  | `/bb setprivate`       |
| `/bubble list`                     | `chattore.bubble`        | List all bubbles                                       | `/bb list`             |
| `/shout <message>`                 | `chattore.bubble`        | Send a message to global chat when in a bubble         | No aliases             |
| `/bubble burst <player>`           | `chattore.bubble.manage` | Burst (delete) someone's bubble                        | `/bb burst`            |
| `/bubble showglobalchat <boolean>` | `chattore.bubble`        | Control the visibility of global chat when in a bubble | `/bb sgc`              |

## Other Commands

| Command                 | Permission            | Description                   | Aliases    |
|-------------------------|-----------------------|-------------------------------|------------|
| `/chattore reload`      | `chattore.manage`     | Reload Chattore configuration | No aliases |
| `/commandspy`           | `chattore.commandspy` | Toggle spying on commands     | No aliases |
| `/chattore version`     | `chattore.manage`     | View the version of Chattore  | No aliases |
| `/emoji <emoji_names>+` | `chattore.emoji`      | View multiple emojis          | No aliases |
