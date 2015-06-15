package net.openhft.chronicle.engine.map;

import net.openhft.chronicle.core.annotation.NotNull;
import net.openhft.chronicle.engine.api.EngineReplication.ModificationIterator;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static net.openhft.chronicle.hash.replication.SingleChronicleHashReplication.builder;

/**
 * Created by Rob Austin
 */

public class EngineReplicatorMap2MapTest {

    final EngineReplicator replicator1 = new EngineReplicator(null);
    final ChronicleMap<String, String> map1 = newMap(1, replicator1, String.class, String.class);

    final EngineReplicator replicator2 = new EngineReplicator(null);
    final ChronicleMap<String, String> map2 = newMap(2, replicator2, String.class, String.class);

    final EngineReplicator replicator3 = new EngineReplicator(null);
    final ChronicleMap<String, String> map3 = newMap(3, replicator3, String.class, String.class);


    public <K, V> ChronicleMap<K, V> newMap(int localIdentifier,
                                            final EngineReplicator replicator,
                                            @NotNull final Class<K> keyClass,
                                            @NotNull final Class<V> valueClass) {
        return ChronicleMapBuilder.of(keyClass, valueClass).
                replication(builder().engineReplication(replicator).createWithId((byte) localIdentifier))
                .create();
    }

    /**
     * tests that the updates from one map are replicated to the other and visa versa
     */
    @Test
    public void testLocalPut() throws Exception {

        final ModificationIterator iterator1for2 = replicator1.acquireModificationIterator
                (replicator2.identifier());

        final ModificationIterator iterator1for3 = replicator1.acquireModificationIterator
                (replicator3.identifier());

        final ModificationIterator iterator2for1 = replicator2.acquireModificationIterator
                (replicator1.identifier());

        final ModificationIterator iterator2for3 = replicator2.acquireModificationIterator
                (replicator3.identifier());

        final ModificationIterator iterator3for1 = replicator3.acquireModificationIterator
                (replicator1.identifier());

        final ModificationIterator iterator3for2 = replicator3.acquireModificationIterator
                (replicator2.identifier());


        map1.put("hello1", "world1");
        map2.put("hello2", "world2");
        map3.put("hello3", "world3");

        iterator1for2.forEach(replicator2.identifier(), replicator2::onEntry);
        iterator1for3.forEach(replicator3.identifier(), replicator3::onEntry);

        iterator2for1.forEach(replicator1.identifier(), replicator1::onEntry);
        iterator2for3.forEach(replicator3.identifier(), replicator3::onEntry);

        iterator3for1.forEach(replicator1.identifier(), replicator1::onEntry);
        iterator3for2.forEach(replicator2.identifier(), replicator2::onEntry);

        for (Map m : new Map[]{map1, map2, map3}) {
            Assert.assertEquals("world1", m.get("hello1"));
            Assert.assertEquals("world2", m.get("hello2"));
            Assert.assertEquals("world3", m.get("hello3"));
            Assert.assertEquals(3, m.size());
        }

    }

}

