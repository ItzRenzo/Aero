# Aero Plugin

A comprehensive trial fly system for Minecraft servers running Paper/Bukkit. Aero provides temporary flight abilities with advanced time management, actionbar displays, voucher system, and extensive customization options.

## Features

### 🚀 Trial Fly System
- **Temporary Flight**: Give players limited flight time that counts down only while flying
- **Time Stacking**: Multiple trial fly grants stack together for extended flight time
- **Persistent Storage**: Flight time persists across server restarts and player disconnections
- **Smart Pausing**: Flight timer only decreases when actively flying, pauses when on ground

### 🎫 Voucher System
- **Flight Vouchers**: Create redeemable items that grant trial fly time
- **Right-click Redemption**: Players can redeem vouchers by right-clicking
- **Batch Creation**: Generate multiple vouchers at once
- **Secure NBT Data**: Vouchers use NBT data to prevent duplication

### 📊 Statistics & Tracking
- **Session Tracking**: Monitor current flight session time
- **Total Time Statistics**: Track lifetime flight usage per player
- **Database Storage**: Supports both SQLite and MySQL databases
- **Auto-save**: Configurable automatic data saving

### 🌍 World Management
- **World Whitelist**: Restrict trial fly to specific worlds
- **Flexible Restrictions**: Configure whether restrictions apply to giving or using flight
- **Cross-world Persistence**: Flight time transfers between allowed worlds

### 🎮 User Experience
- **Actionbar Display**: Real-time countdown display with customizable preferences
- **Warning System**: Configurable time warnings (60s, 30s, 10s, 5s, countdown)
- **Smart Toggling**: Players can toggle flight on/off while maintaining time
- **Safe Landing**: Automatic safe teleportation when flight expires mid-air

## Commands

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/aero tfly give <player> <time>` | Give trial fly time to a player | `aero.tfly.give` |
| `/aero tfly time [player]` | Check trial fly time remaining | `aero.tfly.give` |
| `/aero tfly stats [player]` | View flight statistics | `aero.tfly.give` |
| `/aero tfly voucher <time> [amount] [player]` | Create trial fly vouchers | `aero.tfly.voucher` |

### Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/tfly` | Toggle flight on/off | `aero.tfly.toggle` |
| `/tfly time` | Check remaining flight time | `aero.tfly.toggle` |
| `/tfly stats` | View your flight statistics | `aero.tfly.toggle` |
| `/tfly actionbar` | Toggle actionbar countdown display | `aero.tfly.toggle` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `aero.use` | Basic plugin access | `op` |
| `aero.tfly.give` | Give trial fly and manage others | `op` |
| `aero.tfly.toggle` | Toggle own flight and view stats | `true` |
| `aero.tfly.voucher` | Create trial fly vouchers | `op` |

## Installation

1. **Download** the latest `aero-1.0.jar` from releases
2. **Place** the jar file in your server's `plugins` folder
3. **Restart** your server
4. **Configure** the plugin using the generated config files

## Configuration

### Database Configuration
```yaml
database:
  type: "sqlite"  # "sqlite" or "mysql"
  sqlite:
    filename: "aero.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "aero"
    username: "your_username"
    password: "your_password"
```

### Plugin Settings
```yaml
settings:
  auto_save_interval: 300    # Auto-save every 5 minutes
  debug: false              # Enable debug logging
  actionbar_countdown: true # Default actionbar setting
```

### World Restrictions
```yaml
world_restrictions:
  enabled: false           # Enable world whitelist
  whitelisted_worlds:      # Allowed worlds
    - "world"
    - "world_nether"
  restrict_giving: true    # Check restrictions when giving flight
```

## Usage Examples

### Giving Trial Fly
```
/aero tfly give PlayerName 300    # Give 5 minutes of flight
/aero tfly give PlayerName 1800   # Give 30 minutes of flight
```

### Creating Vouchers
```
/aero tfly voucher 600           # Create 1 voucher for 10 minutes
/aero tfly voucher 300 5         # Create 5 vouchers for 5 minutes each
/aero tfly voucher 1200 3 Player # Give 3 vouchers (20min each) to Player
```

### Player Usage
```
/tfly                    # Toggle flight on/off
/tfly time              # Check remaining time
/tfly actionbar         # Toggle countdown display
```

## Messages Customization

All plugin messages are fully customizable in `messages.yml`. The plugin supports:
- **Color Codes**: Use `&` codes for colors (e.g., `&a` for green)
- **Placeholders**: Dynamic content like `{player}`, `{time}`, etc.
- **Multi-language Support**: Easy translation by editing message values

## Database Schema

The plugin automatically creates the following tables:

### `aero_players`
- `uuid` (VARCHAR) - Player UUID
- `session_time` (INT) - Current session flight time
- `total_fly_time` (BIGINT) - Lifetime flight time used

## API Integration

Aero provides a simple API for other plugins to interact with:

```java
// Check if player has active trial fly
boolean hasTrialFly = plugin.hasTrialFly(player);

// Get remaining flight time
int remainingTime = plugin.getRemainingFlyTime(player);

// Give trial fly time
plugin.giveTrialFly(player, timeInSeconds, commandSender);
```

## Requirements

- **Minecraft**: 1.21+ (Paper/Bukkit)
- **Java**: 21+
- **Dependencies**: None (uses Paper API)

## Building from Source

1. **Clone** the repository
2. **Run** `mvn clean package`
3. **Find** the compiled jar in `target/aero-1.0.jar`

## Support

For issues, feature requests, or questions:
- Create an issue on GitHub
- Join our Discord server
- Check the wiki for detailed guides

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Credits

**Author**: ItzRenzo  
**Version**: 1.0  
**API**: Paper 1.21.1