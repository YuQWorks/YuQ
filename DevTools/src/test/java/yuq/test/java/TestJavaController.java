package yuq.test.java;

import com.IceCreamQAQ.Yu.annotation.Action;
import com.icecreamqaq.yuq.annotation.PrivateController;

@PrivateController
public class TestJavaController {

    @Action("HelloJava")
    public String javaHello(long qq){
        return "Hello Java! QQ: " + qq + ".";
    }

}
