package com.example.mobiletechstack.domain.analyzer

import com.example.mobiletechstack.domain.model.LibraryCategory


object LibraryPatterns {

    data class LibraryPattern(
        val packagePattern: String,
        val libraryName: String
    )

    private val analyticsPatterns = listOf(
        LibraryPattern("com.google.firebase.analytics", "Firebase Analytics"),
        LibraryPattern("com.google.android.gms.measurement", "Firebase Analytics (GMS)"),
        LibraryPattern("com.google.android.gms.analytics", "Google Analytics"),
        LibraryPattern("com.facebook.appevents", "Facebook Analytics"),
        LibraryPattern("com.facebook.FacebookSdk", "Facebook SDK"),
        LibraryPattern("com.appsflyer", "AppsFlyer"),
        LibraryPattern("com.amplitude", "Amplitude"),
        LibraryPattern("com.mixpanel.android", "Mixpanel"),
        LibraryPattern("io.branch.referral", "Branch.io"),
        LibraryPattern("com.adjust.sdk", "Adjust"),
        LibraryPattern("com.crashlytics", "Crashlytics"),
        LibraryPattern("com.google.firebase.crashlytics", "Firebase Crashlytics"),
        LibraryPattern("com.flurry.android", "Flurry Analytics"),
        LibraryPattern("com.localytics.android", "Localytics"),
        LibraryPattern("com.segment.analytics", "Segment"),
        LibraryPattern("ly.count.android.sdk", "Countly"),
        LibraryPattern("com.kochava.base", "Kochava"),
        LibraryPattern("io.sentry", "Sentry")
    )

    private val advertisingPatterns = listOf(
        LibraryPattern("com.google.android.gms.ads", "Google AdMob"),
        LibraryPattern("com.google.ads.mediation", "Google Ad Mediation"),
        LibraryPattern("com.facebook.ads", "Facebook Audience Network"),
        LibraryPattern("com.unity3d.ads", "Unity Ads"),
        LibraryPattern("com.applovin", "AppLovin"),
        LibraryPattern("com.mopub", "MoPub"),
        LibraryPattern("com.ironsource", "IronSource"),
        LibraryPattern("com.chartboost.sdk", "Chartboost"),
        LibraryPattern("com.adcolony.sdk", "AdColony"),
        LibraryPattern("com.vungle", "Vungle"),
        LibraryPattern("com.inmobi", "InMobi"),
        LibraryPattern("com.startapp.android", "StartApp"),
        LibraryPattern("com.tapjoy", "Tapjoy"),
        LibraryPattern("com.bytedance.sdk.openadsdk", "Pangle (TikTok Ads)"),
        LibraryPattern("com.fyber", "Fyber"),
        LibraryPattern("com.google.ads.mediation.admob", "AdMob Mediation Adapter")
    )

    private val socialPatterns = listOf(
        LibraryPattern("com.facebook.login", "Facebook Login"),
        LibraryPattern("com.facebook.share", "Facebook Share"),
        LibraryPattern("com.google.android.gms.auth", "Google Sign-In"),
        LibraryPattern("com.twitter.sdk", "Twitter SDK"),
        LibraryPattern("twitter4j", "Twitter4J"),
        LibraryPattern("com.linkedin.platform", "LinkedIn SDK"),
        LibraryPattern("com.instagram.common", "Instagram SDK"),
        LibraryPattern("com.vk.sdk", "VK SDK"),
        LibraryPattern("com.tencent.mm.opensdk", "WeChat SDK"),
        LibraryPattern("com.linecorp", "Line SDK"),
        LibraryPattern("org.telegram", "Telegram SDK"),
        LibraryPattern("com.pinterest", "Pinterest SDK"),
        LibraryPattern("com.snapchat.kit.sdk", "Snapchat SDK"),
        LibraryPattern("com.bytedance.ies.ugc.aweme", "TikTok SDK"),
        LibraryPattern("com.discord", "Discord SDK")
    )

    private val paymentPatterns = listOf(
        LibraryPattern("com.stripe.android", "Stripe"),
        LibraryPattern("com.paypal.android", "PayPal"),
        LibraryPattern("com.google.android.gms.wallet", "Google Pay"),
        LibraryPattern("com.braintreepayments", "Braintree"),
        LibraryPattern("com.squareup.sdk", "Square"),
        LibraryPattern("com.razorpay", "Razorpay"),
        LibraryPattern("com.paytm", "Paytm"),
        LibraryPattern("com.payu", "PayU"),
        LibraryPattern("com.adyen", "Adyen"),
        LibraryPattern("com.klarna", "Klarna"),
        LibraryPattern("com.checkout", "Checkout.com")
    )

    private val uiPatterns = listOf(
        LibraryPattern("androidx.compose.ui", "Jetpack Compose"),
        LibraryPattern("androidx.compose.runtime", "Jetpack Compose Runtime"),
        LibraryPattern("androidx.compose.foundation", "Jetpack Compose Foundation"),
        LibraryPattern("androidx.compose.material", "Jetpack Compose Material"),
        LibraryPattern("androidx.compose.material3", "Jetpack Compose Material 3"),
        LibraryPattern("androidx.databinding", "Data Binding"),
        LibraryPattern("androidx.viewbinding", "View Binding"),
        LibraryPattern("androidx.recyclerview", "RecyclerView"),
        LibraryPattern("androidx.constraintlayout", "ConstraintLayout"),
        LibraryPattern("androidx.coordinatorlayout", "CoordinatorLayout"),
        LibraryPattern("androidx.viewpager2", "ViewPager2"),
        LibraryPattern("androidx.swiperefreshlayout", "SwipeRefreshLayout"),
        LibraryPattern("com.google.android.material", "Material Components"),
        LibraryPattern("com.airbnb.lottie", "Lottie"),
        LibraryPattern("androidx.navigation", "Jetpack Navigation")
    )

    private val reactivePatterns = listOf(
        LibraryPattern("io.reactivex.rxjava3", "RxJava 3"),
        LibraryPattern("io.reactivex.rxjava2", "RxJava 2"),
        LibraryPattern("io.reactivex", "RxJava"),
        LibraryPattern("io.reactivex.rxkotlin", "RxKotlin"),
        LibraryPattern("io.reactivex.android", "RxAndroid"),
        LibraryPattern("kotlinx.coroutines", "Kotlin Coroutines"),
        LibraryPattern("kotlinx.coroutines.flow", "Kotlin Flow"),
        LibraryPattern("androidx.lifecycle.livedata", "LiveData"),
        LibraryPattern("org.reactivestreams", "Reactive Streams")
    )

    private val allPatterns: Map<LibraryCategory, List<LibraryPattern>> = mapOf(
        LibraryCategory.ANALYTICS to analyticsPatterns,
        LibraryCategory.ADVERTISING to advertisingPatterns,
        LibraryCategory.SOCIAL to socialPatterns,
        LibraryCategory.PAYMENT to paymentPatterns,
        LibraryCategory.UI to uiPatterns,
        LibraryCategory.REACTIVE to reactivePatterns,
        LibraryCategory.OTHER to emptyList()
    )

    fun getAllPatterns(): Map<LibraryCategory, List<LibraryPattern>> = allPatterns

    fun getPatternsForCategory(category: LibraryCategory): List<LibraryPattern> {
        return allPatterns[category] ?: emptyList()
    }
}