package chattore;

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

fun fixHexFormatting(str: String): String = str.replace(Regex("#([0-9a-f]{6})")) { "&${it.groupValues.first()}" }

fun String.componentize(): Component =
    LegacyComponentSerializer.builder()
        .character('&')
        .hexCharacter('#')
        .extractUrls()
        .build()
        .deserialize(fixHexFormatting(this))


fun String.formatGlobal(
    prefix: String = "",
    sender: String = "",
    recipient: String = "",
    message: String = "",
    preserveRawMessage: Boolean = false
): Component {
    val message = message
        .replace(Regex("""\s+"""), " ")
        .trim()
    return this
        .replaceFirst("%prefix%", prefix)
        .replaceFirst("%sender%", sender)
        .replaceFirst("%recipient%", recipient)
        .componentize()
        .replaceText(
            TextReplacementConfig
                .builder()
                .matchLiteral("""%message%""")
                .replacement(
                    if (preserveRawMessage) {
                        PlainTextComponentSerializer.plainText().deserialize(message)
                    } else {
                        LegacyComponentSerializer
                            .legacy('&')
                            .deserialize(fixHexFormatting(message))
                    }
                )
                .build()
        )
}

