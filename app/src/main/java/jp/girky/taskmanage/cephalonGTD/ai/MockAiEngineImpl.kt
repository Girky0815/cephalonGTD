package jp.girky.taskmanage.cephalonGTD.ai

import jp.girky.taskmanage.cephalonGTD.data.model.TaskContextTag
import jp.girky.taskmanage.cephalonGTD.data.model.TaskItem
import jp.girky.taskmanage.cephalonGTD.data.model.WbsSubTask
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockAiEngineImpl @Inject constructor() : AiEngine {
    override suspend fun isAvailable(): Boolean = true

    override suspend fun decomposeWbs(taskText: String): List<WbsSubTask> {
        val clean = taskText.trim()
        val actions = when {
            clean.contains("電話") || clean.contains("連絡") -> listOf(
                "連絡先と要件メモを手元に準備する (初動5秒)",
                "相手先へ発信し要件を伝達・調整する",
                "通話内容と決定事項をメモへ記録する"
            )
            clean.contains("起案") || clean.contains("申請") || clean.contains("決裁") -> listOf(
                "必要書類・過去の類似決裁フォルダを開く (初動5秒)",
                "起案理由および添付書類のドラフトを作成する",
                "決裁システムへ登録し担当者・上長へ回付する"
            )
            clean.contains("訪問") || clean.contains("面談") || clean.contains("調査") -> listOf(
                "対象者台帳・事前確認事項を印刷または確認する (初動5秒)",
                "現地へ向かい要件の聴取・調査を実施する",
                "聴取内容に基づきケース記録へ入力する"
            )
            else -> listOf(
                "作業フォルダまたは資料をPCで開く (初動5秒)",
                "『${clean}』の骨子・概要を作成する",
                "最終確認を行い完了・送付する"
            )
        }

        return actions.map {
            WbsSubTask(
                id = UUID.randomUUID().toString(),
                title = it,
                isCompleted = false
            )
        }
    }

    override suspend fun generateMicroStep(taskText: String): WbsSubTask {
        val title = "【初動5秒】${taskText.take(10)}…の関連ファイルまたはメモ帳を画面に開く"
        return WbsSubTask(
            id = UUID.randomUUID().toString(),
            title = title,
            isCompleted = false
        )
    }

    override suspend fun analyzePriorityAndMetadata(taskText: String): TaskMetadataResult {
        val clean = taskText.trim()
        val contextTag = when {
            clean.contains("電話") || clean.contains("TEL") -> TaskContextTag.PHONE
            clean.contains("PC") || clean.contains("メール") || clean.contains("システム") || clean.contains("入力") -> TaskContextTag.PC
            clean.contains("訪問") || clean.contains("外出") || clean.contains("現場") -> TaskContextTag.EXTERNAL
            clean.contains("待ち") || clean.contains("確認待ち") -> TaskContextTag.WAITING
            else -> TaskContextTag.OFFICE
        }

        val isTwoMinute = clean.contains("確認") && clean.length < 15 || clean.contains("返信")
        val estimatedMinutes = if (isTwoMinute) 2 else when (contextTag) {
            TaskContextTag.PHONE -> 10
            TaskContextTag.EXTERNAL -> 60
            TaskContextTag.PC -> 25
            else -> 15
        }

        val priorityScore = when {
            clean.contains("至急") || clean.contains("本日中") || clean.contains("緊急") -> 95
            clean.contains("決裁") || clean.contains("起案") -> 80
            clean.contains("訪問") || clean.contains("面談") -> 75
            isTwoMinute -> 60
            else -> 50
        }

        return TaskMetadataResult(
            title = clean,
            isTwoMinuteRule = isTwoMinute,
            estimatedMinutes = estimatedMinutes,
            contextTag = contextTag,
            priorityScore = priorityScore,
            nextActions = listOf("関連資料の確認", "作業実施", "完了記録")
        )
    }

    override suspend fun generateDraft(task: TaskItem, type: DraftType): String {
        return when (type) {
            DraftType.CASE_RECORD -> """
                【ケース記録ドラフト】
                ・日時: 令和${(java.time.Year.now().value - 2018)}年${java.time.LocalDate.now().monthValue}月${java.time.LocalDate.now().dayOfMonth}日
                ・対象件名: ${task.title}
                ・概要・経過: 本件に関し、要件の確認および対応を実施。
                ・対応内容:
                  - 関連法令・規定および台帳の照合完了。
                  - 次回対応方針を策定。
                ・特記事項: なし
            """.trimIndent()

            DraftType.APPROVAL_REASON -> """
                【起案・伺い理由書】
                件名: ${task.title}について
                1. 理由:
                   業務運営基準に基づき、所定の手続きを進める必要があるため。
                2. 根拠・内容:
                   関係書類の審査を行い、適正と認められるため承認を求める。
                3. 添付書類:
                   関係調書一式、確認資料
            """.trimIndent()

            DraftType.REPLY_MESSAGE -> """
                お疲れ様です。標記『${task.title}』の件についてご連絡申し上げます。
                現在、内容の精査および準備を進めております。
                詳細が整い次第、改めて正式にご案内いたしますので今しばらくお待ちいただけますと幸いです。
                よろしくお願いいたします。
            """.trimIndent()
        }
    }

    override suspend fun generateDailySummary(
        completedTasks: List<TaskItem>,
        pendingTasks: List<TaskItem>
    ): String {
        val today = java.time.LocalDate.now().toString()
        val completedSection = if (completedTasks.isEmpty()) {
            "（完了タスクなし）"
        } else {
            completedTasks.joinToString("\n") { "- [x] ${it.title} (${it.contextTag.label})" }
        }

        val pendingSection = if (pendingTasks.isEmpty()) {
            "（繰越タスクなし）"
        } else {
            pendingTasks.joinToString("\n") { "- [ ] ${it.title} (${it.contextTag.label}) 推定${it.estimatedMinutes}分" }
        }

        return """
            # 日次業務実績サマリー ($today)

            ## 本日の完了タスク (${completedTasks.size}件)
            $completedSection

            ## 翌日繰り越し・未完了タスク (${pendingTasks.size}件)
            $pendingSection

            ---
            *総括: 本日もお疲れ様でした。残タスクは翌営業日に優先着手してください。*
        """.trimIndent()
    }
}
