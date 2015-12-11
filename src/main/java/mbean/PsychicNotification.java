package mbean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import java.util.HashMap;
import java.util.Map;

/**
 * PsychicNotification.
 *
 * @author Thibault Meyer
 * @version 1.3.0
 * @since 1.3.0
 */
public class PsychicNotification extends NotificationBroadcasterSupport implements PsychicNotificationMBean {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PsychicMetric.class.getName());

    /**
     * Set the default constructor private.
     *
     * @since 1.3.0
     */
    private PsychicNotification() {
    }

    /**
     * Get the singleton instance of {@code PsychicNotification}.
     *
     * @return The {@code PsychicNotification} instance
     * @since 1.3.0
     */
    public static PsychicNotification getInstance() {
        return PsychicNotificationSingletonHolder.instance;
    }

    /**
     * Called on user state change.
     *
     * @param userLogin The user's login
     * @param state     The new user' state
     * @param source    The source of the event
     * @since 1.3.0
     */
    @Override
    public void onUserChangeState(final String userLogin, final String state, final String source) {
        LOG.trace("onUserChangeState({}, {})", userLogin, state);
        final Notification notification = new Notification("onUserChangeState", source, System.currentTimeMillis() / 1000);
        final Map<String, Object> userData = new HashMap<>();
        userData.put("login", userLogin);
        userData.put("state", state);
        notification.setUserData(userData);
        this.sendNotification(notification);
    }

    /**
     * PsychicNotificationSingletonHolder.
     *
     * @author Thibault Meyer
     * @version 1.3.0
     * @since 1.3.0
     */
    private static class PsychicNotificationSingletonHolder {
        private final static PsychicNotification instance = new PsychicNotification();
    }
}
