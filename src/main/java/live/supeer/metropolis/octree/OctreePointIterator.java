package live.supeer.metropolis.octree;

import org.joml.Vector3i;

/**
 * Implementation of {@link OctreeIterator} that iterates towards a single point
 * stored in the tree. All other values are ignored.
 * 
 * @param <T> The type of the values stored in the tree
 */
public class OctreePointIterator<T> extends OctreeIterator<T> {
    private int point_x;
    private int point_y;
    private int point_z;

    public OctreePointIterator(Octree<T> tree, Vector3i point) {
        this(tree, point.x, point.y, point.z);
    }

    public OctreePointIterator(Octree<T> tree, int x, int y, int z) {
        super(tree);
        this.point_x = x;
        this.point_y = y;
        this.point_z = z;
    }

    /**
     * Resets the iterator and points it to a different point
     * 
     * @param x The x coordinate of the point
     * @param y The y coordinate of the point
     * @param z The z coordinate of the point
     */
    public void reset(int x, int y, int z) {
        super.reset();
        this.point_x = x;
        this.point_y = y;
        this.point_z = z;
    }

    @Override
    protected Intersection intersect() {
        int min_x = getX();
        int min_y = getY();
        int min_z = getZ();
        int mask = getScale()-1;
        int max_x = min_x | mask;
        int max_y = min_y | mask;
        int max_z = min_z | mask;

        if (min_x > point_x || min_y > point_y || min_z > point_z) {
            return Intersection.OUTSIDE;
        }
        if (max_x < point_x || max_y < point_y || max_z < point_z) {
            return Intersection.OUTSIDE;
        }

        return (mask == 0) ? Intersection.INSIDE : Intersection.PARTIAL;
    }
}
