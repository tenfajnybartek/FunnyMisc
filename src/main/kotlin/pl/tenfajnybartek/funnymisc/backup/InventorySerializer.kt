package pl.tenfajnybartek.funnymisc.backup

import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Utility do serializacji i deserializacji inwentarzy z kompresją GZIP
 */
object InventorySerializer {

    /**
     * Serializuje inwentarz do Stringa z kompresją GZIP i kodowaniem Base64
     *
     * @param items Tablica itemów do serializacji
     * @return Zakodowany string (Base64 + GZIP)
     */
    fun serialize(items: Array<ItemStack?>): String {
        val byteStream = ByteArrayOutputStream()

        // Serializacja Bukkit
        BukkitObjectOutputStream(byteStream).use { dataOutput ->
            dataOutput.writeInt(items.size)
            items.forEach { item ->
                dataOutput.writeObject(item)
            }
        }

        // Kompresja GZIP + Base64
        return compressAndEncode(byteStream.toByteArray())
    }

    /**
     * Deserializuje string do tablicy itemów
     *
     * @param data Zakodowany string (Base64 + GZIP)
     * @return Tablica itemów
     */
    fun deserialize(data: String): Array<ItemStack?> {
        // Dekompresja Base64 + GZIP
        val bytes = decodeAndDecompress(data)

        // Deserializacja Bukkit
        BukkitObjectInputStream(ByteArrayInputStream(bytes)).use { dataInput ->
            val size = dataInput.readInt()
            return Array(size) { dataInput.readObject() as? ItemStack }
        }
    }

    /**
     * Kompresuje dane GZIP i koduje Base64
     *
     * @param data Surowe dane
     * @return Zakodowany string
     */
    private fun compressAndEncode(data: ByteArray): String {
        val compressedStream = ByteArrayOutputStream()

        GZIPOutputStream(compressedStream).use { gzip ->
            gzip.write(data)
            gzip.finish()
        }

        return Base64.getEncoder().encodeToString(compressedStream.toByteArray())
    }

    /**
     * Dekoduje Base64 i dekompresuje GZIP
     *
     * @param data Zakodowany string
     * @return Surowe dane
     */
    private fun decodeAndDecompress(data: String): ByteArray {
        val decodedBytes = Base64.getDecoder().decode(data)
        val outputStream = ByteArrayOutputStream()

        GZIPInputStream(ByteArrayInputStream(decodedBytes)).use { gzip ->
            gzip.copyTo(outputStream)
        }

        return outputStream.toByteArray()
    }

    /**
     * Tworzy hash SHA-256 z inwentarza dla deduplikacji
     *
     * @param items Tablica itemów
     * @return Hash jako string
     */
    fun hashInventory(items: Array<ItemStack?>): String {
        val byteStream = ByteArrayOutputStream()

        BukkitObjectOutputStream(byteStream).use { dataOutput ->
            items.forEach { item ->
                dataOutput.writeObject(item)
            }
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(byteStream.toByteArray())

        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Waliduje czy dane są poprawnie zakodowane
     *
     * @param data Zakodowany string
     * @return true jeśli dane są poprawne
     */
    fun validate(data: String): Boolean {
        return try {
            decodeAndDecompress(data)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Oblicza rozmiar skompresowanych danych w bajtach
     *
     * @param data Zakodowany string
     * @return Rozmiar w bajtach
     */
    fun getCompressedSize(data: String): Int {
        return try {
            Base64.getDecoder().decode(data).size
        } catch (e: Exception) {
            0
        }
    }
}
