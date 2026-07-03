package com.district.core.persistence

import com.district.domain.DownloadedAlbum

interface DownloadStore {
    suspend fun load(): List<DownloadedAlbum>
    suspend fun save(albums: List<DownloadedAlbum>)
}
