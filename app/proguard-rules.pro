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
-keep class * {
    public private *;
}

-keep class org.apache.commons.logging.**
-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.**
-keep class kotlin.** { public protected *;}
-keep class kotlin.Metadata { *; }
-keep public class * extends java.lang.Exception
-keep enum ** {public protected *;}
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepattributes InnerClasses,EnclosingMethod,Signature,*Annotation*,SourceFile,LineNumberTable
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
    @org.jetbrains.annotations public *;
}
-dontwarn com.google.android.gms.**
-dontwarn com.google.common.**
-dontwarn kotlin.reflect.jvm.internal.**
-dontwarn org.jetbrains.annotations.**

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*



