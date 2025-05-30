# Aero Plugin

A comprehensive trial fly system for Minecraft servers running Paper/Bukkit. Aero provides temporary flight abilities with advanced time management, actionbar displays, voucher system, economy integration, and extensive customization options.

## Features

### 🚀 Trial Fly System
- **Temporary Flight**: Give players limited flight time that counts down only while flying
- **Time Stacking**: Multiple trial fly grants stack together for extended flight time
- **Persistent Storage**: Flight time persists across server restarts and player disconnections
- **Smart Pausing**: Flight timer only decreases when actively flying, pauses when on ground

### 💰 Economy Integration
- **Vault Support**: Full integration with Vault-compatible economy plugins
- **Multiple Currency Types**: Support for Vault money, experience points, or player levels
- **Automatic Detection**: Seamlessly detects and connects to installed economy plugins
- **Customizable Formatting**: Configure currency symbols and display formats via messages.yml
- **Transaction Safety**: Secure money handling with proper balance checking

### 🛒 Trial Fly Shop
- **Interactive GUI**: Beautiful, customizable shop interface for purchasing flight time
- **Configurable Items**: Set up multiple flight packages with different durations and prices
- **Smart Pricing**: Automatically displays prices in the correct currency format
- **Real-time Balance**: Checks player funds before allowing purchases
- **Purchase Feedback**: Clear success/error messages for all transactions

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
- **Non-italic Text**: Clean, readable item names and GUI text

## Commands

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/aero tfly give <player> <time>` | Give trial fly time to a player | `aero.tfly.give` |
| `/aero tfly time [player]` | Check trial fly time remaining | `aero.tfly.give` |
| `/aero tfly stats [player]` | View flight statistics | `aero.tfly.give` |
| `/aero tfly voucher <time> [amount] [player]` | Create trial fly vouchers | `aero.tfly.voucher` |
| `/aero tfly shop [player]` | Open trial fly shop for a player | `aero.tfly.shop` |
| `/aero reload` | Reload plugin configuration | `aero.reload` |

### Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/tfly` | Toggle flight on/off | `aero.tfly.toggle` |
| `/tfly time` | Check remaining flight time | `aero.tfly.toggle` |
| `/tfly stats` | View your flight statistics | `aero.tfly.toggle` |
| `/tfly actionbar` | Toggle actionbar countdown display | `aero.tfly.toggle` |
| `/tfly shop` | Open the trial fly shop | `aero.tfly.shop` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `aero.use` | Basic plugin access | `op` |
| `aero.tfly.give` | Give trial fly and manage others | `op` |
| `aero.tfly.toggle` | Toggle own flight and view stats | `true` |
| `aero.tfly.voucher` | Create trial fly vouchers | `op` |
| `aero.tfly.shop` | Access the trial fly shop | `true` |
| `aero.reload` | Reload plugin configuration | `op` |

## Installation

1. **Download** the latest `aero-1.0.jar` from releases
2. **(Optional)** Install a Vault-compatible economy plugin (EssentialsX, CMI, etc.)
3. **Place** the jar file in your server's `plugins` folder
4. **Restart** your server
5. **Configure** the plugin using the generated config files

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

### Shop Configuration
```yaml
shop:
  gui:
    title: "&b&lTrial Fly Shop"
    size: 27                        # GUI size (9, 18, 27, 36, 45, 54)
    fill-empty: true               # Fill empty slots
    filler-material: "GRAY_STAINED_GLASS_PANE"
    close-after-purchase: true     # Auto-close after buying
  
  currency:
    type: "vault"                  # "vault", "exp", or "levels"
  
  items:                           # List of shop items
    - "flight_5min"
    - "flight_10min"
    - "flight_30min"
    - "flight_1hour"
  
  items-config:
    flight_5min:
      slot: 10
      material: "FEATHER"
      time: 300                    # 5 minutes in seconds
      price: 50                    # Price in selected currency
    
    flight_10min:
      slot: 12
      material: "ELYTRA"
      time: 600                    # 10 minutes
      price: 90
```

## Economy Integration

### Supported Currency Types
- **Vault Money**: Integrates with any Vault-compatible economy plugin
- **Experience Points**: Uses player's experience points as currency
- **Player Levels**: Uses player's experience levels as currency

### Vault Setup
1. Install a Vault-compatible economy plugin (EssentialsX, CMI, etc.)
2. Install Vault plugin
3. Set shop currency type to "vault" in config.yml
4. Restart server - Aero will automatically detect and connect

### Custom Currency Formatting
Customize how prices are displayed in `messages.yml`:
```yaml
shop:
  item:
    name: "<!italic>&e&l{time}s Trial Fly &7- &a${price}"  # Change $ to any symbol
    lore:
      price: "<!italic>&7Price: &a${price}"                # Customize currency display
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

### Shop Management
```
/aero tfly shop              # Open shop for yourself
/aero tfly shop PlayerName   # Open shop for another player
/aero reload                 # Reload config after changes
```

### Player Usage
```
/tfly                    # Toggle flight on/off
/tfly time              # Check remaining time
/tfly shop              # Open the flight shop
/tfly actionbar         # Toggle countdown display
```

## Messages Customization

All plugin messages are fully customizable in `messages.yml`. The plugin supports:
- **Color Codes**: Use `&` codes for colors (e.g., `&a` for green)
- **Placeholders**: Dynamic content like `{player}`, `{time}`, `{price}`, etc.
- **Multi-language Support**: Easy translation by editing message values
- **Text Formatting**: Use `<!italic>` to disable italic text on items
- **Currency Symbols**: Fully customizable currency display formats

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

// Check Vault integration
boolean vaultEnabled = plugin.getVaultManager().isVaultEnabled();

// Check player balance (if Vault enabled)
double balance = plugin.getVaultManager().getBalance(player);
```

## Requirements

- **Minecraft**: 1.21+ (Paper/Bukkit)
- **Java**: 21+
- **Dependencies**: None (Vault optional for economy features)
- **Compatible Economy Plugins**: EssentialsX, CMI, Towny, any Vault-compatible plugin

## Building from Source

1. **Clone** the repository
2. **Run** `mvn clean package`
3. **Find** the compiled jar in `target/aero-1.0.jar`

## Changelog

### Version 1.0
- ✅ Core trial fly system with time management
- ✅ Voucher system with NBT security
- ✅ Statistics tracking and database storage
- ✅ World restrictions and management
- ✅ Actionbar countdown display
- ✅ Vault economy integration
- ✅ Interactive shop GUI system
- ✅ Multiple currency support (Vault/XP/Levels)
- ✅ Customizable messages and formatting
- ✅ Reload command and hot-swapping
- ✅ Clean, non-italic text formatting

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
**Economy**: Vault API Integration