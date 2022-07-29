package com.mparticle.kits.mocks

import android.content.*
import android.content.IntentSender.SendIntentException
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.telephony.TelephonyManager
import android.view.Display
import junit.framework.Assert
import org.mockito.Mockito
import java.io.*

class MockContext : Context() {
    private var sharedPreferences: SharedPreferences = MockSharedPreferences()
    private var resources: Resources = MockResources()
    var application: MockApplication? = null
    fun setSharedPreferences(prefs: SharedPreferences) {
        sharedPreferences = prefs
    }

    override fun getApplicationContext(): Context {
        if (application == null) {
            application = MockApplication(this)
        }
        return application as MockApplication
    }

    override fun checkCallingOrSelfPermission(permission: String): Int {
        return PackageManager.PERMISSION_GRANTED
    }

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return sharedPreferences
    }

    override fun getResources(): Resources? {
        return resources
    }

    override fun getSystemService(name: String): Any? {
        return if (name == TELEPHONY_SERVICE) {
            Mockito.mock(TelephonyManager::class.java)
        } else null
    }

    override fun getPackageManager(): PackageManager {
        val manager = Mockito.mock(PackageManager::class.java)
        val info = Mockito.mock(PackageInfo::class.java)
        info.versionName = "42"
        info.versionCode = 42
        val appInfo = Mockito.mock(ApplicationInfo::class.java)
        try {
            Mockito.`when`(manager.getPackageInfo(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(info)
            Mockito.`when`(manager.getInstallerPackageName(Mockito.anyString()))
                .thenReturn("com.mparticle.test.installer")
            Mockito.`when`(manager.getApplicationInfo(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(appInfo)
            Mockito.`when`(manager.getApplicationLabel(appInfo)).thenReturn("test label")
        } catch (e: Exception) {
            Assert.fail(e.toString())
        }
        return manager
    }

    override fun getPackageName(): String {
        return "com.mparticle.test"
    }

    override fun getApplicationInfo(): ApplicationInfo {
        return ApplicationInfo()
    }

    /**
     * Stubbed methods
     */
    override fun setTheme(resid: Int) {}
    override fun getTheme(): Theme? {
        return null
    }

    override fun getClassLoader(): ClassLoader? {
        return null
    }

    override fun sendBroadcast(intent: Intent) {}
    override fun sendBroadcast(intent: Intent, receiverPermission: String?) {}
    override fun sendOrderedBroadcast(intent: Intent, receiverPermission: String?) {}
    override fun sendOrderedBroadcast(
        intent: Intent,
        receiverPermission: String?,
        resultReceiver: BroadcastReceiver?,
        scheduler: Handler?,
        initialCode: Int,
        initialData: String?,
        initialExtras: Bundle?
    ) {
    }

    override fun sendBroadcastAsUser(intent: Intent, user: UserHandle) {}
    override fun sendBroadcastAsUser(
        intent: Intent,
        user: UserHandle,
        receiverPermission: String?
    ) {
    }

    override fun sendOrderedBroadcastAsUser(
        intent: Intent,
        user: UserHandle,
        receiverPermission: String?,
        resultReceiver: BroadcastReceiver,
        scheduler: Handler?,
        initialCode: Int,
        initialData: String?,
        initialExtras: Bundle?
    ) {
    }

    override fun sendStickyBroadcast(intent: Intent) {}
    override fun sendStickyOrderedBroadcast(
        intent: Intent,
        resultReceiver: BroadcastReceiver,
        scheduler: Handler?,
        initialCode: Int,
        initialData: String?,
        initialExtras: Bundle?
    ) {
    }

    override fun removeStickyBroadcast(intent: Intent) {}
    override fun sendStickyBroadcastAsUser(intent: Intent, user: UserHandle) {}
    override fun sendStickyOrderedBroadcastAsUser(
        intent: Intent,
        user: UserHandle,
        resultReceiver: BroadcastReceiver,
        scheduler: Handler?,
        initialCode: Int,
        initialData: String?,
        initialExtras: Bundle?
    ) {
    }

    override fun removeStickyBroadcastAsUser(intent: Intent, user: UserHandle) {}
    override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter): Intent? {
        return null
    }

    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter,
        flags: Int
    ): Intent? {
        return null
    }

    override fun registerReceiver(
        receiver: BroadcastReceiver,
        filter: IntentFilter,
        broadcastPermission: String?,
        scheduler: Handler?
    ): Intent? {
        return null
    }

    override fun registerReceiver(
        receiver: BroadcastReceiver,
        filter: IntentFilter,
        broadcastPermission: String?,
        scheduler: Handler?,
        flags: Int
    ): Intent? {
        return null
    }

    override fun unregisterReceiver(receiver: BroadcastReceiver) {}
    override fun startService(service: Intent): ComponentName? {
        return null
    }

    override fun startForegroundService(service: Intent): ComponentName? {
        return null
    }

    override fun stopService(service: Intent): Boolean {
        return false
    }

    override fun bindService(service: Intent, conn: ServiceConnection, flags: Int): Boolean {
        return false
    }

    override fun unbindService(conn: ServiceConnection) {}
    override fun startInstrumentation(
        className: ComponentName,
        profileFile: String?,
        arguments: Bundle?
    ): Boolean {
        return false
    }

    override fun checkSelfPermission(permission: String): Int {
        return 0
    }

    override fun enforcePermission(permission: String, pid: Int, uid: Int, message: String?) {}
    override fun enforceCallingPermission(permission: String, message: String?) {}
    override fun enforceCallingOrSelfPermission(permission: String, message: String?) {}
    override fun grantUriPermission(toPackage: String, uri: Uri, modeFlags: Int) {}
    override fun revokeUriPermission(uri: Uri, modeFlags: Int) {}
    override fun revokeUriPermission(toPackage: String, uri: Uri, modeFlags: Int) {}
    override fun checkUriPermission(uri: Uri, pid: Int, uid: Int, modeFlags: Int): Int {
        return 0
    }

    override fun checkCallingUriPermission(uri: Uri, modeFlags: Int): Int {
        return 0
    }

    override fun checkCallingOrSelfUriPermission(uri: Uri, modeFlags: Int): Int {
        return 0
    }

    override fun checkUriPermission(
        uri: Uri?,
        readPermission: String?,
        writePermission: String?,
        pid: Int,
        uid: Int,
        modeFlags: Int
    ): Int {
        return 0
    }

    override fun enforceUriPermission(
        uri: Uri,
        pid: Int,
        uid: Int,
        modeFlags: Int,
        message: String
    ) {
    }

    override fun enforceCallingUriPermission(uri: Uri, modeFlags: Int, message: String) {}
    override fun enforceCallingOrSelfUriPermission(uri: Uri, modeFlags: Int, message: String) {}
    override fun enforceUriPermission(
        uri: Uri?,
        readPermission: String?,
        writePermission: String?,
        pid: Int,
        uid: Int,
        modeFlags: Int,
        message: String?
    ) {
    }

    @Throws(NameNotFoundException::class)
    override fun createPackageContext(packageName: String, flags: Int): Context? {
        return null
    }

    @Throws(NameNotFoundException::class)
    override fun createContextForSplit(splitName: String): Context? {
        return null
    }

    override fun createConfigurationContext(overrideConfiguration: Configuration): Context? {
        return null
    }

    override fun createDisplayContext(display: Display): Context? {
        return null
    }

    override fun createDeviceProtectedStorageContext(): Context? {
        return null
    }

    override fun isDeviceProtectedStorage(): Boolean {
        return false
    }

    override fun moveSharedPreferencesFrom(sourceContext: Context, name: String): Boolean {
        return false
    }

    override fun deleteSharedPreferences(name: String): Boolean {
        return false
    }

    @Throws(FileNotFoundException::class)
    override fun openFileInput(name: String): FileInputStream? {
        return null
    }

    @Throws(FileNotFoundException::class)
    override fun openFileOutput(name: String, mode: Int): FileOutputStream? {
        return null
    }

    override fun deleteFile(name: String): Boolean {
        return false
    }

    override fun getFileStreamPath(name: String): File? {
        return null
    }

    override fun getDataDir(): File? {
        return null
    }

    override fun getFilesDir(): File? {
        return null
    }

    override fun getNoBackupFilesDir(): File? {
        return null
    }

    override fun getExternalFilesDir(type: String?): File? {
        return null
    }

    override fun getExternalFilesDirs(type: String): Array<File?> {
        return arrayOfNulls(0)
    }

    override fun getObbDir(): File? {
        return null
    }

    override fun getObbDirs(): Array<File?> {
        return arrayOfNulls(0)
    }

    override fun getCacheDir(): File? {
        return null
    }

    override fun getCodeCacheDir(): File? {
        return null
    }

    override fun getExternalCacheDir(): File? {
        return null
    }

    override fun getExternalCacheDirs(): Array<File?> {
        return arrayOfNulls(0)
    }

    override fun getExternalMediaDirs(): Array<File?> {
        return arrayOfNulls(0)
    }

    override fun fileList(): Array<String?> {
        return arrayOfNulls(0)
    }

    override fun getDir(name: String, mode: Int): File? {
        return null
    }

    override fun openOrCreateDatabase(
        name: String,
        mode: Int,
        factory: CursorFactory
    ): SQLiteDatabase? {
        return null
    }

    override fun openOrCreateDatabase(
        name: String,
        mode: Int,
        factory: CursorFactory,
        errorHandler: DatabaseErrorHandler?
    ): SQLiteDatabase? {
        return null
    }

    override fun moveDatabaseFrom(sourceContext: Context, name: String): Boolean {
        return false
    }

    override fun deleteDatabase(name: String): Boolean {
        return false
    }

    override fun getDatabasePath(name: String): File? {
        return null
    }

    override fun databaseList(): Array<String?> {
        return arrayOfNulls(0)
    }

    override fun getWallpaper(): Drawable? {
        return null
    }

    override fun peekWallpaper(): Drawable? {
        return null
    }

    override fun getWallpaperDesiredMinimumWidth(): Int {
        return 0
    }

    override fun getWallpaperDesiredMinimumHeight(): Int {
        return 0
    }

    @Throws(IOException::class)
    override fun setWallpaper(bitmap: Bitmap) {
    }

    @Throws(IOException::class)
    override fun setWallpaper(data: InputStream) {
    }

    @Throws(IOException::class)
    override fun clearWallpaper() {
    }

    override fun startActivity(intent: Intent) {}
    override fun startActivity(intent: Intent, options: Bundle?) {}
    override fun startActivities(intents: Array<Intent>) {}
    override fun startActivities(intents: Array<Intent>, options: Bundle) {}
    @Throws(SendIntentException::class)
    override fun startIntentSender(
        intent: IntentSender,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int
    ) {
    }

    @Throws(SendIntentException::class)
    override fun startIntentSender(
        intent: IntentSender,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int,
        options: Bundle?
    ) {
    }

    override fun getSystemServiceName(serviceClass: Class<*>): String? {
        return null
    }

    override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
        return 0
    }

    override fun checkCallingPermission(permission: String): Int {
        return 0
    }

    override fun getContentResolver(): ContentResolver? {
        return null
    }

    override fun getMainLooper(): Looper? {
        return null
    }

    override fun getPackageResourcePath(): String? {
        return null
    }

    override fun getPackageCodePath(): String? {
        return null
    }

    override fun getAssets(): AssetManager? {
        return null
    }
}
