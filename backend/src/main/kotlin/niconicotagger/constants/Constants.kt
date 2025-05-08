package niconicotagger.constants

import niconicotagger.dto.inner.misc.ReleaseEventCategory.AlbumRelease
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Club
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Concert
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Convention
import niconicotagger.dto.inner.misc.ReleaseEventCategory.Other
import niconicotagger.dto.inner.nnd.EqualFilter
import niconicotagger.dto.inner.nnd.OrFilter

object Constants {
    const val DEFAULT_USER_AGENT = "NicoNicoTagger 2.0"
    const val PVS_DISABLED_EDIT_NOTE = "Disabling NND PVs "
    const val COOKIE_HEADER_KEY = ".AspNetCore.Cookies"
    const val FIRST_WORK_TAG_ID = 158L
    const val API_SEARCH_FIELDS =
        "contentId,title,tags,userId,channelId,startTime,lengthSeconds,description,viewCounter,likeCounter"
    val GENRE_FILTER = OrFilter(listOf(EqualFilter("genre", "音楽・サウンド"), EqualFilter("genre", null)))
    val OFFLINE_EVENTS = setOf(AlbumRelease, Club, Concert, Convention, Other)
}
