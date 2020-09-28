## Overview
This repository is create for songbai
## Getting Started

```
dependencies {
    classpath 'com.google.gms:google-services:4.2.0'  // Google Services 
}

allprojects {
	repositories {
	    maven { url 'https://jitpack.io' }
        maven { url 'https://dl.bintray.com/umsdk/release' }
        maven { url "https://dl.bintray.com/laxygd/easyweb" }
}
 
implementation 'com.lax.ezweb:easyweb-fcm:0.1.8'
```

## In app gradle
```
manifestPlaceholders = [
        CHANNEL           : "google",
        FACEBOOK_APP_ID   : "",
]

 productFlavors {
    google {}
    ...
 }
 
 productFlavors.all { flavor ->
    if (flavor.name != 'dev' && flavor.name != 'alpha') {
        flavor.manifestPlaceholders = [CHANNEL: name]
    }
 }
 
 apply plugin: 'com.google.gms.google-services'
```

## In Strings
```
<string name="android_web_agent">ANDROID_AGENT_NATIVE/2.0&#8194;%1$s</string>
<string name="facebook_app_id">Facebook App ID</string>

can be:  
ANDROID_AGENT_NATIVE/1.0
ANDROID_AGENT_NATIVE/2.0(default)
```

### FCM
```
Project/
    |- app/
    |    |- src/
    |       |- main/
    |         |- res/
    |             |- mipmap-xhdpi
    |                 |- push.png
    |             |- mipmap-xxhdpi
    |                 |- push.png
    |	          |- mipmap-xxxhdpi
    |                 |- push.png
    | ......

```

## In Manifest
```
<meta-data
    android:name="io.branch.sdk.BranchKey"
    android:value="{Branch Key}" />
<meta-data
    android:name="io.branch.sdk.BranchKey.test"
    android:value="" />
<meta-data
    android:name="io.branch.sdk.TestMode"
    android:value="false" />
<meta-data
    android:name="CHANNEL"
    android:value="${CHANNEL}" /> 
<meta-data
    android:name="com.facebook.sdk.ApplicationId"
    android:value="@string/facebook_app_id" />

<provider
    android:name="com.facebook.FacebookContentProvider"
    android:authorities="com.facebook.app.FacebookContentProvider${FACEBOOK_APP_ID}"
    android:exported="false" />

<activity
    android:name="com.facebook.FacebookActivity"
    android:configChanges="keyboard|keyboardHidden|screenLayout|screenSize|orientation"
    android:label="@string/app_name" />

<activity
    android:name="com.facebook.CustomTabActivity"
    android:exported="true">
    <intent-filter><action android:name="android.intent.action.VIEW" />

        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />

        <data android:scheme="fb${FACEBOOK_APP_ID}" />
    </intent-filter>
</activity>

<provider
	android:name="com.facebook.todo.ContentProvider"
	android:authorities="${applicationId}.ContentProvider"
	android:exported="false" />

```
### In launcher activity
### example

```
<activity
android:name=".SplashActivity"
android:theme="@style/AppTheme.Splash">
	<intent-filter>
	    <action android:name="android.intent.action.MAIN" />
	
	    <category android:name="android.intent.category.LAUNCHER" />
	</intent-filter>
	
	
	<!-- Branch URI Scheme -->
	<intent-filter>
	    <data android:scheme="${your scheme}" />
	    <action android:name="android.intent.action.VIEW" />
	
	    <category android:name="android.intent.category.DEFAULT" />
	    <category android:name="android.intent.category.BROWSABLE" />
	</intent-filter>
	
	<!-- Branch App Links (optional) -->
	<intent-filter android:autoVerify="true">
	    <action android:name="android.intent.action.VIEW" />
	
	    <category android:name="android.intent.category.DEFAULT" />
	    <category android:name="android.intent.category.BROWSABLE" />
	
	    <data
	        android:host="${Default Link Domain}"
	        android:scheme="https" />
	    <data
	        android:host="${Alternate Link Domaininfo_outline}"
	        android:scheme="https" />
	</intent-filter>
</activity>
```
## In Launch Activity

```
override fun onStart() {
    super.onStart()
    Branch.sessionBuilder(this).withCallback(BranchListener).withData(intent.data).init()
}

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    this.intent = intent
    Branch.sessionBuilder(this).withCallback(BranchListener).withData(intent.data).reInit()
}

object BranchListener : Branch.BranchReferralInitListener {
    override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
        if (error == null) {
            Log.i("BRANCH SDK", referringParams.toString())
        } else {
            Log.e("BRANCH SDK", error.message)
        }
    }
}
```

## Branch Proguard Settings
```
-keep class com.google.android.gms.** { *; }

-keep class com.facebook.applinks.** { *; }
-keepclassmembers class com.facebook.applinks.** { *; }
-keep class com.facebook.FacebookSdk { *; }

-keep class com.huawei.hms.ads.** { *; }
-keep interface com.huawei.hms.ads.** { *; }
```

## Channels
```
google  
samsung  
Tencent
oppo  
vivo  
xiaomi  
huawei  
others  
```