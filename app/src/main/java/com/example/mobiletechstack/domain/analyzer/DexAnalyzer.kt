package com.example.mobiletechstack.domain.analyzer

import android.content.Context
import com.example.mobiletechstack.domain.model.DetectedLibrary
import com.example.mobiletechstack.domain.model.LibraryCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import java.io.File
import java.util.zip.ZipFile


class DexAnalyzer(private val context: Context) {

    private val analyticsPatterns = mapOf(
        // Firebase
        "com.google.firebase.analytics" to "Firebase Analytics",
        "com.google.android.gms.measurement" to "Firebase Analytics (GMS)",

        // Google Analytics
        "com.google.android.gms.analytics" to "Google Analytics",

        // Facebook
        "com.facebook.appevents" to "Facebook Analytics",
        "com.facebook.FacebookSdk" to "Facebook SDK",

        // AppsFlyer
        "com.appsflyer" to "AppsFlyer",

        // Amplitude
        "com.amplitude" to "Amplitude",

        // Mixpanel
        "com.mixpanel.android" to "Mixpanel",

        // Branch.io
        "io.branch.referral" to "Branch.io",

        // Adjust
        "com.adjust.sdk" to "Adjust",

        // Crashlytics / Firebase Crashlytics
        "com.crashlytics" to "Crashlytics",
        "com.google.firebase.crashlytics" to "Firebase Crashlytics",

        // Flurry
        "com.flurry.android" to "Flurry Analytics",

        // Localytics
        "com.localytics.android" to "Localytics",

        // Segment
        "com.segment.analytics" to "Segment",

        // Countly
        "ly.count.android.sdk" to "Countly",

        // Kochava
        "com.kochava.base" to "Kochava"
    )

    private val advertisingPatterns = mapOf(
        // Google
        "com.google.android.gms.ads" to "Google AdMob",
        "com.google.ads.mediation" to "Google Ad Mediation",

        // Facebook
        "com.facebook.ads" to "Facebook Audience Network",

        // Unity
        "com.unity3d.ads" to "Unity Ads",

        // AppLovin
        "com.applovin" to "AppLovin",

        // MoPub
        "com.mopub" to "MoPub",

        // IronSource
        "com.ironsource" to "IronSource",

        // Chartboost
        "com.chartboost.sdk" to "Chartboost",

        // AdColony
        "com.adcolony.sdk" to "AdColony",

        // Vungle
        "com.vungle" to "Vungle",

        // InMobi
        "com.inmobi" to "InMobi",

        // StartApp
        "com.startapp.android" to "StartApp",

        // Tapjoy
        "com.tapjoy" to "Tapjoy",

        // Pangle (TikTok)
        "com.bytedance.sdk.openadsdk" to "Pangle (TikTok Ads)",

        // Fyber
        "com.fyber" to "Fyber"
    )

    private val socialPatterns = mapOf(
        // Facebook
        "com.facebook.login" to "Facebook Login",
        "com.facebook.share" to "Facebook Share",

        // Google
        "com.google.android.gms.auth" to "Google Sign-In",

        // Twitter
        "com.twitter.sdk" to "Twitter SDK",
        "twitter4j" to "Twitter4J",

        // LinkedIn
        "com.linkedin.platform" to "LinkedIn SDK",

        // Instagram
        "com.instagram.common" to "Instagram SDK",

        // VK (VKontakte)
        "com.vk.sdk" to "VK SDK",

        // WeChat
        "com.tencent.mm.opensdk" to "WeChat SDK",

        // Line
        "com.linecorp" to "Line SDK",

        // Telegram
        "org.telegram" to "Telegram SDK",

        // Pinterest
        "com.pinterest" to "Pinterest SDK",

        // Snapchat
        "com.snapchat.kit.sdk" to "Snapchat SDK",

        // TikTok
        "com.bytedance.ies.ugc.aweme" to "TikTok SDK"
    )

    /**
     * База паттернов для Payment SDKs
     */
    private val paymentPatterns = mapOf(
        // Stripe
        "com.stripe.android" to "Stripe",

        // PayPal
        "com.paypal.android" to "PayPal",

        // Google Pay
        "com.google.android.gms.wallet" to "Google Pay",

        // Braintree
        "com.braintreepayments" to "Braintree",

        // Square
        "com.squareup.sdk" to "Square",

        // Razorpay
        "com.razorpay" to "Razorpay",

        // Paytm
        "com.paytm" to "Paytm",

        // PayU
        "com.payu" to "PayU",

        // Adyen
        "com.adyen" to "Adyen",

        // Klarna
        "com.klarna" to "Klarna"
    )

    suspend fun detectLibraries(apkPath: String): List<DetectedLibrary> = withContext(Dispatchers.IO) {
        try {
            val detectedLibraries = mutableSetOf<DetectedLibrary>()

            val dexFiles = extractDexFiles(apkPath)

            dexFiles.forEach { dexFile ->
                val libraries = analyzeDexFile(dexFile)
                detectedLibraries.addAll(libraries)
            }

            dexFiles.forEach { it.delete() }

            detectedLibraries.toList().sortedBy { it.name }

        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractDexFiles(apkPath: String): List<File> {
        val dexFiles = mutableListOf<File>()
        val tempDir = context.cacheDir

        ZipFile(apkPath).use { zipFile ->
            zipFile.entries().asSequence()
                .filter { it.name.matches(Regex("classes\\d*\\.dex")) }
                .forEach { entry ->
                    val tempFile = File(tempDir, entry.name)

                    zipFile.getInputStream(entry).use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    dexFiles.add(tempFile)
                }
        }

        return dexFiles
    }

    private fun analyzeDexFile(dexFile: File): List<DetectedLibrary> {
        val detectedLibraries = mutableSetOf<DetectedLibrary>()

        try {
            val dex = DexFileFactory.loadDexFile(dexFile, Opcodes.getDefault())

            dex.classes.forEach { classDef ->
                val className = classDef.type
                    .removePrefix("L")  // Убираем "L" в начале
                    .removeSuffix(";")  // Убираем ";" в конце
                    .replace('/', '.')  // Заменяем "/" на "."

                checkPattern(className, analyticsPatterns, LibraryCategory.ANALYTICS, detectedLibraries)
                checkPattern(className, advertisingPatterns, LibraryCategory.ADVERTISING, detectedLibraries)
                checkPattern(className, socialPatterns, LibraryCategory.SOCIAL, detectedLibraries)
                checkPattern(className, paymentPatterns, LibraryCategory.PAYMENT, detectedLibraries)
            }

        } catch (e: Exception) {
            //ignore
        }

        return detectedLibraries.toList()
    }

    private fun checkPattern(
        className: String,
        patterns: Map<String, String>,
        category: LibraryCategory,
        result: MutableSet<DetectedLibrary>
    ) {
        patterns.forEach { (pattern, libraryName) ->
            if (className.startsWith(pattern)) {
                result.add(
                    DetectedLibrary(
                        name = libraryName,
                        packageName = pattern,
                        category = category
                    )
                )
            }
        }
    }

    suspend fun detectAnalyticsLibraries(apkPath: String): List<DetectedLibrary> {
        return detectLibraries(apkPath)
    }
}