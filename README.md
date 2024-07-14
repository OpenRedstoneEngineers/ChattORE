# ChattORE

Because we want to have a chat system that actually wOREks for us.

## Commands

| Command                               | Permission                      | Description                                              | Aliases                                            |
|---------------------------------------|---------------------------------|----------------------------------------------------------|----------------------------------------------------|
| `/chattore version`                   | `chattore.manage`               | View the version of Chattore                             | No aliases                                         |
| `/chattore reload`                    | `chattore.manage`               | Reload Chattore configuration                            | No aliases                                         |
| `/emoji <emoji_names>+`               | `chattore.emoji`                | View multiple emojis                                     | No aliases                                         |
| `/ac <message>`                       | `chattore.helpop`               | Message ORE Staff                                        | No aliases                                         |
| `/mail mailbox`                       | `chattore.mail`                 | Manage your mailbox                                      | `/mailbox\|/mail`                                  |
| `/mail send <player> <message>`       | `chattore.mail`                 | Send a mail message                                      | No aliases                                         |
| `/mail read <mail ID>`                | `chattore.mail`                 | Read a mail message (Designed for usage with `/mailbox`) | No aliases                                         |
| `/me <message>`                       | `chattore.me`                   | Have a thought in chat                                   | No aliases                                         |
| `/message <player> <message>`         | `chattore.message`              | Send a message to a player                               | `/m\|/pm\|/msg\|/vmsg\|/vmessage\|/whisper\|/tell` |
| `/reply <message>`                    | `chattore.message`              | Reply to a message                                       | `/playerprofile`                                   |
| `/profile info <player>`              | `chattore.profile`              | View a player's profile                                  | `/playerprofile`                                   |
| `/profile about <player>`             | `chattore.profile.about`        | Set your about                                           | `/playerprofile`                                   |
| `/profile setabout <player> <about>`  | `chattore.profile.about.others` | Set another player's about                               | `/playerprofile`                                   |
| `/nick color <color>+`                | `chattore.nick`                 | Set your nickname with at least one color (up to three)  | No aliases                                         |
| `/nick presets <presets>`             | `chattore.nick.preset`          | View available presets                                   | No aliases                                         |
| `/nick preset <preset>`               | `chattore.nick.preset`          | Apply a nickname preset                                  | No aliases                                         |
| `/nick nick <player> <nickname>`      | `chattore.nick.others`          | Set a player's nickname                                  | No aliases                                         |
| `/nick remove <player>`               | `chattore.nick.remove`          | Remove a player's nickname                               | No aliases                                         |
| `/nick setgradient <player> <color>+` | `chattore.nick.setgradient`     | Set a gradient for a user                                | No aliases                                         |
