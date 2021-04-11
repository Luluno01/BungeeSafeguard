package vip.untitled.bungeesafeguard.helpers

import net.md_5.bungee.api.plugin.Plugin
import vip.untitled.bungeesafeguard.MetaHolderPlugin
import java.io.FileNotFoundException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

object UserUUIDHelper {
    private fun getUUIDFromUsername(context: Plugin, username: String, callback: (Throwable?, UUID?, String?) -> Unit) {
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
                            uuid,
                            username
                        )
                    }
                    204 -> {
                        throw UserNotFoundException("User $username cannot be found from Mojang")
                    }
                    else -> throw IOException("Unable to handle response with status code ${connection.responseCode}")
                }
            } catch (e: Throwable) {
                callback(e, null, null)
            } finally {
                try {
                    // InputStream should have been closed
                    // Will this really close the connection? (I hope it will be reused)
                    connection?.disconnect()
                } catch (err: IOException) {}
            }
        }
    }

    fun getUUIDFromString(context: Plugin, usernameOrUUID: String, callback: (Throwable?, UUID?, String?) -> Unit) {
        try {
            callback(null, UUID.fromString(usernameOrUUID), null)
            return
        } catch (e: IllegalArgumentException) {}
        getUUIDFromUsername(
            context,
            usernameOrUUID,
            callback
        )
    }

    fun getUUIDFromXUID(xuid: Long): UUID {
        return UUID.fromString(
            "00000000-0000-0000-${xuid.ushr(56).and(0xff).toString(16).padStart(2, '0')}${
                xuid.ushr(
                    48
                ).and(0xff).toString(16).padStart(2, '0')
            }-${xuid.ushr(32).and(0xff).toString(16).padStart(2, '0')}${
                xuid.ushr(
                    24
                ).and(0xff).toString(16).padStart(2, '0')
            }${xuid.ushr(16).and(0xff).toString(16).padStart(2, '0')}${
                xuid.ushr(8).and(
                    0xff
                ).toString(16).padStart(2, '0')
            }${xuid.and(0xff).toString(16).padStart(2, '0')}"
        )
    }

    private fun doGetUUIDFromXBOXTag(context: MetaHolderPlugin, tag: String, callback: (Throwable?, UUID?, String?) -> Unit) {
        var xblWebAPIUrl = context.config.xblWebAPIUrl ?:
            error("XBL Web API URL must be specified for XUID look up")
        if (!xblWebAPIUrl.endsWith('/')) xblWebAPIUrl += '/'
        context.proxy.scheduler.runAsync(context) {
            var connection: HttpURLConnection? = null
            try {
                connection = URL("${xblWebAPIUrl}xuid/$tag/raw").openConnection() as HttpURLConnection
                connection.connect()
                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }
                when (connection.responseCode) {
                    200 -> {
                        callback(
                            null,
                            getUUIDFromXUID(response.toLong()),
                            tag
                        )
                    }
                    else -> throw IOException("Unable to handle response with status code ${connection.responseCode}")
                }
            } catch (e: FileNotFoundException) {
                callback(UserNotFoundException("User $tag cannot be found from XBOX Live", e), null, null)
            } catch (e: Throwable) {
                callback(e, null, null)
            } finally {
                try {
                    // InputStream should have been closed
                    // Will this really close the connection? (I hope it will be reused)
                    connection?.disconnect()
                } catch (err: IOException) {}
            }
        }
    }

    fun getUUIDFromXBOXTag(context: MetaHolderPlugin, tagOrUUID: String, callback: (Throwable?, UUID?, String?) -> Unit) {
        try {
            callback(null, UUID.fromString(tagOrUUID), null)
            return
        } catch (e: IllegalArgumentException) {}
        doGetUUIDFromXBOXTag(
            context,
            tagOrUUID,
            callback
        )
    }
}