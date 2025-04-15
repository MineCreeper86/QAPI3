package org.qo.datas

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.qo.utils.Logger
import org.qo.services.messageServices.Msg
import org.springframework.stereotype.Service
import java.io.FileReader
import java.io.IOException

data class Node(
    @SerializedName("name") val name: String,
    @SerializedName("id") val id: Int,
    @SerializedName("role") val role: Role,
    @SerializedName("token") val token: String
) {
    fun validate(id: Int, token: String): Boolean {
        return id == this.id && token == this.token
    }
}

data class MessageIn(
    @SerializedName("message") val message: String,
    @SerializedName("from") val from: Int,
    @SerializedName("token") val token: String,
    @SerializedName("type") val data: String,
    @SerializedName("time") val time: Long,
    @SerializedName("sender") val sender: String,
) {
    fun doHideToken() : JsonObject {
        return JsonObject().apply {
            addProperty("message", message)
            addProperty("from", from)
            addProperty("sender", sender)
            addProperty("time", time)
            addProperty("sender", sender)
        }
    }
}

enum class Role {
    SERVER,
    ROOT_NODE,
    CHILD_NODE
}
@Service
class Nodes {
    private val SERVER_NODES = "nodes.json"
    private val gson = Gson()
    var nodesData: List<Node> = try {
        val reader = FileReader(SERVER_NODES)
        val nodeListType = object : TypeToken<List<Node>>() {}.type
        gson.fromJson(reader, nodeListType)
    } catch (e: IOException) {
        Logger.log("No nodes data found.", Logger.LogLevel.ERROR)
        emptyList()
    }

    fun validate_message(input: String): Boolean {
        return try {
            val messageIn = gson.fromJson(input, MessageIn::class.java)
            nodesData.any { node ->
                if (node.validate(messageIn.from, messageIn.token)) {
                    Msg.put(messageIn.doHideToken())
                    true
                } else {
                    false
                }
            }
        } catch (e: JsonSyntaxException) {
            Logger.log("Invalid message format: ${e.message}", Logger.LogLevel.ERROR)
            false
        }
    }
    fun getServerFromToken(input: String): Int {
        nodesData.forEach {
            if (it.token == input) {
                return it.id
            }
        }
        return -1
    }
}
