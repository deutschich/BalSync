# BalSync

BalSync is a powerful and reliable balance synchronization system for Minecraft servers. It ensures that player balances are securely stored, automatically updated, and consistently synchronized across your server network.

---

## 🎯 Features

### Automatic Balance Backup
- Player balances are automatically saved to a MySQL database at configurable intervals.
- No manual intervention required – everything runs seamlessly in the background.

### Seamless Login Synchronization
- Player balances are automatically loaded from the database when they join the server.
- Optional: Reset balances to 0 before loading from the database (ideal for events or test servers).

### Real-Time Monitoring
- The plugin regularly checks the database for external changes (e.g., made by admins or other systems).
- Any changes are immediately applied to online players.

### Intelligent Offline Detection
- Changes to a player's balance are recognized even while they are offline.
- Example: If a player earns money in single-player mode, it is updated when they join the server.

### Multi-Language Notifications
- Players are notified of important balance changes.
- Supports 7 languages: German, English, Spanish, French, Polish, Portuguese (Brazil), Russian.

---

## 🎮 Player Experience

- On server join: *"Your balance has been synchronized with the database."*
- On external updates: *"Your balance was updated externally: 100 → 150"*
- No data loss: Balances are always safely stored.
- Server switching supported: Players can move between servers and retain their balances.

---

## 👨‍💼 Admin Commands

| Command                  | Description                             |
|--------------------------|-----------------------------------------|
| `/balsync reload`         | Reloads plugin configuration           |
| `/balsync save`           | Immediately saves all player balances  |
| `/balsync load`           | Reload your own balance from the database |
| `/balsync status`         | Displays system status                  |

---

## ⚙️ Configuration Options

- Set automatic save intervals (e.g., every minute)
- Enable or disable notifications
- Configure database polling intervals
- Set starting balance for new players
- Customize the database table name

---

## 🔒 Security & Performance

- All transactions are logged
- Database connection supports SSL
- Connection pooling for optimal performance
- Fault-tolerant architecture ensures reliability

---

## 📌 Supported Platforms

- Paper
- Spigot
- Purpur
- And other compatible Minecraft server forks

---

## 🚀 Getting Started

1. Place the `BalSync.jar` file into your server's `plugins` folder.
2. Start the server once to generate the default configuration file.
3. Configure your MySQL database credentials and plugin settings in `config.yml`.
4. Restart the server to apply changes.
5. Enjoy secure, automatic balance synchronization for all your players!

---

## 💬 Feedback & Support

If you encounter issues or have feature suggestions, please open an issue on GitHub. Community contributions are welcome!

