package com.district.core.download

import android.net.Uri
import com.district.domain.Album
import com.district.domain.DownloadedAlbum
import com.district.domain.RemoteResource
import java.io.File

/** A domain [Album] view of a downloaded album, with a local file cover so it renders offline. */
fun DownloadedAlbum.toAlbum(): Album =
    Album(
        id = id,
        title = title,
        artist = artist,
        productionYear = productionYear,
        trackCount = tracks.size,
        coverArt = coverPath?.let { RemoteResource(url = Uri.fromFile(File(it)).toString(), authHeaders = null) },
    )
