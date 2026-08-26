package com.shouna.manager.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.shouna.manager.data.AppGraph
import com.shouna.manager.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(private val context: Context) {

    data class Summary(
        val items: Int,
        val zones: Int,
        val photos: Int,
        val exportedAt: Long
    )

    private fun dbVersion(): Int =
        AppDatabase.get(context).openHelper.writableDatabase.version

    /** 导出备份到指定目录 Uri（SAF 授权目录），返回统计摘要 */
    suspend fun export(dirUri: Uri): Summary = withContext(Dispatchers.IO) {
        runCatching {
            val db = AppDatabase.get(context).openHelper.writableDatabase
            db.query("PRAGMA wal_checkpoint(FULL)").close()
        }

        val dbFile = context.getDatabasePath(AppDatabase.NAME)
        val items = AppGraph.itemRepository.getAllItems().size
        val zones = AppGraph.zoneRepository.getAllZones().size
        val exportedAt = System.currentTimeMillis()

        val photosDir = File(context.filesDir, "photos")
        val photoFiles = photosDir.listFiles()?.filter { it.isFile } ?: emptyList()

        val fileName = "shouna-manager-backup-${
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA).format(Date(exportedAt))
        }.zip"
        // createDocument 需要目录的 document Uri（tree Uri 会报 Invalid URI）
        val treeDocId = DocumentsContract.getTreeDocumentId(dirUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(dirUri, treeDocId)
        val docUri = DocumentsContract.createDocument(
            context.contentResolver, parentDocUri, "application/zip", fileName
        ) ?: throw IOException("无法在备份目录创建文件")

        context.contentResolver.openOutputStream(docUri)?.use { out ->
            ZipOutputStream(BufferedOutputStream(out)).use { zip ->
                listOf(
                    "database.db" to dbFile,
                    "database.db-wal" to File(dbFile.path + "-wal"),
                    "database.db-shm" to File(dbFile.path + "-shm"),
                    "settings.preferences_pb" to File(context.filesDir, "datastore/settings.preferences_pb")
                ).forEach { (name, file) ->
                    if (file.exists()) {
                        zip.putNextEntry(ZipEntry(name))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
                photoFiles.forEach { f ->
                    zip.putNextEntry(ZipEntry("photos/${f.name}"))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                val manifest = JSONObject()
                    .put("version", 1)
                    .put("dbVersion", dbVersion())
                    .put("exportedAt", exportedAt)
                    .put("items", items)
                    .put("zones", zones)
                    .put("photos", photoFiles.size)
                    .toString()
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        } ?: throw IOException("无法写入备份文件")

        Summary(items = items, zones = zones, photos = photoFiles.size, exportedAt = exportedAt)
    }

    /** 恢复备份：安全校验 + 原子替换数据库 + 替换照片目录 + 恢复设置 */
    suspend fun restore(uri: Uri): Summary = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val tmpDir = File(context.cacheDir, "restore-${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            resolver.openInputStream(uri)?.use { input ->
                ZipInputStream(BufferedInputStream(input)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name.contains("..") || entry.name.startsWith("/")) {
                            throw IOException("备份文件包含非法路径条目")
                        }
                        val outFile = File(tmpDir, entry.name)
                        outFile.outputStream().use { zip.copyTo(it) }
                        entry = zip.nextEntry
                    }
                }
            } ?: throw IOException("无法读取备份文件")

            val manifestFile = File(tmpDir, "manifest.json")
            val dbBackup = File(tmpDir, "database.db")
            if (!manifestFile.exists() || !dbBackup.exists()) {
                throw IOException("备份文件不完整")
            }
            val manifest = runCatching {
                JSONObject(manifestFile.readText(Charsets.UTF_8))
            }.getOrElse { throw IOException("备份文件损坏或不是有效的备份包") }
            if (manifest.optInt("version", 0) != 1) {
                throw IOException("不支持的备份版本")
            }

            val backupDbVersion = manifest.optInt("dbVersion", 0)
            val currentDbVersion = dbVersion()
            if (backupDbVersion > currentDbVersion) {
                throw IOException("备份来自更新版本的 App（数据库 v$backupDbVersion > 当前 v$currentDbVersion），请先升级 App 再恢复")
            }

            val dbFile = context.getDatabasePath(AppDatabase.NAME)
            val stageDir = File(context.cacheDir, "restore-stage-${System.currentTimeMillis()}").apply { mkdirs() }
            try {
                listOf("database.db", "database.db-wal", "database.db-shm").forEach { name ->
                    val src = File(tmpDir, name)
                    if (src.exists()) src.copyTo(File(stageDir, name), overwrite = true)
                }
            } catch (e: IOException) {
                stageDir.deleteRecursively()
                throw IOException("恢复数据写入失败（存储空间不足？）：${e.message}")
            }

            AppDatabase.closeForRestore()
            listOf("", "-wal", "-shm").forEach { ext -> File(dbFile.path + ext).delete() }
            listOf("database.db", "database.db-wal", "database.db-shm").forEach { name ->
                val staged = File(stageDir, name)
                if (staged.exists()) staged.renameTo(File(dbFile.path + name.removePrefix("database.db")))
            }
            stageDir.deleteRecursively()

            val photosBackup = File(tmpDir, "photos")
            val photosDir = File(context.filesDir, "photos")
            if (photosBackup.exists()) {
                val bak = File(context.filesDir, "photos.bak")
                bak.deleteRecursively()
                if (photosDir.exists()) photosDir.renameTo(bak)
                if (!photosBackup.renameTo(photosDir)) {
                    if (bak.exists()) bak.renameTo(photosDir)
                    throw IOException("恢复照片目录失败")
                }
                bak.deleteRecursively()
            }

            runCatching {
                val settingsBackup = File(tmpDir, "settings.preferences_pb")
                if (settingsBackup.exists()) {
                    val dest = File(context.filesDir, "datastore/settings.preferences_pb")
                    dest.parentFile?.mkdirs()
                    settingsBackup.copyTo(dest, overwrite = true)
                }
            }

            Summary(
                items = manifest.optInt("items", 0),
                zones = manifest.optInt("zones", 0),
                photos = manifest.optInt("photos", 0),
                exportedAt = manifest.optLong("exportedAt", 0L)
            )
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    /** 清理备份目录，只保留最近 keep 份 */
    suspend fun cleanupOld(dirUri: Uri, keep: Int = 7) = withContext(Dispatchers.IO) {
        try {
            val files = listBackupFiles(dirUri).sortedByDescending { it.first }
            if (files.size > keep) {
                files.drop(keep).forEach { (_, uri) ->
                    runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("Backup", "cleanupOld failed", t)
        }
    }

    private fun listBackupFiles(dirUri: Uri): List<Pair<String, Uri>> {
        val treeDocId = DocumentsContract.getTreeDocumentId(dirUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(dirUri, treeDocId)
        val result = mutableListOf<Pair<String, Uri>>()
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null, null, null
        )?.use { c ->
            val idCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (c.moveToNext()) {
                val name = c.getString(nameCol)
                if (name?.startsWith("shouna-manager-backup") == true && name.endsWith(".zip")) {
                    result.add(name to DocumentsContract.buildDocumentUriUsingTree(dirUri, c.getString(idCol)))
                }
            }
        }
        return result
    }

    fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
