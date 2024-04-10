package yuq.message.chain

import yuq.message.MessageItem

data class ChainItem(var item: MessageItem, var next: ChainItem? = null, var previous: ChainItem? = null)