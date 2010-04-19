/*
 * Copyright 2009 JavaBin
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package no.java.ems.server.web;

import no.java.ems.server.DerbyService;
import no.java.ems.server.domain.EmsServer;
import no.java.ems.server.domain.EmsServerConfiguration;
import org.slf4j.*;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.web.context.support.WebApplicationContextUtils.getRequiredWebApplicationContext;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

/**
 * @author <a href="mailto:trygvis@java.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class EmsListener implements ServletContextListener, ServletRequestListener {
    private Logger requestLog = LoggerFactory.getLogger("log.request");
    private Logger headerLog = LoggerFactory.getLogger("log.header");

    private Logger log = LoggerFactory.getLogger(getClass());

    public void contextInitialized(ServletContextEvent sce) {
        WebApplicationContext context = getRequiredWebApplicationContext(sce.getServletContext());
        log.warn("Booting EMS");
        // These lookups ensure that the core beans are initialized before the rest of the Jersey stuff start up
        context.getBean("derbyService", DerbyService.class);
        context.getBean("emsServer", EmsServer.class);
        context.getBean("emsServerConfiguration", EmsServerConfiguration.class);
        log.warn("EMS web server started!");
    }

    public void contextDestroyed(ServletContextEvent sce) {
        log.warn("EmsListener.contextDestroyed");
    }

    public void requestInitialized(ServletRequestEvent event) {
        HttpServletRequest request = (HttpServletRequest) event.getServletRequest();

        requestLog.info("REQUEST: url={} contentType={}", request.getPathInfo(), request.getContentType());

        Enumeration names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement().toString();
            Enumeration values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                String value = values.nextElement().toString();
                headerLog.info("HEADER: {}: {}", name, value);
            }
        }
    }

    public void requestDestroyed(ServletRequestEvent event) {
    }
}
