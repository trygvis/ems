package no.java.ems.domain;

import no.java.ems.client.*;

public class EventSummary {
    public final String name;
    public final ResourceHandle handle;

    public EventSummary(String name, ResourceHandle handle) {
        this.name = name;
        this.handle = handle;
    }
}
