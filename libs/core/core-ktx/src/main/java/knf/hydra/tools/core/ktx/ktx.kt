package knf.hydra.tools.core.ktx

import org.json.JSONArray
import org.json.JSONObject

/**
 * Maps a [JSONArray] by the [block] function
 */
fun <T> JSONArray.map(block: (JSONObject) -> T): List<T> {
    return (0 until this.length()).toList().map { block(this.getJSONObject(it)) }
}

/**
 * Maps a [JSONArray] by the [block] function excluding null values
 */
fun <T> JSONArray.mapNotNull(block: (JSONObject) -> T?): List<T> {
    return (0 until this.length()).toList().mapNotNull { block(this.getJSONObject(it)) }
}

/**
 * Transform a [JSONArray] to a list of [JSONObject]
 */
fun JSONArray.toList(): List<JSONObject> {
    return (0 until this.length()).toList().map { this.getJSONObject(it) }
}

/**
 * Transform a [JSONArray] to a list of [String]
 */
fun JSONArray.toStringList(): List<String> {
    return (0 until this.length()).toList().map { this.getString(it) }
}