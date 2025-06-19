package org.openredstone.chattore

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentBuilderApplicable
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent

/** Create a text component from this string with [style] */
operator fun String.get(style: ComponentBuilderApplicable) =
    Component.text().content(this).applicableApply(style).build()

/** Apply [style] to this Component */
operator fun Component.get(style: ComponentBuilderApplicable) =
    Component.text().applicableApply(this).applicableApply(style).build()

operator fun Component.plus(other: Component) = append(other)

/** Turn this string into a text component */
val String.c: Component get() = Component.text(this)

/** Combine styles */
operator fun ComponentBuilderApplicable.plus(other: ComponentBuilderApplicable) = ComponentBuilderApplicable {
    it.applicableApply(this).applicableApply(other)
}

// TODO maybe use adventure-extra-kotlin?
fun Iterable<Component>.join(separator: Component = Component.empty()): Component =
    Component.join(JoinConfiguration.separator(separator), this)

object Buttons {
    fun suggest(hover: Component, command: String): ComponentBuilderApplicable =
        HoverEvent.showText(hover) + ClickEvent.suggestCommand(command)

    fun run(hover: Component, command: String): ComponentBuilderApplicable =
        HoverEvent.showText(hover) + ClickEvent.runCommand(command)

    fun url(hover: Component, url: String): ComponentBuilderApplicable =
        HoverEvent.showText(hover) + ClickEvent.openUrl(url)
}
