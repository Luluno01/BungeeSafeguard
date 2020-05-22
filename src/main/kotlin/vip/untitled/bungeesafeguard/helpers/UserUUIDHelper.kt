package vip.untitled.bungeesafeguard.helpers

import net.md_5.bungee.api.plugin.Plugin
import java.io.IOException
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

object UserUUIDHelper {
    fun getUUIDFromUsername(context: Plugin, username: String, callback: (Throwable?, UUID?) -> Unit) {
        context.proxy.scheduler.runAsync(context) {
            var connection: HttpsURLConnection? = null
            try {
                connection = URL("https://api.mojang.com/users/profiles/minecraft/${username}").openConnection() as HttpsURLConnection
                connection.connect()
                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }
                when (connection.responseCode) {
                    200 -> {
                        val json = TypedJSON.fromString(response)
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
                    }
                    204 -> {

                        throw UserNotFoundException("User $username cannot be found from Mojang")
                    }
                    else -> throw IOException("Unable to handle response with status code ${connection.responseCode}")
                }
            } catch (e: Throwable) {
                callback(e, null)
            } finally {
                try {
                    // InputStream should have been closed
                    // Will this really close the connection? (I hope it will be reused)
                    connection?.disconnect()
                } catch (err: IOException) {}
            }
        }
    }

    fun getUUIDFromString(context: Plugin, usernameOrUUID: String,callback: (Throwable?, UUID?) -> Unit) {
        try {
            callback(null, UUID.fromString(usernameOrUUID))
            return
        } catch (e: IllegalArgumentException) {}
        getUUIDFromUsername(
            context,
            usernameOrUUID,
            callback
        )
    }
}