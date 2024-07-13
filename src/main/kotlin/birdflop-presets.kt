// This file adds the rgb.birdflop.com presets

package chattore

val birdflopGradients = mapOf(
    "birdflop" to arrayOf(
        "##084CFB",
        "#ADF3FD"
    ),
)

val birdflopPresets = birdflopGradients.mapValues { (_, colors) ->
        "<gradient:${colors.joinToString(':'.toString())}><username></gradient>"
    }.toSortedMap()
