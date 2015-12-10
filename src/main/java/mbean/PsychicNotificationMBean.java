package mbean;

/**
 * PsychicNotificationMBean.
 *
 * @author Thibault Meyer
 * @version 1.3.0
 * @since 1.3.0
 */
public interface PsychicNotificationMBean {

    /**
     * Called on user state change.
     *
     * @param userLogin The user's login
     * @param state     The new user' state
     * @param source    The source of the event
     * @since 1.3.0
     */
    void onUserChangeState(final String userLogin, final String state, final String source);
}
