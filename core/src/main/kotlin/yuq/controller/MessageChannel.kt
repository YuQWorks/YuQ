package yuq.controller

enum class MessageChannel(val channel: String) {
    // 群聊消息
    Group("Group"),
    // 好友消息
    Friend("Friend"),
    // 群临时会话
    GroupTemporary("GroupTemporary"),
    // 频道消息
    Guild("Guild"),
}