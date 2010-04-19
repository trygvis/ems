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

import fj.*;
import static fj.data.List.iterableList;
import no.java.ems.domain.search.ObjectType;
import no.java.ems.domain.search.SearchResult;
import no.java.ems.external.v2.*;
import no.java.ems.domain.*;
import no.java.ems.client.f.ExternalV2F;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.net.URI;
import java.io.InputStream;

import org.codehaus.httpcache4j.HTTPMethod;
import org.codehaus.httpcache4j.cache.HTTPCache;
import org.codehaus.httpcache4j.cache.MemoryCacheStorage;
import org.codehaus.httpcache4j.client.HTTPClientResponseResolver;
import org.codehaus.httpcache4j.HTTPRequest;
import org.codehaus.httpcache4j.HTTPResponse;
import org.codehaus.httpcache4j.Status;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import fj.data.Option;

/**
 * @author <a href="mailto:erlend@hamnaberg.net">Erlend Hamnaberg</a>
 * @version $Id $
 */
public class RESTEmsService {
    private EmsV2Client client;
    private final HTTPCache cache;

    public RESTEmsService(String baseURI, String username, String password) {
        cache = new HTTPCache(
                new MemoryCacheStorage(),
                new HTTPClientResponseResolver(new HttpClient(new MultiThreadedHttpConnectionManager())));
        client = createEmsClient(username, password);
        client.login(URI.create(baseURI));
    }

    private RESTfulEmsV2Client createEmsClient(String username, String password) {
        return new RESTfulEmsV2Client(cache, username, password);
    }

    public RESTEmsService(String baseURI) {
        this(baseURI, null, null);
    }

    public List<Person> getContacts() {
        PersonListV2 people = client.getPeople();
        Collection<Person> persons = iterableList(people.getPerson()).map(ExternalV2F.person).toCollection();
        return new ArrayList<Person>(persons);
    }

    public Person getContact(ResourceHandle id) {
        Option<Person> person = client.getPerson(id).map(ExternalV2F.person);
        if (person.isSome()) {
            return person.some();
        }
        return null;
    }

    public Person saveContact(Person person) {
        Option<PersonV2> option = Option.some(person).map(ExternalV2F.personV2);
        if (person.getHandle() == null) {
            ResourceHandle handle = client.addPerson(option.some());
            person.setHandle(handle);
        }
        else {
            client.updatePerson(option.some());
        }
        return person;
    }

    public List<Event> getEvents() {
        EventListV2 listV2 = client.getEvents();
        Collection<Event> events = iterableList(listV2.getEvent()).
            map(ExternalV2F.event).
            toCollection();
        return new ArrayList<Event>(events);
    }

    public Event getEvent(ResourceHandle id) {
        return client.getEvent(id).
            map(ExternalV2F.eventFromRequest).
            orSome((Event) null);
    }

    public Event saveEvent(final Event event) {
        EventV2 eventToSave = ExternalV2F.eventV2.f(event);
        ResourceHandle handle = event.getHandle();
        if (handle == null) {
            handle = client.addEvent(eventToSave);
        }
        else {
            client.updateEvent(event.getHandle(), eventToSave);
        }

        Option<Event> updatedEvent = client.getEvent(handle).map(ExternalV2F.eventFromRequest);

        updatedEvent.foreach(new Effect<Event>() {
            @Override
            public void e(Event e) {
                event.sync(e);
            }
        });

        return updatedEvent.orSome((Event) null);
    }

    public List<Session> getSessions(Event event) {
        SessionListV2 sessions = client.getSessions(new ResourceHandle(event.getSessionURI()));
        Collection<Session> list = iterableList(sessions.getSession()).map(ExternalV2F.session).toCollection();
        return new ArrayList<Session>(list);
    }

    public Session getSession(ResourceHandle uri) {
        Option<Session> option = client.getSession(uri).map(ExternalV2F.session);
        if (option.isSome()) {
            return option.some();
        }
        return null;
    }

    public Session saveSession(Session session) {
        Option<SessionV2> option = Option.some(session).map(ExternalV2F.sessionV2);
        if (option.isSome()) {
            if (session.getHandle() == null) {
                ResourceHandle handle = client.addSession(session.getEventHandle(), option.some());
                session.setHandle(handle);
                return session;
            }
            else {
                client.updateSession(session.getHandle(), option.some());
                option = client.getSession(session.getHandle().toUnconditional());
                Option<Session> sessionOption = option.map(ExternalV2F.session);
                if (sessionOption.isSome()) {
                    session.sync(sessionOption.some());
                }
                return session;
            }
        }
        throw new IllegalArgumentException("Unable to save session");
    }

    public void delete(ResourceHandle handle) {
        HTTPResponse response = cache.doCachedRequest(new HTTPRequest(handle.getURI(), HTTPMethod.DELETE));
        if (response.getStatus() != Status.NO_CONTENT) {
            throw new HttpException(handle.getURI(), response.getStatus());
        }
    }

    public InputStream readBinary(Binary binary) {
        if (binary instanceof URIBinary) {
            URIBinary URIBinary = (URIBinary) binary;
            HTTPRequest request = new HTTPRequest(URIBinary.getURI());
            HTTPResponse response = cache.doCachedRequest(request);
            if (response.getStatus() == Status.OK && response.hasPayload()) {
                return response.getPayload().getInputStream();
            }
        }
        else if (binary instanceof ByteArrayBinary) {
            ByteArrayBinary array = (ByteArrayBinary) binary;
            return array.getDataStream();
        }
        throw new IllegalArgumentException("Uknown binary...");
    }

    public Binary saveBinary(Binary binary) {
        // Always create the binary, no update at this point
        throw new UnsupportedOperationException("Not implemented yet...");
    }

    public void deleteBinary(URI binaryURI) {
        throw new UnsupportedOperationException("Not implemented yet...");
    }

    // -----------------------------------------------------------------------
    //
    // -----------------------------------------------------------------------


    public List<Room> getRooms() {
        throw new UnsupportedOperationException("Not implemented yet...");
    }

    public List<SearchResult> search(String query, ObjectType type) {
        List<SearchResult> list = new ArrayList<SearchResult>();
        for (int i = 0; i < 10; i++) {
            list.add(new SearchResult(URI.create("" + i), "Title" + i, type, "Summary" + i));
        }
        return list;
    }
}
