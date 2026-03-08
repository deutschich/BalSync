# BalSync 1.3.1

## Permission System and Hotfix Update

## Updated config.yml
I have used AI for one of the first Updates and the AI deleted the Database Settings from the config.yml. I recovered it because I discovered this issue myself.

The new Part:
```yaml
# Database Configuration
database:
  host: "localhost"
  port: 3306
  database: "minecraft"
  username: "root"
  password: "password"
  use-ssl: false
  connection-pool:
    maximum-pool-size: 10
    minimum-idle: 5
    connection-timeout: 30000
    idle-timeout: 600000
```

### New Feature: Per-Player Sync Control

We've added a new permission node `balsync.sync` (default: `true`) that lets you control which players are automatically synchronized with the database.

- **Players with `balsync.sync: true`** (default) will have their balance loaded on join and receive external balance updates as usual.
- **Players with `balsync.sync: false`** will **not** be synchronized – their balance will not be loaded when they join, and external database changes will be ignored for them.

This allows server admins to exclude specific players (e.g., bots, restricted accounts) from the synchronization process without affecting the core functionality.

All admin commands (`/balsync reload`, `save`, `load`, `status`) remain protected by the `balsync.admin` permission (default: `op`). Regular players still cannot execute these commands.

No configuration changes are required – simply assign the permission via your permissions plugin (e.g., LuckPerms) to opt players out.