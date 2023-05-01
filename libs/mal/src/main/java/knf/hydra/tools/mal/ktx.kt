package knf.hydra.tools.mal

import com.github.kittinunf.fuel.Fuel
import knf.hydra.core.models.data.ClickAction
import knf.hydra.core.models.data.CollectionData
import knf.hydra.core.models.data.CollectionItem
import knf.hydra.core.models.data.ExtraSection
import knf.hydra.core.models.data.GalleryData
import knf.hydra.core.models.data.ImageMediaItem
import knf.hydra.core.models.data.MediaItem
import knf.hydra.core.models.data.TextData
import knf.hydra.core.models.data.VerticalImageItem
import knf.hydra.core.models.data.YoutubeData
import knf.hydra.core.models.data.YoutubeItem
import knf.hydra.tools.core.anime.AnimeNotFoundException
import knf.hydra.tools.core.ktx.map
import knf.hydra.tools.core.ktx.mapNotNull
import knf.hydra.tools.core.ktx.toList
import knf.hydra.tools.core.ktx.toStringList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar

/**
 * Create a new searcher using a MAL id
 *
 * @param id MAL id
 * @return A searcher using the MAL [id]
 */
fun searchMAL(id: Int): MALSearcher = MALSearcher(id)

/**
 * Create a new searcher using the [title]
 *
 * @param title The anime title for search
 * @param type Optional anime type, by default [AnimeType.ANY]
 * @param search Optional function to select a result from the list, by default the first result is selected
 * @return A searcher using the anime [title]
 * @throws AnimeNotFoundException if the search returns empty
 */
@Throws(AnimeNotFoundException::class)
suspend fun searchMAL(title: String, type: AnimeType = AnimeType.ANY, search: (results: List<AnimeResult>) -> AnimeResult = { it.first() }): MALSearcher {
    val searchResponseJson = withContext(Dispatchers.IO) {
        val searchLink = StringBuilder("https://api.jikan.moe/v4/anime?q=${URLEncoder.encode(title, "utf-8")}").apply {
            if (type != AnimeType.ANY) {
                append("&type=${type.value}")
            }
            append("&limit=1")
        }.toString()
        withTimeout(2000) {
            JSONObject(URL(searchLink).readText())
        }
    }
    val searchResults = searchResponseJson.getJSONArray("data")
    if (searchResults.length() > 0) {
        return MALSearcher(search(searchResults.map { AnimeResult(it) }).id)
    } else {
        throw AnimeNotFoundException(title)
    }
}

/**
 * Anime types
 */
enum class AnimeType(val value: String) {
    ANY(""),
    TV("tv"),
    MOVIE("movie"),
    OVA("ova"),
    SPECIAL("special"),
    ONA("ona");

    /** @hide */
    fun fromValue(value: String) = values().find { it.value == value }
}

/**
 * Anime result from a query
 */
class AnimeResult internal constructor(json: JSONObject) {
    /** Default name */
    val name_default = json.getString("title")

    /** English name */
    val name_english = json.getString("title_english")

    /** Japanese name */
    val name_japanese = json.getString("title_japanese")

    /** Anime type */
    val type = json.getString("type")

    /** Is anime airing */
    val airing = json.getBoolean("airing")

    /** MAL id */
    val id = json.getInt("mal_id")
}

/**
 * MAL searcher
 *
 * @property id MAL id
 */
class MALSearcher internal constructor(private val id: Int) {
    /** @hide */
    private val basicData by lazy { BasicData(id) }

    /** @hide */
    private val charactersData by lazy { CharactersData(id) }

    /** @hide */
    private val staffData by lazy { StaffData(id) }

    /** @hide */
    private val galleryCreatorData by lazy { GalleryCreatorData(id) }

    /**
     * Basic data retriever
     *
     * @return A new [BasicData] object
     */
    fun basicData(): BasicData = basicData

    /**
     * Characters data retriever
     *
     * @return A new [CharactersData] object
     */
    fun charactersData(): CharactersData = charactersData

    /**
     * Staff data retriever
     *
     * @return A new [StaffData] object
     */
    fun staffData(): StaffData = staffData

    /**
     * Gallery data retriever
     *
     * @return A new [GalleryCreatorData] object
     */
    fun galleryData(): GalleryCreatorData = galleryCreatorData


    /**
     * Basic data of an anime
     */
    class BasicData internal constructor(id: Int) {
        /** @hide */
        private var delayed = false

        /** @hide */
        private val info: JSONObject by lazy {
            delayed = true
            JSONObject(URL("https://api.jikan.moe/v4/anime/$id").readText()).getJSONObject("data")
        }


        /**
         * Raw json, see api reference [here](https://docs.api.jikan.moe/#tag/anime/operation/getAnimeById)
         *
         * @return A [JSONObject] with the raw response from the api
         */
        suspend fun raw(): JSONObject = withContext(Dispatchers.IO) {
            if (!delayed) {
                delay(400)
            }
            info
        }

        fun alternativeTitlesSection(title: String): ExtraSection {
            return ExtraSection(title, flow {
                val hashList = hashSetOf<String>()
                raw().getJSONArray("titles").map { it.getString("title") }.forEach { hashList.add(it) }
                emit(TextData(hashList.joinToString("\n"), ClickAction.Clipboard(hashList.joinToString("\n"))))
            })
        }

        /**
         * Creates an [ExtraSection] with a simple [TextData] using the response from [transform], by default the date is
         * formatted like `MMMM d, yyyy`.
         *
         * When is no longer airing:
         * ```
         * {from} to {to}
         * ```
         *
         * When is airing:
         * ```
         * {from}
         * ```
         *
         * @param title Section title
         * @param transform Optional function for custom format
         * @return An [ExtraSection] using the [title]
         */
        fun airedSection(title: String, transform: (from: Calendar, to: Calendar?) -> String = { from, to ->
                val format = SimpleDateFormat("MMMM d, yyyy")
                val builder = StringBuilder().apply {
                    append(format.format(from.time))
                    if (to != null) {
                        append(" to ${format.format(to.time)}")
                    }
                }
                builder.toString()
            }
        ): ExtraSection {
            return ExtraSection(title, flow {
                val aired = raw().getJSONObject("aired")
                val from = Calendar.getInstance().apply {
                    val json = aired.getJSONObject("prop").getJSONObject("from")
                    set(json.getInt("year"), json.getInt("month"), json.getInt("day"))
                }
                val to = Calendar.getInstance().let {
                    if (aired.isNull("to")) return@let null
                    val json = aired.getJSONObject("to")
                    it.set(json.getInt("year"), json.getInt("month"), json.getInt("day"))
                    it
                }
                emit(TextData(transform(from, to)))
            })
        }

        /**
         * Creates an [ExtraSection] with the trailer using a [YoutubeData]
         *
         * @param title Section title
         * @return An [ExtraSection] using the [title]
         */
        fun trailerSection(title: String): ExtraSection {
            return ExtraSection(title, flow {
                emit(
                    YoutubeData(raw().getJSONObject("trailer").getString("youtube_id"))
                )
            })
        }
    }


    /**
     * Characters data of an anime
     */
    class CharactersData internal constructor(id: Int) {
        /** @hide */
        private var delayed = false

        /** @hide */
        private val array: JSONArray by lazy {
            delayed = true
            JSONObject(URL("https://api.jikan.moe/v4/anime/$id/characters").readText()).getJSONArray("data")
        }

        /** @hide */
        private val mediaOrNull: (String) -> ImageMediaItem? = {
            if (it.contains("questionmark_23.gif"))
                null
            else
                VerticalImageItem(it)
        }

        /**
         * Raw json, see api reference [here](https://docs.api.jikan.moe/#tag/anime/operation/getAnimeCharacters)
         *
         * @return A [JSONArray] with the raw response from the api
         */
        suspend fun raw(): JSONArray = withContext(Dispatchers.IO) {
            if (!delayed) {
                delay(400)
            }
            array
        }

        /**
         * Creates an [ExtraSection] with a [CollectionData] from the anime characters list
         *
         * @param title Section title
         * @return An [ExtraSection] using the [title]
         */
        fun charactersSection(title: String): ExtraSection {
            return ExtraSection(title, flow {
                emit(
                    CollectionData(
                        raw().map {
                            val info = it.getJSONObject("character")
                            CollectionItem(
                                info.getString("name"),
                                it.getString("role"),
                                mediaOrNull(info.getJSONObject("images").getJSONObject("jpg").getString("image_url")),
                                ClickAction.Web(info.getString("url"))
                            )
                        }
                    )
                )
            })
        }

        /**
         * Creates an [ExtraSection] with a [CollectionData] from the anime voice actors list
         *
         * @param title Section title
         * @return An [ExtraSection] using the [title]
         */
        fun voiceActorsSection(title: String): ExtraSection {
            return ExtraSection(title, flow {
                emit(
                    CollectionData(
                        raw().mapNotNull { data ->
                            val infoArray = data.getJSONArray("voice_actors")
                            infoArray.toList().find { it.getString("language") == "Japanese" }?.getJSONObject("person")?.let { info ->
                                CollectionItem(
                                    info.getString("name"),
                                    data.getJSONObject("character").getString("name"),
                                    mediaOrNull(info.getJSONObject("images").getJSONObject("jpg").getString("image_url")),
                                    ClickAction.Web(info.getString("url"))
                                )
                            }

                        }
                    )
                )
            })
        }
    }

    /**
     * Staff data of an anime
     */
    class StaffData internal constructor(id: Int) {
        /** @hide */
        private var delayed = false

        /** @hide */
        private val data: JSONArray by lazy {
            delayed = true
            JSONObject(URL("https://api.jikan.moe/v4/anime/$id/staff").readText()).getJSONArray("data")
        }

        /** @hide */
        private val mediaOrNull: (String) -> ImageMediaItem? = {
            if (it.contains("questionmark_23.gif"))
                null
            else
                VerticalImageItem(it)
        }

        /**
         * Raw json, see api reference [here](https://docs.api.jikan.moe/#tag/anime/operation/getAnimeStaff)
         *
         * @return A [JSONArray] with the raw response from the api
         */
        suspend fun raw(): JSONArray = withContext(Dispatchers.IO) {
            if (!delayed) {
                delay(400)
            }
            data
        }

        /**
         * Creates an [ExtraSection] with a [CollectionData] from the anime staff list
         *
         * @param title Section title
         * @return An [ExtraSection] using the [title]
         */
        fun staffSection(title: String): ExtraSection {
            return ExtraSection(title, flow {
                emit(
                    CollectionData(
                        raw().map {
                            val info = it.getJSONObject("person")
                            CollectionItem(
                                info.getString("name"),
                                it.getJSONArray("positions").toStringList().joinToString(),
                                mediaOrNull(info.getJSONObject("images").getJSONObject("jpg").getString("image_url")),
                                ClickAction.Web(info.getString("url"))
                            )
                        }
                    )
                )
            })
        }
    }

    /**
     * Gallery creator from the anime data
     */
    class GalleryCreatorData internal constructor(id: Int) {
        /** @hide */
        private var videosDelayed = false

        /** @hide */
        private var picturesDelayed = false

        /** @hide */
        private val videosData: JSONObject by lazy {
            videosDelayed = true
            JSONObject(URL("https://api.jikan.moe/v4/anime/$id/videos").readText()).getJSONObject("data")
        }

        /** @hide */
        private val picturesData: JSONArray by lazy {
            picturesDelayed = true
            JSONObject(Fuel.get("https://api.jikan.moe/v4/anime/$id/pictures").responseString().third.get()).getJSONArray("data")
        }

        /**
         * Raw json, see api reference [here](https://docs.api.jikan.moe/#tag/anime/operation/getAnimeVideos)
         *
         * @return A [JSONObject] with the raw response from the api
         */
        suspend fun videosRaw(): JSONObject = withContext(Dispatchers.IO) {
            if (!videosDelayed) {
                delay(500)
            }
            videosData
        }

        /**
         * Raw json, see api reference [here](https://docs.api.jikan.moe/#tag/anime/operation/getAnimePictures)
         *
         * @return A [JSONArray] with the raw response from the api
         */
        suspend fun picturesRaw(): JSONArray = withContext(Dispatchers.IO) {
            if (!picturesDelayed) {
                delay(500)
            }
            picturesData
        }

        /**
         * Creates an [ExtraSection] with a [GalleryData] from the videos and pictures list given by the creator [block]
         *
         * @param title Section title
         * @param block Creator function to add one or more elements to a single gallery
         * @return An [ExtraSection] using the [title] and the creator [block]
         */
        fun gallerySection(title: String, block: Creator.() -> Unit): ExtraSection {
            return ExtraSection(title, flow {
                val vRaw = videosRaw()
                delay(500)
                val pRaw = picturesRaw()
                val creator = Creator(vRaw, pRaw)
                block(creator)
                emit(
                    GalleryData(creator.create())
                )
            })
        }

        /**
         * Creator object that adds different types of media to a single list, see api reference
         * [videos](https://docs.api.jikan.moe/#tag/anime/operation/getAnimeVideos), [pictures](https://docs.api.jikan.moe/#tag/anime/operation/getAnimePictures)
         */
        class Creator(private val videosData: JSONObject, private val picturesData: JSONArray) {
            /** @hide */
            private val list = mutableListOf<MediaItem>()

            /**
             * Add promo videos to the gallery
             *
             */
            fun addPromoVideos() {
                list.addAll(
                    videosData.getJSONArray("promo").map {
                        YoutubeItem(
                            it.getJSONObject("trailer").getString("youtube_id")
                        )
                    }
                )
            }

            /**
             * Add music videos to the gallery
             *
             */
            fun addMusicVideos() {
                list.addAll(
                    videosData.getJSONArray("music_videos").map {
                        YoutubeItem(
                            it.getJSONObject("video").getString("youtube_id")
                        )
                    }
                )
            }

            /**
             * Add pictures to the gallery
             *
             */
            fun addPictures() {
                list.addAll(
                    picturesData.map {
                        VerticalImageItem(
                            it.getJSONObject("jpg").getString("large_image_url")
                        )
                    }
                )
            }

            /** @hide */
            fun create(): List<MediaItem> = list
        }
    }


}