package com.android.customize.overlay.controller

import android.content.Context
import com.android.customize.common.logger.MyLogger
import com.android.customize.overlay.model.CategoryInfo
import com.android.launcher3.model.data.AppInfo
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class CategoryController() {
    private var categoryInfos = emptyList<CategoryInfo>()
    private val appInfosCache: HashMap<String, AppInfo> = HashMap()

    fun getCategories(context: Context, appInfos: List<AppInfo>): List<CategoryInfo> {
        categoryInfos = categoryInfos.ifEmpty { readFromAssets(context) }
        appInfos.forEach { appInfosCache.put(it.componentName.flattenToString(), it) }
        val categoryInfos = categoryInfos.mapIndexed { index, categoryInfo ->
            if (index + 1 < categoryInfos.size) {
                val newComponentNames = categoryInfo.componentNames
                    .mapNotNull { componentName ->
                        val appInfo = appInfos.firstOrNull {
                            it.componentName.flattenToString()
                                .contains(componentName)
                        }
                        appInfo?.componentName?.flattenToString()
                    }.toSet()
                categoryInfo.copy(componentNames = newComponentNames)
            } else {
                val componentNames = categoryInfos.flatMap { it.componentNames }
                val newComponentNames = appInfos.filter { appInfo ->
                    componentNames.none { appInfo.componentName.flattenToString().contains(it) }
                }.map { it.componentName.flattenToString() }.toSet()
                categoryInfo.copy(componentNames = newComponentNames)
            }
        }
        myLogger.d("getCategories: $categoryInfos")
        return categoryInfos
    }

    fun getAppInfo(componentName: String): AppInfo? {
        return appInfosCache[componentName]
    }

    private fun readFromAssets(context: Context): List<CategoryInfo> {
        myLogger.d("readFromAssets, language: ${context.resources.configuration.locales[0].language}")
        try {
            val categories = mutableListOf<CategoryInfo>()
            val jsonString = context.readAssetFileAsString("folders.json")
            val jsonObject = JSONObject(jsonString)
            val foldersArray = jsonObject.getJSONArray("folders")
            for (i in 0 until foldersArray.length()) {
                val folderObject = foldersArray.getJSONObject(i)
                val language = context.resources.configuration.locales[0].language
                val folderName = if (language.contains("zh")) {
                    folderObject.getJSONObject("name")
                        .getString("zh")
                } else {
                    folderObject.getJSONObject("name")
                        .getString("en")
                }
                
                val appsArray = folderObject.optJSONArray("apps")
                if (appsArray != null) {
                    val apps = mutableSetOf<String>()
                    for (j in 0 until appsArray.length()) {
                        val packageName = appsArray.getString(j)
                        apps.add(packageName)
                    }
                    categories.add(CategoryInfo(folderName, apps))
                } else {
                    categories.add(CategoryInfo(folderName, emptySet()))
                }
            }
            myLogger.d("readFromAssets: $categories")
            return categories
        } catch (e: Exception) {
            myLogger.e("readFromAssets: ${e.stackTraceToString()}", e)
        }
        myLogger.w("readFromAssets: empty")
        return emptyList()
    }

    companion object {
        private val myLogger = MyLogger("FolderController")
        private fun Context.readAssetFileAsString(fileName: String): String {
            return assets.open(fileName)
                .bufferedReader(StandardCharsets.UTF_8)
                .use { it.readText() }
        }
    }
}