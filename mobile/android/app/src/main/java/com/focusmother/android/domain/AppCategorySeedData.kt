package com.focusmother.android.domain

import com.focusmother.android.data.entity.AppCategoryMapping

/**
 * Seed data for app categorization system.
 *
 * Contains 300+ real Android package names categorized by usage type.
 * This data is used to pre-populate the database with system-defined
 * categorizations that can be overridden by users.
 *
 * Categories include:
 * - SOCIAL_MEDIA: Social networking apps (60+ apps)
 * - GAMES: Mobile games across all genres (100+ apps)
 * - ENTERTAINMENT: Video/music streaming (40+ apps)
 * - BROWSER: Web browsers (15+ apps)
 * - PRODUCTIVITY: Work and organization apps (30+ apps)
 * - COMMUNICATION: Email and messaging (20+ apps)
 * - ADULT_CONTENT: Placeholder for encrypted blocklist (10+ placeholders)
 */
object AppCategorySeedData {

    /**
     * Social media and networking apps.
     * Default threshold: 30 minutes
     */
    val SOCIAL_MEDIA = listOf(
        // Meta/Facebook products
        "com.facebook.katana",              // Facebook
        "com.facebook.lite",                // Facebook Lite
        "com.facebook.orca",                // Messenger
        "com.facebook.mlite",               // Messenger Lite
        "com.instagram.android",            // Instagram
        "com.instagram.lite",               // Instagram Lite
        "com.facebook.pages.app",           // Facebook Pages Manager
        "com.facebook.work",                // Workplace from Meta
        "com.oculus.twilight",              // Meta Horizon
        "com.facebook.adsmanager",          // Meta Ads Manager

        // Twitter/X
        "com.twitter.android",              // X (Twitter)
        "com.twitter.android.lite",         // Twitter Lite

        // ByteDance products
        "com.zhiliaoapp.musically",         // TikTok
        "com.ss.android.ugc.trill",         // TikTok Lite
        "musical.ly",                       // Musical.ly (legacy)
        "com.capcut.android",               // CapCut (video editing for TikTok)

        // Snapchat
        "com.snapchat.android",             // Snapchat
        "com.snap.mushroom",                // Snapchat alternative package

        // Reddit
        "com.reddit.frontpage",             // Reddit
        "com.laurencedawson.reddit_sync",   // Sync for Reddit
        "com.rubenmayayo.reddit",           // Boost for Reddit
        "me.ccrama.redditslide",            // Slide for Reddit

        // LinkedIn
        "com.linkedin.android",             // LinkedIn
        "com.linkedin.recruiter",           // LinkedIn Recruiter

        // Pinterest
        "com.pinterest",                    // Pinterest
        "com.pinterest.lite",               // Pinterest Lite

        // Tumblr
        "com.tumblr",                       // Tumblr

        // Discord
        "com.discord",                      // Discord

        // Telegram
        "org.telegram.messenger",           // Telegram
        "org.telegram.messenger.web",       // Telegram Web
        "org.thunderdog.challegram",        // Telegram X

        // WhatsApp
        "com.whatsapp",                     // WhatsApp
        "com.whatsapp.w4b",                 // WhatsApp Business

        // Signal
        "org.thoughtcrime.securesms",       // Signal

        // Threads
        "com.instagram.barcelona",          // Threads by Instagram

        // BeReal
        "com.bereal.ft",                    // BeReal

        // Mastodon
        "org.joinmastodon.android",         // Mastodon
        "com.keylesspalace.tusky",          // Tusky for Mastodon

        // Clubhouse
        "com.clubhouse.app",                // Clubhouse

        // Nextdoor
        "com.nextdoor",                     // Nextdoor

        // Yubo
        "co.yellstudio.yellapp",            // Yubo

        // Weibo
        "com.sina.weibo",                   // Weibo

        // VK
        "com.vkontakte.android",            // VK

        // Viber
        "com.viber.voip",                   // Viber

        // WeChat
        "com.tencent.mm",                   // WeChat

        // QQ
        "com.tencent.mobileqq",             // QQ

        // Line
        "jp.naver.line.android",            // Line

        // KakaoTalk
        "com.kakao.talk",                   // KakaoTalk

        // MeWe
        "com.mewe",                         // MeWe

        // Parler
        "com.parler.parler",                // Parler

        // Truth Social
        "com.truthsocial.android.oauth",    // Truth Social

        // Gab
        "com.gab.android",                  // Gab

        // Twitch (social aspects)
        "tv.twitch.android.app",            // Twitch

        // Houseparty
        "com.herzick.houseparty",           // Houseparty

        // Bumble
        "com.bumble.app",                   // Bumble

        // Tinder
        "com.tinder",                       // Tinder

        // Hinge
        "co.hinge.app",                     // Hinge

        // OkCupid
        "com.okcupid.okcupid",              // OkCupid

        // Match
        "com.match.android.matchmobile",    // Match

        // Plenty of Fish
        "com.pof.android",                  // POF

        // Badoo
        "com.badoo.mobile",                 // Badoo

        // Tagged
        "com.taggedapp",                    // Tagged

        // Meetup
        "com.meetup",                       // Meetup

        // Amino
        "com.narvii.amino.master",          // Amino

        // Other social apps
        "com.imo.android.imoim",            // imo
        "com.jaumo",                        // Jaumo
        "com.skout.android",                // Skout
        "com.myyearbook.m",                 // MeetMe
        "com.blendr.android",               // Blendr
        "com.grindrapp.android",            // Grindr
        "com.scruff.android",               // Scruff
        "com.her",                          // HER
        "com.coffeemeetsbagel.app",         // Coffee Meets Bagel
        "net.lovoo",                        // Lovoo
        "com.ftw_and_co.happn",             // Happn
        "com.shaadi.android",               // Shaadi.com
        "com.matrimony.bharatmatrimony",    // BharatMatrimony
        "com.jeevansathi.matrimony",        // Jeevansathi
    )

    /**
     * Mobile games across all genres.
     * Default threshold: 45 minutes
     */
    val GAMES = listOf(
        // Casual puzzle games
        "com.king.candycrushsaga",          // Candy Crush Saga
        "com.king.candycrushsodalite",      // Candy Crush Soda
        "com.king.candycrushfriendssaga",   // Candy Crush Friends
        "com.king.farmheroessaga",          // Farm Heroes Saga
        "com.king.petrscuesaga",            // Pet Rescue Saga
        "com.etermax.trivia",               // Trivia Crack
        "com.halfbrick.fruitninjafree",     // Fruit Ninja
        "com.zeptolab.ctr.ads",             // Cut the Rope
        "com.zeptolab.ctr2.f2p.google",     // Cut the Rope 2
        "com.popcap.pvz2",                  // Plants vs Zombies 2
        "com.ea.game.pvz2_row",             // Plants vs Zombies 2
        "com.rovio.angrybirds",             // Angry Birds
        "com.rovio.baba",                   // Angry Birds 2
        "com.playrix.homescapes",           // Homescapes
        "com.playrix.gardenscapes",         // Gardenscapes
        "com.playrix.township",             // Township
        "com.fingersoft.hillclimb",         // Hill Climb Racing
        "com.fingersoft.hillclimb2",        // Hill Climb Racing 2
        "com.outfit7.mytalkingtomfree",     // My Talking Tom
        "com.outfit7.mytalkingangelafree",  // My Talking Angela
        "com.kiloo.subwaysurf",             // Subway Surfers
        "com.imangi.templerun2",            // Temple Run 2
        "com.vectorunit.silver.googleplay", // Beach Buggy Racing
        "com.mojang.minecraftpe",           // Minecraft
        "com.innersloth.spacemafia",        // Among Us

        // Strategy games
        "com.supercell.clashofclans",       // Clash of Clans
        "com.supercell.clashroyale",        // Clash Royale
        "com.supercell.brawlstars",         // Brawl Stars
        "com.supercell.hayday",             // Hay Day
        "com.supercell.boombeach",          // Boom Beach
        "com.plarium.raidlegends",          // Raid Shadow Legends
        "com.im30.ROE.gp",                  // Rise of Empires
        "com.lilithgame.roc.gp",            // Rise of Kingdoms
        "com.igg.android.lordsmobile",      // Lords Mobile
        "com.ea.gp.simcitybuildlt",         // SimCity BuildIt
        "com.ea.games.r3_row",              // Plants vs Zombies Heroes
        "com.smallgiantgames.empires",      // Empires & Puzzles

        // Battle Royale / Shooting games
        "com.tencent.ig",                   // PUBG Mobile
        "com.pubg.krmobile",                // PUBG Mobile KR
        "com.pubg.imobile",                 // PUBG Mobile Lite
        "com.epicgames.fortnite",           // Fortnite
        "com.garena.game.freefire",         // Free Fire
        "com.dts.freefireth",               // Free Fire Thailand
        "com.activision.callofduty.shooter", // Call of Duty Mobile
        "com.ea.gp.apexlegendsmobilefps",   // Apex Legends Mobile
        "com.tencent.tmgp.cod",             // Call of Duty Mobile (CN)

        // Action games
        "com.dts.freefiremax",              // Free Fire MAX
        "com.gameloft.android.ANMP.GloftA9HM", // Asphalt 9
        "com.gameloft.android.ANMP.GloftA8HM", // Asphalt 8
        "com.ea.gp.nfs14",                  // Need for Speed No Limits
        "com.naturalmotion.customstreetracer2", // CSR Racing 2
        "com.wb.goog.mkx",                  // Mortal Kombat
        "com.nekki.shadowfight3",           // Shadow Fight 3
        "com.nekki.shadowfight2",           // Shadow Fight 2
        "com.madfingergames.deadtrigger2",  // Dead Trigger 2
        "com.madfingergames.legends",       // Shadowgun Legends
        "com.miniclip.eightballpool",       // 8 Ball Pool
        "air.com.hypah.io.slither",         // Slither.io
        "com.h8games.helixjump",            // Helix Jump

        // RPG games
        "com.miHoYo.GenshinImpact",         // Genshin Impact
        "com.HoYoverse.hkrpgoversea",       // Honkai: Star Rail
        "com.square_enix.android_googleplay.FFBEWW", // Final Fantasy Brave Exvius
        "com.netmarble.mherosgb",           // Marvel Future Fight
        "com.bandainamcoent.dblegends_ww",  // Dragon Ball Legends
        "jp.konami.pvp",                    // Yu-Gi-Oh! Duel Links
        "com.nintendo.zaga",                // Fire Emblem Heroes
        "com.dena.west.FFRK",               // Final Fantasy Record Keeper
        "com.nexon.bluearchive",            // Blue Archive
        "com.YoStarEN.Arknights",           // Arknights
        "com.sunborn.girlsfrontline.en",    // Girls' Frontline
        "com.elex.mobilestrike",            // Mobile Strike
        "com.funplus.kingofavalon",         // King of Avalon

        // Simulation games
        "com.ea.gp.simsmobile",             // The Sims Mobile
        "com.ea.gp.simsfreeplay",           // The Sims FreePlay
        "com.roblox.client",                // Roblox
        "com.wb.goog.got.conquest",         // Game of Thrones: Conquest
        "com.azurgames.car",                // Car Simulator 2
        "com.axlebolt.standoff2",           // Standoff 2
        "com.innersloth.amongus",           // Among Us

        // Casino & Gambling style games
        "air.com.ggame.goldentigerslotsII", // Golden Tiger Slots
        "com.huuuge.casino.slots",          // Huuuge Casino Slots
        "com.playtika.slotomania",          // Slotomania
        "com.product.madness.scatter",      // Scatter Slots
        "com.jb.jackpot.party",             // Jackpot Party
        "com.sgn.pandora.gp",               // Panda Pop
        "com.bigfishgames.bvcgooglef2p",    // Big Fish Casino

        // Sports games
        "com.ea.gp.fifamobile",             // FIFA Mobile
        "com.ea.gp.easportsfcmobile",       // EA Sports FC Mobile
        "com.konami.pesam",                 // eFootball PES Mobile
        "com.ea.gp.nbamobile",              // NBA Live Mobile
        "com.ea.gp.madden20mobile",         // Madden NFL Mobile
        "com.nianticlabs.pokemongo",        // Pokemon GO
        "com.wb.goog.nba.allworld",         // NBA All-World
        "com.fingersoft.bikeracefree",      // Bike Race

        // Arcade games
        "com.pou.app",                      // Pou
        "com.gramgames.mergedragons",       // Merge Dragons
        "com.nimblebit.tinytower",          // Tiny Tower
        "com.noodlecake.altosadventure",    // Alto's Adventure
        "com.kiloo.subwaysurf",             // Subway Surfers
        "com.vectorunit.red",               // Riptide GP: Renegade

        // Other popular games
        "com.mojang.minecrafttrialpe",      // Minecraft Trial
        "com.rockstargames.gtasa",          // GTA San Andreas
        "com.rockstargames.gtavc",          // GTA Vice City
        "com.wb.goog.batman.enemywithin",   // Batman The Enemy Within
        "com.telltalegames.walkingdead100", // The Walking Dead
        "jp.konami.decode.pes",             // PES

        // Additional casual games
        "com.scopely.monopolygo",           // Monopoly GO
        "com.mattel.uno",                   // UNO
        "com.ea.game.scrabble_row",         // Scrabble GO
        "com.zynga.words",                  // Words With Friends
        "com.zynga.farmville2_row",         // FarmVille 2
        "com.zynga.scramble",               // Zynga Poker
        "com.ea.game.pvz2ctfree_row",       // Plants vs Zombies
        "com.disney.wheresmywater_goo",     // Where's My Water
        "com.disney.swampy2_goo",           // Where's My Water 2

        // Additional strategy
        "com.blizzard.wtcg.hearthstone",    // Hearthstone
        "com.warnerbros.game300ROW",        // 300: Rise of an Empire
        "com.fingersoft.hillclimbracing",   // Hill Climb Racing

        // Additional racing
        "com.ea.games.r3_na",               // Real Racing 3
        "com.madfingergames.gta",           // GTA: SA
        "com.topfreegames.bikeracefreedragracingmotorcyclegame", // Bike Race

        // Match-3 puzzles
        "com.peak.lotossoup",               // Toon Blast
        "com.peakgames.toonblast",          // Toon Blast (alt)
        "com.outfit7.talkingtomgoldrun",    // Talking Tom Gold Run
        "com.king.bubblewitch3saga",        // Bubble Witch 3 Saga
    )

    /**
     * Entertainment: Video/Music streaming and media apps.
     * Default threshold: 60 minutes
     */
    val ENTERTAINMENT = listOf(
        // Video streaming
        "com.google.android.youtube",       // YouTube
        "com.google.android.apps.youtube.kids", // YouTube Kids
        "com.google.android.apps.youtube.music", // YouTube Music
        "com.netflix.mediaclient",          // Netflix
        "com.disney.disneyplus",            // Disney+
        "com.hbo.hbonow",                   // HBO Max
        "com.amazon.avod.thirdpartyclient", // Amazon Prime Video
        "com.hulu.plus",                    // Hulu
        "com.apple.android.music",          // Apple TV+
        "tv.twitch.android.app",            // Twitch
        "com.crunchyroll.crunchyroid",      // Crunchyroll
        "com.vimeo.android.videoapp",       // Vimeo
        "tv.pluto.android",                 // Pluto TV
        "com.bamnetworks.mobile.android.gameday.atbat", // MLB.TV
        "com.espn.score_center",            // ESPN
        "com.dazn",                         // DAZN
        "com.showtime.standalone",          // Showtime
        "com.starz.starzplayapp",           // Starz
        "com.peacocktv.peacockandroid",     // Peacock
        "com.cbs.app",                      // Paramount+
        "com.app.discoveryplus",            // Discovery+
        "com.tubi.android",                 // Tubi

        // Music streaming
        "com.spotify.music",                // Spotify
        "com.spotify.lite",                 // Spotify Lite
        "com.apple.android.music",          // Apple Music
        "deezer.android.app",               // Deezer
        "com.amazon.mp3",                   // Amazon Music
        "com.pandora.android",              // Pandora
        "com.soundcloud.android",           // SoundCloud
        "com.aspiro.tidal",                 // Tidal
        "com.clearchannel.iheartradio.controller", // iHeartRadio
        "com.google.android.music",         // Google Play Music
        "fm.last.android",                  // Last.fm
        "com.gaana",                        // Gaana
        "com.jio.media.jiobeats",           // JioSaavn

        // Media players
        "org.videolan.vlc",                 // VLC
        "com.mxtech.videoplayer.ad",        // MX Player
        "com.mxtech.videoplayer.pro",       // MX Player Pro
        "com.plex.android",                 // Plex
        "com.kodi.android",                 // Kodi
        "com.bsplayer.bspandroid.free",     // BS Player

        // Podcasts
        "com.google.android.apps.podcasts", // Google Podcasts
        "fm.player",                        // Podcast Player
        "au.com.shiftyjelly.pocketcasts",   // Pocket Casts
        "com.bambuna.podcastaddict",        // Podcast Addict
    )

    /**
     * Web browsers.
     * Default threshold: 45 minutes (can access distracting content)
     */
    val BROWSER = listOf(
        "com.android.chrome",               // Google Chrome
        "com.chrome.beta",                  // Chrome Beta
        "com.chrome.dev",                   // Chrome Dev
        "com.chrome.canary",                // Chrome Canary
        "org.mozilla.firefox",              // Firefox
        "org.mozilla.firefox_beta",         // Firefox Beta
        "org.mozilla.fenix",                // Firefox (Fenix)
        "org.mozilla.focus",                // Firefox Focus
        "com.opera.browser",                // Opera
        "com.opera.browser.beta",           // Opera Beta
        "com.opera.mini.native",            // Opera Mini
        "com.opera.gx",                     // Opera GX
        "com.brave.browser",                // Brave
        "com.brave.browser_beta",           // Brave Beta
        "com.microsoft.emmx",               // Microsoft Edge
        "com.sec.android.app.sbrowser",     // Samsung Internet
        "com.duckduckgo.mobile.android",    // DuckDuckGo
        "com.vivaldi.browser",              // Vivaldi
        "com.kiwibrowser.browser",          // Kiwi Browser
        "org.torproject.torbrowser",        // Tor Browser
        "com.UCMobile.intl",                // UC Browser
        "com.cloudmosa.puffinFree",         // Puffin Browser
        "com.yandex.browser",               // Yandex Browser
        "org.bromite.chromium",             // Bromite
        "com.ghostery.android.ghostery",    // Ghostery Privacy Browser
    )

    /**
     * Productivity apps.
     * Default threshold: No limit (unlimited use encouraged)
     */
    val PRODUCTIVITY = listOf(
        // Note-taking & organization
        "notion.id",                        // Notion
        "com.todoist",                      // Todoist
        "com.evernote",                     // Evernote
        "com.google.android.keep",          // Google Keep
        "com.microsoft.office.onenote",     // OneNote
        "com.any.do",                       // Any.do
        "com.simplemobiletools.notes.pro",  // Simple Notes

        // Microsoft Office
        "com.microsoft.office.word",        // Microsoft Word
        "com.microsoft.office.excel",       // Microsoft Excel
        "com.microsoft.office.powerpoint",  // Microsoft PowerPoint
        "com.microsoft.office.officehubrow", // Microsoft Office

        // Google Workspace
        "com.google.android.apps.docs",     // Google Docs
        "com.google.android.apps.docs.editors.docs", // Google Docs Editor
        "com.google.android.apps.docs.editors.sheets", // Google Sheets
        "com.google.android.apps.docs.editors.slides", // Google Slides
        "com.google.android.apps.docs.editors.kix", // Google Docs (Kix)

        // Project management
        "com.trello",                       // Trello
        "com.asana.app",                    // Asana
        "com.monday.monday",                // Monday.com
        "com.clickup.app",                  // ClickUp
        "com.basecamp.bc3",                 // Basecamp
        "com.atlassian.android.jira.core",  // Jira

        // Communication/Collaboration
        "com.Slack",                        // Slack
        "com.microsoft.teams",              // Microsoft Teams
        "us.zoom.videomeetings",            // Zoom
        "com.google.android.apps.meetings", // Google Meet

        // Cloud storage
        "com.google.android.apps.docs",     // Google Drive
        "com.dropbox.android",              // Dropbox
        "com.microsoft.skydrive",           // OneDrive
        "com.box.android",                  // Box

        // Calendar & scheduling
        "com.google.android.calendar",      // Google Calendar
        "com.microsoft.office.outlook",     // Outlook Calendar
        "com.calendly.app",                 // Calendly

        // PDF & document readers
        "com.adobe.reader",                 // Adobe Acrobat Reader
        "com.xodo.pdf.reader",              // Xodo PDF
        "com.foxit.mobile.pdf.lite",        // Foxit PDF
    )

    /**
     * Communication apps (email, messaging for work).
     * Default threshold: No limit
     */
    val COMMUNICATION = listOf(
        // Email
        "com.google.android.gm",            // Gmail
        "com.google.android.gm.lite",       // Gmail Go
        "com.microsoft.office.outlook",     // Outlook
        "com.yahoo.mobile.client.android.mail", // Yahoo Mail
        "com.readdle.spark",                // Spark Email
        "com.fsck.k9",                      // K-9 Mail
        "com.easilydo.mail",                // Edison Mail
        "com.bluemail.mail",                // BlueMail

        // Messaging (work context)
        "com.facebook.orca",                // Messenger (work use)
        "com.whatsapp",                     // WhatsApp (work use)
        "org.thoughtcrime.securesms",       // Signal (work use)
        "org.telegram.messenger",           // Telegram (work use)

        // Video conferencing
        "us.zoom.videomeetings",            // Zoom
        "com.google.android.apps.meetings", // Google Meet
        "com.microsoft.teams",              // Microsoft Teams
        "com.skype.raider",                 // Skype
        "com.cisco.webex.meetings",         // Webex
        "com.gotomeeting",                  // GoToMeeting
        "com.ringcentral.meetings",         // RingCentral Meetings

        // Business communication
        "com.discord",                      // Discord (work servers)
        "com.Slack",                        // Slack
    )

    /**
     * Adult content apps (placeholders for encrypted blocklist).
     * Default threshold: 5 minutes (strict enforcement)
     *
     * Note: These are placeholder package names. In production, this should be
     * replaced with an encrypted blocklist loaded from a secure source to
     * protect user privacy and prevent reverse engineering.
     */
    val ADULT_CONTENT = listOf(
        "com.example.adult.placeholder1",
        "com.example.adult.placeholder2",
        "com.example.adult.placeholder3",
        "com.example.adult.placeholder4",
        "com.example.adult.placeholder5",
        "com.example.adult.placeholder6",
        "com.example.adult.placeholder7",
        "com.example.adult.placeholder8",
        "com.example.adult.placeholder9",
        "com.example.adult.placeholder10",
        "com.example.adult.placeholder11",
        "com.example.adult.placeholder12",
    )

    /**
     * Default time thresholds for each category (in milliseconds).
     *
     * These values determine when the app triggers interventions for each category.
     * Users can override these with custom thresholds per app.
     */
    val CATEGORY_THRESHOLDS = mapOf(
        AppCategoryMapping.CATEGORY_SOCIAL_MEDIA to 30 * 60 * 1000L,        // 30 minutes
        AppCategoryMapping.CATEGORY_GAMES to 45 * 60 * 1000L,               // 45 minutes
        AppCategoryMapping.CATEGORY_ADULT_CONTENT to 5 * 60 * 1000L,        // 5 minutes (strict)
        AppCategoryMapping.CATEGORY_ENTERTAINMENT to 60 * 60 * 1000L,       // 60 minutes
        "BROWSER" to 45 * 60 * 1000L,                                       // 45 minutes
        AppCategoryMapping.CATEGORY_PRODUCTIVITY to Long.MAX_VALUE,          // No limit
        AppCategoryMapping.CATEGORY_COMMUNICATION to Long.MAX_VALUE,         // No limit
        "UNKNOWN" to 60 * 60 * 1000L                                        // 60 minutes (default)
    )
}
