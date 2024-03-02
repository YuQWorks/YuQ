package yuq.test.java;

import com.icecreamqaq.yuq.annotation.PrivateController;

@PrivateController
public class TestJavaController {

    public String javaHello(long qq){
        return "Hello Java! QQ: " + qq + ".";
    }

}
