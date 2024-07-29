package gg.solara.discord.core.support

import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.button
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import gg.solara.discord.core.punishments.PunishmentRepository
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
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.FileUpload
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.io.File
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 7/1/2024
 */
@Service
class SupportTicketService(
    private val supportTicketRepository: SupportTicketRepository,
    private val punishmentRepository: PunishmentRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val tebexService: TebexService,
    private val discord: JDA
) : InitializingBean
{
    @Value("\${solara.support.roles.general}") lateinit var generalSupportRoleIDs: String
    @Value("\${solara.support.roles.punishments}") lateinit var punishmentsSupportRoleIDs: String
    @Value("\${solara.support.roles.transactions}") lateinit var transactionsSupportRoleIDs: String
    @Value("\${solara.applications.staff.google-docs-link}") lateinit var staffApplicationsLink: String

    @Value("\${solara.support.categories}") lateinit var supportCategoryIDs: String

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy, hh:mm:ss a")
    override fun afterPropertiesSet()
    {
        discord.listener<ChannelDeleteEvent> {
            val supportTicket = supportTicketRepository
                .findByChannelID(it.channel.idLong)
                ?: return@listener

            supportTicketRepository.delete(supportTicket)
        }

        discord.listener<ButtonInteractionEvent> { event ->
            if (event.button.id != "ticket-close")
            {
                return@listener
            }

            val supportTicket = supportTicketRepository.findByChannelID(event.channelIdLong)
            if (supportTicket == null)
            {
                event.replyEmbeds(Embed {
                    color = Colors.Failure
                    title = "Not A Ticket"
                    description = "This channel is not a ticket channel!"
                }).queue()
                return@listener
            }

            if (supportTicket.assignedToUser != null && supportTicket.assignedToUser != event.user.idLong)
            {
                event.replyEmbeds(Embed {
                    color = Colors.Failure
                    title = "Claimed"
                    description = "You cannot close this ticket! Only staff members can close this ticket with the `/close` command."
                }).queue()
                return@listener
            }

            event.replyEmbeds(Embed {
                color = Colors.Success
                title = "Closed"
                description = "This ticket will be closed in five seconds!"
            }).queue {
                event.messageChannel.delete().queueAfter(5L, TimeUnit.SECONDS)
            }
        }

        discord.listener<ButtonInteractionEvent> {
            if (it.button.id != "ticket-claim")
            {
                return@listener
            }

            it.deferReply(true).queue()

            val supportTicket = supportTicketRepository.findByChannelID(it.channelIdLong)
            if (supportTicket == null)
            {
                it.hook.sendMessageEmbeds(Embed {
                    color = Colors.Failure
                    title = "Not a Ticket"
                    description = "This channel is not a support ticket!"
                }).queue()
                return@listener
            }

            if (supportTicket.assignedToUser != null)
            {
                it.hook.sendMessageEmbeds(Embed {
                    color = Colors.Failure
                    title = "Already Claimed"
                    description = "This support ticket has already been claimed!"
                }).queue()
                return@listener
            }

            supportTicket.assignedToUser = it.user.idLong
            supportTicketRepository.save(supportTicket)

            it.channel.asTextChannel()
                .upsertPermissionOverride(it.member!!)
                .grant(
                    Permission.VIEW_CHANNEL,
                    Permission.MESSAGE_HISTORY,
                    Permission.MESSAGE_SEND,
                    Permission.MESSAGE_MANAGE
                )
                .queue { _ ->
                    supportTicket.roleIDs.split(",")
                        .map { role -> role.toLong() }
                        .forEach { role ->
                            val idRole = it.guildChannel.guild.getRoleById(role)
                                ?: return@forEach

                            it.channel.asTextChannel()
                                .upsertPermissionOverride(idRole)
                                .deny(Permission.VIEW_CHANNEL)
                                .queue()
                        }

                    it.hook.sendMessageEmbeds(Embed {
                        color = Colors.Success
                        title = "Claimed"
                        description = "You are now responsible for this ticket!"
                    }).queue { _ ->
                        it.channel.sendMessageEmbeds(Embed {
                            color = Colors.Gold
                            title = "Claimed"
                            description =
                                "A support staff member (${it.user.asMention}) has claimed your ticket. Please wait patiently for the support staff member to respond."
                        }).queue()
                    }
                }
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

        discord.listener<ButtonInteractionEvent> {
            if (it.button.id != "staff-application")
            {
                return@listener
            }

            it.replyEmbeds(Embed {
                color = Colors.Gold
                title = "\uD83D\uDCDC Staff Applications"
                description = """
                    Requirements:
                    - Must be at least 15 years of age.
                    - Must have prior moderation experience.
                    - Must be mature and responsible.
                    - Must be active both In-Game and In-Discord.
                    - Must maintain a professional demeanor.
                    - Must be fluent in English.

                    If you meet these requirements and are ready to contribute to our awesome community, we want to hear from you!

                    Apply here: [Staff Applications]($staffApplicationsLink)
                """.trimIndent()
            }).setEphemeral(true).queue()
        }

        discord.listener<ButtonInteractionEvent> {
            if (it.button.id != "media-application")
            {
                return@listener
            }

            it.reply(MessageCreate {
                embed {
                    color = Colors.Gold
                    title = "\uD83C\uDFA5 Media Applications"
                    description = "Refer to the image below for our media applications!"
                }
                files += FileUpload.fromData(File("assets", "requirements.png"))
            }).setEphemeral(true).queue()
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

            createNewTicket(generalSupportRoleIDs) {
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

            createNewTicket(transactionsSupportRoleIDs) {
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

        buildSupportResponseToButton(
            "punishments",
            Modal(
                "punishment-support",
                "Punishment Support"
            ) {
                short("punishment-id", "Punishment ID", requiredLength = 8..9)

                short("unfair", "Type \"yes\" if your punishment was unfair.", required = false)
                paragraph("problem", "If yes, why?", required = false)
            }
        ) {
            val punishmentID = (getValue("punishment-id")?.asString ?: "none").removePrefix("#")
            val punishment = punishmentRepository.findByIdStartingWith(punishmentID)
            if (punishment == null)
            {
                replyEmbeds(Embed {
                    color = Colors.Failure
                    title = "No Punishment ID"
                    description =
                        "We found no punishment with the ID you defined! Make sure you provide all of the letters and numbers in proper fashion!"
                }).setEphemeral(true).queue()
                return@buildSupportResponseToButton
            }

            createNewTicket(punishmentsSupportRoleIDs) {
                sendMessageEmbeds(Embed {
                    color = Colors.Primary
                    title = "Punishment Details"
                    thumbnail = "https://skins.mcstats.com/bust/${
                        punishment.target
                    }"

                    description = ""
                    getValue("unfair")?.apply {
                        if (asString.lowercase() == "yes")
                        {
                            description += "**User believes the punishment is unfair!**"

                            getValue("problem")?.apply {
                                description += asString
                            }
                        }
                    }

                    field("Added At") {
                        inline = true
                        value = dateFormat.format(Date(punishment.addedAt.toLong()))
                    }

                    field("Reason") {
                        inline = true
                        value = punishment.addedReason
                    }

                    val username = redisTemplate.opsForHash<String, String>()
                        .get(
                            "DataStore:UuidCache:UniqueId",
                            punishment.target
                        )

                    footer {
                        name = "Against $username"
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

    fun ModalInteractionEvent.createNewTicket(roleIDs: String, autonomous: Boolean = false, postConstruct: TextChannel.() -> Unit)
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
                if (autonomous)
                {
                    return@apply
                }

                roleIDs.split(",")
                    .map { it.toLong() }
                    .forEach {
                        addRolePermissionOverride(
                            it,
                            listOf(
                                Permission.VIEW_CHANNEL,
                                Permission.MESSAGE_HISTORY,
                                Permission.MESSAGE_SEND,
                                Permission.MESSAGE_MANAGE
                            ),
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
                    roleIDs = roleIDs,
                    ownerID = user.idLong
                )

                textChannel.sendMessage(MessageCreate {
                    embed {
                        color = Colors.Primary
                        title = "Welcome"
                        description = "A support representative will be with you soon."
                    }

                    actionRow(
                        button(
                            "ticket-claim",
                            label = "Claim",
                            emoji = Emoji.fromUnicode("\uD83D\uDEC4"),
                            style = ButtonStyle.SUCCESS
                        ),
                        button(
                            "ticket-close",
                            label = "Close",
                            emoji = Emoji.fromUnicode("\uD83D\uDED1"),
                            style = ButtonStyle.DANGER
                        )
                    )
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
