package dev.wndavenz.notely.vault

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Penyimpanan PIN brankas catatan rahasia.
 *
 * PIN tidak pernah disimpan apa adanya: yang disimpan adalah garam acak
 * dan hash SHA-256 (diulang) dari garam + PIN. Verifikasi membandingkan
 * hash dengan cara konstan-waktu lewat [MessageDigest.isEqual].
 */
object VaultStore {

    private const val PREFS_NAME = "vault"
    private const val KEY_SALT = "pin_salt"
    private const val KEY_HASH = "pin_hash"
    private const val ITERATIONS = 10_000
    private const val SALT_BYTES = 16

    fun isPinSet(context: Context): Boolean =
        prefs(context).contains(KEY_HASH)

    fun setPin(context: Context, pin: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        prefs(context).edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(
                KEY_HASH,
                Base64.encodeToString(hashPin(pin, salt), Base64.NO_WRAP),
            )
            .apply()
    }

    /** Hapus PIN tersimpan (reset); catatan rahasia tidak ikut terhapus. */
    fun clearPin(context: Context) {
        prefs(context).edit()
            .remove(KEY_SALT)
            .remove(KEY_HASH)
            .apply()
    }

    fun verify(context: Context, pin: String): Boolean {
        val saltB64 = prefs(context).getString(KEY_SALT, null) ?: return false
        val hashB64 = prefs(context).getString(KEY_HASH, null) ?: return false
        val salt = runCatching { Base64.decode(saltB64, Base64.NO_WRAP) }
            .getOrNull() ?: return false
        val expected = runCatching { Base64.decode(hashB64, Base64.NO_WRAP) }
            .getOrNull() ?: return false
        return MessageDigest.isEqual(expected, hashPin(pin, salt))
    }

    /** Hash murni (tanpa Android API) agar bisa diuji di JVM lokal. */
    fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        var hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
        repeat(ITERATIONS - 1) {
            digest.reset()
            digest.update(salt)
            hash = digest.digest(hash)
        }
        return hash
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
