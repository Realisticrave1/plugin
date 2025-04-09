# RavenRPG - Custom Minecraft 1.20.1 RPG Plugin

RavenRPG is a comprehensive RPG plugin for Minecraft 1.20.1 servers that adds custom ravens, races, skills, and an economy system.

## Features

### Custom Economy
- Fully compatible with Essentials and Vault
- Pay other players, check balance, and more
- Admin commands for modifying player balances

### Player Ravens
- Each player can have a personal raven companion
- Ravens appear as floating entities that follow the player
- Multiple raven types with unique abilities (Scout, Guardian, Hunter, Arcane)
- Raven leveling system
- Customizable raven appearance (colors)

### RPG System
- Race selection (Human, Elf, Orc, Vampire, Dwarf)
- Each race has unique passive effects and active abilities
- Custom mana system for using abilities
- Integration with LuckPerms for rank-based features

### Skills System
- Level up skills through gameplay (Mining, Combat, Fishing, Crafting, Magic)
- Unlock special perks and abilities at skill thresholds
- XP gained through normal gameplay activities

### Custom Nametags
- Display player race and name above their head
- Integration with LuckPerms for rank display
- Colorized based on race

## Dependencies

- **Required**:
    - Vault - For economy integration

- **Optional but Recommended**:
    - Essentials - For extended economy features
    - LuckPerms - For rank integration
    - PlaceholderAPI - For extended placeholder support

## Installation

1. Download the RavenRPG.jar file
2. Place it in your server's `plugins` folder
3. Restart your server
4. Edit the configuration in `plugins/RavenRPG/config.yml` if desired
5. Restart again or use `/rpg reload` to apply changes

## Commands

### Raven Commands
- `/raven summon` - Call your raven
- `/raven dismiss` - Dismiss your raven
- `/raven info` - Show information about your raven
- `/raven ability` - Use your raven's special ability
- `/raven color <color>` - Change your raven's color
- `/raven type <type>` - Change your raven's type
- `/raven list` - List available raven types

### Race Commands
- `/race info` - Show information about your race
- `/race select <race>` - Choose a race
- `/race list` - List available races
- `/race ability <ability>` - Use a race ability
- `/race abilities` - List your race's abilities

### Skill Commands
- `/skill list` - List all of your skills
- `/skill info <skill>` - Show detailed skill information

### General RPG Commands
- `/rpg info` - Show info about the RPG system
- `/rpg status` - Show your character status
- `/rpg balance` - Show your current balance
- `/rpg pay <player> <amount>` - Pay another player

### Admin Commands
- `/rpg admin set <player> <type> <value>` - Set player data
- `/rpg admin give <player> <type> <amount>` - Give resources to a player
- `/rpg admin reset <player>` - Reset player data

## Permissions

- `ravenrpg.raven` - Access to raven commands
- `ravenrpg.race` - Access to race commands
- `ravenrpg.skill` - Access to skill commands
- `ravenrpg.rpg` - Access to general RPG commands
- `ravenrpg.admin` - Access to admin commands

## Configuration

The plugin is highly configurable. You can:
- Add or modify races
- Create new raven types
- Adjust skill rewards and XP requirements
- Change economy settings
- Switch between YAML and MySQL storage

See the `config.yml` file for all available options.

## Future Plans

- Add more races and abilities
- Expand the skill system
- Add custom items and crafting
- Add RPG quests and missions
- Add more raven customization options

## Support

For issues, suggestions, or questions, please contact the developer or open an issue on the GitHub repository.

## License

This plugin is licensed under the MIT License. See the LICENSE file for details.