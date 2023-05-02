package knf.hydra.tools.anime_music

import knf.hydra.core.models.data.ExtraSection
import knf.hydra.core.models.data.Music
import knf.hydra.core.models.data.MusicData
import knf.hydra.tools.core.ktx.map
import knf.hydra.tools.core.ktx.toList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

/**
 * Media type to extract
 */
enum class MediaType {
    /**
     * Video format, heavier but supports duration
     */
    WEBM,

    /**
     * Audio format, lighter but doesn't supports duration
     */
    OGG
}

/**
 * Creates a Music section for animes
 *
 * @param sectionTitle The title to be used in the section
 * @param animeTitle The anime title to be searched
 * @param selector Optional selector for more advanced search see api [here](https://api-docs.animethemes.moe/anime/), by default the first element is selected
 * @return An [ExtraSection] with anime music
 */
fun animeMusicSection(sectionTitle: String,  animeTitle: String, mediaType: MediaType, selector: (list: List<JSONObject>) -> JSONObject = { it.first() }): ExtraSection {
    return ExtraSection(sectionTitle, flow {
        val searchJson = withContext(Dispatchers.IO) {
            val encodedTitle = URLEncoder.encode(animeTitle, "utf-8")
            URL("https://api.animethemes.moe/search?q=$encodedTitle&include[anime]=animethemes.animethemeentries.videos.audio,animethemes.song.artists").readText()
        }
        val searchArray = JSONObject(searchJson).getJSONObject("search").getJSONArray("anime")
        if (searchArray.length() == 0) {
            emit(null)
            return@flow
        }
        val songsArray = selector(searchArray.toList()).getJSONArray("animethemes")
        if (songsArray.length() == 0) {
            emit(null)
            return@flow
        }
        val songList = songsArray.map { songObject ->
            var title = songObject.getJSONObject("song").getString("title")
            val artists = songObject.getJSONObject("song").getJSONArray("artists")
            if (artists.length() > 0) {
                title += " - ${artists.getJSONObject(0).getString("name")}"
            }
            val type = songObject.getString("slug")
            val song = songObject.getJSONArray("animethemeentries").getJSONObject(0).getJSONArray("videos").getJSONObject(0).let {
                when(mediaType) {
                    MediaType.OGG -> it.getJSONObject("audio")
                    MediaType.WEBM -> it
                }
            }.getString("link")
            Music(title, song, type)
        }
        if (songList.isNotEmpty())
            emit(MusicData(songList))
        else
            emit(null)
    })
}



