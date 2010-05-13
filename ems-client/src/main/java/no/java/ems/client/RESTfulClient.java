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

package no.java.ems.client;

import org.apache.commons.io.IOUtils;
import org.codehaus.httpcache4j.cache.HTTPCache;
import org.codehaus.httpcache4j.payload.Payload;
import org.codehaus.httpcache4j.*;
import org.apache.commons.lang.Validate;
import fj.data.Option;
import static fj.data.Option.none;
import static fj.data.Option.some;
import fj.Unit;
import org.codehaus.httpcache4j.preference.Preferences;

import static fj.Unit.unit;

import java.io.InputStream;
import java.util.*;
import java.net.URI;


/**
 * @author <a href="mailto:erlend@hamnaberg.net">Erlend Hamnaberg</a>
 * @version $Revision: #5 $ $Date: 2008/09/15 $
 */
public abstract class RESTfulClient {
    private final HTTPCache cache;
    private final List<Handler> handlers = new ArrayList<Handler>();
    private final Challenge challenge;

    protected RESTfulClient(HTTPCache cache, String username, String password) {
        Validate.notNull(cache, "Cache may not be null");
        if (username != null) {
            challenge = new UsernamePasswordChallenge(username, password);
        }
        else {
            challenge = null;
        }
        this.cache = cache;
    }

    protected void registerHandler(Handler handler) {
        handlers.add(handler);
    }

    protected List<Handler> getHandlers() {
        return handlers;
    }

    protected HTTPCache getCache() {
        return cache;
    }

    public Unit update(ResourceHandle handle, Payload payload) {
        Validate.notNull(handle, "Handle may not be null");
        Validate.notNull(payload, "Payload may not be null");
        HTTPRequest request = new HTTPRequest(handle.getURI(), HTTPMethod.PUT).
                challenge(challenge).
                conditionals(new Conditionals().addIfMatch(!handle.isTagged() ? handle.toUnconditional().getTag().some() : handle.getTag().some())).
                payload(payload);               
        HTTPResponse response = cache.doCachedRequest(request);
        response.consume();
        if (response.getStatus() != Status.OK) {
            throw new HttpException(handle.getURI(), response.getStatus());
        }
        return unit();
    }

    public void remove(ResourceHandle handle) {
        Validate.notNull(handle, "Handle may not be null");
        List<Status> acceptedStatuses = Arrays.asList(Status.OK, Status.NO_CONTENT);
        HTTPRequest request = new HTTPRequest(handle.getURI(), HTTPMethod.DELETE);
        HTTPResponse response = cache.doCachedRequest(request);
        response.consume();
        if (!acceptedStatuses.contains(response.getStatus())) {
            throw new HttpException(handle.getURI(), response.getStatus());
        }
    }

    public Option<Resource> process(ResourceHandle handle, Payload payload) {
        Validate.notNull(handle, "Handle may not be null");
        Validate.notNull(payload, "Payload may not be null");
        HTTPRequest request = new HTTPRequest(handle.getURI(), HTTPMethod.POST).payload(payload);        
        HTTPResponse response = cache.doCachedRequest(request);
        if (response.getStatus().isClientError() || response.getStatus().isServerError()) {
            response.consume();
            throw new HttpException(handle.getURI(), response.getStatus());
        }
        if (response.hasPayload()) {
            return handle(handle, response);
        }
        return Option.none();
    }

    public Option<Resource> createAndRead(ResourceHandle handle, Payload payload, List<MIMEType> types) {
        Validate.notNull(handle, "Handle may not be null");
        Validate.notNull(payload, "Payload may not be null");
        HTTPRequest request = new HTTPRequest(handle.getURI(), HTTPMethod.POST).payload(payload);
        HTTPResponse response = cache.doCachedRequest(request);
        if (response.getStatus() != Status.CREATED) {
            response.consume();
            throw new HttpException(handle.getURI(), response.getStatus());
        }
        if (!response.hasPayload()) {
            String location = response.getHeaders().getFirstHeaderValue("Location");
            return read(new ResourceHandle(URI.create(location)), types);
        }
        else {
            return handle(handle, response);
        }
    }

    public ResourceHandle create(ResourceHandle handle, Payload payload) {
        Validate.notNull(handle, "Handle may not be null");
        Validate.notNull(payload, "Payload may not be null");
        HTTPRequest request = new HTTPRequest(handle.getURI(), HTTPMethod.POST).payload(payload);
        request = request.challenge(challenge);
        HTTPResponse response = cache.doCachedRequest(request);
        if (response.getStatus() != Status.CREATED) {
            throw new HttpException(handle.getURI(), response.getStatus());
        }
        response.consume();
        return new ResourceHandle(URI.create(response.getHeaders().getFirstHeaderValue("Location")), Option.<Tag>none());
    }

    public Option<Resource> read(ResourceHandle handle, List<MIMEType> types) {
        Validate.notNull(handle, "Handle may not be null");
        HTTPRequest request = new HTTPRequest(handle.getURI(), HTTPMethod.GET);
        if (handle.isTagged()) {
            request = request.conditionals(request.getConditionals().addIfNoneMatch(handle.getTag().some()));
        }
        else if (handle.isUnconditional()) {
            request = request.conditionals(request.getConditionals().addIfMatch(handle.getTag().some()));
        }
        if (types != null) {
            Preferences preferences = new Preferences();
            for (MIMEType type : types) {
                preferences = preferences.addMIMEType(type);
            }
            request = request.preferences(preferences);
        }
        HTTPResponse response = cache.doCachedRequest(request);
        ResourceHandle updatedHandle = new ResourceHandle(handle.getURI(), Option.fromNull(response.getETag()));
        if (updatedHandle.equals(handle) && response.getStatus() == Status.NOT_MODIFIED) {
            response.consume();
            return Option.none();
        }
        if (response.getStatus() == Status.OK) {
            return handle(updatedHandle, response);
        }
        throw new HttpException(handle.getURI(), response.getStatus());
    }

    protected Option<Resource> handle(ResourceHandle handle, HTTPResponse response) {
        if (response.hasPayload()) {
            for (Handler handler : getHandlers()) {
                if (handler.supports(response.getPayload().getMimeType())) {
                    InputStream payload = response.getPayload().getInputStream();
                    try {
                        return Option.<Resource>some(new DefaultResource(handle, response.getHeaders(), handler.handle(payload)));
                    } catch (RuntimeException e) {
                        IOUtils.closeQuietly(payload);
                        throw e;
                    } finally {
                        if (!handler.needStreamAfterHandle()) {
                            IOUtils.closeQuietly(payload);
                        }
                    }
                }
            }
        }
        return none();
    }

  public Option<Headers> inspect(final ResourceHandle pHandle) {
    HTTPResponse response = cache.doCachedRequest(new HTTPRequest(pHandle.getURI(), HTTPMethod.HEAD));
    if (response.getStatus() == Status.OK) {
      return some(response.getHeaders());
    }
    return none();
  }
}
