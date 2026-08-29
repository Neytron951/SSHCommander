package com.neytron.sshcommander.ui

data class TerminalTheme(
    val id: String,
    val name: String,
    val description: String,
    val backgroundColor: String,
    val textColor: String
)

object TerminalThemes {
    val presets = listOf(
        TerminalTheme("tokyo_night", "Tokyo Night", "A clean theme that celebrates the neon lights of Tokyo at 3 AM. Modern and sharp.", "#1A1B26", "#A9B1D6"),
        TerminalTheme("monokai", "Monokai Pro", "The legendary theme for elite developers. High contrast, low eye-strain.", "#272822", "#F8F8F2"),
        TerminalTheme("dracula", "Dracula", "A dark theme for vampires... and night owls who forgot what sun looks like.", "#282A36", "#F8F8F2"),
        TerminalTheme("gruvbox_dark", "Gruvbox Dark", "Retro 'groove' colors with a warm, paper-like feel. Very popular.", "#282828", "#EBDBB2"),
        TerminalTheme("synthwave", "Synthwave '84", "Outrun the 80s with this neon-infused, retro-futuristic theme.", "#262335", "#FF7EDB"),
        TerminalTheme("night_owl", "Night Owl", "A deep blue theme for those who like to work late into the night.", "#011627", "#D6DEEB"),
        TerminalTheme("material_ocean", "Material Ocean", "A deep, vast blue ocean for your terminal commands.", "#0F111A", "#8F93A2"),
        TerminalTheme("solarized_dark", "Solarized Dark", "Precision colors designed by actual scientists to be easy on the eyes.", "#002B36", "#839496"),
        TerminalTheme("one_dark", "One Dark", "The Atom-inspired classic. Clean, modern, and professional.", "#282C34", "#ABB2BF"),
        TerminalTheme("nord", "Nord", "Arctic, north-bluish elegance. Cool, calm, and collected.", "#2E3440", "#D8DEE9"),
        TerminalTheme("cyberpunk", "Cyberpunk 2077", "High-tech, low-life. Neon yellow text on an abyss of black.", "#000000", "#FCEE09"),
        TerminalTheme("retro_green", "Classic Terminal (Hollywood Hacker 1995)", "For when you want to feel like you're in 'The Matrix'. High cringe, high nostalgia. 'I'm in!'", "#000000", "#00FF00"),
        TerminalTheme("amber_crt", "Amber CRT (Pip-Boy 3000)", "The warm, radiation-free glow of a vintage 80s terminal. War never changes.", "#000000", "#FFB000"),
        TerminalTheme("github_dark", "GitHub Dark", "Official GitHub look. Perfect for looking busy on open-source projects.", "#0D1117", "#C9D1D9"),
        TerminalTheme("ubuntu", "Ubuntu Bash", "That familiar purple warmth of a fresh Linux install.", "#300A24", "#FFFFFF"),
        TerminalTheme("powershell", "PowerShell Core", "The deep blue abyss of modern Windows administration.", "#012456", "#FFFFFF"),
        TerminalTheme("cobalt2", "Cobalt 2", "The famous theme by Wes Bos. Vibrant blue and yellow.", "#193549", "#FFC600"),
        TerminalTheme("shades_of_purple", "Shades of Purple", "A professional theme with hand-picked shades of purple.", "#2D2B55", "#FAD000"),
        TerminalTheme("ayu_mirage", "Ayu Mirage", "A theme that is both simple and elegant.", "#212733", "#D9D7CE"),
        TerminalTheme("everforest", "Everforest", "A green based color scheme, designed to be comfortable.", "#2D333B", "#D3C6AA"),
        TerminalTheme("catppuccin_mocha", "Catppuccin Mocha", "Soothing pastel theme for the high-spirited!", "#1E1E2E", "#CDD6F4"),
        TerminalTheme("rose_pine", "Rosé Pine", "All natural pine, faux fur and a bit of soho vibes.", "#191724", "#E0DEF4"),
        TerminalTheme("vsc_dark_plus", "VS Code Dark+", "The default Visual Studio Code dark theme.", "#1E1E1E", "#D4D4D4"),
        TerminalTheme("light_console", "Pure Light (Flashbang)", "Crisp black text on a clean white background. Use it to wake up.", "#FFFFFF", "#000000"),
        TerminalTheme("default", "Standard Terminal", "Simple and reliable gray-on-black terminal look.", "#000000", "#CCCCCC"),
        TerminalTheme("custom", "Custom Colors", "Total control for those who know better. Pick your own colors below.", "#000000", "#FFFFFF")
    )
    
    val modernFonts = listOf(
        "JetBrains Mono",
        "Roboto Mono",
        "Fira Code",
        "Source Code Pro",
        "Cascadia Code",
        "Ubuntu Mono",
        "Inconsolata",
        "Hack",
        "IBM Plex Mono",
        "Consolas",
        "Monaco",
        "DejaVu Sans Mono",
        "Monospace"
    )
}
