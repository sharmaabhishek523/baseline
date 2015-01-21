/*
 * Copyright 2015 Air Computing Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aerofs.baseline.auth;

import com.google.common.collect.Lists;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * User-specified list of {@link com.aerofs.baseline.auth.Authenticator}
 * instances that can be used to verify incoming HTTP requests.
 * <p>
 * Conceptually this functions as an {@code Authenticator} chain.
 * The implementation iterates through the chain until it finds the
 * first {@code Authenticator} that can process the request,
 * whether successfully or not.
 */
@ThreadSafe
public final class Authenticators implements Iterable<Authenticator> {

    private final List<Authenticator> authenticators = Lists.newCopyOnWriteArrayList();

    public void add(Authenticator authenticator) {
        authenticators.add(authenticator);
    }

    @Override
    public Iterator<Authenticator> iterator() {
        return Collections.unmodifiableList(authenticators).iterator();
    }

    @Override
    public void forEach(Consumer<? super Authenticator> action) {
        Collections.unmodifiableList(authenticators).forEach(action);
    }

    @Override
    public Spliterator<Authenticator> spliterator() {
        return Collections.unmodifiableList(authenticators).spliterator();
    }
}
