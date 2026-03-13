# BalSync 1.4

## New Feature: Update Checker
>**Update Checker**: BalSync now automatically checks for new versions on Modrinth. When an update is available and compatible with your server, players with the `balsync.update.notify` permission (OP by default) will receive a clickable notification upon joining. You can disable this feature in `config.yml` (`settings.check-for-updates: false`).

## New Feature: Rollback from Backups
>
>BalSync now allows you to restore player balances from previously created backup files!
>
>- **List available backups:**  
>  `/balsync backups` – shows all backup files with timestamps.
>
>- **Rollback all players or a single player:**  
>  `/balsync rollback <filename> [player]`
>    - If you specify a player name, only that player's balance is restored.
>    - If you omit the player, all players in the backup are restored.
>
>The rollback runs asynchronously and updates both the database and online players' balances instantly. The plugin’s internal tracking is kept consistent to avoid any conflicts with auto-save or polling.
>
>**Example:**  
`/balsync rollback backup-2025-03-13-12-30-00.json` – restores all balances from that backup.  
`/balsync rollback backup-2025-03-13-12-30-00.json Steve` – restores only Steve's balance.
>
>Make sure to enable backups in your `config.yml` (`backup.enabled: true`) and set the interval to your preference. Backups are stored in the `backups/` folder inside the plugin directory.
>
>*Note:* Rollback requires the `balsync.admin` permission (default OP).