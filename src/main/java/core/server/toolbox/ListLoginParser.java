package core.server.toolbox;

import core.server.session.Session;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Helper to parse string containing single or multiple
 * login / file descriptor. File descriptor are resolved
 * as login.
 *
 * @author Thibault Meyer
 * @version 1.1.0
 * @since 1.0.0
 */
public final class ListLoginParser {

    /**
     * Parse the login (or login list) string to a list of login. Duplicated entries are removed.
     *
     * @param data              The containing the login or the list of login
     * @param connectedSessions The collection of current connected session
     * @return A list of login (String)
     */
    public static List<String> parseToLogin(final String data, final Collection<Session> connectedSessions) {
        List<String> lstLoginDest = new LinkedList<>(Arrays.asList(data.replaceAll("[\\{\\}]", "").split("[,;]")));
        int i = 0;
        while (i < lstLoginDest.size()) {
            if (lstLoginDest.get(i).startsWith(":")) {
                try {
                    final long fd = Long.valueOf(lstLoginDest.get(i).substring(1));
                    final Session sess = connectedSessions.stream().filter(s -> s.network.fd == fd).findFirst().orElse(null);
                    if (sess != null) {
                        if (!lstLoginDest.contains(sess.user.login)) {
                            lstLoginDest.remove(i);
                            lstLoginDest.add(i, sess.user.login);
                        }
                        i += 1;
                    } else {
                        lstLoginDest.remove(i);
                    }
                } catch (NumberFormatException ignore) {
                    lstLoginDest.remove(i);
                }
            } else {
                i += 1;
            }
        }
        return lstLoginDest;
    }

    /**
     * Parse the login (or login list) string to a build a list of Session. Duplicated entries are removed.
     *
     * @param data              The containing the login or the list of login
     * @param connectedSessions The collection of current connected session
     * @return A list of Session
     * @since 1.1.0
     */
    public static List<Session> parseToSession(final String data, final Collection<Session> connectedSessions) {
        final List<Session> lstSessionDest = new LinkedList<>();
        for (final String login : Arrays.asList(data.replaceAll("[\\{\\}]", "").split("[,;]"))) {
            if (login.startsWith(":")) {
                final long fd = Long.valueOf(login.substring(1));
                final Session tmpSess = connectedSessions.stream().filter(s -> s.network.fd == fd && s.user.login != null).findFirst().orElse(null);
                if (tmpSess != null) {
                    if (!lstSessionDest.contains(tmpSess)) {
                        lstSessionDest.add(tmpSess);
                    }
                }
            } else {
                connectedSessions.stream().filter(s -> s.user.login != null && s.user.login.compareTo(login) == 0).forEach(s -> {
                    if (!lstSessionDest.contains(s)) {
                        lstSessionDest.add(s);
                    }
                });
            }
        }
        return lstSessionDest;
    }
}
