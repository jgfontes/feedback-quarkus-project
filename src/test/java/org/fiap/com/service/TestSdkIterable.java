package org.fiap.com.service;

import software.amazon.awssdk.core.pagination.sync.SdkIterable;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Test helper to simulate SdkIterable from DynamoDB scan results.
 */
class TestSdkIterable<T> implements SdkIterable<T> {

    private final List<T> items;

    TestSdkIterable(List<T> items) {
        this.items = items;
    }

    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        items.forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return items.spliterator();
    }

    @Override
    public Stream<T> stream() {
        return items.stream();
    }
}
