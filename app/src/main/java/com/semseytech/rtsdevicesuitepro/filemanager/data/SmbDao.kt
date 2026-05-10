package com.semseytech.rtsdevicesuitepro.filemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SmbDao {
    @Query("SELECT * FROM smb_connections ORDER BY lastConnected DESC")
    fun getAllConnections(): Flow<List<SmbConnection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: SmbConnection)

    @Update
    suspend fun updateConnection(connection: SmbConnection)

    @Delete
    suspend fun deleteConnection(connection: SmbConnection)

    @Query("SELECT * FROM smb_connections WHERE host = :host LIMIT 1")
    suspend fun getConnectionByHost(host: String): SmbConnection?
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY name ASC")
    fun getAllFavorites(): Flow<List<FavoriteLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteLocation)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteLocation)

    @Query("DELETE FROM favorites WHERE path = :path")
    suspend fun deleteFavoriteByPath(path: String)
}
