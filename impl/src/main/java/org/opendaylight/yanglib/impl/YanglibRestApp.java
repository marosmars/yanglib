package org.opendaylight.yanglib.impl;

import java.util.Collections;
import java.util.Set;
import javax.ws.rs.core.Application;

public class YanglibRestApp extends Application {

    @Override
    public Set<Object> getSingletons() {
        return Collections.<Object>singleton(YanglibService.getInstance());
    }
}
