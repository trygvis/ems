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

package no.java.ems.server.solr;

import no.java.ems.server.domain.*;
import org.apache.solr.common.SolrInputDocument;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author <a href="mailto:erlend@hamnaberg.net">Erlend Hamnaberg</a>
 */
public class ResourceToSolrTranslator {

    protected static final String UTC_TIMEFORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private SolrInputDocument inputDocument = new SolrInputDocument();
    private static final DateTimeZone UTC_TIMEZONE = DateTimeZone.forTimeZone(TimeZone.getTimeZone("UTC"));

    /**
     * This method will always add these fields to the solr index:
     * <ul>
     * <li>id</li>
     * <li>tags</li>
     * <li>type</li>
     * </ul>
     * It will also try to add the keywords list and name properties if they exist.
     *
     * @param resource
     * @param additionalFields
     * @throws Exception
     */
    public void add(final AbstractEntity resource, final Map<String, Object> additionalFields) {
        addField("id", resource.getId());
        addField("tags", resource.getTags());
        addTypeField(resource);
        addTitleProperty(resource);
        addKeywordsProperty(resource);
        if (additionalFields != null) {
            for (Map.Entry<String, Object> entry : additionalFields.entrySet()) {
                addField(entry.getKey(), entry.getValue());
            }
        }
    }

    private void addTypeField(AbstractEntity resource) {
        String type;
        if (resource instanceof Session) {
            type = ObjectType.session.name();
        }
        else if (resource instanceof Person) {
            type = ObjectType.person.name();
        }
        else if (resource instanceof Event) {
            type = ObjectType.event.name();
        }
        else {
            throw new IllegalArgumentException("I cannot index this object type: " + resource.getClass());
        }
        addField("type", type);
    }

    private void addTitleProperty(AbstractEntity resource) {
        String title = null;
        if (resource instanceof Session) {
            title = ((Session) resource).getTitle();
        }
        else if (resource instanceof Person) {
            title = ((Person) resource).getName();
        }
        else if (resource instanceof Event) {
            title = ((Event) resource).getName();
        }
        if (title != null) {
            addField("title", title);
        }
    }

    private void addKeywordsProperty(AbstractEntity resource) {
        List<String> keywords = null;
        if (resource instanceof Session) {
            keywords = ((Session) resource).getKeywords();
        }
        addField("keywords", keywords);
    }

    private void addField(String name, Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof LocalDate) { //TODO: is this correct? Shouldnt we use LocalTime here?
            DateTime time = ((LocalDate) value).toDateTimeAtStartOfDay();
            addDateTimeInUTC(name, time);
        } else if (value instanceof Interval) {
            Interval interval = (Interval) value;
            DateTime start = interval.getStart();
            DateTime end = interval.getEnd();
            addDateTimeInUTC(name + "_start", start);
            addDateTimeInUTC(name + "_end", end);
            inputDocument.removeField(name);
        } else if (value instanceof ValueObject) {
            ValueObject object = (ValueObject) value;
            inputDocument.addField(name, object.getIndexingValue());
        } else if (value instanceof AbstractEntity) {
            AbstractEntity object = (AbstractEntity) value;
            inputDocument.addField(name, object.getId());
        } else if (value instanceof Collection) {
            Collection list = (Collection) value;
            if (!list.isEmpty()) {
                for (Object object : list) {
                    addField(name, object);
                }
            }
        } else {
            inputDocument.addField(name, value.toString());
        }
    }

    private void addDateTimeInUTC(String name, DateTime time) {
        DateTimeFormatter format = DateTimeFormat.
                forPattern(UTC_TIMEFORMAT).
                withZone(UTC_TIMEZONE);
        inputDocument.addField(name, format.print(time));
    }

    public SolrInputDocument getInputDocument() {
        return inputDocument;
    }
}
