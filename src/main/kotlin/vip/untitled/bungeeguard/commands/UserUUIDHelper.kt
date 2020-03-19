package vip.untitled.bungeeguard.commands

import net.md_5.bungee.api.plugin.Plugin
import vip.untitled.bungeeguard.TypedJSON
import java.io.IOException
import java.net.URL
import java.util.*

object UserUUIDHelper {
    fun getUUIDFromUsername(context: Plugin, username: String, callback: (Throwable?, UUID?) -> Unit) {
        context.proxy.scheduler.runAsync(context) {
            try {
                val json = TypedJSON.fromString(
                    URL("https://api.mojang.com/users/profiles/minecraft/${username}")
                        .openStream()
                        .bufferedReader()
                        .use { it.readText() }
                )
                json.assertObject()
                val id = json.getString("id") ?: throw IOException("Invalid response")
                val uuid = UUID.fromString(
                    StringBuilder(id)
                        .insert(8, '-')
                        .insert(13, '-')
                        .insert(18, '-')
                        .insert(23, '-')
                        .toString()
                )
                callback(
                    null,
                    uuid
                )
            } catch (e: Throwable) {
                callback(e, null)
            }
        }
    }

    fun getUUIDFromString(context: Plugin, usernameOrUUID: String,callback: (Throwable?, UUID?) -> Unit) {
        try {
            callback(null, UUID.fromString(usernameOrUUID))
            return
        } catch (e: IllegalArgumentException) {}
        getUUIDFromUsername(context, usernameOrUUID, callback)
    }
}