package com.example.nes.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class RomItem(
    val id: String,
    val title: String,
    val uriString: String? = null,
    val isBuiltIn: Boolean = false,
    val lastPlayed: Long = 0L,
    val isFavorite: Boolean = false,
    val fileSizeFormatted: String = ""
)

class GameRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nes_game_library", Context.MODE_PRIVATE)

    companion object {
        const val BUILT_IN_ID = "builtin_hardware_test_rom"
        private const val KEY_GAMES = "saved_rom_list"
    }

    private val defaultBuiltInRom = RomItem(
        id = BUILT_IN_ID,
        title = "NES Hardware & Graphics Test ROM",
        uriString = null,
        isBuiltIn = true,
        lastPlayed = System.currentTimeMillis(),
        isFavorite = true,
        fileSizeFormatted = "24 KB (Legal Homebrew)"
    )

    fun getGames(): List<RomItem> {
        val jsonStr = prefs.getString(KEY_GAMES, null)
        val list = mutableListOf<RomItem>()
        // Always ensure built-in ROM is present
        list.add(defaultBuiltInRom)

        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    if (id == BUILT_IN_ID) {
                        // Update favorite and last played for built-in
                        val fav = obj.optBoolean("isFavorite", true)
                        val lp = obj.optLong("lastPlayed", defaultBuiltInRom.lastPlayed)
                        list[0] = defaultBuiltInRom.copy(isFavorite = fav, lastPlayed = lp)
                        continue
                    }
                    list.add(
                        RomItem(
                            id = id,
                            title = obj.getString("title"),
                            uriString = obj.optString("uriString", null),
                            isBuiltIn = false,
                            lastPlayed = obj.optLong("lastPlayed", 0L),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            fileSizeFormatted = obj.optString("fileSizeFormatted", "")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    fun saveGames(games: List<RomItem>) {
        try {
            val array = JSONArray()
            for (game in games) {
                val obj = JSONObject().apply {
                    put("id", game.id)
                    put("title", game.title)
                    put("uriString", game.uriString ?: "")
                    put("isBuiltIn", game.isBuiltIn)
                    put("lastPlayed", game.lastPlayed)
                    put("isFavorite", game.isFavorite)
                    put("fileSizeFormatted", game.fileSizeFormatted)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_GAMES, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addGame(title: String, uriString: String, sizeBytes: Long): RomItem {
        val sizeFormatted = formatFileSize(sizeBytes)
        val newRom = RomItem(
            id = "rom_${System.currentTimeMillis()}",
            title = title,
            uriString = uriString,
            isBuiltIn = false,
            lastPlayed = System.currentTimeMillis(),
            isFavorite = false,
            fileSizeFormatted = sizeFormatted
        )
        val current = getGames().toMutableList()
        current.add(1, newRom) // Add after built-in
        saveGames(current)
        return newRom
    }

    fun toggleFavorite(id: String) {
        val current = getGames().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = current[index]
            current[index] = item.copy(isFavorite = !item.isFavorite)
            saveGames(current)
        }
    }

    fun updateLastPlayed(id: String) {
        val current = getGames().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = current[index]
            current[index] = item.copy(lastPlayed = System.currentTimeMillis())
            saveGames(current)
        }
    }

    fun deleteGame(id: String) {
        if (id == BUILT_IN_ID) return // Cannot delete built-in
        val current = getGames().filter { it.id != id }
        saveGames(current)
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "${bytes / 1024} KB"
            else -> "$bytes B"
        }
    }
}
