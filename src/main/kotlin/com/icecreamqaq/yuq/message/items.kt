package com.icecreamqaq.yuq.message

import com.icecreamqaq.yuq.annotation.PathVar
import com.icecreamqaq.yuq.entity.Contact

interface MessageItem : MessagePlus {
    fun toLocal(contact: Contact): Any
    fun toPath(): String
    fun convertByPathVar(type: PathVar.Type): Any?

    fun toMessage(): Message
}

interface Text : MessageItem {
    val text: String
}

interface At : MessageItem {
    val user: Long
}

interface Face : MessageItem {
    val faceId: Int
}

interface Image : MessageItem {
    val id: String
    val url: String
}

interface XmlEx : MessageItem {
    val serviceId: Int
    val value: String
}

interface JsonEx : MessageItem {
    val value: String
}

interface Voice : MessageItem {
    val url: String
}

interface NoImplItem : MessageItem {
    val source: Any
}
