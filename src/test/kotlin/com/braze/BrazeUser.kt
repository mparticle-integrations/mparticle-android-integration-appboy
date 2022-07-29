package com.braze

import com.appboy.enums.Month
import java.util.HashMap
import java.util.ArrayList
import java.lang.NullPointerException

class BrazeUser {
    var dobYear = -1
    var dobMonth: Month? = null
    var dobDay = -1
    fun setDateOfBirth(year: Int, month: Month?, day: Int): Boolean {
        dobYear = year
        dobMonth = month
        dobDay = day
        return true
    }

    val customAttributeArray = HashMap <String, MutableList<String>>()
    val customUserAttributes = HashMap<String, Any>()



    fun addToCustomAttributeArray(key: String, value: String): Boolean {
        var customArray = customAttributeArray[key]
        if (customArray == null) {
            customArray = ArrayList()
        }
        customArray.add(value)
        customAttributeArray[key] = customArray
        return true
    }

    fun removeFromCustomAttributeArray(key: String, value: String): Boolean {
        return try {
            customAttributeArray[key]!!.remove(value)
            true
        } catch (npe: NullPointerException) {
            false
        }
    }

    fun setCustomUserAttribute(key: String, value: String): Boolean {
        customUserAttributes[key] = value
        return true
    }

    fun setCustomUserAttribute(key: String, value: Boolean): Boolean {
        customUserAttributes[key] = value
        return true
    }

    fun setCustomUserAttribute(key: String, value: Int): Boolean {
        customUserAttributes[key] = value
        return true
    }

    fun setCustomUserAttribute(key: String, value: Double): Boolean {
        customUserAttributes[key] = value
        return true
    }
}
