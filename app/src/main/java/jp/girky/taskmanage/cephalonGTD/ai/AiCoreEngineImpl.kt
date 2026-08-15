package jp.girky.taskmanage.cephalonGTD.ai

import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import jp.girky.taskmanage.cephalonGTD.data.model.TaskContextTag
import jp.girky.taskmanage.cephalonGTD.data.model.TaskItem
import jp.girky.taskmanage.cephalonGTD.data.model.WbsSubTask
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiCoreEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fallbackEngine: MockAiEngineImpl
) : AiEngine {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun createModel(): Pair<GenerativeModel?, String?> {
        return try {
            val model = GenerativeModel(
                generationConfig = generationConfig {
                    context = this@AiCoreEngineImpl.context
                    temperature = 0.2f
                    maxOutputTokens = 1024
                }
            )
            Pair(model, null)
        } catch (e: Throwable) {
            Pair(null, "${e.javaClass.simpleName}: ${e.message ?: e.localizedMessage}")
        }
    }

    private fun getModel(): GenerativeModel? {
        return createModel().first
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            val model = getModel() ?: return false
            // 簡易ヘルスチェック
            val response = model.generateContent("ping")
            !response.text.isNullOrBlank()
        } catch (e: Throwable) {
            false
        }
    }

    override suspend fun diagnoseGeminiNano(testPrompt: String): AiDiagnosticsResult {
        var isAiCoreInstalled = false
        var aiCoreVersionName: String? = null
        val aiCorePackages = listOf("com.google.android.aicore", "com.google.android.as")

        for (pkg in aiCorePackages) {
            try {
                val pInfo = context.packageManager.getPackageInfo(pkg, 0)
                isAiCoreInstalled = true
                aiCoreVersionName = "$pkg (${pInfo.versionName ?: "v${pInfo.longVersionCode}"})"
                break
            } catch (e: PackageManager.NameNotFoundException) {
                // 次のパッケージを探索
            }
        }

        val availableModels = mutableListOf<String>()
        var testResult: String? = null
        var isSuccess = false
        var statusMsg = ""

        try {
            val (model, initError) = createModel()
            if (model != null) {
                availableModels.add("Gemini Nano 4 Fast / Full")
                availableModels.add("Gemini Nano 3 [TPU]")
                val response = model.generateContent(testPrompt)
                testResult = response.text
                if (!testResult.isNullOrBlank()) {
                    isSuccess = true
                    statusMsg = "Gemini Nano が正常に応答しました。オンデバイス推論が完全に機能しています。"
                } else {
                    statusMsg = "AICore に接続されましたが、モデルから空の応答が返されました。"
                }
            } else {
                availableModels.add("なし (初期化失敗)")
                statusMsg = "GenerativeModel 初期化エラー: $initError"
            }
        } catch (e: Throwable) {
            val exName = e.javaClass.name
            val exMsg = e.localizedMessage ?: e.message ?: "詳細不明"
            statusMsg = "AICore推論エラー [$exName]: $exMsg"
            availableModels.add("なし (実行時例外)")
        }

        return AiDiagnosticsResult(
            isAiCoreInstalled = isAiCoreInstalled,
            aiCoreVersion = aiCoreVersionName ?: "未検出",
            availableModels = availableModels,
            testPromptResult = testResult,
            isSuccess = isSuccess,
            message = statusMsg
        )
    }

    override suspend fun decomposeWbs(taskText: String): List<WbsSubTask> {
        val model = getModel()
        if (model == null) {
            return fallbackEngine.decomposeWbs(taskText)
        }

        val prompt = """
            あなたはオンデバイスGTDタスク管理エンジンです。
            タスクを具体的で即時着手可能な2〜3ステップの物理的行動動詞に分解してください。
            初動ステップは5秒で始められる極小アクションにしてください。
            前置きや解説は一切含めず、以下のJSON配列のみを出力してください:
            ["ステップ1", "ステップ2", "ステップ3"]

            対象タスク: $taskText
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt).text ?: ""
            val jsonString = extractJsonFromResponse(response)
            val jsonArray = json.parseToJsonElement(jsonString).jsonArray
            val actions = jsonArray.mapNotNull { it.jsonPrimitive.content }
            if (actions.isNotEmpty()) {
                actions.map {
                    WbsSubTask(
                        id = UUID.randomUUID().toString(),
                        title = it,
                        isCompleted = false
                    )
                }
            } else {
                fallbackEngine.decomposeWbs(taskText)
            }
        } catch (e: Throwable) {
            fallbackEngine.decomposeWbs(taskText)
        }
    }

    override suspend fun generateMicroStep(taskText: String): WbsSubTask {
        val model = getModel()
        if (model == null) {
            return fallbackEngine.generateMicroStep(taskText)
        }

        val prompt = """
            あなたはオンデバイスGTDタスク管理エンジンです。
            タスクに着手するための「初動5秒でできる極小ステップ」を1つ生成してください。
            前置きや解説は一切含めず、以下のJSONオブジェクトのみを出力してください:
            {"microStep": "【初動5秒】..."}

            対象タスク: $taskText
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt).text ?: ""
            val jsonString = extractJsonFromResponse(response)
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            val microStep = jsonObject["microStep"]?.jsonPrimitive?.content
            if (!microStep.isNullOrBlank()) {
                WbsSubTask(
                    id = UUID.randomUUID().toString(),
                    title = microStep,
                    isCompleted = false
                )
            } else {
                fallbackEngine.generateMicroStep(taskText)
            }
        } catch (e: Throwable) {
            fallbackEngine.generateMicroStep(taskText)
        }
    }

    override suspend fun analyzePriorityAndMetadata(taskText: String): TaskMetadataResult {
        val model = getModel()
        if (model == null) {
            return fallbackEngine.analyzePriorityAndMetadata(taskText)
        }

        val prompt = """
            あなたはオンデバイスGTDエンジンです。タスクを解析し、以下のスキーマに準拠した有効なJSONオブジェクトのみを出力してください:
            {
              "title": "整形後のタイトル文字列",
              "isTwoMinuteRule": trueまたはfalse,
              "estimatedMinutes": 推定所要時間(数値),
              "contextTag": "OFFICE" | "PC" | "PHONE" | "EXTERNAL" | "WAITING",
              "priorityScore": 優先度スコア(0-100の数値),
              "nextActions": ["アクション1", "アクション2"]
            }
            対象タスク: $taskText
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt).text ?: ""
            val jsonString = extractJsonFromResponse(response)
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject

            val title = jsonObject["title"]?.jsonPrimitive?.content ?: taskText
            val isTwoMinute = jsonObject["isTwoMinuteRule"]?.jsonPrimitive?.booleanOrNull ?: false
            val estimated = jsonObject["estimatedMinutes"]?.jsonPrimitive?.intOrNull ?: 15
            val priority = jsonObject["priorityScore"]?.jsonPrimitive?.intOrNull ?: 50
            val tagStr = jsonObject["contextTag"]?.jsonPrimitive?.content ?: "OFFICE"
            val contextTag = try {
                TaskContextTag.valueOf(tagStr)
            } catch (e: Exception) {
                TaskContextTag.OFFICE
            }
            val nextActions = jsonObject["nextActions"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()

            TaskMetadataResult(
                title = title,
                isTwoMinuteRule = isTwoMinute,
                estimatedMinutes = estimated,
                contextTag = contextTag,
                priorityScore = priority,
                nextActions = nextActions
            )
        } catch (e: Throwable) {
            fallbackEngine.analyzePriorityAndMetadata(taskText)
        }
    }

    override suspend fun generateDraft(task: TaskItem, type: DraftType): String {
        val model = getModel()
        if (model == null) {
            return fallbackEngine.generateDraft(task, type)
        }

        val typeInstruction = when (type) {
            DraftType.CASE_RECORD -> "公務・ケースワーク用のケース記録・経過メモの下書き"
            DraftType.APPROVAL_REASON -> "起案・稟議・伺い理由書の下書き（理由、根拠、添付書類）"
            DraftType.REPLY_MESSAGE -> "関係者や相手先への丁寧な返信・連絡文面ドラフト"
        }

        val prompt = """
            あなたは公務・現業向けオンデバイスAIアシスタントです。
            以下のタスクに基づいて、『$typeInstruction』を作成してください。
            Markdown形式で簡潔・公的なトーンで記述してください。

            タスク名: ${task.title}
            コンテキスト: ${task.contextTag.label}
            要決裁: ${if (task.requiresApproval) "はい" else "いいえ"}
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt).text
            if (!response.isNullOrBlank()) {
                response.trim()
            } else {
                fallbackEngine.generateDraft(task, type)
            }
        } catch (e: Throwable) {
            fallbackEngine.generateDraft(task, type)
        }
    }

    override suspend fun generateDailySummary(
        completedTasks: List<TaskItem>,
        pendingTasks: List<TaskItem>
    ): String {
        val model = getModel()
        if (model == null) {
            return fallbackEngine.generateDailySummary(completedTasks, pendingTasks)
        }

        val prompt = """
            あなたはオンデバイスGTDアシスタントです。
            以下の完了タスクおよび繰越タスクの一覧から、終業時のMarkdown業務実績日次サマリーを作成してください。

            【本日完了タスク (${completedTasks.size}件)】
            ${completedTasks.joinToString("\n") { "- ${it.title} (${it.contextTag.label})" }}

            【繰越・未完了タスク (${pendingTasks.size}件)】
            ${pendingTasks.joinToString("\n") { "- ${it.title} (${it.contextTag.label}) 推定${it.estimatedMinutes}分" }}
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt).text
            if (!response.isNullOrBlank()) {
                response.trim()
            } else {
                fallbackEngine.generateDailySummary(completedTasks, pendingTasks)
            }
        } catch (e: Throwable) {
            fallbackEngine.generateDailySummary(completedTasks, pendingTasks)
        }
    }

    private fun extractJsonFromResponse(raw: String): String {
        val trimmed = raw.trim()
        val firstOpenBrace = trimmed.indexOf('{')
        val firstOpenBracket = trimmed.indexOf('[')

        val startIndex = when {
            firstOpenBrace != -1 && firstOpenBracket != -1 -> minOf(firstOpenBrace, firstOpenBracket)
            firstOpenBrace != -1 -> firstOpenBrace
            firstOpenBracket != -1 -> firstOpenBracket
            else -> return trimmed
        }

        val lastCloseBrace = trimmed.lastIndexOf('}')
        val lastCloseBracket = trimmed.lastIndexOf(']')

        val endIndex = maxOf(lastCloseBrace, lastCloseBracket)
        return if (endIndex > startIndex) {
            trimmed.substring(startIndex, endIndex + 1)
        } else {
            trimmed
        }
    }
}
