# GEMINI.md - Cephalon GTD (プロジェクト仕様書およびエージェント向け開発ガイドライン)

## 1. プロジェクト概要および基本原則
* **プロジェクト名:** Cephalon GTD (別名候補: Noctua Task)
* **目的:** 公務・現業・ケースワーク等の高い守秘義務が求められる業務に向けた、完全オンデバイスAI（Gemini Nano）駆動型のプライバシー重視GTDタスク管理Androidアプリ。
* **基本原則:**
  * **完全オフライン動作 (Zero Cloud Dependency):** プライバシーと機密保持のため、タスクデータやプロンプトを外部へ一切送信しない。
  * **ユーザー主導型AI (User-Driven AI):** 常時バックグラウンド実行を行わず、明示的なUI操作をトリガーとしてAICore (Gemini Nano) を駆動（端末負荷・バッテリー消費の抑制）。
  * **業務ステートマシンの分離:** 通常タスクと決裁フローが必要な行政タスク（生活保護等の起案・決裁待ち）の状態遷移を明確に分離。

---

## 2. 技術スタックおよび対象環境
* **プラットフォーム:** Android APK
* **ターゲットSDK:** 37 (Android 17)
* **コンパイルSDK:** 37
* **最小SDK:** 34 または 35 (Android AICore / Gemini Nanoをサポートする端末、Pixel 9シリーズ以降を想定)
* **開発言語:** Kotlin 2.x (Coroutines & StateFlowを全面採用)
* **UIフレームワーク:** Jetpack Compose (Material 3 Expressive)
* **ローカルDB:** Room Database (SQLite) + kotlinx.serialization
* **オンデバイスAI:** Google AI Edge SDK / Android AICore (Gemini Nano)
* **DI (依存性注入):** Hilt (Dagger)
* **OTA更新機構:** Noctua Hub Composeに準拠したGitHub Releases経由の直接APKアップデート機能 (※ネットワーク権限はこのOTAチェッカーにのみ限定して使用)

---

## 3. セキュリティおよびプライバシー制約
* **アプリ起動時認証 (設定連動):**
  * アプリ内設定で切り替え可能: 「無効」、「生体認証 / 端末認証 (BiometricPrompt利用)」、「アプリ独自PIN / パスワード (Android Keystoreで暗号化保持)」。
  * コールドブート時およびバックグラウンド復帰時の自動ロック制御。
* **IMEシークレットモード強制 (IME Incognito Mode):**
  * すべてのTextFieldおよびOutlinedTextFieldコンポーネントにおいて、学習・クラウド送信を防止する設定を強制適用:
    keyboardOptions = KeyboardOptions(privateImeOptions = "nm") または EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING。
* **画面キャプチャ・録画保護 (設定連動):**
  * アプリ内設定で「スクリーンショット禁止」のON/OFFを提供（デフォルトは開発利便性のためOFF）。
  * 有効化時、動的にWindowへ WindowManager.LayoutParams.FLAG_SECURE を付与。

---

## 4. データモデルおよび状態遷移仕様

### 4.1 ステータス遷移フロー (State Machine)
* **通常タスク (requiresApproval = false):**
  TODO -> (チェックボックス押下) -> DONE
* **要決裁タスク (requiresApproval = true):**
  TODO (起案作成/作業中) -> (チェック1回目: 起案回付) -> WAITING_APPROVAL (決裁待ち/GTDの連絡待ち) -> (チェック2回目: 決裁戻り) -> DONE

### 4.2 コアデータモデル (Kotlin定義)
```
package com.cephalon.gtd.data.model

import kotlinx.serialization.Serializable

enum class TaskStatus {
    TODO,
    WAITING_APPROVAL,
    DONE
}

enum class TaskContextTag {
    OFFICE,     // @職場
    PC,         // @PC
    PHONE,      // @電話
    EXTERNAL,   // @外出
    WAITING     // @連絡待ち
}

@Serializable
data class WbsSubTask(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)

@Serializable
data class TaskItem(
    val id: String,
    val groupId: String = "default",
    val rawInput: String,
    val title: String,
    val requiresApproval: Boolean = false,
    val status: TaskStatus = TaskStatus.TODO,
    val contextTag: TaskContextTag = TaskContextTag.OFFICE,
    val estimatedMinutes: Int = 15,
    val isTwoMinuteRule: Boolean = false,
    val priorityScore: Int = 0,
    val nextPhysicalActions: List<WbsSubTask> = emptyList(),
    val approvalInitiatedAt: Long? = null,
    val draftingDeadline: Long? = null,
    val finalDeadline: Long? = null,
    val isStalled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

---

## 5. Material 3 Expressive UI実装仕様

### 5.1 サーフェス階層およびカラートークン (ライトテーマ基準)
* **アプリ / ウィンドウ背景色:** MaterialTheme.colorScheme.surfaceContainer
* **グループ化リストコンテナ (カード背景色):** MaterialTheme.colorScheme.surface
* **Heroタスク (Top Focus Card):** MaterialTheme.colorScheme.primaryContainer (文字色: onPrimaryContainer)
* **決裁待ちコンテナ:** MaterialTheme.colorScheme.surfaceContainerHigh (不透明度 85%)
* **区切り線 (Dividers):** MaterialTheme.colorScheme.outlineVariant

### 5.2 コンポーネント仕様
* **グループ化インセットリスト (Grouped Lists):** RoundedCornerShape(24.dp) を適用したサーフェスコンテナ内にリスト行を配置し、アイテム間を HorizontalDivider で区切る。
* **Top Focus (Hero) Card:** 画面最上部に着手可能な最優先のTODOタスクを1件ハイライト表示。大型チェックボックスと直近の物理的初動アクションを明示。
* **連結ボタングループ (Bottom Action Strip):**
  画面下部に連結ボタングループを配置:
  [AI WBS分解] | [AI 優先度解析] | [日次締めサマリー]
* **エクスプレッシブモーション:** タスク状態遷移時のスプリングアニメーション、WBS展開時の animateContentSize、リスト並び替え時の Modifier.animateItem() を適用。

---

## 6. AI機能パイプライン (Gemini Nano / AICore)

1. **把握 (Inbox):** AI推論を挟まずローカルDB (Room) へ即時保存。
2. **見極め (WBS分解 / マイクロステップ):** 抽象的なタスクを2〜3ステップの具体的な行動動詞へ分解（着手困難なタスクには初動5秒の極小ステップを生成）。
3. **整理 (メタデータ付与):** コンテキストタグ付与 (@PC, @職場等)、2分ルール判定、決裁リードタイム（半日〜1日）を逆算した起案締切日時の算出。
4. **選択・実行 (着手支援ドラフト):** メモからケース記録下書き、起案理由、返信文面などの初期ドラフトをローカル生成。
5. **更新 (日次レビュー):** 終業時のMarkdown実績サマリー生成および未完了タスクの翌日繰り越し。

### 6.1 プロンプト出力スキーマ制約
すべてのGemini Nanoプロンプトは前置きやMarkdown解説文を排除し、厳格にJSONスキーマのみを出力させること:

```
あなたはオンデバイスGTDエンジンです。タスクを解析し、以下のスキーマに準拠した有効なJSONオブジェクトのみを出力してください:
{
  "title": "整形後のタイトル文字列",
  "isTwoMinuteRule": trueまたはfalse,
  "estimatedMinutes": 推定所要時間(数値),
  "contextTag": "OFFICE" | "PC" | "PHONE" | "EXTERNAL",
  "nextActions": ["アクション1", "アクション2"]
}
対象タスク: {{task_text}}
```
---

## 7. エージェント向け開発・実装規約
* **アーキテクチャ設計:** Modern Android Architecture (Clean Architecture + Repository Pattern + MVVM/MVI) に厳格に従う。
* **不要なネットワークコードの排除:** サードパーティの解析SDK、リモートクラッシュログ、クラウド型LLM SDKを一切含めず、ロジックを完全ローカルで完結させる。
* **Composeファースト:** レガシーなXMLレイアウトを排除し、すべてのUIをJetpack ComposeおよびMaterial 3 Expressiveコンポーネントで構築する。