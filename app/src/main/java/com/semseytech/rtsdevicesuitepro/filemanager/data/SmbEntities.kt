package com.semseytech.rtsdevicesuitepro.filemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smb_connections")
data class SmbConnection(
    @PrimaryKey val host: String,
    val name: String,
    val user: String = "",
    val pass: String = "",
    val lastConnected: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val path: String,
    val type: String // "LOCAL", "SMB", etc.
)
