package live.supeer.metropolis.octree;

/**
 * Iterable that returns a {@link OctreeIterator}
 * 
 * @param <T> The value type of the Octree
 */
public interface OctreeIterable<T> extends Iterable<T> {
    @Override
    public OctreeIterator<T> iterator();
}
