package yuq.controller;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Named;
import rain.api.annotation.Nullable;
import yuq.annotation.BotAction;
import yuq.annotation.BotController;
import yuq.contact.GroupMember;

@BotController
public class JavaParameterParsingController {
    @BotAction("java-null {target?} 确认")
    public String nullable(@Named("target") @Nullable GroupMember target) {
        return target == null ? "none" : target.getId();
    }

    @BotAction("java-members {members}+ 确认")
    public String members(@Named("members") List<GroupMember> members) {
        return members.stream().map(GroupMember::getId).collect(Collectors.joining(","));
    }
}
