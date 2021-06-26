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

### Get the Plugin Instance

```Kotlin
import cyou.untitled.bungeesafeguard.BungeeSafeguard

// ...
val bsg = BungeeSafeguard.getPlugin()
bsg.whitelist.add(someId)
// ...
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

TODO: how

## Storage Backend

TODO: what and how
