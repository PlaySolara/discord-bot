package gg.solara.discord.core.support

import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.messages.Embed
import gg.solara.discord.core.retrofit.tebex.TebexService
import gg.solara.discord.core.utilities.Colors
import gg.solara.discord.core.utilities.snowflake
import gg.solara.discord.core.utilities.subscribeToModal
import gg.solara.discord.core.utilities.toUuidDashed
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.modals.Modal
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 7/1/2024
 */
@Service
class SupportTicketService(
    private val supportTicketRepository: SupportTicketRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val tebexService: TebexService,
    private val discord: JDA
) : InitializingBean
{
    @Value("\${solara.support.roles}") lateinit var supportRoleIDs: String
    @Value("\${solara.support.categories}") lateinit var supportCategoryIDs: String

    override fun afterPropertiesSet()
    {
        discord.listener<ChannelDeleteEvent> {
            val supportTicket = supportTicketRepository
                .findByChannelID(it.channel.idLong)
                ?: return@listener

            supportTicketRepository.delete(supportTicket)
        }

        fun buildSupportResponseToButton(
            buttonId: String,
            modal: Modal,
            interaction: ModalInteractionEvent.() -> Unit
        )
        {
            discord.listener<ButtonInteractionEvent> {
                if (it.button.id != buttonId)
                {
                    return@listener
                }

                it.replyModal(modal).queue()
            }

            discord.subscribeToModal(modal.id, interaction)
        }

        buildSupportResponseToButton(
            "general",
            Modal(
                "general-support",
                "General Support"
            ) {
                short("username", "In-Game Username", requiredLength = 1..16, required = true)
                paragraph("topic", "Topic", required = true)
            }
        ) {
            val username = getValue("username")?.asString ?: "none"
            val uniqueId = redisTemplate.opsForHash<String, String>()
                .get(
                    "DataStore:UuidCache:Username",
                    username.lowercase()
                )

            if (uniqueId == null)
            {
                replyEmbeds(Embed {
                    color = Colors.Failure
                    title = "No Account"
                    description = "We found no in-game account with the username you defined!"
                }).setEphemeral(true).queue()
                return@buildSupportResponseToButton
            }

            createNewTicket {
                sendMessageEmbeds(Embed {
                    color = Colors.Primary
                    title = "Topic"

                    description = getValue("topic")?.asString ?: "none"
                    thumbnail = "https://skins.mcstats.com/bust/$uniqueId"

                    footer {
                        name = "Created by $username"
                    }
                }).queue()
            }
        }

        buildSupportResponseToButton(
            "transactions",
            Modal(
                "transactions-support",
                "Transaction Support"
            ) {
                short("txn-id", "Transaction ID")
                paragraph("problem", "Problem", required = true)
            }
        ) {
            val transactionID = getValue("txn-id")?.asString ?: "none"

            val transaction = tebexService.transaction(transactionID).execute().body()
            if (transaction == null)
            {
                replyEmbeds(Embed {
                    color = Colors.Failure
                    title = "No Transaction"
                    description = "We found no transaction with the transaction ID you defined!"
                }).setEphemeral(true).queue()
                return@buildSupportResponseToButton
            }

            if (transaction.status == "Chargeback")
            {
                replyEmbeds(Embed {
                    color = Colors.Failure
                    title = "Illegal Transaction"
                    description = "You have a chargeback transaction! You are blacklisted from transaction support."
                }).setEphemeral(true).queue()
                return@buildSupportResponseToButton
            }

            createNewTicket {
                sendMessageEmbeds(Embed {
                    color = Colors.Primary
                    title = "Transaction Details"
                    thumbnail = "https://skins.mcstats.com/bust/${
                        transaction.player.uuid.toUuidDashed()
                    }"

                    field("Price") {
                        inline = true
                        value = "${transaction.currency.symbol}${transaction.amount} ${transaction.currency.iso4217}"
                    }

                    field("Purchased") {
                        inline = true
                        value = transaction.packages.joinToString(", ") { it.name }
                    }

                    footer {
                        name = "Created by ${transaction.player.name} | ${transaction.date}"
                    }
                }).queue()
            }
        }
    }

    private val random = SecureRandom()
    fun SecureRandom.randomHexString() = nextInt(0x1000000).toString().format("%06x")

    fun getBestCategory(guild: Guild): Category?
    {
        val potentialCategories = supportCategoryIDs.split(",").map { it.toLong() }
        for (potentialCategory in potentialCategories)
        {
            val discordCategory = guild.getCategoryById(potentialCategory)
                ?: continue

            if (discordCategory.channels.size >= 50)
            {
                continue
            }
            return discordCategory
        }
        return null
    }

    fun ModalInteractionEvent.createNewTicket(postConstruct: TextChannel.() -> Unit)
    {
        deferReply(true).queue()

        val existingTickets = supportTicketRepository.findAllByOwnerID(user.idLong)
        if (existingTickets.size >= 3)
        {
            hook
                .sendMessageEmbeds(Embed {
                    color = Colors.Failure
                    title = "Too Many Tickets"
                    description = "You already have three or more tickets open!"
                })
                .setEphemeral(true)
                .queue()
            return
        }

        val category = getBestCategory(guild!!)
        if (category == null)
        {
            hook
                .sendMessageEmbeds(Embed {
                    color = Colors.Failure
                    title = "System Overload"
                    description = "We are facing issues right now! Try again later!"
                })
                .setEphemeral(true)
                .queue()
            return
        }

        val supportTicketID = snowflake()
        category
            .createTextChannel("support-${random.randomHexString()}")
            .apply {
                supportRoleIDs.split(",")
                    .map { it.toLong() }
                    .forEach {
                        addRolePermissionOverride(
                            it,
                            listOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND, Permission.MESSAGE_MANAGE),
                            listOf()
                        )
                    }
            }
            .addMemberPermissionOverride(
                user.idLong,
                listOf(
                    Permission.VIEW_CHANNEL,
                    Permission.MESSAGE_SEND,
                    Permission.MESSAGE_EMBED_LINKS,
                    Permission.MESSAGE_ATTACH_FILES,
                    Permission.MESSAGE_ADD_REACTION
                ),
                emptyList()
            )
            .queue { textChannel ->
                val supportTicket = SupportTicket(
                    id = supportTicketID,
                    channelID = textChannel.idLong,
                    ownerID = user.idLong
                )

                textChannel.sendMessageEmbeds(Embed {
                    color = Colors.Primary
                    title = "Welcome"
                    description = "A support representative will be with you soon."
                }).queue {
                    postConstruct(textChannel)
                }

                supportTicketRepository.save(supportTicket)
                hook.sendMessageEmbeds(Embed {
                    color = Colors.Success
                    title = "Ticket Created"
                    description = "View your new ticket at: ${textChannel.asMention}"
                }).queue {
                    textChannel
                        .sendMessage("@here")
                        .queueAfter(2L, TimeUnit.SECONDS)
                }
            }
    }
}
