package com.fortune.stall.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** 卦局记录 —— 对应服务端 fortune_sessions 表 */
data class Session(
    val id: Int,
    val createdAt: String,
    val question: String,
    val method: String,
    val mode: String,
    val face: String,
    val seal: String,
    val verdict: String,
    val favorite: Boolean = false
) {
    val badge: String get() = when (method) {
        "xiaoliuren" -> "壬"; "liuyao" -> "爻"; "tarot" -> "塔"; else -> "卦"
    }
}

class FortuneDb(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "fortune.db"
        const val DB_VERSION = 2
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS fortune_sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "created_at TEXT, question TEXT, method TEXT, mode TEXT, " +
                    "face TEXT, seal TEXT DEFAULT '', verdict TEXT DEFAULT '', " +
                    "favorite INTEGER DEFAULT 0)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE fortune_sessions ADD COLUMN favorite INTEGER DEFAULT 0")
        }
    }

    /** 落账：写入一卦，返回自增 id —— 这就是「印章」的编号 */
    fun insert(
        createdAt: String,
        question: String,
        method: String,
        mode: String,
        face: String,
        seal: String
    ): Int {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("created_at", createdAt)
            put("question", question.take(200))
            put("method", method)
            put("mode", mode)
            put("face", face.take(12000))
            put("seal", seal)
        }
        val rowId = db.insert("fortune_sessions", null, cv)
        db.close()
        return rowId.toInt()
    }

    fun recent(n: Int = 30): List<Session> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id,created_at,question,method,mode,face,seal,verdict,favorite FROM fortune_sessions " +
                    "ORDER BY favorite DESC, id DESC LIMIT ?", arrayOf(n.coerceIn(1, 30).toString())
        )
        val list = mutableListOf<Session>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(Session(
                    id = it.getInt(0), createdAt = it.getString(1), question = it.getString(2),
                    method = it.getString(3), mode = it.getString(4), face = it.getString(5),
                    seal = it.getString(6), verdict = it.getString(7) ?: "",
                    favorite = it.getInt(8) == 1
                ))
            }
        }
        db.close()
        return list
    }

    /** 只看收藏 */
    fun favorites(n: Int = 30): List<Session> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id,created_at,question,method,mode,face,seal,verdict,favorite FROM fortune_sessions " +
                    "WHERE favorite=1 ORDER BY id DESC LIMIT ?", arrayOf(n.coerceIn(1, 30).toString())
        )
        val list = mutableListOf<Session>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(Session(
                    id = it.getInt(0), createdAt = it.getString(1), question = it.getString(2),
                    method = it.getString(3), mode = it.getString(4), face = it.getString(5),
                    seal = it.getString(6), verdict = it.getString(7) ?: "",
                    favorite = it.getInt(8) == 1
                ))
            }
        }
        db.close()
        return list
    }

    fun favoriteCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM fortune_sessions WHERE favorite=1", null)
        val n = cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
        db.close()
        return n
    }

    fun get(id: Int): Session? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id,created_at,question,method,mode,face,seal,verdict,favorite FROM fortune_sessions WHERE id=?",
            arrayOf(id.toString())
        )
        val result = cursor.use {
            if (it.moveToFirst()) Session(
                id = it.getInt(0), createdAt = it.getString(1), question = it.getString(2),
                method = it.getString(3), mode = it.getString(4), face = it.getString(5),
                seal = it.getString(6), verdict = it.getString(7) ?: "",
                favorite = it.getInt(8) == 1
            ) else null
        }
        db.close()
        return result
    }

    fun saveVerdict(id: Int, verdict: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("verdict", verdict.take(4000)) }
        db.update("fortune_sessions", cv, "id=?", arrayOf(id.toString()))
        db.close()
    }

    /** 回填完整卦面（先落账拿 id，再生成带印章编号的卦面） */
    fun updateFace(id: Int, face: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("face", face.take(12000)) }
        db.update("fortune_sessions", cv, "id=?", arrayOf(id.toString()))
        db.close()
    }

    /** 收藏 / 取消收藏 */
    fun setFavorite(id: Int, favorite: Boolean) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("favorite", if (favorite) 1 else 0) }
        db.update("fortune_sessions", cv, "id=?", arrayOf(id.toString()))
        db.close()
    }

    /** 删除一卦 */
    fun delete(id: Int) {
        val db = writableDatabase
        db.delete("fortune_sessions", "id=?", arrayOf(id.toString()))
        db.close()
    }

    /** 批量删除若干卦（多选删除） */
    fun deleteIds(ids: List<Int>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        val marks = ids.joinToString(",") { "?" }
        db.delete("fortune_sessions", "id IN ($marks)", ids.map { it.toString() }.toTypedArray())
        db.close()
    }

    /** 清空全部卦象记录（收藏页调用时只清收藏） */
    fun clearAll(onlyFavorites: Boolean = false) {
        val db = writableDatabase
        if (onlyFavorites) db.delete("fortune_sessions", "favorite=1", null)
        else db.delete("fortune_sessions", null, null)
        db.close()
    }

    /** 删除某一类卦象（按 method 精确匹配） */
    fun deleteByMethod(method: String) {
        val db = writableDatabase
        db.delete("fortune_sessions", "method=?", arrayOf(method))
        db.close()
    }
}
