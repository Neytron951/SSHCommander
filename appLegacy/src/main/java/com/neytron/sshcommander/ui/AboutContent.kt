package com.neytron.sshcommander.ui

/**
 * Редактируй этот файл свободно — текст подтянется в приложение.
 * Edit this file freely — the text will be picked up by the app.
 * Английский и русский варианты расположены рядом, чтобы удобно править оба.
 * English and Russian versions are side by side for easy editing.
 */
object AboutContent {

    /** Содержимое экрана «О приложении». */
    data class AboutText(
        /** Название приложения (показывается крупно). */
        val name: String,
        /** Абзацы описания (каждый элемент — отдельный абзац). */
        val description: List<String>,
        /** Небольшой текст внизу (оставь пустым, если не нужен). */
        val disclaimer: String
    )

    // 🇬🇧 ENGLISH
    val en = AboutText(
        name = "SSH Commander",
        description = listOf(
            "SSH Commander is a handy tool for managing your servers over SSH.",
            "Connect, run commands, check status and manage files right from your phone.",
            "Everything you need is just a few taps away."
        ),
        disclaimer = ""
    )

    // 🇷🇺 РУССКИЙ
    val ru = AboutText(
        name = "SSH Commander",
        description = listOf(
            "SSH Commander — удобный инструмент для управления серверами по SSH.",
            "Подключайтесь, выполняйте команды, следите за статусом и управляйте файлами прямо с телефона.",
            "Всё, что нужно, — в паре касаний."
        ),
        disclaimer = ""
    )

    /** Выбирает текст по текущему языку приложения. */
    fun forLanguage(language: String?): AboutText = if (language == "ru") ru else en
}
