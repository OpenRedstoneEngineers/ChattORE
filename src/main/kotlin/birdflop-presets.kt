// This file adds the rgb.birdflop.com presets

package chattore

val birdflopGradients = mapOf(
    "birdflop" to arrayOf(
        "#084CFB",
        "#ADF3FD"
    ),
    "SimplyMC" to arrrayOf (
        "#084CFB",
        "#ADF3FD"
    ),
    "Rainbow" to arrrayOf (
        "#FF0000",
        "#FF7F00",
        "#FFFF00",
        "#00FF00",
        "#0000FF",
        "#EAE4AA"
    ),
    "Skyline" to arrrayOf (
        "#1488CC",
        "#2B32B2"
    ),
    "Mango" to arrrayOf (
        "#1488CC",
        "#2B32B2"
    ),
    "Vice City" to arrrayOf (
        "#1488CC",
        "#2B32B2"
    ),
    "Dawn" to arrrayOf (
        "#1488CC",
        "#2B32B2"
    ),
    "Rose" to arrrayOf (
        "#1488CC",
        "#2B32B2"
    ),
    "Firewatch" to arrrayOf (
        "#1488CC",
        "#2B32B2"
    ),        
)

val birdflopPresets = birdflopGradients.mapValues { (_, colors) ->
        "<gradient:${colors.joinToString(':'.toString())}><username></gradient>"
    }.toSortedMap()
