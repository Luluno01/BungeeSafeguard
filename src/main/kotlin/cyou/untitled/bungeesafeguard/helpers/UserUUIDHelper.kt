package cyou.untitled.bungeesafeguard.helpers

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
object UserUUIDHelper {
    data class NameAndUUID(val name: String?, val id: UUID)
    data class ResolutionResult(
        val err: Throwable?,
        val result: NameAndUUID?,
        val query: String
    )

    val client = HttpClient(CIO)

    private suspend fun getUUIDFromUsername(username: String, client: HttpClient = this.client): UUID {
        val res: HttpResponse = client.get("https://api.mojang.com/users/profiles/minecraft/${username}")
        when (val status = res.status) {
            HttpStatusCode.OK -> {
                val json = TypedJSON.fromString(res.readText())
                json.assertObject()
                val id = json.getString("id") ?: throw IOException("Invalid response")
                return UUID.fromString(
                    StringBuilder(id)
                        .insert(8, '-')
                        .insert(13, '-')
                        .insert(18, '-')
                        .insert(23, '-')
                        .toString()
                )
            }
            HttpStatusCode.NoContent -> throw UserNotFoundException("User $username cannot be found from Mojang")
            else -> throw IOException("Unable to handle response with status $status")
        }
    }

    suspend fun getUUIDFromString(usernameOrUUID: String, client: HttpClient = this.client): NameAndUUID {
        return try {
            NameAndUUID(null, UUID.fromString(usernameOrUUID))
        } catch (e: IllegalArgumentException) {
            NameAndUUID(usernameOrUUID, getUUIDFromUsername(usernameOrUUID, client))
        }
    }

    fun getUUIDFromXUID(xuid: Long): UUID {
        return UUID.fromString(
            StringBuilder("00000000-0000-0000-")
                .append(xuid.toString(16).padStart(16, '0'))
                .insert(23, '-').toString()
        )
    }

    private suspend fun doGetUUIDFromXBOXTag(context: BungeeSafeguard, tag: String, client: HttpClient = this.client): UUID {
        var xblWebAPIUrl = context.config.xblWebAPIUrl ?:
        error("XBL Web API URL must be specified for XUID look up")
        if (!xblWebAPIUrl.endsWith('/')) xblWebAPIUrl += '/'
        val res: HttpResponse = try {
            client.get("${xblWebAPIUrl}xuid/$tag/raw")
        } catch (e: ClientRequestException) {
            throw UserNotFoundException("User $tag cannot be found from XBOX Live", e)
        }
        when (val status = res.status) {
            HttpStatusCode.OK -> return getUUIDFromXUID(res.readText().toLong())
            else -> throw IOException("Unable to handle response with status $status")
        }
    }

    suspend fun getUUIDFromXBOXTag(context: BungeeSafeguard, tagOrUUID: String, client: HttpClient = this.client): NameAndUUID {
        return try {
            NameAndUUID(null, UUID.fromString(tagOrUUID))
        } catch (e: IllegalArgumentException) {
            NameAndUUID(tagOrUUID, doGetUUIDFromXBOXTag(context, tagOrUUID, client))
        }
    }

    /**
     * Resolve UUIDs for given usernames (or UUIDs)
     *
     * In principle, this method and the `action` should not throw any exception
     */
    suspend fun resolveUUIDs(context: BungeeSafeguard, queries: Array<out String>, xbox: Boolean, client: HttpClient = this.client, action: suspend (ResolutionResult) -> Unit) {
        for (usernameOrUUID in queries) {
            coroutineScope {
                launch {
                    try {
                        val res = if (xbox) getUUIDFromXBOXTag(context, usernameOrUUID, client)
                        else getUUIDFromString(usernameOrUUID, client)
                        try {
                            action(ResolutionResult(null, res, usernameOrUUID))
                        } catch (err: Throwable) {
                            err.printStackTrace()
                        }
                    } catch (err: Throwable) {
                        action(ResolutionResult(err, null, usernameOrUUID))
                    }
                }
            }
        }
    }
}