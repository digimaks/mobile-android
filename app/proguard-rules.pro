# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# WebView
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}

# Preserve JavaScript interface
-dontnote android.webkit.JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve JavascriptInterface annotations and members in all web-bridge classes
-keepattributes JavascriptInterface
-keep class lv.zzdats.webbridge.** { *; }
-keepclassmembers class lv.zzdats.webbridge.** {
    @android.webkit.JavascriptInterface <methods>;
}

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-keep class lv.zzdats.networklogic.model.** { *; }

-keep enum * { *; }

# Biometric
-keep class lv.zzdats.commonfeature.features.biometric.** { *; }

# Keep navigation config related classes
-keep class lv.zzdats.navigation.** { *; }
-keep class lv.zzdats.uilogic.navigation.** { *; }

# Keep all classes referenced in BiometricUiConfig
-keepclassmembers class * {
    @lv.zzdats.uilogic.serializer.* *;
}

-keep interface lv.zzdats.uilogic.serializer.UiSerializableParser
-keep interface lv.zzdats.uilogic.serializer.UiSerializable
-keepclassmembers class * implements lv.zzdats.uilogic.serializer.adapter.SerializableAdapterType { *; }
-keepclassmembers class * implements lv.zzdats.uilogic.serializer.UiSerializableParser { *; }
-keepclassmembers class * implements lv.zzdats.uilogic.serializer.UiSerializable { *; }

# Keep Gson TypeToken and related classes
-keep class com.google.common.reflect.TypeToken { *; }
-keep class * extends com.google.common.reflect.TypeToken

# Keep generic signature of TypeToken (important for reflection)
-keepattributes Signature

# Keep Gson classes used with reflection
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.SuspendLambda {
    <fields>;
    <methods>;
}

# Keep navigation and web bridge classes
-keep class lv.zzdats.webbridge.** { *; }
-keep class lv.zzdats.webfeature.** { *; }

# Keep PaymentStatusState and related classes
-keep class lv.zzdats.presentationfeature.interactor.PaymentStatusState** { *; }
-keep class lv.zzdats.presentationfeature.interactor.PaymentPresentationInteractor** { *; }

# Keep navigation command classes
-keep class lv.zzdats.webfeature.ui.NavigationCommand** { *; }
-keep class lv.zzdats.webfeature.ui.WebEffect** { *; }

# Prevent obfuscation of suspend function parameters
-keepclassmembers class * {
    kotlin.coroutines.Continuation *;
}

# Keep Flow related classes
-keep class kotlinx.coroutines.flow.** { *; }
-keepclassmembers class * {
    kotlinx.coroutines.flow.FlowCollector *;
}

-keep class androidx.appcompat.app.AppCompatDelegateImpl** { *; }
-keep class androidx.appcompat.app.** { *; }

# Keep payment-related classes from being obfuscated
-keep class lv.zzdats.presentationfeature.bridge.** { *; }

-keep class lv.zzdats.presentationfeature.interactor.PaymentPresentationInteractorImpl** { *; }
-keep class lv.zzdats.presentationfeature.interactor.PaymentPresentationInteractorImpl$** { *; }

# Keep coroutine continuation classes used in payment polling
-keep class lv.zzdats.presentationfeature.interactor.PaymentPresentationInteractorImpl$pollPaymentStatus$** { *; }

# Keep all lambda classes in presentation feature
-keep class lv.zzdats.presentationfeature.interactor.** extends kotlin.coroutines.jvm.internal.SuspendLambda { *; }

# Bouncycastle
-keep class org.bouncycastle.** { *; }

-dontwarn com.eygraber.uri.JvmUriKt

-keep class com.eygraber.uri.** { *; }
-keepclassmembers class com.eygraber.uri.** { *; }

# Core Libs
-keep class com.nimbusds.jwt.**{ *; }
-keep class com.nimbusds.jose.**{ *; }

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Koin DI framework
-keep class org.koin.** { *; }
-keep interface org.koin.** { *; }
-dontwarn org.koin.**

# Keep Koin generated modules
-keep class org.koin.ksp.generated.** { *; }
-keepclassmembers class org.koin.ksp.generated.** { *; }

# Keep Koin annotations
-keep class org.koin.core.annotation.** { *; }
-keepattributes *Annotation*

# Keep Koin scopes and scope management
-keep class org.koin.core.scope.** { *; }
-keepclassmembers class org.koin.core.scope.** { *; }

# Keep presentation feature specific classes
-keep class lv.zzdats.presentationfeature.** { *; }
-keepclassmembers class lv.zzdats.presentationfeature.** { *; }

# Keep generated Koin modules for presentation feature
-keep class **FeaturePresentationModule** { *; }
-keepclassmembers class **FeaturePresentationModule** { *; }

# Keep lambda expressions used in Koin modules
-keepclassmembers class * {
    *** lambda$*(...);
}

# Keep synthetic methods and classes
-keep class **$$ExternalSyntheticLambda* { *; }
-keepclassmembers class **$$ExternalSyntheticLambda* { *; }

# Keep R8 lambda methods
-keep class **$r8$lambda$** { *; }
-keepclassmembers class **$r8$lambda$** { *; }

# Keep all factory methods in DI modules
-keepclassmembers class * {
    *** provide*(...);
}

# Keep Koin module builders and DSL
-keep class org.koin.dsl.** { *; }
-keepclassmembers class org.koin.dsl.** { *; }

# Keep Koin instance creation
-keep class org.koin.core.instance.** { *; }
-keepclassmembers class org.koin.core.instance.** { *; }

# Keep Koin registry
-keep class org.koin.core.registry.** { *; }
-keepclassmembers class org.koin.core.registry.** { *; }

-dontwarn org.koin.androidx.viewmodel.GetViewModelKt
-dontwarn org.koin.compose.stable.StableHoldersKt
-dontwarn org.koin.compose.stable.StableParametersDefinition

# Keep BC DRBG and provider classes (instantiated reflectively by JCE)
-keepattributes InnerClasses,EnclosingMethod

-keep class org.bouncycastle.jcajce.provider.drbg.** { *; }
-keep class org.bouncycastle.jcajce.provider.keystore.** { *; }
-keep class org.bouncycastle.crypto.prng.** { *; }

# Avoid warnings if any
-dontwarn org.bouncycastle.**

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
