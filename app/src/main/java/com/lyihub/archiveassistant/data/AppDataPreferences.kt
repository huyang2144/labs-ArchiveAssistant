package com.lyihub.archiveassistant.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lyihub.archiveassistant.domain.ContentType
import com.lyihub.archiveassistant.domain.DocumentFormat
import com.lyihub.archiveassistant.domain.KnowledgeItem
import com.lyihub.archiveassistant.domain.Topic
import org.json.JSONArray
import org.json.JSONObject

object AppDataPreferences {
  val TopicsKey = stringPreferencesKey("app_topics_json")
  val ItemsKey = stringPreferencesKey("app_items_json")
  val DemoDataVersionKey = intPreferencesKey("demo_data_version")

  fun decodeTopics(preferences: Preferences): List<Topic> {
    val json = preferences[TopicsKey] ?: return emptyList()
    return try {
      val array = JSONArray(json)
      (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        Topic(
          id = obj.getString("id"),
          title = obj.getString("title"),
          iconName = obj.getString("iconName"),
          iconColor = obj.getString("iconColor"),
          updatedAtEpochMillis = obj.getLong("updatedAtEpochMillis"),
        )
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun encodeTopics(topics: List<Topic>, preferences: MutablePreferences) {
    val array = JSONArray()
    topics.forEach { topic ->
      array.put(
        JSONObject().apply {
          put("id", topic.id)
          put("title", topic.title)
          put("iconName", topic.iconName)
          put("iconColor", topic.iconColor)
          put("updatedAtEpochMillis", topic.updatedAtEpochMillis)
        }
      )
    }
    preferences[TopicsKey] = array.toString()
  }

  fun decodeItems(preferences: Preferences): List<KnowledgeItem> {
    val json = preferences[ItemsKey] ?: return emptyList()
    return try {
      val array = JSONArray(json)
      (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        KnowledgeItem(
          id = obj.getString("id"),
          topicId = obj.getString("topicId"),
          contentType = ContentType.valueOf(obj.getString("contentType")),
          title = obj.getString("title"),
          summary = obj.optString("summary", ""),
          fullText = obj.optString("fullText", ""),
          sourceUrl = obj.optString("sourceUrl", "").takeIf { it.isNotEmpty() },
          imageResName = obj.optString("imageResName", "").takeIf { it.isNotEmpty() },
          documentFormat =
            obj
              .optString("documentFormat", "")
              .takeIf { it.isNotEmpty() }
              ?.let { runCatching { DocumentFormat.valueOf(it) }.getOrNull() },
          fileName = obj.optString("fileName", "").takeIf { it.isNotEmpty() },
          fileSize =
            if (obj.has("fileSize") && !obj.isNull("fileSize")) obj.getLong("fileSize") else null,
          createdAtEpochMillis = obj.getLong("createdAtEpochMillis"),
        )
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun encodeItems(items: List<KnowledgeItem>, preferences: MutablePreferences) {
    val array = JSONArray()
    items.forEach { item ->
      array.put(
        JSONObject().apply {
          put("id", item.id)
          put("topicId", item.topicId)
          put("contentType", item.contentType.name)
          put("title", item.title)
          put("summary", item.summary)
          put("fullText", item.fullText)
          item.sourceUrl?.let { put("sourceUrl", it) }
          item.imageResName?.let { put("imageResName", it) }
          item.documentFormat?.let { put("documentFormat", it.name) }
          item.fileName?.let { put("fileName", it) }
          item.fileSize?.let { put("fileSize", it) }
          put("createdAtEpochMillis", item.createdAtEpochMillis)
        }
      )
    }
    preferences[ItemsKey] = array.toString()
  }

  fun decodeDemoDataVersion(preferences: Preferences): Int = preferences[DemoDataVersionKey] ?: 0

  fun encodeDemoDataVersion(version: Int, preferences: MutablePreferences) {
    preferences[DemoDataVersionKey] = version
  }
}
