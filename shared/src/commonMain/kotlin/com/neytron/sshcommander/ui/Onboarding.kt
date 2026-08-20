package com.neytron.sshcommander.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neytron.sshcommander.data.AppSettings
import kotlinx.coroutines.launch

/** One page of the first-run tour. */
data class TourStep(val title: String, val description: String)

private enum class OnboardingStep { Welcome, Language, ImportChoice, Tour }

/**
 * First-run setup shown once (until the user finishes it).
 *
 * Flow:
 *  1. Welcome dialog — "Would you like to learn how to use SSH Commander?"
 *  2. "Yes"  → language selection → tour of the app tabs.
 *     "No"   → offer to import an existing JSON backup.
 *
 * [tourSteps] differ per platform (desktop panes vs phone screens).
 * [onImportJson] wires the platform file picker + backup import.
 */
@Composable
fun OnboardingGate(
    settings: AppSettings?,
    tourSteps: List<TourStep>,
    onImportJson: (() -> Unit)? = null,
    content: @Composable () -> Unit = {}
) {
    val completed by settings?.onboardingCompleted?.collectAsState(initial = null)
        ?: remember { mutableStateOf(true) }
    var step by remember { mutableStateOf<OnboardingStep?>(null) }
    var tourIndex by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // Show the welcome dialog once, right after the first frame is laid out.
    LaunchedEffect(completed) {
        if (completed == true) {
            step = null
        } else if (completed == false && step == null) {
            step = OnboardingStep.Welcome
        }
    }

    fun finishOnboarding() {
        scope.launch { settings?.setOnboardingCompleted(true) }
        step = null
        tourIndex = 0
    }

    content()

    when (step) {
        OnboardingStep.Welcome -> WelcomeDialog(
            onYes = { step = OnboardingStep.Language },
            onNo = { step = OnboardingStep.ImportChoice }
        )

        OnboardingStep.Language -> LanguageDialog(
            settings = settings,
            onContinue = {
                tourIndex = 0
                step = OnboardingStep.Tour
            }
        )

        OnboardingStep.ImportChoice -> ImportChoiceDialog(
            onImport = {
                onImportJson?.invoke()
                finishOnboarding()
            },
            onSkip = { finishOnboarding() }
        )

        OnboardingStep.Tour -> TourDialog(
            steps = tourSteps,
            index = tourIndex,
            onNext = { if (tourIndex < tourSteps.lastIndex) tourIndex++ },
            onBack = { if (tourIndex > 0) tourIndex-- },
            onFinish = { finishOnboarding() },
            onSkip = { finishOnboarding() }
        )

        null -> Unit
    }
}

@Composable
private fun WelcomeDialog(onYes: () -> Unit, onNo: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNo,
        title = { Text(AppStrings.welcomeTitle, fontWeight = FontWeight.Bold) },
        text = { Text(AppStrings.welcomeQuestion) },
        confirmButton = {
            Button(onClick = onYes) { Text(AppStrings.yes) }
        },
        dismissButton = {
            TextButton(onClick = onNo) { Text(AppStrings.no) }
        }
    )
}

@Composable
private fun LanguageDialog(settings: AppSettings?, onContinue: () -> Unit) {
    val currentLanguage = AppStrings.language
    var selected by remember { mutableStateOf(currentLanguage) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { /* language step cannot be skipped */ },
        title = { Text(AppStrings.languageLabel, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                LanguageOption(
                    label = AppStrings.langEn,
                    selected = selected == "en",
                    onClick = { selected = "en" }
                )
                LanguageOption(
                    label = AppStrings.langRu,
                    selected = selected == "ru",
                    onClick = { selected = "ru" }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch { settings?.setLanguage(selected) }
                AppStrings.language = selected
                onContinue()
            }) {
                Text(AppStrings.next)
            }
        }
    )
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ImportChoiceDialog(onImport: () -> Unit, onSkip: () -> Unit) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(AppStrings.dataManagement, fontWeight = FontWeight.Bold) },
        text = { Text(AppStrings.importJsonQuestion) },
        confirmButton = {
            Button(onClick = onImport) { Text(AppStrings.importJsonButton) }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text(AppStrings.skip) }
        }
    )
}

@Composable
private fun TourDialog(
    steps: List<TourStep>,
    index: Int,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    if (steps.isEmpty()) {
        LaunchedEffect(Unit) { onSkip() }
        return
    }
    val step = steps[index.coerceIn(0, steps.lastIndex)]
    val isLast = index >= steps.lastIndex

    AlertDialog(
        onDismissRequest = onSkip,
        title = {
            Text(
                String.format(AppStrings.tourStep, index + 1, steps.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                Text(step.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = if (isLast) onFinish else onNext) {
                Text(if (isLast) AppStrings.finish else AppStrings.next)
            }
        },
        dismissButton = {
            Row {
                if (index > 0) {
                    OutlinedButton(onClick = onBack) { Text(AppStrings.back) }
                }
                TextButton(onClick = onSkip) { Text(AppStrings.skip) }
            }
        }
    )
}

// --- Platform tour steps --------------------------------------------------

/** Desktop panes/tabs tour (Windows + Linux). */
fun desktopTourSteps(): List<TourStep> =
    if (AppStrings.language == "ru") {
        listOf(
            TourStep(
                "Серверы",
                "Слева — список ваших SSH-серверов. Кнопка «+» добавляет новый, по клику открывается сессия. Серверы можно группировать в папки."
            ),
            TourStep(
                "Терминал",
                "SSH-консоль для выполнения команд на сервере. Быстрые команды (список, диск, память) — в панели справа."
            ),
            TourStep(
                "SFTP",
                "Файловый менеджер: просмотр папок, загрузка, скачивание, предпросмотр и редактирование файлов."
            ),
            TourStep(
                "Мониторинг",
                "Дашборд со статистикой сервера: нагрузка CPU, RAM, диск, аптайм и процессы."
            ),
            TourStep(
                "Раздельный вид",
                "Терминал и SFTP одновременно в одном окне. Разделитель можно перетаскивать."
            ),
            TourStep(
                "Рабочие области",
                "Сохраняйте наборы открытых сессий и возвращайтесь к ним одним кликом."
            ),
            TourStep(
                "Настройки",
                "Тема оформления, язык, шрифт терминала, приватность и безопасность."
            )
        )
    } else {
        listOf(
            TourStep(
                "Servers",
                "On the left is your list of SSH servers. Use \"+\" to add one, click it to open a session. Servers can be grouped into folders."
            ),
            TourStep(
                "Terminal",
                "SSH console for running commands on the server. Quick commands (list, disk, memory) live in the right-hand panel."
            ),
            TourStep(
                "SFTP",
                "File manager: browse folders, upload, download, preview and edit files."
            ),
            TourStep(
                "Monitoring",
                "Server dashboard: CPU load, RAM, disk, uptime and processes."
            ),
            TourStep(
                "Split view",
                "Terminal and SFTP side by side in one window. Drag the divider to resize."
            ),
            TourStep(
                "Workspaces",
                "Save sets of open sessions and restore them with one click."
            ),
            TourStep(
                "Settings",
                "App theme, language, terminal font, privacy and security."
            )
        )
    }

/** Phone screens tour (Android). */
fun androidTourSteps(): List<TourStep> =
    if (AppStrings.language == "ru") {
        listOf(
            TourStep(
                "Серверы",
                "Главный экран со списком ваших SSH-серверов. Кнопка «+» добавляет новый, по клику открывается сессия."
            ),
            TourStep(
                "Терминал",
                "SSH-консоль: ввод команд, история, быстрые команды и контроль над сессией."
            ),
            TourStep(
                "Мониторинг",
                "Статистика сервера: CPU, RAM, диск, аптайм и процессы."
            ),
            TourStep(
                "SFTP",
                "Проводник файлов на сервере: просмотр, загрузка, скачивание и редактирование."
            ),
            TourStep(
                "Настройки",
                "Тема, язык, биометрия, экспорт и импорт данных."
            )
        )
    } else {
        listOf(
            TourStep(
                "Servers",
                "Main screen with your list of SSH servers. Use \"+\" to add one, tap it to open a session."
            ),
            TourStep(
                "Terminal",
                "SSH console: command input, history, quick commands and session control."
            ),
            TourStep(
                "Monitoring",
                "Server stats: CPU, RAM, disk, uptime and processes."
            ),
            TourStep(
                "SFTP",
                "Remote file explorer: browse, upload, download and edit files."
            ),
            TourStep(
                "Settings",
                "Theme, language, biometric lock, export and import data."
            )
        )
    }
