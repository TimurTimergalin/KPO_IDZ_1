package data.access

import java.security.MessageDigest

fun hash(s: String): String {
    val bytes = MessageDigest
        .getInstance("SHA-256")
        .digest(s.toByteArray())
    return bytes.toString(Charsets.UTF_8)
}