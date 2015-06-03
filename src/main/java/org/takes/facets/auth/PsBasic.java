/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.takes.facets.auth;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.bind.DatatypeConverter;
import lombok.EqualsAndHashCode;
import org.takes.Request;
import org.takes.Response;
import org.takes.misc.Opt;

/**
 * Pass that checks the user according RFC-2617.
 *
 * <p>The class is immutable and thread-safe.
 *
 * @author Endrigo Antonini (teamed@endrigo.com.br)
 * @version $Id$
 * @since 0.20
 */
@EqualsAndHashCode(of = { "entry" })
public final class PsBasic implements Pass {

    /**
     * Authorization response HTTP head.
     */
    private static final String AUTH_HEAD = "Authorization: Basic";

    /**
     * Entry to validate user information.
     */
    private final transient PsBasic.Entry entry;

    /**
     * Ctor.
     * @param basic Entry
     */
    public PsBasic(final PsBasic.Entry basic) {
        this.entry = basic;
    }

    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Opt<Identity> enter(final Request request) throws IOException {
        BasicAuth auth = new BasicAuth("", "");
        Opt<Identity> identity = new Opt.Empty<Identity>();
        for (final String head : request.head()) {
            if (head.startsWith(AUTH_HEAD)) {
                auth = this.readAuthContentOnHead(head);
                break;
            }
        }
        if (!auth.isEmpty() && this.entry.check(
            auth.getUser(),
            auth.getPass()
        )) {
            final ConcurrentMap<String, String> props =
                new ConcurrentHashMap<String, String>(0);
            identity = new Opt.Single<Identity>(
                new Identity.Simple(
                    String.format("urn:basic:%s", auth.getUser()),
                    props
                )
            );
        }
        return identity;
    }

    @Override
    public Response exit(final Response response, final Identity identity)
        throws IOException {
        return response;
    }

    /**
     * Read authentication content that is received on the head.
     * @param head Head
     * @return BasicAuth instance.
     */
    private BasicAuth readAuthContentOnHead(final String head) {
        final String authorization = new String(
            DatatypeConverter.parseBase64Binary(
                head.split(AUTH_HEAD)[1].trim()
            )
        );
        final String user = authorization.split(":")[0];
        return new BasicAuth(
            user,
            authorization.substring(user.length() + 1)
        );
    }

    /**
     * Entry interface that is used to check if the received information is
     * valid.
     *
     * @author Endrigo Antonini (teamed@endrigo.com.br)
     * @version $Id$
     * @since 0.20
     */
    public interface Entry {

        /**
         * Check if is a valid user.
         * @param user User
         * @param pwd Password
         * @return If valid it return <code>true</code>.
         */
        boolean check(String user, String pwd);
    }

    /**
     * Used to transfer authentication information.
     *
     * @author Endrigo Antonini (teamed@endrigo.com.br)
     * @version $Id$
     * @since 0.20
     */
    private final class BasicAuth {

        /**
         * User.
         */
        private final String user;

        /**
         * Password.
         */
        private final String pass;

        /**
         * Ctor.
         * @param username User
         * @param password Password
         */
        public BasicAuth(final String username, final String password) {
            super();
            this.user = username;
            this.pass = password;
        }

        /**
         * Return user.
         * @return User.
         */
        public String getUser() {
            return this.user;
        }

        /**
         * Return Password.
         * @return Password.
         */
        public String getPass() {
            return this.pass;
        }

        /**
         * Check if the object is empty.
         * @return Return <code>true</code> if user and password is empty.
         */
        public boolean isEmpty() {
            return this.getUser().isEmpty() && this.getPass().isEmpty();
        }
    }
}
