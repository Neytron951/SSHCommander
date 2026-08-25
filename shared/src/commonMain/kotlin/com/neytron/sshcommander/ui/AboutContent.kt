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
            "SSH Commander is a professional, multi-platform tool designed for system administrators, developers, and power users who need reliable access to their servers.",
            "Our goal is to provide a seamless experience across all your devices, combining a powerful terminal, intuitive SFTP file manager, and advanced workspace management in a single app.",
            "Key features include:\n• Full-featured SSH terminal with customizable styles\n• Secure SFTP explorer for easy file transfers\n• Multi-session support with pinned tabs\n• Dark and Light themes with dynamic color support\n• Biometric protection for your sensitive data",
            "Thank you for choosing SSH Commander. We are constantly working to improve your experience and add new features."
        ),
        disclaimer = ""
    )

    // 🇷🇺 РУССКИЙ
    val ru = AboutText(
        name = "SSH Commander",
        description = listOf(
            "SSH Commander — это профессиональный мультиплатформенный инструмент, созданный для системных администраторов, разработчиков и опытных пользователей, которым нужен надежный доступ к своим серверам.",
            "Наша цель — обеспечить удобную работу на всех ваших устройствах, объединив в одном приложении мощный терминал, интуитивно понятный SFTP-менеджер файлов и продвинутое управление рабочими пространствами.",
            "Основные возможности:\n• Полнофункциональный SSH-терминал с настраиваемыми стилями\n• Безопасный SFTP-проводник для удобной передачи файлов\n• Поддержка нескольких сессий одновременно с закреплением вкладок\n• Темная и светлая темы с поддержкой системных акцентов\n• Биометрическая защита ваших данных",
            "Спасибо, что выбрали SSH Commander. Мы постоянно работаем над улучшением приложения и добавлением новых функций."
        ),
        disclaimer = ""
    )

    /** Выбирает текст по текущему языку приложения. */
    fun forLanguage(language: String?): AboutText = if (language == "ru") ru else en
}
