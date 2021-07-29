# BungeeSafeguard

A blacklist and whitelist BungeeCord plugin with support of UUID look up.

This plugin was formerly named BungeeGuard. In order not to conflict with the existing plugin BungeeGuard, this plugin is renamed to BungeeSafeguard from v2.0.

Tested on Waterfall, version: `git:Waterfall-Bootstrap:1.17-R0.1-SNAPSHOT:93773f9:448`.

- [BungeeSafeguard](#bungeesafeguard)
  - [Features](#features)
  - [Usage](#usage)
  - [New Features of v3.0 and How To Migrate](#new-features-of-v30-and-how-to-migrate)
  - [Migrate to v2.0](#migrate-to-v20)
  - [XBOX Live Player Support (Bedrock Edition Support)](#xbox-live-player-support-bedrock-edition-support)
  - [Replaceable Storage Backend for Lists](#replaceable-storage-backend-for-lists)
  - [Optional Extension Plugins](#optional-extension-plugins)
  - [Configuration](#configuration)
  - [Commands](#commands)
    - [Whitelist](#whitelist)
      - [whitelist add](#whitelist-add)
      - [whitelist x-add](#whitelist-x-add)
      - [whitelist lazy-add](#whitelist-lazy-add)
      - [whitelist remove](#whitelist-remove)
      - [whitelist x-remove](#whitelist-x-remove)
      - [whitelist lazy-remove](#whitelist-lazy-remove)
      - [whitelist import](#whitelist-import)
      - [whitelist on](#whitelist-on)
      - [whitelist off](#whitelist-off)
      - [whitelist confirm](#whitelist-confirm)
      - [whitelist list](#whitelist-list)
    - [Blacklist](#blacklist)
      - [blacklist add](#blacklist-add)
      - [blacklist x-add](#blacklist-x-add)
      - [blacklist lazy-add](#blacklist-lazy-add)
      - [blacklist remove](#blacklist-remove)
      - [blacklist x-remove](#blacklist-x-remove)
      - [blacklist lazy-remove](#blacklist-lazy-remove)
      - [blacklist import](#blacklist-import)
      - [blacklist on](#blacklist-on)
      - [blacklist off](#blacklist-off)
      - [blacklist confirm](#blacklist-confirm)
      - [blacklist list](#blacklist-list)
    - [Main Command](#main-command)
      - [bungeesafeguard load](#bungeesafeguard-load)
      - [bungeesafeguard reload](#bungeesafeguard-reload)
      - [bungeesafeguard status](#bungeesafeguard-status)
      - [bungeesafeguard dump](#bungeesafeguard-dump)
      - [bungeesafeguard import](#bungeesafeguard-import)
      - [bungeesafeguard merge](#bungeesafeguard-merge)
      - [bungeesafeguard export](#bungeesafeguard-export)
  - [Permission Nodes](#permission-nodes)
  - [Lazy Lists](#lazy-lists)
  - [Operation Confirmation](#operation-confirmation)
  - [Important Notes](#important-notes)

## Features

* First **UUID-based** blacklist and whitelist plugin for BungeeCord
* Add and remove players by their **username** or UUID (username-change-proof)
* Add and remove XBOX Live players by their **Gamer Tag** or UUID (Gamertag-change-proof) (from v2.3, see [XBOX Live Player Support](#xbox-live-player-support-bedrock-edition-support) for more details)
* Lazy translation from username to UUID (from v1.2, **offline server friendly**, see [lazy lists](#lazy-lists) for details)
* Switch between multiple configuration files (e.g., a config for maintenance mode which whitelists administrators only; from v2.4)
* Optional confirmation before adding or removing list entries (from v2.4, see [Operation Confirmation](#operation-confirmation) for more details)
* Import from old `whitelist.json` or `banned-players.json` (from v2.5, resolves issue #7; see [whitelist import](#whitelist-import) and [blacklist import](#blacklist-import) for more details)
* API support backed by (possibly) well-structured and documented code base (from v3.0; see [Developing Extension Plugin for BungeeSafeguard](./developer.md) for more details)
  * Manage the lists via a GUI Web interface (TODO)
  * SQL database storage support (TODO)
  * Redis storage support (TODO)

## Usage

Download pre-compiled jar file from [release page](https://github.com/Luluno01/BungeeSafeguard/releases). Put downloaded jar file under `<path/to/BungeeCord/plugins>`.

## New Features of v3.0 and How To Migrate

If you are a developer who has been using the unannounced API, breaking changes in v3.0:

* Internal package name (from `vip.untitled.bungeeguard` to `cyou.untitled.bungeesafeguard`)
* Other massive internal refactoring

Otherwise, v3.0 is a major remastered version with backward compatibility. **You don't need to do anything to upgrade from older versions**.
However, you may want to know some new features brought by v3.0.

For **normal users**, we have the followings:

* [Replaceable Storage Backend for Lists](#replaceable-storage-backend-for-lists)
* [Optional Extension Plugins](#optional-extension-plugins)

For **developers** who **want to interact with BungeeSafeguard** gracefully, please refer to [Developing Extension Plugin for BungeeSafeguard](./developer.md).

## Migrate to v2.0

Breaking changes in v2.0:

* Plugin name (from BungeeGuard to BungeeSafeguard)
* Internal package name (from `vip.untitled.bungeeguard` to `vip.untitled.bungeesafeguard`)
* Internal class names
* Main command name (from `bungeesafeguard` to `bungeesafeguard`, from `bg` to `bsg`)
* Configuration directory (from `plugins/BungeeGuard` to `plugins/BungeeSafeguard`)

To migrate to v2.0 from lower versions, do the following:

1. Remove old plugin jar file
2. Install new plugin jar file
3. Rename old `plugins/BungeeGuard` directory to `plugins/BungeeSafeguard`
4. Update assigned permissions, change `bungeeguard` to `bungeesafeguard` in permission nodes

You are now good to go.

## XBOX Live Player Support (Bedrock Edition Support)

Since version `v2.3`, BungeeSafeguard now supports automatic conversion from XBOX Live Gamer Tag to Minecraft-compatible UUID following the conversion rule as defined by [Geyser](https://geysermc.org/). This new feature hopefully resolves the issue #5.
XBOX Live Gamer Tags are now added via the command [`x-add`](#whitelist-x-add) and removed via the command [`x-rm`](#whitelist-x-remove).
There is no need to implement lazy lists for XBOX Live players because current lazy lists are compatible with XBOX Live players.

Note that you need to specify an [`xbl-web-api`](https://github.com/Prouser123/xbl-web-api) instance (you can either deploy your own or use the public one provided by [`xbl-web-api`](https://xbl-api.prouser123.me/)) by setting its URL as the value of the configuration entry `xbl-web-api` (see section [Configuration](#configuration)).

## Replaceable Storage Backend for Lists

Start from v3.0, BungeeSafeguard supports custom storage backend for the lists. That is, you can store the list records in the config file, the database, or wherever you want.
The use case of custom storage backend is when you have really large lists, or when you want to share lists among multiple networks, you will want a non-toy, dedicated, professional backend.

Current available storage backend extension plugins:

| Name | Feature |
| ---- | ------- |
| [Redis-BSG](https://github.com/Luluno01/Redis-BSG) | Store the whitelist/blacklist in a Redis store |

## Optional Extension Plugins

Start from v3.0, BungeeSafeguard exposes a handful of APIs for third-party plugins to manipulate the lists or register custom backend.
For example, you can now implement a standalone plugin that programmatically whitelist or blacklist someone; or a plugin that wraps BungeeSafeguard APIs and exposes them as RESTful API.

Current available extension plugins (storage backend not included):

| Name | Feature |
| ---- | ------- |
| [RESTful-BSG](https://github.com/Luluno01/RESTful-BSG) | Access the whitelist/blacklist via RESTful API |

## Configuration

The configuration file for BungeeSafeguard is `plugins/BungeeSafeguard/config.yml`.

```yaml
#########################################
#     BungeeSafeguard Configuration     #
#            Version: 3.1               #
#          Author: Untitled             #
#########################################

# You can safely ignore this
version: "3.1"

# Message to be sent to the player when that player is blocked for not being whitelisted
whitelist-message: :( You are not whitelisted on this server

# Message to be sent to the player when that player is blocked for being blacklisted
blacklist-message: :( We can't let you enter this server

# Message to be sent to the player when that player is blocked for not having a UUID 
no-uuid-message: :( Name yourself

# Whether to use whitelist
enable-whitelist: true

# Lazy-whitelist (array of usernames)
# lazy-whitelist:
# - <lazy-whitelisted username>
lazy-whitelist:

# Whitelist (array of UUIDs)
# whitelist:
# - <whitelisted UUID>
whitelist:

# Whether to use blacklist
enable-blacklist: false

# Lazy-blacklist (array of usernames)
# lazy-blacklist:
# - <lazy-blacklisted username>
lazy-blacklist:

# Blacklist (array of UUIDs)
# blacklist:
# - <banned UUID>
blacklist:

# xbl-web-api: <a deployment of https://github.com/Prouser123/xbl-web-api>
xbl-web-api: https://xbl-api.prouser123.me

# confirm: <should request confirmation for add/remove operation?>
confirm: false
```

Note that if you enable both blacklist and whitelist (which is weird, but it is possible to do that), player in both lists will be blocked because blacklist has a higher priority over whitelist.

## Commands

### Whitelist

Alias: `wlist`.

#### whitelist add

Add player(s) to whitelist:

```
whitelist add <space separated usernames or UUIDs>
```

Example:

```
whitelist add DummyPlayer0 DummyPlayer1 7be767e5-327c-4abd-852b-afab3ec1e2ff DummyPlayer2
```

#### whitelist x-add

Alias: `xadd`.

Add XBOX Live player(s) to whitelist:

```
whitelist x-add <space separated GamerTags or converted UUIDs>
```

Example:

```
whitelist x-add DummyPlayer0 DummyPlayer1 00000000-0000-0000-852b-afab3ec1e2ff DummyPlayer2
```

#### whitelist lazy-add

Alias: `whitelist lazyadd` or `whitelist ladd`.

Add player(s) to lazy-whitelist:

```
whitelist lazy-add <space separated usernames or UUIDs>
```

Example:

```
whitelist lazy-add DummyPlayer0 DummyPlayer1 7be767e5-327c-4abd-852b-afab3ec1e2ff DummyPlayer2
```

#### whitelist remove

Alias: `whitelist rm`.

Remove player(s) from whitelist:

```
whitelist remove <space separated usernames or UUIDs>
```

Example:

```
whitelist remove DummyPlayer0 DummyPlayer1 7be767e5-327c-4abd-852b-afab3ec1e2ff DummyPlayer2
```

#### whitelist x-remove

Alias: `whitelist xremove`, `whitelist x-rm` or `whitelist xrm`.

Remove XBOX Live player(s) from whitelist:

```
whitelist x-remove <space separated GamerTags or converted UUIDs>
```

Example:

```
whitelist x-remove DummyPlayer0 DummyPlayer1 00000000-0000-0000-852b-afab3ec1e2ff DummyPlayer2
```

#### whitelist lazy-remove

Alias: `whitelist lazyremove`, `whitelist lremove` or `whitelist lrm`.

Remove player(s) from lazy-whitelist:

```
whitelist lazy-remove <space separated usernames or UUIDs>
```

Example:

```
whitelist lazy-remove DummyPlayer0 DummyPlayer1 7be767e5-327c-4abd-852b-afab3ec1e2ff DummyPlayer2
```

#### whitelist import

Import UUID(s) from an existing JSON file, e.g. your old `whitelist.json`, to the whitelist.

```
whitelist import <path to whitelist.json>
```

Note that `<path to whitelist.json>` could be either an absolute path, e.g. `/home/mc/old-mc-server/whitelist.json`,
or a path relative to the **working directory** of the running BungeeCord process, e.g. `../old-mc-server/whitelist.json`.

Example:

```
whitelist import whitelist.json
```

*This feature is added as requested by issue #7.*

#### whitelist on

Turn on whitelist:

```
whitelist on
```

#### whitelist off

Turn off whitelist:

```
whitelist off
```

#### whitelist confirm

Confirm the last issued whitelist command:

```
whitelist confirm
```

#### whitelist list

Alias: `whitelist ls`, `whitelist show` or `whitelist dump`.

Dump whitelist and lazy whitelist with at most 10 last known usernames:

```
whitelist list
```

Example output:

```
Whitelist ENABLED
2 lazy record(s)
  foo
  bar
3 UUID record(s) and the last known names (in reverse chronological order)
  00000000-1111-2222-3333-666666666666 LatestName, OldNameLastMonth, OldNameLastYear
  ffffffff-1111-2222-3333-666666666666
  eeeeeeee-1111-2222-3333-666666666666 LatestName123
```

*This feature is added as requested by issue #8.*

### Blacklist

Alias: `blist`.

#### blacklist add

Add player(s) to blacklist:

```
blacklist add <space separated usernames or UUIDs>
```

Example:

```
blacklist add DummyPlayer0 DummyPlayer1 7be767e5-327c-4abd-852b-afab3ec1e2ff DummyPlayer2
```

#### blacklist x-add

Alias: `blacklist xadd`.

Add XBOX Live player(s) to blacklist:

```
blacklist x-add <space separated GamerTags or converted UUIDs>
```

Example:

```
blacklist x-add DummyPlayer0 DummyPlayer1 00000000-0000-0000-852b-afab3ec1e2ff DummyPlayer2
```

#### blacklist lazy-add

Alias: `blacklist lazyadd` or `blacklist ladd`.

Add player(s) to lazy-blacklist:

```
blacklist lazy-add <space separated usernames or UUIDs>
```

Example:

```
blacklist lazy-add DummyPlayer0 DummyPlayer1 7be767e5-327c-4abd-852b-afab3ec1e2ff DummyPlayer2
```

#### blacklist remove

Alias: `blacklist rm`.

Remove player(s) from blacklist:

```
blacklist remove <space separated usernames or UUIDs>
```

Example:

```
blacklist remove DummyPlayer0 DummyPlayer1 7be767e5-327c-4abd-852b-afab3ec1e2ff DummyPlayer2
```

#### blacklist x-remove

Alias: `blacklist xremove`, `blacklist x-rm`, `blacklist xrm`.

Remove XBOX Live player(s) from blacklist:

```
blacklist x-remove <space separated GamerTags or converted UUIDs>
```

Example:

```
blacklist x-remove DummyPlayer0 DummyPlayer1 00000000-0000-0000-852b-afab3ec1e2ff DummyPlayer2
```

#### blacklist lazy-remove

Alias: `blacklist lazyremove`, `blacklist lremove` or `blacklist lrm`.

Remove player(s) from lazy-blacklist:

```
blacklist lazy-remove <space separated usernames or UUIDs>
```

Example:

```
blacklist lazy-remove DummyPlayer0 DummyPlayer1 7be767e5-327c-4abd-852b-afab3ec1e2ff DummyPlayer2
```

#### blacklist import

Import UUID(s) from an existing JSON file, e.g. your old `banned-players.json`, to the blacklist.

```
blacklist import <path to banned-players.json>
```

Note that `<path to banned-players.json>` could be either an absolute path, e.g. `/home/mc/old-mc-server/banned-players.json`,
or a path relative to the **working directory** of the running BungeeCord process, e.g. `../old-mc-server/banned-players.json`.

Example:

```
blacklist import banned-players.json
```

*This feature is added as requested by issue #7.*

#### blacklist on

Turn on blacklist:

```
blacklist on
```

#### blacklist off

Turn off blacklist:

```
blacklist off
```

#### blacklist confirm

Confirm the last issued blacklist command:

```
blacklist confirm
```

#### blacklist list

Alias: `blacklist ls`, `blacklist show` or `blacklist dump`.

Dump blacklist and lazy blacklist with at most 10 last known usernames:

```
whitelist list
```

Example output:

```
Blacklist ENABLED
2 lazy record(s)
  foo
  bar
3 UUID record(s) and the last known names (in reverse chronological order)
  00000000-1111-2222-3333-666666666666 LatestName, OldNameLastMonth, OldNameLastYear
  ffffffff-1111-2222-3333-666666666666
  eeeeeeee-1111-2222-3333-666666666666 LatestName123
```

*This feature is added as requested by issue #8.*

### Main Command

Alias: `bsg`.

#### bungeesafeguard load

Alias: `bsg use`.

Load configuration from a specific `.yml` file under `plugins/BungeeSafeguard/` (the extension `.yml` can be omitted):

```
bungeesafeguard load <config file>
```

Example:

```
bungeesafeguard load maintenance-config.yml
```

**Note: enabling [confirmation](#operation-confirmation) is suggested in order not to modify an unexpected configuration file if you are to use multiple configuration files.**

*This feature is added as requested by issue #6.*

#### bungeesafeguard reload

Reload configuration (from file `plugins/BungeeSafeguard/config.yml`):

```
bungeesafeguard reload
```

#### bungeesafeguard status

Check status of blacklist and whitelist:

```
bungeesafeguard status
```

#### bungeesafeguard dump

Dump currently loaded blacklist and whitelist:

```
bungeesafeguard dump
```

#### bungeesafeguard import

Alias: `bsg i`.

Import all whitelist/blacklist records, including both UUID records and lazy records, from a YAML file:

```
bungeesafeguard import <config-file-name>
```

Example:

```
bungeesafeguard import old-config.yml
```

Note this command will refuse to overwrite/merge current lists if current storage backend is non-empty. For list merging, use [`bungeesafeguard merge`](#bungeesafeguard-merge)

#### bungeesafeguard merge

Alias: `bsg m`.

Merge all whitelist/blacklist records, including both UUID records and lazy records, in a YAML file, with current lists in use:

```
bungeesafeguard merge <config-file-name>
```

Example:

```
bungeesafeguard merge old-config.yml
```

#### bungeesafeguard export

Alias: `bsg e`.

Export all whitelist/blacklist records, including both UUID records and lazy records, to a YAML file:

```
bungeesafeguard export <lists.yml>
```

Example:

```
bungeesafeguard export list-backup.yml
```

You can combine this command with [`bungeesafeguard import`](#bungeesafeguard-import) for backend migration.

## Permission Nodes

BungeeSafeGuard uses BungeeCord's built-in permission system. There are 3 permission nodes for the aforementioned 3 category of commands respectively. Only players granted **with** the permission can issue corresponding command **in game** (this restriction does not apply to console).

| Permission                  | Commands      |
| --------------------------- | ------------- |
| `bungeesafeguard.whitelist` | [`whitelist *`](#whitelist) |
| `bungeesafeguard.blacklist` | [`blacklist *`](#blacklist) |
| `bungeesafeguard.main`      | [`bungeesafeguard *`](#main-command) |

Note that despite that BungeeCord has a built-in permission system, it does not provide a permission manager (or does it?). You will need to install third-party permission plugin so that you can grant permissions to players.

## Lazy Lists

Records are added to/removed from lazy lists via `whitelist lazy-*` and `blacklist lazy-*` commands.

Lazy-whitelist and lazy-blacklist work in a very similar way. Let's take lazy-whitelist as example, and you will understand how both of them work. Lazy-whitelist is a different list from the plain whitelist you access via `whitelist add` and `whitelist remove`. Upon record addition, username is added to lazy-whitelist rather than translated UUID, which may take some considerable time or even fail to translate. What's more, because the translation requests are sent to Mojang, implicitly requiring that the server is running in online mode (unless you hijack the requests and redirect them to your own authentication server). The workaround (or maybe it is actually a great feature) is not to do the translation immediately but to save the username in a temporary list, i.e. lazy-whitelist. Because server will be told the UUID of the player upon client connection (if I am right), we are able to lazily translate username to UUID without sending HTTP request. In other words, usernames in lazy-whitelist are translated into UUIDs and moved to whitelist (the plain one) once the server knows the corresponding UUID of the username, i.e. when player with the username connect to the server for the first time.

In this way, offline servers should be able to use this plugin painlessly.

## Operation Confirmation

**By default**, BungeeSafeguard will **NOT** ask for confirmation of records addition/removal commands. If you want to be cautious, set the config entry `confirm` to `true`. Then you will need to use `whitelist confirm` (or `blacklist confirm`) to confirm your last issued `whitelist`-accessing (or `blacklist`-accessing) command in **10 seconds**.

For example, suppose that you are using the default configuration file `config.yml` (to switch to a different configuration file, use command [bungeesafeguard load](#bungeesafeguard-load)). You just enabled confirmation and want to add the player `DummyPlayer` to the whitelist by executing the command `whitelist add DummyPlayer`.
Then you will be asked:

    Are you sure you want to add the following Minecraft player(s) to the whitelist in the config file config.yml?
      DummyPlayer
    Please use /whitelist confirm in 10s to confirm

If you everything looks fine for you, use `whitelist confirm` in 10 seconds and `DummyPlayer` will be added into the whitelist.

*This feature is added as requested by issue #6.*

## Important Notes

BungeeSafeguard does asynchronous UUID look up when you execute add/remove on the lists.
It's recommended to execute those command only in console, and wait patiently for the completion feedback from the command before executing other commands of BungeeSafeguard.

Offline servers should be able to use this plugin by using lazy lists or supplying BungeeSafeguard with players' UUIDs rather than their usernames. However, offline servers are still suffering from UUID abuse if they have no authentication plugin installed or have no external authentication mechanism. Offline server owners need to fully understand whitelist and blacklist is **NOT** a prevention of UUID abuse.

Last but not least, you should always be prepared for the worst situation, for example, when BungeeCord or BungeeSafeguard somehow, magically, fail to protect your servers. Backup is a good way to counter Murphy's law.
