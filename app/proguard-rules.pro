    # Keep Kotlin coroutines internals
    -keepclassmembers class kotlinx.coroutines.internal.* {
        <fields>;
        <methods>;
    }
    -keepclassmembers class kotlin.coroutines.jvm.internal.* {
        <fields>;
        <methods>;
    }
    -keepclassmembers class kotlin.coroutines.intrinsics.* {
        <fields>;
        <methods>;
    }

    # Keep suspend functions and their continuation implementations
    -keepclassmembers class **$*ContinuationImpl {
        <fields>;
        <methods>;
    }
    -keepclassmembers class **$*Continuation {
        <fields>;
        <methods>;
    }

    # Keep specific classes that might contain suspend lambdas if the above doesn't work
    # This is less ideal, but can be a targeted fix.
    # Replace com.fastpack.ui.login.LoginScreenKt with the actual class.
    # -keep class com.fastpack.ui.login.LoginScreenKt$LoginScreen$1$1 { *; }
    # -keep class com.fastpack.ui.login.** { *; } # More broad, be careful

    # If you are using kotlinx.serialization with coroutines
    -keepattributes Signature
    -keepclassmembers class kotlinx.serialization.internal.* {
        <methods>;
    }