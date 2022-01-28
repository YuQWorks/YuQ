package com.icecreamqaq.yuq.message

data class MessagePackage(
    /***
     * 发送方式
     * 0 -> 通过转发多条消息方式发送。
     * 10 -> 将所有内容做为一条消息通过分片消息发送。
     * 20 -> 将所有内容组合为一条消息，通过长消息方式发送。
     * 注:
     *  当 Runtime 不支持分片消息时会转为普通消息发送。
     *  当 Runtime 不支持转发多条消息时，将发送失败。
     *  当发送方式为转发多条时，如果 Message 未提供 source 或是内容物为 MessageItemChain 则将为本条消息发送者设置为机器人。
     */
    var type:Int = 0,
    val body:MutableList<IMessageItemChain> = arrayListOf()
)