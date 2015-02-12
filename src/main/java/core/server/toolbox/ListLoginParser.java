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
 * @since 1.0.0
 */
public final class ListLoginParser {
    public static List<String> parse(final String data, final Collection<Session> connectedSessions) {
        //TODO: Change this method to return Session directly.
        List<String> lstLoginDest = new LinkedList<String>(Arrays.asList(data.replaceAll("[\\{\\}]", "").split("[,;]")));
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
}
