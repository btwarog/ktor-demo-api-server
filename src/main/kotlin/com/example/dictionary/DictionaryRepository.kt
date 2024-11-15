package com.example.dictionary

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.*
import java.time.*

class DictionaryRepository {
  fun create(entry: DictionaryEntry): DictionaryEntry =
    transaction {
      val id =
        DictionaryEntries.insert {
          it[name] = entry.name
          it[definition] = entry.definition
          it[examples] = entry.examples.joinToString("|")
          it[relatedTerms] = entry.relatedTerms.joinToString("|")
          it[tags] = entry.tags.joinToString("|")
          it[category] = entry.category
          it[languages] = entry.languages.joinToString("|")
          it[createdAt] = LocalDateTime.now()
          it[updatedAt] = LocalDateTime.now()
          it[resources] = entry.resources.joinToString("|")
        } get DictionaryEntries.id
      entry.copy(id = id)
    }

  fun getAll(): List<DictionaryEntry> =
    transaction {
      DictionaryEntries.selectAll().map { toDictionaryEntry(it) }
    }

  fun getById(id: Int): DictionaryEntry? =
    transaction {
      DictionaryEntries
        .selectAll()
        .where { DictionaryEntries.id eq id }
        .map { toDictionaryEntry(it) }
        .singleOrNull()
    }

  fun update(
    id: Int,
    entry: DictionaryEntry,
  ): Boolean =
    transaction {
      val rowsUpdated =
        DictionaryEntries.update({ DictionaryEntries.id eq id }) {
          it[name] = entry.name
          it[definition] = entry.definition
          it[examples] = entry.examples.joinToString("|")
          it[relatedTerms] = entry.relatedTerms.joinToString("|")
          it[tags] = entry.tags.joinToString("|")
          it[category] = entry.category
          it[languages] = entry.languages.joinToString("|")
          it[updatedAt] = LocalDateTime.now()
        }
      rowsUpdated > 0
    }

  fun delete(id: Int): Boolean =
    transaction {
      val rowsDeleted = DictionaryEntries.deleteWhere { DictionaryEntries.id eq id }
      rowsDeleted > 0
    }

  fun search(query: String): List<DictionaryEntry> =
    transaction {
      val lowercaseQuery = query.lowercase()
      DictionaryEntries
        .selectAll()
        .where {
          (DictionaryEntries.name.lowerCase() like "%$lowercaseQuery%") or
            (DictionaryEntries.definition.lowerCase() like "%$lowercaseQuery%") or
            (DictionaryEntries.tags.lowerCase() like "%$lowercaseQuery%") or
            (DictionaryEntries.category.lowerCase() like "%$lowercaseQuery%")
        }.map { toDictionaryEntry(it) }
    }

  fun getAll(
    page: Int,
    pageSize: Int,
  ): List<DictionaryEntry> =
    transaction {
      DictionaryEntries
        .selectAll()
        .orderBy(DictionaryEntries.id)
        .limit(pageSize)
        .offset(((page - 1) * pageSize).toLong())
        .map { toDictionaryEntry(it) }
    }

  fun count(): Long =
    transaction {
      DictionaryEntries.selectAll().count()
    }

  private fun toDictionaryEntry(row: ResultRow): DictionaryEntry =
    DictionaryEntry(
      id = row[DictionaryEntries.id],
      name = row[DictionaryEntries.name],
      definition = row[DictionaryEntries.definition],
      examples = row[DictionaryEntries.examples].split("|"),
      relatedTerms = row[DictionaryEntries.relatedTerms].split("|"),
      tags = row[DictionaryEntries.tags].split("|"),
      category = row[DictionaryEntries.category],
      languages = row[DictionaryEntries.languages].split("|"),
      createdAt = row[DictionaryEntries.createdAt],
      updatedAt = row[DictionaryEntries.updatedAt],
      resources = row[DictionaryEntries.resources]?.split("|") ?: emptyList(),
    )
}
