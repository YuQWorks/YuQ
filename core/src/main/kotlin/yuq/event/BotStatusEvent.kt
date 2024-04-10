package yuq.event

import yuq.Bot


open class BotStatusEvent(override val bot: Bot) : BotEvent {
    // Bot 上线事件
    open class Online(bot: Bot) : BotStatusEvent(bot)

    // Bot 离线事件
    open class Offline(bot: Bot) : BotStatusEvent(bot)

    /*** Bot 重新上线事件
     * Bot 可能因为网络波动等出现掉线问题。
     * 重新上线可能不会触发离线，和上线事件。
     */
    open class ReOnline(bot: Bot) : BotStatusEvent(bot)

    /*** Bot 被添加事件
     * 部分 Platform/Runtime 的 Bot 可能不会触发上线，离线等事件。
     */
    open class Add(bot: Bot) : BotStatusEvent(bot)

    /*** Bot 被移除事件
     * 部分 Platform/Runtime 的 Bot 可能不会触发上线，离线等事件。
     */
    open class Remove(bot: Bot) : BotStatusEvent(bot)
}