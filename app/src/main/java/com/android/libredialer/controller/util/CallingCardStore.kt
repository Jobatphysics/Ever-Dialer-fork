package com.android.libredialer.controller.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.libredialer.modal.data.Contact
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

class CallingCardStore(private val directory: File) {
    fun get(contactId: String): File? =
        if (contactId.isBlank()) null else fileFor(contactId).takeIf { it.isFile && it.length() > 0L }

    @Throws(IOException::class)
    fun save(contactId: String, source: InputStream): File {
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create calling-card storage")
        }
        val destination = fileFor(contactId)
        val temporary = File(directory, "${destination.name}.tmp")
        try {
            source.use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            }
            if (temporary.length() == 0L) {
                throw IOException("Calling-card image is empty")
            }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
        } catch (error: IOException) {
            temporary.delete()
            throw error
        } catch (error: SecurityException) {
            temporary.delete()
            throw error
        }
        return destination
    }

    fun remove(contactId: String): Boolean {
        if (contactId.isBlank()) return true
        val file = fileFor(contactId)
        return !file.exists() || file.delete()
    }

    private fun fileFor(contactId: String): File {
        val key = MessageDigest.getInstance("SHA-256")
            .digest(contactId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$key.image")
    }

    companion object {
        fun from(context: Context) = CallingCardStore(File(context.filesDir, "calling_cards"))

        fun updateFromUri(
            context: Context,
            contactId: String,
            initialUri: String?,
            selectedUri: String?
        ): Boolean {
            if (initialUri == selectedUri) return true
            val store = from(context)
            if (selectedUri == null) return store.remove(contactId)
            val stream = context.contentResolver.openInputStream(Uri.parse(selectedUri))
                ?: throw FileNotFoundException("Unable to open selected calling-card image")
            store.save(contactId, stream)
            return true
        }

        fun reportUpdateFailure(context: Context, error: Throwable) {
            Log.e("CallingCardStore", "Unable to update calling card", error)
        }
    }
}

fun findContactForCaller(callerNumber: String, contacts: List<Contact>): Contact? {
    if (callerNumber.isBlank()) return null
    return contacts.firstOrNull { contact ->
        (contact.phones.map { it.number } + contact.phoneNumbers)
            .any { numbersLikelyMatch(callerNumber, it) }
    }
}
