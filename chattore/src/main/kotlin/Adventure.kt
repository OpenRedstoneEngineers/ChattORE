package org.openredstone.chattore

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.StyleBuilderApplicable
import net.kyori.adventure.text.format.TextColor

/** Concatenate [components] */
fun c(vararg components: Component): Component = Component.textOfChildren(*components)

/** Turn this string into a text component */
val String.c: Component get() = Component.text(this)

/** Turn this string into a text component with [color] */
// purely an efficiency overload
operator fun String.get(color: TextColor): Component = Component.text(this, color)

/** Turns this string into a text component, with [styles] */
operator fun String.get(vararg styles: StyleBuilderApplicable) = Component.text(this, Style.style(*styles))

fun Component.with(vararg styles: StyleBuilderApplicable) =
    this.style { builder -> styles.forEach { it.styleApply(builder) } }

/** Combine styles */
operator fun StyleBuilderApplicable.plus(other: StyleBuilderApplicable) = StyleBuilderApplicable {
    this.styleApply(it)
    other.styleApply(it)
}

fun buildTextComponent(f: TextComponent.Builder.() -> Unit): Component = Component.text().apply(f).build()

fun Iterable<Component>.join(
    separator: Component? = null,
    prefix: Component? = null,
    suffix: Component? = null,
    lastSeparator: Component? = null,
): Component =
    Component.join(
        JoinConfiguration.builder().separator(separator).prefix(prefix).suffix(suffix).lastSeparator(lastSeparator),
        this,
    )

object Buttons {
    fun suggest(hover: Component, command: String): StyleBuilderApplicable =
        HoverEvent.showText(hover) + ClickEvent.suggestCommand(command)

    fun run(hover: Component, command: String): StyleBuilderApplicable =
        HoverEvent.showText(hover) + ClickEvent.runCommand(command)

    fun url(hover: Component, url: String): StyleBuilderApplicable =
        HoverEvent.showText(hover) + ClickEvent.openUrl(url)
}
