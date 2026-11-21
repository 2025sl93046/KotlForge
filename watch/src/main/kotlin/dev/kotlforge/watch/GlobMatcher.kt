package dev.kotlforge.watch

fun globToRegex(pattern: String): Regex {
    val sb = StringBuilder()
    var i = 0
    while (i < pattern.length) {
        when {
            pattern.startsWith("**/", i) -> { sb.append("(.+/)?"); i += 3 }
            pattern.startsWith("**", i)  -> { sb.append(".*"); i += 2 }
            pattern[i] == '*'            -> { sb.append("[^/]*"); i++ }
            pattern[i] == '?'            -> { sb.append("[^/]"); i++ }
            pattern[i] == '.'            -> { sb.append("\\."); i++ }
            else                         -> { sb.append(Regex.escape(pattern[i].toString())); i++ }
        }
    }
    return Regex(sb.toString())
}
