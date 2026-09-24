// SPDX-License-Identifier: EUPL-1.2

package project.convention.logic

open class AppVersion(
    val major: Int = 1,
    val minor: Int = 0,
    val patch: Int = 0,
    val build: Int = 0
) {
    val code: Int = major * 100000 + minor * 10000 + patch * 100 + (build.coerceIn(0, 99))
    val name: String = "$major.$minor.$patch"
    val qualifiedName: String = if (build > 0) "$name-$build" else name

    companion object {
        fun parse(version: String): AppVersion {
            return try {
                // Supported inputs:
                // - 1.2.3
                // - 1.2.3-7
                // - 1.2.3+7
                // - 1.2.3 (7)
                // - 1.2.3.7
                val cleaned = version.trim()

                val (vParts, buildPart) = when {
                    cleaned.contains("-") -> cleaned.split("-", limit = 2).let { it[0] to it[1] }
                    cleaned.contains("+") -> cleaned.split("+", limit = 2).let { it[0] to it[1] }
                    cleaned.matches(Regex(""".*\(\s*\d+\s*\)$""")) -> {
                        val m = Regex("""\((\s*\d+\s*)\)$""").find(cleaned)!!
                        cleaned.replace(m.value, "").trim() to m.groupValues[1].trim()
                    }
                    cleaned.count { it == '.' } == 3 -> {
                        val idx = cleaned.lastIndexOf('.')
                        cleaned.substring(0, idx) to cleaned.substring(idx + 1)
                    }
                    else -> cleaned to ""
                }

                val parts = vParts.split(".")
                require(parts.size == 3)
                val build = buildPart.toIntOrNull() ?: 0

                AppVersion(
                    major = parts[0].toInt(),
                    minor = parts[1].toInt(),
                    patch = parts[2].toInt(),
                    build = build
                )
            } catch (e: Exception) {
                AppVersion()
            }
        }
    }
}
