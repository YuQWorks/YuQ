package yuq.controller

import rain.controller.ActionCreator
import rain.controller.ControllerProcessFlowInfo
import rain.controller.ProcessInfo
import yuq.controller.router.BotRouter

class BotControllerProcessFlowInfo(
    controllerClass: Class<*>,
    controllerInstance: Any,
    controllerChannels: List<String>,
    controllerRouter: BotRouter,
    actions: MutableList<ActionCreator<BotActionContext, BotActionInvoker>> = arrayListOf(),
    beforeProcesses: MutableList<ProcessInfo<BotActionContext>> = arrayListOf(),
    afterProcesses: MutableList<ProcessInfo<BotActionContext>> = arrayListOf(),
    catchProcesses: MutableList<ProcessInfo<BotActionContext>> = arrayListOf(),
    val controllerPattern: String = ""
) : ControllerProcessFlowInfo<BotActionContext, BotRouter, BotActionInvoker>(
    controllerClass,
    controllerInstance,
    controllerChannels,
    controllerRouter,
    actions,
    beforeProcesses,
    afterProcesses,
    catchProcesses
)
