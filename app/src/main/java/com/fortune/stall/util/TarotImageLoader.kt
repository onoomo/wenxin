package com.fortune.stall.util

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.fortune.stall.engine.Tarot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

/**
 * 塔罗牌面加载器：不内置牌图。
 * 用户在设置里点「下载塔罗牌」后，把 78 张牌面下载到内部存储；
 * 之后牌面只读本地缓存，完全离线、零流量。
 * 数据源：https://gitee.com/ricky-ai/tarot （Rider-Waite 公版牌面）
 */
object TarotImageLoader {
    private const val BASE = "https://gitee.com/ricky-ai/tarot/raw/master/assets/tarot/pkt/"
    private const val CACHE_DIR = "tarot_cards"

    private val mem = LruCache<String, ImageBitmap>(24)

    /** 牌 -> Gitee 文件名（不含扩展名） */
    private fun giteeName(card: Tarot.Card): String? = if (card.type == "Major") {
        MAJOR_INDEX[card.number]?.let { "ar%02d".format(it) }
    } else {
        val suit = when (card.suit) {
            "Wands" -> "wa"; "Cups" -> "cu"; "Swords" -> "sw"; "Pentacles" -> "pe"
            else -> null
        }
        val rank = MINOR_RANK[card.rank]
        if (suit != null && rank != null) "$suit$rank" else null
    }

    private val MAJOR_INDEX = mapOf(
        "0" to 0, "I" to 1, "II" to 2, "III" to 3, "IV" to 4, "V" to 5, "VI" to 6, "VII" to 7,
        "VIII" to 8, "IX" to 9, "X" to 10, "XI" to 11, "XII" to 12, "XIII" to 13, "XIV" to 14,
        "XV" to 15, "XVI" to 16, "XVII" to 17, "XVIII" to 18, "XIX" to 19, "XX" to 20, "XXI" to 21
    )

    private val MINOR_RANK = mapOf(
        "Ace" to "ac", "Two" to "02", "Three" to "03", "Four" to "04", "Five" to "05",
        "Six" to "06", "Seven" to "07", "Eight" to "08", "Nine" to "09", "Ten" to "10",
        "Page" to "pa", "Knight" to "kn", "Queen" to "qu", "King" to "ki"
    )

    private fun cacheDir(context: Context) = File(context.filesDir, CACHE_DIR)

    /** 已下载的牌面张数（本地文件数） */
    fun downloadedCount(context: Context): Int =
        cacheDir(context).listFiles()?.count { it.extension == "jpg" && it.length() > 0 } ?: 0

    /** 只读本地缓存，不联网。未下载返回 null */
    fun loadCached(context: Context, card: Tarot.Card): ImageBitmap? {
        val name = giteeName(card) ?: return null
        mem.get(name)?.let { return it }
        val file = File(cacheDir(context), "$name.jpg")
        if (!file.exists() || file.length() == 0L) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()?.also { mem.put(name, it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 下载全部 78 张到内部存储（后台线程，24 线程并发加速）。
     * @param onProgress 回调 (已完成数, 总数)
     * @return 成功下载张数
     */
    suspend fun downloadAll(context: Context, onProgress: (Int, Int) -> Unit): Int = withContext(Dispatchers.IO) {
        val dir = cacheDir(context)
        dir.mkdirs()
        val files = Tarot.DECK.mapNotNull { giteeName(it)?.plus(".jpg") }.distinct()
        val total = files.size
        val done = AtomicInteger(0)
        val succeeded = AtomicInteger(0)
        // 24 并发限流
        val gate = Semaphore(24)

        files.map { fileName ->
            async {
                gate.withPermit {
                    val file = File(dir, fileName)
                    // 已存在且非空则跳过（断点续传）
                    if (!file.exists() || file.length() == 0L) {
                        val bytes = download(fileName)
                        if (bytes != null) {
                            try {
                                file.writeBytes(bytes)
                                succeeded.incrementAndGet()
                            } catch (e: Exception) { }
                        }
                    } else {
                        succeeded.incrementAndGet()
                    }
                    val n = done.incrementAndGet()
                    onProgress(n, total)
                }
            }
        }.awaitAll()

        mem.evictAll()
        TarotSettings.bumpImageVersion()
        succeeded.get()
    }

    /** 删除已下载的牌面（释放空间） */
    fun clearAll(context: Context) {
        cacheDir(context).deleteRecursively()
        mem.evictAll()
        TarotSettings.bumpImageVersion()
    }

    private fun download(fileName: String): ByteArray? = try {
        val conn = URL(BASE + fileName).openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 20000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 FortuneStall/1.0")
        try {
            if (conn.responseCode == HttpURLConnection.HTTP_OK) conn.inputStream.use { it.readBytes() }
            else null
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}
