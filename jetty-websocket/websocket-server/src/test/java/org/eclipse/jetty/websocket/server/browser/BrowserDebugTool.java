//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.server.browser;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
 * Tool to help debug websocket circumstances reported around browsers.
 * <p>
 * Provides a server, with a few simple websocket's that can be twiddled from a browser. This helps with setting up breakpoints and whatnot to help debug our
 * websocket implementation from the context of a browser client.
 */
public class BrowserDebugTool implements WebSocketCreator
{
    private static final Logger LOG = Log.getLogger(BrowserDebugTool.class);

    public static void main(String[] args)
    {
        int port = 8080;

        for (int i = 0; i < args.length; i++)
        {
            String a = args[i];
            if ("-p".equals(a) || "--port".equals(a))
            {
                port = Integer.parseInt(args[++i]);
            }
        }

        try
        {
            BrowserDebugTool tool = new BrowserDebugTool();
            tool.prepare(port);
            tool.start();
        }
        catch (Throwable t)
        {
            LOG.warn(t);
        }
    }

    private Server server;
    private ServerConnector connector;

    @Override
    public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp)
    {
        LOG.debug("Creating BrowserSocket");

        if (req.getSubProtocols() != null)
        {
            if (!req.getSubProtocols().isEmpty())
            {
                String subProtocol = req.getSubProtocols().get(0);
                resp.setAcceptedSubProtocol(subProtocol);
            }
        }

        String ua = req.getHeader("User-Agent");
        String rexts = req.getHeader("Sec-WebSocket-Extensions");
        
        LOG.debug("User-Agent: {}", ua);
        LOG.debug("Sec-WebSocket-Extensions (Request) : {}", rexts);
        return new BrowserSocket(ua,rexts);
    }

    public void start() throws Exception
    {
        server.start();
        LOG.info("Server available on port {}", getPort());
    }

    public void stop() throws Exception
    {
        server.stop();
    }

    public void prepare(int port)
    {
        server = new Server();
        connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        WebSocketHandler wsHandler = new WebSocketHandler()
        {
            @Override
            public void configure(WebSocketServletFactory factory)
            {
                LOG.debug("Configuring WebSocketServerFactory ...");

                // factory.getExtensionFactory().unregister("deflate-frame");
                // factory.getExtensionFactory().unregister("permessage-deflate");
                // factory.getExtensionFactory().unregister("x-webkit-deflate-frame");

                // Setup the desired Socket to use for all incoming upgrade requests
                factory.setCreator(BrowserDebugTool.this);

                // Set the timeout
                factory.getPolicy().setIdleTimeout(30000);
            }
        };

        server.setHandler(wsHandler);

        String resourceBase = "src/test/resources/browser-debug-tool";

        ResourceHandler rHandler = new ResourceHandler();
        rHandler.setDirectoriesListed(true);
        rHandler.setResourceBase(resourceBase);
        wsHandler.setHandler(rHandler);

        LOG.info("{} setup on port {}",this.getClass().getName(),port);
    }

    public int getPort()
    {
        return connector.getLocalPort();
    }
}
