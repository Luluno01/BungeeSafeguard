# Developing Extension Plugin for BungeeSafeguard

Start from v3.0, BungeeSafeguard officially announced its Java/Kotlin APIs (I will try to keep them as stable as possible). Including the followings:

* Lists manipulation
* Custom storage backend for lists

## Get Started

*Note: this section assumes you have mastered the basics of Kotlin/Java developing, including the usage of a proper IDE and build tools*.

To interact with BungeeSafeguard from your plugin (you need to develop your own BungeeCord plugin to invoke the APIs; see [BungeeCord Plugin Development](https://www.spigotmc.org/wiki/bungeecord-plugin-development/) if you are a beginner).

### Add BungeeSafeguard as a Dependency

Gradle (if you download the `BungeeSafeguard.jar` file into the `libs` folder):

```
repositories {
    mavenCentral()
    flatDir {
        dirs 'libs'
    }
    // ...
}

dependencies {
    compileOnly name: 'BungeeSafeguard'
    // ...
}
```

Remember to declare BungeeSafeguard as a hard dependency in your `plugin.yml`.

### Get the Plugin Instance

```Kotlin
import cyou.untitled.bungeesafeguard.BungeeSafeguard

// ...
val bsg = BungeeSafeguard.getPlugin()
bsg.whitelist.add(someId)
// ...
```

Or

```Kotlin
import cyou.untitled.bungeesafeguard.BungeeSafeguard

// ...
// `this` is your plugin instance
val bsg = this.proxy.pluginManager.getPlugin("BungeeSafeguard") as BungeeSafeguard
// ...
```

Or

```Kotlin
import net.md_5.bungee.api.plugin.Listener
import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.events.BungeeSafeguardEnabledEvent
import net.md_5.bungee.event.EventHandler

class Events(private val context: Plugin): Listener {
    @EventHandler
    fun onBungeeSafeguardEnabled(event: BungeeSafeguardEnabledEvent) {
        // This event will only be captured if you register the listener **before** BungeeSafeguard is enabled
        // To determine if you have missed this event, check `bsg.enabled`
        val bsg: BungeeSafeguard = event.bsg
        // ...
    }
}
```

### Get the Storage Backend

In most cases you don't need to get the backend instance but in case you have to:

```Kotlin
import cyou.untitled.bungeesafeguard.storage.Backend

// ...
Backend.getBackend().add(arrayOf("whitelist", "lazy"), "someone")
// ...
```

### Register Custom Storage Backend

If you implemented your own storage backend, initialize and register it:

```Kotlin
import cyou.untitled.bungeesafeguard.storage.Backend

// ...
val backend = YourBackend()  // Optionally, use the CachedBackend wrapper (please refer to the source code of cyou.untitled.bungeesafeguard.storage.Backend.CachedBackend)
backend.init()
Backend.registerBackend(backend, yourPlugin)
// ...
```

## Lists Manipulation

To access the lists, do it via `bsg.listMgr`, where `bsg` is the instance of BungeeSafeguard plugin and `listMgr` is a [`ListManager`](./src/main/kotlin/cyou/untitled/bungeesafeguard/list/ListManager.kt) object.
Alternatively, access the lists via the shortcuts `bsg.whitelist` and `bsg.blacklist`. All lists are [`UUIDList`](./src/main/kotlin/cyou/untitled/bungeesafeguard/list/UUIDList.kt) objects and managed by [`ListManager`](./src/main/kotlin/cyou/untitled/bungeesafeguard/list/ListManager.kt).

For example, to add a new player with UUID "c6526e46-d718-11eb-b8bc-0242ac130003" to the whitelist:

```Kotlin
bsg.whitelist.add(UUID.fromString("c6526e46-d718-11eb-b8bc-0242ac130003"))
```

Turn on/off the list:

```Kotlin
bsg.whitelist.on()
bsg.whitelist.off()
```

If you want username-to-UUID translation, it is available via [`UserUUIDHelper.resolveUUIDs`](./src/main/kotlin/cyou/untitled/bungeesafeguard/helpers/UserUUIDHelper.kt):

```Kotlin
UserUUIDHelper.resolveUUIDs(bsg, arrayOf("user1", "user2"), xbox = false) {
  if (it.err == null) {
    bsg.whitelist.add(it.result!!.id)
  }
}
```

## Storage Backend

Firstly, the storage backend is an extra layer that abstracts out the details of the lists storage, i.e., where the list records are stored.
By default, the backend is a [`ConfigBackend`](./src/main/kotlin/cyou/untitled/bungeesafeguard/storage/ConfigBackend.kt) (which extends [`YAMLBackend`](./src/main/kotlin/cyou/untitled/bungeesafeguard/storage/YAMLBackend.kt)) wrapped by [`CachedBackend`](./src/main/kotlin/cyou/untitled/bungeesafeguard/storage/CachedBackend.kt). It caches all list contents for fast read access and stores the contents in the same config file used by BungeeSafeguard. When BungeeSafeguard reloads/loads a new config file, it clears the cache and uses the new config file as backing file.

Now, suppose you want to implement your custom backend that stores the lists in Redis (so that your multiple BungeeCord networks can share the same lists), what should you do? In general, there are 4 steps:

1. Create a standalone BungeeCord plugin
2. Implement the [`Backend`](./src/main/kotlin/cyou/untitled/bungeesafeguard/storage/Backend.kt) interface
3. Register an instance of a `Backend` you just implemented (see [here](#register-custom-storage-backend) for example)
4. Massive tests

For examples of how to implement `Backend`, see [`YAMLBackend`](./src/main/kotlin/cyou/untitled/bungeesafeguard/storage/YAMLBackend.kt), [`ConfigBackend`](./src/main/kotlin/cyou/untitled/bungeesafeguard/storage/ConfigBackend.kt) and [`CachedBackend`](./src/main/kotlin/cyou/untitled/bungeesafeguard/storage/CachedBackend.kt).
