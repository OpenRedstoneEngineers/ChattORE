// This file adds the rgb.birdflop.com presets

package chattore

val birdflopGradients = mapOf(
    "birdflop" to arrayOf(
        "#084CFB",
        "#ADF3FD"
    ),
    "SimplyMC" to arrrayOF (
        "#084CFB",
        "#ADF3FD"
    ),
    "Rainbow" to arrrayOF (
        "#FF0000",
        "#FF7F00",
        "#FFFF00",
        "#00FF00",
        "#0000FF",
        "#EAE4AA"
    ),
    "Skyline" to arrrayOF (
        "#1488CC",
        "#2B32B2"
    ),
    "Mango" to arrrayOF (
        "#1488CC",
        "#2B32B2"
    ),
    "Vice City" to arrrayOF (
        "#1488CC",
        "#2B32B2"
    ),
    "Dawn" to arrrayOF (
        "#1488CC",
        "#2B32B2"
    ),
    "Rose" to arrrayOF (
        "#1488CC",
        "#2B32B2"
    ),
    "Firewatch" to arrrayOF (
        "#1488CC",
        "#2B32B2"
    ),        
)

val birdflopPresets = birdflopGradients.mapValues { (_, colors) ->
        "<gradient:${colors.joinToString(':'.toString())}><username></gradient>"
    }.toSortedMap()
