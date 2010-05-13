package no.java.ems.client.swing.events;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import no.java.swing.DefaultPanel;
import no.java.ems.domain.Event;
import org.joda.time.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:erlend@escenic.com">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public class TimeslotSelector extends DefaultPanel {
    private static final Duration HOUR_DURATION = Minutes.minutes(60).toStandardDuration();
    private static final Duration FIFTEEN_MINUTES_DURATION = Minutes.minutes(15).toStandardDuration();
    private static final Duration THIRTY_MINUTES_DURATION = Minutes.minutes(30).toStandardDuration();

    private TimePicker startTimePicker;
    private TimePicker endTimePicker;
    private final Event event;
    private Action generateTimeSlotsAction;
    private PropertyChangeListener dateListener;
    public static final String TIME_SELECTOR_ENABLED = "timeSelectorEnabled";

    protected TimeslotSelector(Event event) {
        this.event = event;
        initialize();
    }

    @Override
    public void initModels() {
    }

    @Override
    public void initActions() {
        generateTimeSlotsAction = new DefaultAction("GenerateTimeSlotAction") {
            @Override
            public void actionPerformed(ActionEvent e) {
                event.setTimeslots(generateTimeSlots());
                System.out.println("event.getTimeslots() = " + event.getTimeslots());
            }
        };
    }

    @Override
    public void initComponents() {
        startTimePicker = new TimePicker();
        endTimePicker = new TimePicker();
    }

    @Override
    public void initBindings() {
    }

    @Override
    public void initListeners() {
        dateListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Boolean enabled = (Boolean) evt.getNewValue();
                setEnabled(enabled);
            }
        };
        addPropertyChangeListener(TIME_SELECTOR_ENABLED, dateListener);
    }

    @Override
    public void initLayout() {
        CellConstraints cc = new CellConstraints();
        setLayout(new FormLayout("f:p", "f:p, 5dlu, f:p"));

        JPanel timePickerPanel = new JPanel(new FormLayout("d, 3dlu, f:p, 5dlu, d, 3dlu, f:p", "f:p"));
        timePickerPanel.add(createLabel("startTime", startTimePicker), cc.xy(1, 1));
        timePickerPanel.add(startTimePicker, cc.xy(3, 1));
        timePickerPanel.add(createLabel("endTime", endTimePicker), cc.xy(5, 1));
        timePickerPanel.add(endTimePicker, cc.xy(7, 1));
        add(timePickerPanel, cc.xy(1, 1));
        add(new JButton(generateTimeSlotsAction), cc.xy(1, 3));
    }

    @Override
    public void initState() {
        LocalTime now = new LocalTime();
        startTimePicker.setSelectedTime(now);
        endTimePicker.setSelectedTime(now);
        setEnabled(false);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        startTimePicker.setEnabled(enabled);
        endTimePicker.setEnabled(enabled);
        generateTimeSlotsAction.setEnabled(enabled);
    }

    private List<Interval> generateTimeSlots() {
        List<Interval> intervals = new ArrayList<Interval>();
        LocalTime time = startTimePicker.getSelectedTime();
        LocalTime endTime = endTimePicker.getSelectedTime();
        int days = findNumberOfDaysInEvent();
        if (!time.isBefore(endTime) || !time.isEqual(endTime)) {
            for (int i = 0; i < days; i++) {
                LocalDate today = event.getStartDate().plusDays(i);
                DateTime actual = today.toDateTime(time);
                DateTime endDateTime = today.toDateTime(endTime);

                while ((actual.isBefore(endDateTime))) {
                    intervals.add(new Interval(actual, FIFTEEN_MINUTES_DURATION));
                    intervals.add(new Interval(actual, HOUR_DURATION));
                    intervals.add(new Interval(actual, THIRTY_MINUTES_DURATION));
                    actual = actual.plus(FIFTEEN_MINUTES_DURATION);
                }
            }
        }

        return intervals;
    }

    private int findNumberOfDaysInEvent() {
        Days days = Days.daysBetween(event.getStartDate(), event.getEndDate());
        return days.getDays() + 1;
    }
}
