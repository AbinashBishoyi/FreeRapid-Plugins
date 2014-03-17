package cz.vity.freerapid.plugins.services.duckload.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * This class implements a sorted list. It is constructed with a comparator
 * that can compare two objects and sort objects accordingly. When you add an
 * object to the list, it is inserted in the correct place. Object that are
 * equal according to the comparator, will be in the list in the order that
 * they were added to this list. Add only objects that the comparator can
 * compare.
 *
 * @author Originally by author(s) of <a href="http://sourceforge.net/projects/nite/">net.sourceforge.nite</a>,
 *         modified/improved by ntoskrnl
 */
public class SortedList<E> extends ArrayList<E> {
    private Comparator<? super E> comparator;

    /**
     * Constructs a new sorted list. The objects in the list will be sorted
     * according to the their natural ordering. All keys inserted into the
     * list must implement the {@link Comparable} interface.
     */
    public SortedList() {
        this.comparator = null;
    }

    /**
     * Constructs a new sorted list containing the elements of the specified
     * collection. The objects in the list will be sorted according to
     * their natural ordering. All keys inserted into the
     * list must implement the {@link Comparable} interface.
     *
     * @param a the collection whose elements are to be placed into this list
     */
    public SortedList(Collection<E> a) {
        this.comparator = null;
        this.addAll(a);
    }

    /**
     * Constructs a new sorted list. The objects in the list will be sorted
     * according to the specified comparator.
     *
     * @param c a comparator
     */
    public SortedList(Comparator<? super E> c) {
        this.comparator = c;
    }

    /**
     * Constructs a new sorted list containing the elements of the specified
     * collection. The objects in the list will be sorted
     * according to the specified comparator.
     *
     * @param a the collection whose elements are to be placed into this list
     * @param c a comparator
     */
    public SortedList(Collection<E> a, Comparator<? super E> c) {
        this.comparator = c;
        this.addAll(a);
    }

    /**
     * Adds an object to the list. The object will be inserted in the correct
     * place so that the objects in the list are sorted. When the list already
     * contains objects that are equal according to the comparator, the new
     * object will be inserted immediately after these other objects.
     *
     * @param e the object to be added
     */
    @Override
    public boolean add(E e) {
        int i = 0;
        boolean found = false;
        while (!found && (i < size())) {
            found = compare(e, get(i)) < 0;
            if (!found) i++;
        }
        super.add(i, e);
        return true;
    }

    /**
     * This method has no effect. It is not allowed to specify an index to
     * insert an element as this might violate the sorting order of the objects
     * in the list.
     */
    @Override
    public void add(int index, E e) {
    }

    /**
     * Adds all of the elements in the specified collection to the list.
     * The objects will be inserted in the correct place so that the objects
     * in the list are sorted.
     *
     * @param a collection containing elements to be added to this list
     */
    @Override
    public boolean addAll(Collection<? extends E> a) {
        for (E e : a) {
            this.add(e);
        }
        return true;
    }

    /**
     * This method has no effect. It is not allowed to specify an index to
     * insert elements as this might violate the sorting order of the objects
     * in the list.
     */
    @Override
    public boolean addAll(int index, Collection<? extends E> a) {
        return false;
    }

    /**
     * Compares two keys using the correct comparison method for this SortedList.
     */
    @SuppressWarnings("unchecked")
    private int compare(E e1, E e2) {
        return comparator == null ? ((Comparable<? super E>) e1).compareTo(e2) : comparator.compare(e1, e2);
    }

}