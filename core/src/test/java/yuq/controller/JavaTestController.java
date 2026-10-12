package yuq.controller;

import javax.inject.Named;
import yuq.annotation.BotAction;
import yuq.annotation.BotController;

@BotController
public class JavaTestController {
    @BotAction("java {number}")
    public String java(@Named("number") int number, BotActionContext context) {
        return number + ":" + context.getSender().getId();
    }
}
