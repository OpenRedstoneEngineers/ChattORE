// This file adds famous Player's presets

package chattore
val playerColors = mapOf(
    "Waffle" to arrrayOF (
        "#1488CC",
        "#2B32B2"
    ),
)

val playerPresets = playerColors.mapValues { (_, colors) ->
        "<gradient:${colors.joinToString(':'.toString())}><username></gradient>"
    }.toSortedMap()
