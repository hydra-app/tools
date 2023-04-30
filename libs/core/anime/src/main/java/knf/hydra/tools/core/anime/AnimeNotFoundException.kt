package knf.hydra.tools.core.anime

class AnimeNotFoundException(title: String): IllegalStateException("Anime with title \"$title\" not found")