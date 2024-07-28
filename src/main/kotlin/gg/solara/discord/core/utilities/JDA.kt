package gg.solara.discord.core.utilities

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

fun JDA.subscribeToModal(id: String, callback: ModalInteractionEvent.() -> Unit)
{
    this.addEventListener(
        object : ListenerAdapter()
        {
            override fun onModalInteraction(event: ModalInteractionEvent)
            {
                if (!event.modalId.startsWith(id)) return

                event.callback()
            }
        }
    )
}

fun ModalInteractionEvent.string(id: String) = this.getValue(id)!!.asString
