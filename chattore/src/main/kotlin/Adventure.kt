package org.openredstone.chattore

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentBuilderApplicable
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent

/** Concatenate components */
operator fun Component.plus(other: Component) = Component.text().append(this).append(other).build()

/** Turn this string into a text component */
val String.c: Component get() = Component.text(this)

operator fun Component.get(style: ComponentBuilderApplicable) =
    Component.text().applicableApply(this).applicableApply(style).build()

/** Combine styles */
operator fun ComponentBuilderApplicable.plus(other: ComponentBuilderApplicable) = ComponentBuilderApplicable {
    it.applicableApply(this).applicableApply(other)
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
    fun suggest(hover: Component, command: String): ComponentBuilderApplicable =
        HoverEvent.showText(hover) + ClickEvent.suggestCommand(command)

    fun run(hover: Component, command: String): ComponentBuilderApplicable =
        HoverEvent.showText(hover) + ClickEvent.runCommand(command)

    fun url(hover: Component, url: String): ComponentBuilderApplicable =
        HoverEvent.showText(hover) + ClickEvent.openUrl(url)
}
