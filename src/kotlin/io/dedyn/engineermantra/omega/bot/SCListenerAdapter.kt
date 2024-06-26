package io.dedyn.engineermantra.omega.bot

import io.dedyn.engineermantra.omega.bot.DiscordUtils.addRolesInServer
import io.dedyn.engineermantra.omega.bot.DiscordUtils.removeRolesInServer
import io.dedyn.engineermantra.omega.shared.ConfigFileJson
import io.dedyn.engineermantra.omega.shared.ConfigMySQL
import io.dedyn.engineermantra.omega.shared.MessageLevel
import io.dedyn.engineermantra.omega.shared.Timer
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.guild.GuildBanEvent
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.role.RoleDeleteEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.sourceforge.tess4j.Tesseract
import java.io.File
import java.time.Instant
import java.util.concurrent.TimeUnit

class SCListenerAdapter : ListenerAdapter() {
    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        //Remove all data we have on this member when they leave the server.
        //Boosting should be removed IMMEDIATELY as there is no reason to keep this information.
        for (booster in ConfigMySQL.getBoosters(event.guild.idLong)) {
            if (booster.userId == event.member!!.idLong) {
                ConfigMySQL.removeBooster(booster)
                break
            }
        }
        /** FIXME:
         * Retention Policy is 30 days, don't delete until 30 days later so if unban/rejoin we can restore
         * this state if it was an accidental ban.
         */
        if(ConfigMySQL.getLevelingPointsOrDefault(event.member!!.idLong, event.guild.idLong).levelingPoints > 0)
        {
            ConfigMySQL.removeLevelingPoints(event.member!!.idLong, event.guild.idLong)
        }
    }

    /**
     * Bans are Synchronized to BOTC Server from SC.
     */
    override fun onGuildBan(event: GuildBanEvent) {
        if(event.guild.idLong == 967140876298092634L)
        {
            event.jda.getGuildById(1165357291629989979L)!!.ban(event.user, 0, TimeUnit.SECONDS).queue()
        }
    }

    /**
     * Unbans are Synchronized to BOTC Server from SC.
     */
    override fun onGuildUnban(event: GuildUnbanEvent) {
        if(event.guild.idLong == 967140876298092634L)
        {
            event.jda.getGuildById(1165357291629989979L)!!.unban(event.user).queue()
        }
    }

    /**
     * Roles are Synchronized to BOTC Server from SC.
     */
    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        if (event.guild.idLong == 967140876298092634L &&
            event.roles.contains(event.guild.getRoleById(1078829209616666705)))
        {
            ConfigMySQL.roleBanUser(event.member.idLong, event.guild.idLong, 1078829209616666705L)
        }
        if(event.guild.idLong == 967140876298092634L)
        {
            removeRolesInServer(event.member.idLong, 1165357291629989979L, event.roles)
        }
    }

    override fun onRoleDelete(event: RoleDeleteEvent) {
        if(event.guild.idLong == 967140876298092634L){
            val otherGuild = event.jda.getGuildById(1165357291629989979L)
            val toDelete = otherGuild!!.getRolesByName(event.role.name, true)
            if(toDelete.isEmpty())
            {
                return
            }
            toDelete[0].delete().queue()
        }
    }

    /**
     * Button logic for SC's Sensitive Topics entry channel
     */

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.isFromGuild && event.guildChannel.idLong == 1121230172155302019 && event.button.id == "agree") {
            val channel = event.guild!!.getGuildChannelById(1087346724474998796)
            channel!!.permissionContainer.upsertPermissionOverride(event.member!!)
                .setAllowed(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL).queue()
            val ruleAgreement = event.guild!!.getGuildChannelById(1121230172155302019)
            ruleAgreement!!.permissionContainer.upsertPermissionOverride(event.member!!)
                .setDenied(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL).queue()
        }
        val message = event.reply("").setEphemeral(true).complete()
        event.message.delete().queue()
        message.deleteOriginal().queue()
    }

    /**
     * SC Specific message logic
     * - Deletes images containing words that are banned. This is a common Automod bypass
     * - Deletes common executables if they are uploaded
     *
     * Image Processing is currently disabled!
     */

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if(!event.isFromGuild)
        {
            return
        }
        if(event.author.idLong == 1083884798189252690L || event.author.idLong == 1174099574965665922L)
        {
            event.message.delete().reason("Monke").queue();
        }
        //println(event.message.contentRaw)
        if (event.guild.idLong == 967140876298092634L) {
            //println("Recieved Message")
            if (event.message.attachments.isNotEmpty()) {
                //println("Message has attachments")
                val instance = Tesseract()
                for (attachment in event.message.attachments) {
                    println(attachment.fileExtension)
                    if (attachment.fileExtension!!.contains("exe") || attachment.fileName.contains(".exe")) {
                        event.message.delete().queue()
                        Auditing.automodEntry(
                            event.guild.idLong,
                            "${event.message.author.asMention} has attempted to post an executable in ${event.message.channel.asMention}\nFile Name: ${attachment.fileName}"
                        )
                        return
                    }
                    if (attachment.fileExtension!!.contains("deb") || attachment.fileName.contains(".deb")) {
                        event.message.delete().queue()
                        Auditing.automodEntry(
                            event.guild.idLong,
                            "${event.message.author.asMention} has attempted to post an executable in ${event.message.channel.asMention}\nFile Name: ${attachment.fileName}"
                        )
                        return
                    }
                    if (attachment.fileExtension!!.contains("pkg") || attachment.fileName.contains(".pkg") || attachment.fileExtension!!.contains("dmg") || attachment.fileName.contains(".dmg")) {
                        event.message.delete().queue()
                        Auditing.automodEntry(
                            event.guild.idLong,
                            "${event.message.author.asMention} has attempted to post an executable in ${event.message.channel.asMention}\nFile Name: ${attachment.fileName}"
                        )
                        return
                    }
                    if (attachment.isImage && BotMain.imageProcessing) {
                        val tempFile = File.createTempFile("ocr_temp", ".${attachment.fileExtension}")
                        println(tempFile.name)
                        val string = instance.doOCR(attachment.proxy.downloadToFile(tempFile).get())
                        println(string)
                        val bannedWords = listOf("kill yourself", "kill yoself", "kill urself")
                        val nword_regex =
                            "\\b([sŚśṤṥŜŝŠšṦṧṠṡŞşṢṣṨṩȘșS̩s̩ꞨꞩⱾȿꟅʂᶊᵴ][a4ÁáÀàĂăẮắẰằẴẵẲẳÂâẤấẦầẪẫẨẩǍǎÅåǺǻÄäǞǟÃãȦȧǠǡĄąĄ́ą́Ą̃ą̃ĀāĀ̀ā̀ẢảȀȁA̋a̋ȂȃẠạẶặẬậḀḁȺⱥꞺꞻᶏẚＡａ][nŃńǸǹŇňÑñṄṅŅņṆṇṊṋṈṉN̈n̈ƝɲŊŋꞐꞑꞤꞥᵰᶇɳȵꬻꬼИиПпＮｎ][dĎďḊḋḐḑD̦d̦ḌḍḒḓḎḏĐđÐðƉɖƊɗᵭᶁᶑȡ])?[nŃńǸǹŇňÑñṄṅŅņṆṇṊṋṈṉN̈n̈ƝɲŊŋꞐꞑꞤꞥᵰᶇɳȵꬻꬼИиПпＮｎ][iÍíi̇́Ììi̇̀ĬĭÎîǏǐÏïḮḯĨĩi̇̃ĮįĮ́į̇́Į̃į̇̃ĪīĪ̀ī̀ỈỉȈȉI̋i̋ȊȋỊịꞼꞽḬḭƗɨᶖİiIıＩｉ1lĺľļḷḹl̃ḽḻłŀƚꝉⱡɫɬꞎꬷꬸꬹᶅɭȴＬｌoÓóÒòŎŏÔôỐốỒồỖỗỔổǑǒÖöȪȫŐőÕõṌṍṎṏȬȭȮȯO͘o͘ȰȱØøǾǿǪǫǬǭŌōṒṓṐṑỎỏȌȍȎȏƠơỚớỜờỠỡỞởỢợỌọỘộO̩o̩Ò̩ò̩Ó̩ó̩ƟɵꝊꝋꝌꝍⱺＯｏІіa4ÁáÀàĂăẮắẰằẴẵẲẳÂâẤấẦầẪẫẨẩǍǎÅåǺǻÄäǞǟÃãȦȧǠǡĄąĄ́ą́Ą̃ą̃ĀāĀ̀ā̀ẢảȀȁA̋a̋ȂȃẠạẶặẬậḀḁȺⱥꞺꞻᶏẚＡａ][gǴǵĞğĜĝǦǧĠġG̃g̃ĢģḠḡǤǥꞠꞡƓɠᶃꬶＧｇqꝖꝗꝘꝙɋʠ]{1,2}(l[e3ЄєЕеÉéÈèĔĕÊêẾếỀềỄễỂểÊ̄ê̄Ê̌ê̌ĚěËëẼẽĖėĖ́ė́Ė̃ė̃ȨȩḜḝĘęĘ́ę́Ę̃ę̃ĒēḖḗḔḕẺẻȄȅE̋e̋ȆȇẸẹỆệḘḙḚḛɆɇE̩e̩È̩è̩É̩é̩ᶒⱸꬴꬳＥｅ]t|[e3ЄєЕеÉéÈèĔĕÊêẾếỀềỄễỂểÊ̄ê̄Ê̌ê̌ĚěËëẼẽĖėĖ́ė́Ė̃ė̃ȨȩḜḝĘęĘ́ę́Ę̃ę̃ĒēḖḗḔḕẺẻȄȅE̋e̋ȆȇẸẹỆệḘḙḚḛɆɇE̩e̩È̩è̩É̩é̩ᶒⱸꬴꬳＥｅaÁáÀàĂăẮắẰằẴẵẲẳÂâẤấẦầẪẫẨẩǍǎÅåǺǻÄäǞǟÃãȦȧǠǡĄąĄ́ą́Ą̃ą̃ĀāĀ̀ā̀ẢảȀȁA̋a̋ȂȃẠạẶặẬậḀḁȺⱥꞺꞻᶏẚＡａ][rŔŕŘřṘṙŖŗȐȑȒȓṚṛṜṝṞṟR̃r̃ɌɍꞦꞧⱤɽᵲᶉꭉ]?|n[ÓóÒòŎŏÔôỐốỒồỖỗỔổǑǒÖöȪȫŐőÕõṌṍṎṏȬȭȮȯO͘o͘ȰȱØøǾǿǪǫǬǭŌōṒṓṐṑỎỏȌȍȎȏƠơỚớỜờỠỡỞởỢợỌọỘộO̩o̩Ò̩ò̩Ó̩ó̩ƟɵꝊꝋꝌꝍⱺＯｏ0][gǴǵĞğĜĝǦǧĠġG̃g̃ĢģḠḡǤǥꞠꞡƓɠᶃꬶＧｇqꝖꝗꝘꝙɋʠ]|[a4ÁáÀàĂăẮắẰằẴẵẲẳÂâẤấẦầẪẫẨẩǍǎÅåǺǻÄäǞǟÃãȦȧǠǡĄąĄ́ą́Ą̃ą̃ĀāĀ̀ā̀ẢảȀȁA̋a̋ȂȃẠạẶặẬậḀḁȺⱥꞺꞻᶏẚＡａ]?)?[sŚśṤṥŜŝŠšṦṧṠṡŞşṢṣṨṩȘșS̩s̩ꞨꞩⱾȿꟅʂᶊᵴ]?\\b"
                        val fword_regex =
                            "[fḞḟƑƒꞘꞙᵮᶂ][aÁáÀàĂăẮắẰằẴẵẲẳÂâẤấẦầẪẫẨẩǍǎÅåǺǻÄäǞǟÃãȦȧǠǡĄąĄ́ą́Ą̃ą̃ĀāĀ̀ā̀ẢảȀȁA̋a̋ȂȃẠạẶặẬậḀḁȺⱥꞺꞻᶏẚＡａ@4][gǴǵĞğĜĝǦǧĠġG̃g̃ĢģḠḡǤǥꞠꞡƓɠᶃꬶＧｇqꝖꝗꝘꝙɋʠ]{1,2}([ÓóÒòŎŏÔôỐốỒồỖỗỔổǑǒÖöȪȫŐőÕõṌṍṎṏȬȭȮȯO͘o͘ȰȱØøǾǿǪǫǬǭŌōṒṓṐṑỎỏȌȍȎȏƠơỚớỜờỠỡỞởỢợỌọỘộO̩o̩Ò̩ò̩Ó̩ó̩ƟɵꝊꝋꝌꝍⱺＯｏ0e3ЄєЕеÉéÈèĔĕÊêẾếỀềỄễỂểÊ̄ê̄Ê̌ê̌ĚěËëẼẽĖėĖ́ė́Ė̃ė̃ȨȩḜḝĘęĘ́ę́Ę̃ę̃ĒēḖḗḔḕẺẻȄȅE̋e̋ȆȇẸẹỆệḘḙḚḛɆɇE̩e̩È̩è̩É̩é̩ᶒⱸꬴꬳＥｅiÍíi̇́Ììi̇̀ĬĭÎîǏǐÏïḮḯĨĩi̇̃ĮįĮ́į̇́Į̃į̇̃ĪīĪ̀ī̀ỈỉȈȉI̋i̋ȊȋỊịꞼꞽḬḭƗɨᶖİiIıＩｉ1lĺľļḷḹl̃ḽḻłŀƚꝉⱡɫɬꞎꬷꬸꬹᶅɭȴＬｌ][tŤťṪṫŢţṬṭȚțṰṱṮṯŦŧȾⱦƬƭƮʈT̈ẗᵵƫȶ]{1,2}([rŔŕŘřṘṙŖŗȐȑȒȓṚṛṜṝṞṟR̃r̃ɌɍꞦꞧⱤɽᵲᶉꭉ][yÝýỲỳŶŷY̊ẙŸÿỸỹẎẏȲȳỶỷỴỵɎɏƳƴỾỿ]|[rŔŕŘřṘṙŖŗȐȑȒȓṚṛṜṝṞṟR̃r̃ɌɍꞦꞧⱤɽᵲᶉꭉ][iÍíi̇́Ììi̇̀ĬĭÎîǏǐÏïḮḯĨĩi̇̃ĮįĮ́į̇́Į̃į̇̃ĪīĪ̀ī̀ỈỉȈȉI̋i̋ȊȋỊịꞼꞽḬḭƗɨᶖİiIıＩｉ1lĺľļḷḹl̃ḽḻłŀƚꝉⱡɫɬꞎꬷꬸꬹᶅɭȴＬｌ][e3ЄєЕеÉéÈèĔĕÊêẾếỀềỄễỂểÊ̄ê̄Ê̌ê̌ĚěËëẼẽĖėĖ́ė́Ė̃ė̃ȨȩḜḝĘęĘ́ę́Ę̃ę̃ĒēḖḗḔḕẺẻȄȅE̋e̋ȆȇẸẹỆệḘḙḚḛɆɇE̩e̩È̩è̩É̩é̩ᶒⱸꬴꬳＥｅ])?)?[sŚśṤṥŜŝŠšṦṧṠṡŞşṢṣṨṩȘșS̩s̩ꞨꞩⱾȿꟅʂᶊᵴ]?\\b"
                        val tranny_regex =
                            "\\b[tŤťṪṫŢţṬṭȚțṰṱṮṯŦŧȾⱦƬƭƮʈT̈ẗᵵƫȶ][rŔŕŘřṘṙŖŗȐȑȒȓṚṛṜṝṞṟR̃r̃ɌɍꞦꞧⱤɽᵲᶉꭉ][aÁáÀàĂăẮắẰằẴẵẲẳÂâẤấẦầẪẫẨẩǍǎÅåǺǻÄäǞǟÃãȦȧǠǡĄąĄ́ą́Ą̃ą̃ĀāĀ̀ā̀ẢảȀȁA̋a̋ȂȃẠạẶặẬậḀḁȺⱥꞺꞻᶏẚＡａ4]+[nŃńǸǹŇňÑñṄṅŅņṆṇṊṋṈṉN̈n̈ƝɲŊŋꞐꞑꞤꞥᵰᶇɳȵꬻꬼИиПпＮｎ]{1,2}([iÍíi̇́Ììi̇̀ĬĭÎîǏǐÏïḮḯĨĩi̇̃ĮįĮ́į̇́Į̃į̇̃ĪīĪ̀ī̀ỈỉȈȉI̋i̋ȊȋỊịꞼꞽḬḭƗɨᶖİiIıＩｉ1lĺľļḷḹl̃ḽḻłŀƚꝉⱡɫɬꞎꬷꬸꬹᶅɭȴＬｌ][e3ЄєЕеÉéÈèĔĕÊêẾếỀềỄễỂểÊ̄ê̄Ê̌ê̌ĚěËëẼẽĖėĖ́ė́Ė̃ė̃ȨȩḜḝĘęĘ́ę́Ę̃ę̃ĒēḖḗḔḕẺẻȄȅE̋e̋ȆȇẸẹỆệḘḙḚḛɆɇE̩e̩È̩è̩É̩é̩ᶒⱸꬴꬳＥｅ]|[yÝýỲỳŶŷY̊ẙŸÿỸỹẎẏȲȳỶỷỴỵɎɏƳƴỾỿ]|[e3ЄєЕеÉéÈèĔĕÊêẾếỀềỄễỂểÊ̄ê̄Ê̌ê̌ĚěËëẼẽĖėĖ́ė́Ė̃ė̃ȨȩḜḝĘęĘ́ę́Ę̃ę̃ĒēḖḗḔḕẺẻȄȅE̋e̋ȆȇẸẹỆệḘḙḚḛɆɇE̩e̩È̩è̩É̩é̩ᶒⱸꬴꬳＥｅ][rŔŕŘřṘṙŖŗȐȑȒȓṚṛṜṝṞṟR̃r̃ɌɍꞦꞧⱤɽᵲᶉꭉ])[sŚśṤṥŜŝŠšṦṧṠṡŞşṢṣṨṩȘșS̩s̩ꞨꞩⱾȿꟅʂᶊᵴ]?\\b"
                        if (string.lowercase().matches(Regex.fromLiteral(nword_regex))) {
                            event.message.delete().queue()
                            Auditing.automodEntry(
                                event.guild.idLong,
                                "${event.message.author.asMention} has attempted to post an image with a bad word in ${event.message.channel.asMention}\n\nFile Name: ${attachment.fileName}\n\nDetected Word: nigger"
                            )
                            return
                        } else if (string.lowercase().matches(Regex.fromLiteral(fword_regex))) {
                            event.message.delete().queue()
                            Auditing.automodEntry(
                                event.guild.idLong,
                                "${event.message.author.asMention} has attempted to post an image with a bad word in ${event.message.channel.asMention}\n\nFile Name: ${attachment.fileName}\n\nDetected Word: faggot"
                            )
                            return
                        }
                        if (string.lowercase().matches(Regex.fromLiteral(tranny_regex))) {
                            event.message.delete().queue()
                            Auditing.automodEntry(
                                event.guild.idLong,
                                "${event.message.author.asMention} has attempted to post an image with a bad word in ${event.message.channel.asMention}\n\nFile Name: ${attachment.fileName}\n\nDetected Word: tranny"
                            )
                            return
                        }
                        for (bannedWord in bannedWords) {
                            if (string.lowercase().contains(bannedWord)) {
                                event.message.delete().queue()
                                Auditing.automodEntry(
                                    event.guild.idLong,
                                    "${event.message.author.asMention} has attempted to post an image with a bad word in ${event.message.channel.asMention}\n\nFile Name: ${attachment.fileName}\n\nDetected Word: $bannedWord"
                                )
                                return
                            }
                        }
                    }
                }
            }
            if (event.message.channel.idLong == 1241883049713598484L && event.message.mentions.members.any()){
                val loggingChannelID: Long = (ConfigFileJson.serverGet(event.guild!!.id, "logging_channel") ?: "967156927731748914").toLong()
                val loggingChannel: MessageChannel = BotMain.jda.getGuildChannelById(loggingChannelID) as MessageChannel
                loggingChannel.sendMessage("Mudae Log\n${event.message.contentRaw}")
            }
        }
    }

    override fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        if(event.guild.idLong != 967140876298092634)
        {
            return
        }
        val autoBanRole = event.guild.getRoleById(1174513214650855444L)
        if(event.roles.contains(autoBanRole))
        {
            Auditing.automodEntry(event.guild.idLong, "Banning ${event.member.effectiveName} in 5 mins for triggering bot detection")
            BotMain.timerThread.registerTimer(
                Timer(
                    {
                        //This looks strange but we need to force it to re-pull from cache instead of the old info as it will always be true
                        if(event.jda.getGuildById(event.guild.idLong)!!.getMemberById(event.member.idLong)!!.roles.contains(autoBanRole)) {
                            event.member.ban(0, TimeUnit.DAYS).reason("Auto-Ban by @Platinum").queue()
                            Auditing.automodEntry(event.guild.idLong, "Banned: ${event.member.effectiveName}")
                        }
                    },
                    5 * 60)
            )
            return
        }
        staffRoleLog(event)
        //This is for the Birthday Role. 24 hours after it's applied, the bot will automatically remove it from the member.
        if(event.roles[0].idLong == 1129010319432364042)
        {
            BotMain.timerThread.registerTimer(Timer({event.guild.removeRoleFromMember(event.member, event.roles[0])}, 24*60*60))
        }
        if(event.guild.idLong == 967140876298092634L)
        {
            addRolesInServer(event.member.idLong, 1165357291629989979L, event.roles)
        }
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        //println("Reaction added to: ${event.messageIdLong}")
        if(event.messageIdLong == 1250224928859357305L){
            val channel = event.guild.getGuildChannelById(1087346724474998796)
            channel!!.permissionContainer.upsertPermissionOverride(event.member!!)
                .setAllowed(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL).queue()
        }
        else if(event.messageIdLong == 1250226375286194266L){
            val channel = event.guild.getGuildChannelById(1241883049713598484)
            channel!!.permissionContainer.upsertPermissionOverride(event.member!!)
                .setAllowed(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL).queue()
        }
    }

    fun staffRoleLog(event: GuildMemberRoleAddEvent){
        val adminRole = event.guild.getRoleById(967142423534903358)
        val moderatorRole = event.guild.getRoleById(967142088296767598)
        val helperRole = event.guild.getRoleById(1073417783326560316)
        val eventHostRole = event.guild.getRoleById(971038281649238046)
        if(event.roles.contains(adminRole) || event.roles.contains(moderatorRole) || event.roles.contains(helperRole) || event.roles.contains(eventHostRole))
        {
            val loggingChannel = event.guild.getTextChannelById(967156927731748914)
            loggingChannel!!.sendMessageEmbeds(staffRoleAddEmbed(event.member, event.roles[0]))
        }
    }

    fun staffRoleAddEmbed(member: Member, role: Role): MessageEmbed {
        val builder = EmbedBuilder()
        val authorAvatar = member.effectiveAvatarUrl
        builder.setTimestamp(Instant.now())
        //The URL here is translated by the Discord client into a #channel > Message link
        builder.setTitle("Added to Staff Role: @" + role.name)
        //builder.setDescription("via <@762217899355013120>")
        //builder.setDescription("**Previous**: ${previous?.content ?: "Unavailable"}\n**Now:**: ${event.message.contentRaw}")
        builder.setColor(MessageLevel.Level.MODIFY.color)
        builder.setAuthor(member.effectiveName, authorAvatar, authorAvatar)
        return builder.build()
    }
}