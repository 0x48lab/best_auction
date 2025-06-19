# 🏪 Best Auction Plugin

A comprehensive and feature-rich auction house plugin for Minecraft Paper/Spigot servers, designed to create vibrant player-driven economies with complete auction functionality. Inspired by popular MMORPG auction systems, this plugin provides a professional-grade trading experience.

## ✨ Key Features

### 🏪 Complete Auction System
- **Item Listing**: Sell items with custom starting prices and buy-it-now options
- **Bidding System**: Competitive bidding with automatic price validation
- **Buy-It-Now**: Instant purchase option for immediate transactions
- **Auction Management**: Full control over auction duration and fees
- **Real-Time Countdown**: Live auction end time display

### 💰 Vault Economy Integration
- **Universal Economy Support**: Works with any Vault-compatible economy plugin
- **Popular Plugin Compatibility**: 
  - EssentialsX Economy
  - iConomy
  - BOSEconomy
  - MultiCurrency
  - GemsEconomy
  - PlayerPoints
  - TokenEnchant
  - And many more!
- **Automatic Currency Detection**: Automatically detects and uses your server's currency
- **Transaction Security**: Safe and reliable money handling with Vault's proven API
- **Fee System**: Configurable listing fees and transaction costs
- **K/M/B Notation**: Large amounts displayed as 1.5K, 2.3M, etc.
- **Currency Formatting**: Respects your economy plugin's formatting settings

### 📬 Mail System
- **Automatic Delivery**: Items are automatically delivered to winners
- **Refund System**: Automatic refunds for outbid players
- **Mailbox Interface**: Easy access to received items and refunds
- **Notification System**: Real-time updates on auction status

### 🌐 Multi-Language Support
- **English & Japanese**: Complete localization for both languages
- **Cultural Adaptation**: Proper date formats and number displays
- **Easy Language Switching**: In-game language selection
- **Extensible System**: Easy to add more languages

### 🔍 Advanced Search & Filtering
- **Category Search**: Browse items by category (weapons, tools, etc.)
- **Keyword Search**: Find specific items quickly
- **Price Filtering**: Search within price ranges
- **Status Filtering**: View active, ended, or your own auctions

### ☁️ Cloud Integration & REST API
- **Real-Time Web Interface**: View auctions at [best-auction-cloud.masafumi-t.workers.dev](http://best-auction-cloud.masafumi-t.workers.dev/)
- **REST API**: Build your own auction websites and applications
- **Cross-Server Support**: Multiple servers can share auction data
- **Event Streaming**: Real-time auction events via HTTP
- **Data Synchronization**: Automatic backup and sync capabilities

### ⚙️ Advanced Configuration
- **Flexible Settings**: Customize auction duration, fees, and limits
- **UI Customization**: Adjust display formats and layouts
- **Debug Controls**: Development and testing features
- **Performance Options**: Optimize for your server size

## 🛠️ Technical Specifications

### Requirements
- **Minecraft**: 1.20.1+
- **Server Type**: Paper/Spigot
- **Java**: 17+
- **Dependencies**: 
  - **Vault** (Required for economy integration)
  - **Economy Plugin** (EssentialsX, iConomy, etc.)

### Architecture
- **Language**: Kotlin 1.9.24
- **Database**: SQLite with Exposed ORM
- **Build System**: Gradle with Kotlin DSL
- **API**: PaperMC 1.20.6
- **Cloud**: Cloudflare Workers integration
- **Economy**: Vault API integration

## 🎮 Commands

### Basic Commands

`/ah` - Open auction house GUI

`/ah sell <price> [buyout]` - List item in hand for auction

`/ah bid <id> <amount>` - Place bid on an item

`/ah cancel <id>` - Cancel your auction listing

`/ah search <keyword>` - Search for items

`/ah mail` - Open mailbox

`/ah confirm` - Confirm cancellation or buyout

`/ah language` - Change language settings

`/ah help` - Show help

### Admin Commands

`/ah cloud sync [force]` - Synchronize data to cloud

`/ah cloud status` - Show cloud status

`/ah cloud validate` - Validate API token

`/ah cloud gettoken` - Get token management URL

`/ah cloud settoken <token>` - Set cloud API token

`/ah testdata [count]` - Generate test data (debug mode)

## 📊 Features Overview

| Feature | Description |
|---------|-------------|
| **Auction System** | Complete bidding and selling functionality |
| **Vault Integration** | Universal economy plugin support |
| **Mail System** | Automatic item delivery and refunds |
| **Search** | Advanced filtering and search capabilities |
| **Multi-Language** | English and Japanese support |
| **Economy Integration** | Vault API compatibility |
| **Real-Time Updates** | Live countdown and status updates |
| **Cloud Integration** | Web interface and REST API |
| **Flexible Configuration** | Extensive customization options |
| **Database Storage** | Reliable SQLite data persistence |

## 🚀 Installation

1. **Install Vault**: Make sure Vault is installed on your server
2. **Install Economy Plugin**: Install your preferred economy plugin (EssentialsX, iConomy, etc.)
3. **Download** the plugin from CurseForge
4. **Place** the JAR file in your `plugins/` folder
5. **Restart** your server
6. **Configure** the plugin in `plugins/BestAuction/config.yml`
7. **Enjoy** your new auction house!

## 🔧 Configuration Example

**Language Settings:**
`language: "en"` - Set language (en, ja)

**Auction Settings:**
`default_duration: 228` - Auction duration in hours (9.5 days)

`listing_fee_rate: 0.05` - Listing fee percentage (5%)

`min_listing_fee: 10` - Minimum listing fee

`max_listing_fee: 1000` - Maximum listing fee

`max_player_listings: 7` - Maximum items per player

**Currency Settings (Vault Integration):**
`use_vault_format: true` - Use Vault formatting (recommended)

`fallback_currency: "gil"` - Fallback currency name

`currency_symbol: ""` - Currency symbol (if not using Vault)

**UI Settings:**
`items_per_page: 45` - Items per page

`date_format: "yyyy-MM-dd"` - Date display format

**Cloud Integration:**
`enabled: false` - Enable cloud features

`base_url: "https://your-worker.your-subdomain.workers.dev"` - Cloudflare Workers URL

`api_token: ""` - Your API token

`server_id: "default-server"` - Server identifier

## 💰 Vault Integration Details

### Supported Economy Plugins
The plugin works seamlessly with any Vault-compatible economy plugin:

**Popular Options:**
- **EssentialsX Economy** - Most popular choice
- **iConomy** - Classic economy plugin
- **BOSEconomy** - Bukkit economy
- **MultiCurrency** - Multiple currency support
- **GemsEconomy** - Modern economy solution
- **PlayerPoints** - Points-based economy
- **TokenEnchant** - Token-based economy
- **ChestShop** - Shop-based economy
- **And many more!**

### Vault Features
- **Automatic Detection**: No manual configuration needed
- **Currency Formatting**: Respects your economy plugin's settings
- **Transaction Safety**: Uses Vault's secure transaction methods
- **Balance Checking**: Real-time balance validation
- **Multi-Server Support**: Works across multiple servers with shared economy

### Economy Plugin Setup
1. **Install Vault** first
2. **Install your preferred economy plugin**
3. **Configure the economy plugin** (EssentialsX, iConomy, etc.)
4. **Install Best Auction Plugin**
5. **The plugin will automatically detect and use your economy**

## ☁️ Cloud Integration Setup

### Web Interface
Visit [best-auction-cloud.masafumi-t.workers.dev](http://best-auction-cloud.masafumi-t.workers.dev/) to:
- View real-time auction data
- Browse items across multiple servers
- Monitor market trends
- Access auction history

### REST API
The plugin provides a complete REST API for:
- **Auction Data**: Get current auctions, bids, and history
- **Event Streaming**: Real-time auction events
- **Data Synchronization**: Backup and restore capabilities
- **Cross-Server Integration**: Share auction data between servers

### API Endpoints

`GET /api/auctions` - List all auctions

`GET /api/auctions/{id}` - Get specific auction

`GET /api/servers` - List connected servers

`POST /api/events` - Send auction events

`GET /api/sync` - Synchronize data

## 🌟 Why Choose Best Auction Plugin?

### **Complete Solution**
Everything you need for a thriving player economy in one plugin.

### **Universal Economy Support**
Works with any Vault-compatible economy plugin - no vendor lock-in!

### **Professional Design**
Inspired by popular MMORPG auction systems for familiar user experience.

### **Cloud Integration**
Real-time web interface and REST API for modern server management.

### **User-Friendly**
Intuitive GUI designed specifically for Minecraft players.

### **Highly Configurable**
Adapt to any server's needs with extensive configuration options.

### **Reliable & Secure**
Built with modern development practices and comprehensive error handling.

### **Multi-Language**
Perfect for international servers with English and Japanese support.

### **Active Development**
Regular updates and improvements based on community feedback.

## 📚 Documentation

For detailed setup instructions, configuration options, and troubleshooting, visit our [GitHub repository](https://github.com/0x48lab/best_auction).

## 🤝 Support

- **Bug Reports**: [GitHub Issues](https://github.com/0x48lab/best_auction/issues)
- **Feature Requests**: [GitHub Discussions](https://github.com/0x48lab/best_auction/discussions)
- **Documentation**: [Plugin Documentation](https://github.com/0x48lab/best_auction/blob/main/PLUGIN_DOCUMENTATION.md)
- **Web Interface**: [best-auction-cloud.masafumi-t.workers.dev](http://best-auction-cloud.masafumi-t.workers.dev/)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](https://github.com/0x48lab/best_auction/blob/main/LICENSE) file for details.

## 🙏 Acknowledgments

This plugin was developed with the assistance of [Claude Code](https://claude.ai/code), showcasing the power of AI-assisted development in creating high-quality Minecraft plugins.

---

**Transform your Minecraft server's economy with the most comprehensive auction house plugin available, featuring universal Vault integration and cloud API support!** 