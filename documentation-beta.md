# EdToolsPerks - Beta Documentation

![Version](https://img.shields.io/badge/version-1.0.0--beta-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.20+-green)
![Java](https://img.shields.io/badge/java-17+-orange)

**EdToolsPerks** is a powerful Minecraft plugin that adds a gacha-style perk system to EdTools omniptools. Players can roll for random perks that provide permanent boosts to their tools, creating an engaging progression system.

## 🎯 Features

### Core System
- **Gacha Roll System**: Roll for random perks using farm-coins currency
- **Persistent Perks**: Perks are permanently saved to tools via NBT + database
- **Pity System**: Guaranteed rare perks after consecutive rolls without rare drops
- **Multiple Perk Tiers**: Common, Uncommon, Rare, Epic, Legendary perks
- **Visual Feedback**: Animated GUI with roll animations and sound effects

### Perk Categories
- **Money Perks**: Increase money drops from farming/mining
- **Level Perks**: Boost XP gains
- **Coin Perks**: Enhanced farm-coin drops
- **Orb Perks**: Improved orb generation
- **Enchant Perks**: Better enchantment rewards
- **Pass Perks**: Season pass progression boosts
- **Crop Perks**: Agricultural efficiency improvements

### Administrative Features
- **Roll Management**: Give/remove rolls from players
- **Pity Control**: Reset pity counters
- **Sync Commands**: Force synchronization between NBT and database
- **UUID Management**: Regenerate unique tool identifiers
- **Configuration**: Fully configurable perk weights, costs, and messages

## 📋 Requirements

- **Minecraft**: 1.20+
- **Java**: 17+
- **Dependencies**: 
  - EdTools plugin (omniptools)
  - EdTools API integration

## 🔧 Installation

1. Download the latest `EdToolsPerks-1.0.0.jar` from releases
2. Place in your server's `plugins/` folder
3. Ensure EdTools is installed and running
4. Restart your server
5. Configure the plugin in `plugins/EdToolsPerks/`

## ⚙️ Configuration

### Main Config (`config.yml`)
```yaml
# Database settings
database:
  type: "h2"  # Built-in H2 database
  file: "edtoolsperks.db"

# Roll system
rolls:
  default-amount: 5  # Starting rolls for new players
  cost-per-10: 9000  # Farm-coins cost for 10 rolls

# Pity system
pity:
  enabled: true
  guaranteed-at: 90  # Guaranteed rare after 90 rolls
```

### Perk Configuration (`perks/`)
Each perk category has its own YAML file with configurable:
- Display names and descriptions
- Level-based boost amounts
- Rarity weights
- Visual elements (colors, lore)

### GUI Configuration (`guis/`)
Customizable interface layouts:
- Main rolling interface
- Animation sequences
- Button positions and actions
- Visual themes

## 🎮 Usage

### For Players

**Opening the Perk GUI:**
- Hold an EdTool and right-click while sneaking
- Or use `/edtoolsperks` command

**Rolling for Perks:**
1. Click the "Roll" button (costs 1 roll)
2. Watch the animation sequence
3. Receive your random perk
4. Perk is automatically applied to your tool

**Purchasing Rolls:**
- Click "Buy 10 Rolls" button
- Costs 9000 farm-coins by default
- Rolls are account-wide, not tool-specific

### For Administrators

**Commands:**
```bash
# Give rolls to a player
/edtoolsperks give <player> <amount>

# Reset player's pity counter
/edtoolsperks reset <player>

# Force sync tool perk (hold tool)
/edtoolsperks sync

# Regenerate tool UUID (hold tool)
/edtoolsperks regen-uuid

# Reload configuration
/edtoolsperks reload

# Show help
/edtoolsperks help
```

**Permissions:**
- `edtoolsperks.use` - Basic usage (default: true)
- `edtoolsperks.admin` - Administrative commands

## 🔍 Technical Details

### Data Storage
- **Database**: H2 embedded database for persistence
- **NBT Storage**: Perk data stored in tool's NBT for immediate access
- **Dual-layer System**: NBT for fast access, database for persistence

### Architecture
```
EdToolsPerks/
├── commands/          # Command handling
├── config/           # Configuration management
├── database/         # H2 database operations
├── gui/             # Interface and animations
├── integration/     # EdTools API integration
├── listeners/       # Event handling
├── perks/           # Perk system logic
└── utils/           # Utility classes
```

### Synchronization System
The plugin uses a dual-storage approach:
1. **NBT**: Fast access, travels with tool
2. **Database**: Persistent storage, survives server restarts

When conflicts occur (desync), NBT takes priority and updates the database.

### UUID Generation
Each tool receives a unique identifier:
- Generated using `UUID.randomUUID()` + timestamp
- Prevents perk conflicts between similar tools
- Stored in tool's NBT as `edtoolsperks:tool_uuid`

## 🐛 Troubleshooting

### Common Issues

**Tools sharing the same perk:**
- Cause: Duplicate tool UUIDs
- Solution: Use `/edtoolsperks regen-uuid` while holding each affected tool

**Perk not showing in lore:**
- Cause: Lore synchronization issue
- Solution: Use `/edtoolsperks sync` to force refresh

**Database desync detected:**
- Cause: NBT and database mismatch
- Solution: Automatic - NBT takes priority and updates database

**Perk not applying boosts:**
- Cause: EdTools integration issue
- Solution: Check EdTools is running and API is accessible

### Debug Information
Enable debug logging in your server.properties:
```properties
debug=true
```

The plugin logs extensive information about:
- Perk detection and application
- Database operations
- UUID generation
- Synchronization processes

## 🔄 Migration & Updates

### From Previous Versions
1. Backup your database file (`edtoolsperks.db`)
2. Replace the plugin JAR
3. Restart server
4. Run `/edtoolsperks reload`

### UUID Regeneration (if needed)
For tools with conflicting UUIDs:
1. Hold the affected tool
2. Run `/edtoolsperks regen-uuid`
3. Repeat for each affected tool

## 📊 Performance

### Optimizations
- **Async Database Operations**: All DB operations are non-blocking
- **Caching**: Frequently accessed data is cached in memory
- **Batch Operations**: Multiple operations grouped when possible
- **Lazy Loading**: Perks loaded only when needed

### Resource Usage
- **Memory**: ~5-10MB for typical server
- **Database**: Lightweight H2 with minimal overhead
- **CPU**: Minimal impact, async processing

## 🤝 Contributing

### Development Setup
1. Clone the repository
2. Import into your IDE
3. Configure Maven dependencies
4. Build with `mvn clean package`

### API Integration
EdToolsPerks integrates with:
- **EdTools API**: For tool detection and boost application
- **EdTools Currency API**: For farm-coins transactions

### Adding New Perks
1. Create perk configuration in `perks/` directory
2. Define levels, boosts, and rarity
3. Add to perk category enum
4. Register in PerkManager

## 📝 Changelog

### Version 1.0.0-beta (Current)
- ✅ Initial release
- ✅ Complete gacha roll system
- ✅ NBT + Database persistence
- ✅ Pity system implementation
- ✅ Administrative commands
- ✅ UUID conflict resolution
- ✅ Lore synchronization fixes
- ✅ EdTools API integration

### Planned Features
- 🔄 Web dashboard for statistics
- 🔄 Perk trading system
- 🔄 Achievement system
- 🔄 Custom perk creation tools
- 🔄 Multi-server synchronization

## 📞 Support

### Getting Help
- **Discord**: [Your Discord Server]
- **Issues**: [GitHub Issues](https://github.com/your-repo/issues)
- **Documentation**: This file and in-game `/edtoolsperks help`

### Reporting Bugs
Please include:
1. Server version and EdToolsPerks version
2. Error logs from console
3. Steps to reproduce
4. Expected vs actual behavior

### Feature Requests
Open an issue with:
1. Clear description of requested feature
2. Use cases and benefits
3. Any implementation ideas

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

**⚠️ Beta Notice**: This is a beta version. While stable for production use, some features may change in future releases. Always backup your data before updating.

**Made with ❤️ for the Minecraft EdTools community**