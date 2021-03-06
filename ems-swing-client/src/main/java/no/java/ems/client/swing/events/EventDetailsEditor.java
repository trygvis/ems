package no.java.ems.client.swing.events;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import no.java.ems.client.swing.EntityEditor;
import no.java.ems.domain.Event;
import org.jdesktop.beansbinding.*;
import org.jdesktop.swingx.JXDatePicker;
import org.joda.time.DateMidnight;
import org.joda.time.LocalDate;

import javax.swing.*;
import java.util.Date;

/**
 * @author <a href="mailto:erlend@hamnaberg.net">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public class EventDetailsEditor extends EntityEditor<Event> {
    private JXDatePicker startDateSelector;
    private JXDatePicker endDateSelector;
    private TimeslotSelector timeSlotSelector;

    protected EventDetailsEditor(final Event entity) {
        super(entity, "name");
        initialize();
    }

    @Override
    public void initModels() {
    }

    @Override
    public void initActions() {
    }

    @Override
    public void initComponents() {
        super.initComponents();
        startDateSelector = new JXDatePicker();
        startDateSelector.setFormats("dd.MM.yyyy");
        startDateSelector.setDate(new Date());
        endDateSelector = new JXDatePicker();
        endDateSelector.setFormats("dd.MM.yyyy");
        endDateSelector.setDate(new Date());
        timeSlotSelector = new TimeslotSelector(getEntity());
    }

    @Override
    public void initBindings() {
        super.initBindings();
        getBindingGroup().addBinding(createDateBinding("startDate", startDateSelector));

        getBindingGroup().addBinding(createDateBinding("endDate", endDateSelector));
        getBindingGroup().addBindingListener(new AbstractBindingListener() {
            @Override
            public void synced(Binding binding) {
                super.synced(binding);
                LocalDate startDate = getEntity().getStartDate();
                LocalDate endDate = getEntity().getEndDate();
                if (startDate != null && endDate != null && (startDate.isBefore(endDate) || startDate.isEqual(endDate))) {
                    timeSlotSelector.firePropertyChange(TimeslotSelector.TIME_SELECTOR_ENABLED, false, true);
                }
                else {
                    timeSlotSelector.firePropertyChange(TimeslotSelector.TIME_SELECTOR_ENABLED, true, false);
                }
            }
        });
    }

    private AutoBinding<Event, LocalDate, JXDatePicker, Date> createDateBinding(final String property, final JXDatePicker component) {
        AutoBinding<Event, LocalDate, JXDatePicker, Date> dateBinding = Bindings.createAutoBinding(
                AutoBinding.UpdateStrategy.READ_WRITE,
                getEntity(),
                BeanProperty.<Event, LocalDate>create(property),
                component,
                BeanProperty.<JXDatePicker, Date>create("date")
        );
        dateBinding.setConverter(new DateConverter());
        return dateBinding;
    }

    @Override
    public void initLayout() {
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new FormLayout("d, 5dlu, p", "f:p, 3dlu, f:p, 3dlu, f:p, 3dlu, f:p"));
        CellConstraints cc = new CellConstraints();
        add(createLabel(titleProperty, titleField), cc.xy(1, 1));
        add(titleField, cc.xy(3, 1));
        add(createLabel("startDate", startDateSelector), cc.xy(1, 3));
        add(startDateSelector, cc.xy(3, 3));
        add(createLabel("endDate", endDateSelector), cc.xy(1, 5));
        add(endDateSelector, cc.xy(3, 5));
        add(timeSlotSelector, cc.xyw(1, 7, 3));
    }

    private static class DateConverter extends Converter<LocalDate, Date> {
        @Override
        public Date convertForward(LocalDate value) {
            return new Date(value.toDateMidnight().getMillis());
        }

        @Override
        public LocalDate convertReverse(Date value) {
            return new DateMidnight(value.getTime()).toLocalDate();
        }
    }
}
