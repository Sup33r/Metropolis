package live.supeer.metropolis.octree;

import org.jetbrains.annotations.NotNull;

/**
 * Iterable that returns a {@link OctreeIterator}
 * 
 * @param <T> The value type of the Octree
 */
public interface OctreeIterable<T> extends Iterable<T> {
    @Override
    @NotNull OctreeIterator<T> iterator();
}
