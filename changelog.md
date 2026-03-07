

## New Feature: Action Bar Messages

You can now choose whether BalSync messages appear in chat or the action bar!  
Simply set `message-display: "actionbar"` in your `config.yml` (under `settings`).  
Keep it as `"chat"` (default) for the classic experience.

This works for all player notifications, including:
- Balance sync on join
- External balance changes
- Command responses (`/balsync reload`, etc.)

Enjoy a cleaner chat experience!

## Added
- New config option `settings.log-save-all-messages` (default: `true`) to control whether the console messages "Saving all player balances to database..." and "Saved X player balances to database." are printed. Set to `false` to silence them.
