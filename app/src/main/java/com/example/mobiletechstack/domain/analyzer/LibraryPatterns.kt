package com.example.mobiletechstack.domain.analyzer

import com.example.mobiletechstack.domain.model.LibraryCategory


object LibraryPatterns {

    data class LibraryPattern(
        val packagePattern: String,
        val libraryName: String
    )

    val analyticsPatterns = listOf(
        // Firebase
        LibraryPattern("com.google.firebase.analytics", "Firebase Analytics"),
        LibraryPattern("com.google.android.gms.measurement", "Firebase Analytics (GMS)"),

        // Google Analytics
        LibraryPattern("com.google.android.gms.analytics", "Google Analytics"),

        // Facebook
        LibraryPattern("com.facebook.appevents", "Facebook Analytics"),
        LibraryPattern("com.facebook.FacebookSdk", "Facebook SDK"),

        // AppsFlyer
        LibraryPattern("com.appsflyer", "AppsFlyer"),

        // Amplitude
        LibraryPattern("com.amplitude", "Amplitude"),

        // Mixpanel
        LibraryPattern("com.mixpanel.android", "Mixpanel"),

        // Branch.io
        LibraryPattern("io.branch.referral", "Branch.io"),

        // Adjust
        LibraryPattern("com.adjust.sdk", "Adjust"),

        // Crashlytics
        LibraryPattern("com.crashlytics", "Crashlytics"),
        LibraryPattern("com.google.firebase.crashlytics", "Firebase Crashlytics"),

        // Flurry
        LibraryPattern("com.flurry.android", "Flurry Analytics"),

        // Localytics
        LibraryPattern("com.localytics.android", "Localytics"),

        // Segment
        LibraryPattern("com.segment.analytics", "Segment"),

        // Countly
        LibraryPattern("ly.count.android.sdk", "Countly"),

        // Kochava
        LibraryPattern("com.kochava.base", "Kochava"),

        // Sentry
        LibraryPattern("io.sentry", "Sentry")
    )

    val advertisingPatterns = listOf(
        // Google
        LibraryPattern("com.google.android.gms.ads", "Google AdMob"),
        LibraryPattern("com.google.ads.mediation", "Google Ad Mediation"),

        // Facebook
        LibraryPattern("com.facebook.ads", "Facebook Audience Network"),

        // Unity
        LibraryPattern("com.unity3d.ads", "Unity Ads"),

        // AppLovin
        LibraryPattern("com.applovin", "AppLovin"),

        // MoPub
        LibraryPattern("com.mopub", "MoPub"),

        // IronSource
        LibraryPattern("com.ironsource", "IronSource"),

        // Chartboost
        LibraryPattern("com.chartboost.sdk", "Chartboost"),

        // AdColony
        LibraryPattern("com.adcolony.sdk", "AdColony"),

        // Vungle
        LibraryPattern("com.vungle", "Vungle"),

        // InMobi
        LibraryPattern("com.inmobi", "InMobi"),

        // StartApp
        LibraryPattern("com.startapp.android", "StartApp"),

        // Tapjoy
        LibraryPattern("com.tapjoy", "Tapjoy"),

        // Pangle (TikTok)
        LibraryPattern("com.bytedance.sdk.openadsdk", "Pangle (TikTok Ads)"),

        // Fyber
        LibraryPattern("com.fyber", "Fyber"),

        // AdMob Mediation Adapters
        LibraryPattern("com.google.ads.mediation.admob", "AdMob Mediation Adapter")
    )

    val socialPatterns = listOf(
        // Facebook
        LibraryPattern("com.facebook.login", "Facebook Login"),
        LibraryPattern("com.facebook.share", "Facebook Share"),

        // Google
        LibraryPattern("com.google.android.gms.auth", "Google Sign-In"),

        // Twitter
        LibraryPattern("com.twitter.sdk", "Twitter SDK"),
        LibraryPattern("twitter4j", "Twitter4J"),

        // LinkedIn
        LibraryPattern("com.linkedin.platform", "LinkedIn SDK"),

        // Instagram
        LibraryPattern("com.instagram.common", "Instagram SDK"),

        // VK (VKontakte)
        LibraryPattern("com.vk.sdk", "VK SDK"),

        // WeChat
        LibraryPattern("com.tencent.mm.opensdk", "WeChat SDK"),

        // Line
        LibraryPattern("com.linecorp", "Line SDK"),

        // Telegram
        LibraryPattern("org.telegram", "Telegram SDK"),

        // Pinterest
        LibraryPattern("com.pinterest", "Pinterest SDK"),

        // Snapchat
        LibraryPattern("com.snapchat.kit.sdk", "Snapchat SDK"),

        // TikTok
        LibraryPattern("com.bytedance.ies.ugc.aweme", "TikTok SDK"),

        // Discord
        LibraryPattern("com.discord", "Discord SDK")
    )

    val paymentPatterns = listOf(
        // Stripe
        LibraryPattern("com.stripe.android", "Stripe"),

        // PayPal
        LibraryPattern("com.paypal.android", "PayPal"),

        // Google Pay
        LibraryPattern("com.google.android.gms.wallet", "Google Pay"),

        // Braintree
        LibraryPattern("com.braintreepayments", "Braintree"),

        // Square
        LibraryPattern("com.squareup.sdk", "Square"),

        // Razorpay
        LibraryPattern("com.razorpay", "Razorpay"),

        // Paytm
        LibraryPattern("com.paytm", "Paytm"),

        // PayU
        LibraryPattern("com.payu", "PayU"),

        // Adyen
        LibraryPattern("com.adyen", "Adyen"),

        // Klarna
        LibraryPattern("com.klarna", "Klarna"),

        // Checkout.com
        LibraryPattern("com.checkout", "Checkout.com")
    )

    fun getPatternsForCategory(category: LibraryCategory): List<LibraryPattern> {
        return when (category) {
            LibraryCategory.ANALYTICS -> analyticsPatterns
            LibraryCategory.ADVERTISING -> advertisingPatterns
            LibraryCategory.SOCIAL -> socialPatterns
            LibraryCategory.PAYMENT -> paymentPatterns
        }
    }

    fun getAllPatterns(): Map<LibraryCategory, List<LibraryPattern>> {
        return mapOf(
            LibraryCategory.ANALYTICS to analyticsPatterns,
            LibraryCategory.ADVERTISING to advertisingPatterns,
            LibraryCategory.SOCIAL to socialPatterns,
            LibraryCategory.PAYMENT to paymentPatterns
        )
    }
}