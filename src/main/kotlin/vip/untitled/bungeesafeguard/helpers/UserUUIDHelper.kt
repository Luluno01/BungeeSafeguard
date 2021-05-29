package vip.untitled.bungeesafeguard.helpers

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import vip.untitled.bungeesafeguard.MetaHolderPlugin
import java.io.IOException
import java.util.*

object UserUUIDHelper {
    data class NameAndUUID(val name: String?, val id: UUID)
    data class ResolutionResult(
        val err: Throwable?,
        val result: NameAndUUID?,
        val query: String
    )

    private suspend fun getUUIDFromUsername(client: HttpClient, username: String): UUID {
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

    suspend fun getUUIDFromString(client: HttpClient, usernameOrUUID: String): NameAndUUID {
        return try {
            NameAndUUID(null, UUID.fromString(usernameOrUUID))
        } catch (e: IllegalArgumentException) {
            NameAndUUID(usernameOrUUID, getUUIDFromUsername(client, usernameOrUUID))
        }
    }

    fun getUUIDFromXUID(xuid: Long): UUID {
        return UUID.fromString(
            StringBuilder("00000000-0000-0000-")
                .append(xuid.toString(16).padStart(16, '0'))
                .insert(23, '-').toString()
        )
    }

    private suspend fun doGetUUIDFromXBOXTag(context: MetaHolderPlugin, client: HttpClient, tag: String): UUID {
        var xblWebAPIUrl = context.config!!.xblWebAPIUrl ?:
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

    suspend fun getUUIDFromXBOXTag(context: MetaHolderPlugin, client: HttpClient, tagOrUUID: String): NameAndUUID {
        return try {
            NameAndUUID(null, UUID.fromString(tagOrUUID))
        } catch (e: IllegalArgumentException) {
            NameAndUUID(tagOrUUID, doGetUUIDFromXBOXTag(context, client, tagOrUUID))
        }
    }

    /**
     * Resolve UUIDs for given usernames (or UUIDs)
     *
     * In principle, this method should not throw an exception
     */
    suspend fun resolveUUIDs(context: MetaHolderPlugin, client: HttpClient, queries: Array<out String>, xbox: Boolean): Channel<ResolutionResult> {
        val result = Channel<ResolutionResult>()
        var pendingQuery = queries.size
        for (usernameOrUUID in queries) {
            GlobalScope.launch {
                try {
                    val res = if (xbox) getUUIDFromXBOXTag(context, client, usernameOrUUID)
                    else getUUIDFromString(client, usernameOrUUID)
                    result.send(ResolutionResult(null, res, usernameOrUUID))
                } catch (err: Throwable) {
                    result.send(ResolutionResult(err, null, usernameOrUUID))
                } finally {
                    if (--pendingQuery <= 0) {
                        result.close()
                    }
                }
            }
        }
        return result
    }
}