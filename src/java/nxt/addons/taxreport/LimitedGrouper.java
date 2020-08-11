package nxt.addons.taxreport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

class LimitedGrouper<K, V> {
    private final LimitedMap<K, List<V>> map = new LimitedMap<>(100);

    Map.Entry<K, List<V>> offer(K key, V value) {
        map.computeIfAbsent(key, k -> new ArrayList<>())
                .add(value);
        return map.removeRemovedOldest();
    }

    void clear() {
        map.clear();
        map.removeRemovedOldest();
    }

    void forEach(BiConsumer<K, List<V>> consumer) {
        map.forEach(consumer);
    }

    private static class LimitedMap<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;
        private Map.Entry<K, V> removedOldest;

        private LimitedMap(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            if (size() > maxSize) {
                removedOldest = eldest;
                return true;
            }
            return false;
        }

        Map.Entry<K, V> removeRemovedOldest() {
            Map.Entry<K, V> result = this.removedOldest;
            this.removedOldest = null;
            return result;
        }
    }
}