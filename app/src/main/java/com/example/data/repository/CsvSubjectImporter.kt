package com.example.data.repository

import android.content.Context
import com.example.data.local.SubjectDao
import com.example.data.local.SubjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class CsvSubjectImporter(
    private val context: Context,
    private val subjectDao: SubjectDao
) {
    suspend fun importInitialSubjectsIfNeeded() = withContext(Dispatchers.IO) {
        if (subjectDao.countPresetSubjects() > 0) return@withContext

        val subjects = mutableListOf<SubjectEntity>()
        try {
            context.assets.open(CSV_ASSET_NAME).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val headerLine = reader.readLine()?.removePrefix("\uFEFF") ?: return@use
                    if (headerLine.isBlank()) return@use

                    var line = reader.readLine()
                    while (line != null) {
                        val trimmed = line.removePrefix("\uFEFF").trim()
                        if (trimmed.isNotEmpty()) {
                            val columns = parseCsvRow(trimmed)
                            if (columns.size >= 4) {
                                val technology = columns[0].trim()
                                val semester = columns[1].trim()
                                val subjectName = columns[2].trim()
                                val subjectCode = columns[3].trim()
                                if (technology.isNotEmpty() && semester.isNotEmpty() && subjectName.isNotEmpty()) {
                                    subjects.add(
                                        SubjectEntity(
                                            technology = technology,
                                            semester = semester,
                                            subjectName = subjectName,
                                            subjectCode = subjectCode,
                                            isPreset = true
                                        )
                                    )
                                }
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }
            if (subjects.isNotEmpty()) {
                subjectDao.insertAllIgnore(subjects)
            }
        } catch (_: Exception) {
            // Keep app functional offline even if asset fails to open in unusual test environments.
        }
    }

    private fun parseCsvRow(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    companion object {
        const val CSV_ASSET_NAME = "semester_1_subjects.csv"
    }
}
