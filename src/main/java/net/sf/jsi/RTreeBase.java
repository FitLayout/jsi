//   RTreeBase.java
//   Java Spatial Index Library
//   Copyright (C) 2002-2005 Infomatiq Limited
//   Copyright (C) 2008-2010 aled@users.sourceforge.net
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package net.sf.jsi;

/**
 */
abstract class RTreeBase
{
	protected RTreeBase()
	{
	}
	
	
	protected abstract int getRoodNodeId();
	
	/**
	 * Get a node object, given the ID of the node.
	 */
	protected abstract Node getNode(int id);

	
	/**
	 * Returns the bounds of all the entries in the spatial index,
	 * or null if there are no entries.
	 */
	protected Area getBounds()
	{
		Area bounds = null;

		Node n = getNode(getRoodNodeId());
		if ( n != null && n.entryCount > 0 ) {
			bounds = new Area();
			bounds.minX = n.mbrMinX;
			bounds.minY = n.mbrMinY;
			bounds.maxX = n.mbrMaxX;
			bounds.maxY = n.mbrMaxY;
		}
		return bounds;
	}


	/**
	 * Finds the nearest rectangles to the passed rectangle and calls
	 * v.execute(id) for each one.
	 *
	 * If multiple rectangles are equally near, they will
	 * all be returned.
	 *
	 * @param p The point for which this method finds the
	 *        nearest neighbours.
	 *
	 * @param v The IntProcedure whose execute() method is is called
	 *        for each nearest neighbour.
	 *
	 * @param furthestDistance The furthest distance away from the rectangle
	 *        to search. Rectangles further than this will not be found.
	 *
	 *        This should be as small as possible to minimise
	 *        the search time.
	 *
	 *        Use Float.POSITIVE_INFINITY to guarantee that the nearest rectangle is found,
	 *        no matter how far away, although this will slow down the algorithm.
	 */
	protected void nearest(Spot p, AreaCallback v, float furthestDistance)
	{
		Node rootNode = getNode(getRoodNodeId());

		float furthestDistanceSq = furthestDistance * furthestDistance;
		IntArray nearestIds = new IntArray();
		nearest(p, rootNode, furthestDistanceSq, nearestIds);
		nearestIds.forEach(v);
	}

	
	/**
	 * Same as nearestN, except the found rectangles are not returned
	 * in sorted order. This will be faster, if sorting is not required
	 */
	protected void nearestNUnsorted(Spot p, AreaCallback v, int count, float furthestDistance)
	{
		// This implementation is designed to give good performance
		// where
		// o N is high (100+)
		// o The results do not need to be sorted by distance.
		//
		// Uses a priority queue as the underlying data structure.
		//
		// Note that more than N items will be returned if items N and N+x have the
		// same priority.
		PriorityQueue distanceQueue = new PriorityQueue(PriorityQueue.SORT_ORDER_DESCENDING);
		createNearestNDistanceQueue(p, count, distanceQueue, furthestDistance);

		while ( distanceQueue.size() > 0 ) {
			v.processArea(distanceQueue.getValue());
			distanceQueue.pop();
		}
	}


	/**
	 * Finds the N nearest rectangles to the passed rectangle, and calls
	 * execute(id, distance) on each one, in order of increasing distance.
	 *
	 * Note that fewer than N rectangles may be found if fewer entries
	 * exist within the specified furthest distance, or more if rectangles
	 * N and N+1 have equal distances.
	 *
	 * @param p The point for which this method finds the
	 *        nearest neighbours.
	 *
	 * @param v The IntfloatProcedure whose execute() method is is called
	 *        for each nearest neighbour.
	 *
	 * @param count The desired number of rectangles to find (but note that
	 *        fewer or more may be returned)
	 *
	 * @param furthestDistance The furthest distance away from the rectangle
	 *        to search. Rectangles further than this will not be found.
	 *
	 *        This should be as small as possible to minimise
	 *        the search time.
	 *
	 *        Use Float.POSITIVE_INFINITY to guarantee that the nearest rectangle is found,
	 *        no matter how far away, although this will slow down the algorithm.
	 */
	protected void nearestN(Spot p, AreaCallback v, int count, float furthestDistance)
	{
		PriorityQueue distanceQueue = new PriorityQueue(PriorityQueue.SORT_ORDER_DESCENDING);
		createNearestNDistanceQueue(p, count, distanceQueue, furthestDistance);
		distanceQueue.setSortOrder(PriorityQueue.SORT_ORDER_ASCENDING);

		while ( distanceQueue.size() > 0 ) {
			v.processArea(distanceQueue.getValue());
			distanceQueue.pop();
		}
	}


	/**
	 * Finds all rectangles that intersect the passed rectangle.
	 *
	 * @param r The rectangle for which this method finds
	 *        intersecting rectangles.
	 *
	 * @param v The IntProcedure whose execute() method is is called
	 *        for each intersecting rectangle.
	 */
	protected void intersects(Area r, AreaCallback v)
	{
		Node rootNode = getNode(getRoodNodeId());
		intersects(r, v, rootNode);
	}


	/**
	 * Finds all rectangles contained by the passed rectangle.
	 *
	 * @param r The rectangle for which this method finds
	 *        contained rectangles.
	 *
	 * @param v The procedure whose visit() method is is called
	 *        for each contained rectangle.
	 */
	protected void contains(Area r, AreaCallback v)
	{
		// find all rectangles in the tree that are contained by the passed rectangle
		// written to be non-recursive (should model other searches on this?)
		IntArray parents = new IntArray();
		parents.push(getRoodNodeId());

		IntArray parentsEntry = new IntArray();
		parentsEntry.push(-1);

		// TODO: possible shortcut here - could test for intersection with the
		// MBR of the root node. If no intersection, return immediately.

		LOOP:
		while ( parents.size() > 0 ) {
			Node n = getNode(parents.peek());
			int startIndex = parentsEntry.peek() + 1;

			if ( !n.isLeaf() ) {
				// go through every entry in the index node to check
				// if it intersects the passed rectangle. If so, it
				// could contain entries that are contained.
				for ( int i = startIndex; i < n.entryCount; i++ ) {
					if ( Area.intersects(r.minX, r.minY, r.maxX, r.maxY,
						n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i]) )
					{
						parents.push(n.ids[i]);
						parentsEntry.pop();
						parentsEntry.push(i); // this becomes the start index when the child has been searched
						parentsEntry.push(-1);
						continue LOOP;
				}
				}
			} else {
				// go through every entry in the leaf to check if
				// it is contained by the passed rectangle
				for ( int i = 0; i < n.entryCount; i++ ) {
					if ( Area.contains(r.minX, r.minY, r.maxX, r.maxY, n.entriesMinX[i], n.entriesMinY[i],
						n.entriesMaxX[i], n.entriesMaxY[i]) )
					{
						if ( !v.processArea(n.ids[i]) ) return;
					}
				}
			}
			parents.pop();
			parentsEntry.pop();
		}
	}


	private void createNearestNDistanceQueue(Spot p, int count, PriorityQueue distanceQueue, float furthestDistance)
	{
		// return immediately if given an invalid "count" parameter
		if ( count <= 0 ) {
			return;
		}

		IntArray parents = new IntArray();
		parents.push(getRoodNodeId());

		IntArray parentsEntry = new IntArray();
		parentsEntry.push(-1);

		IntArray savedValues = new IntArray();
		float savedPriority = 0;

		// TODO: possible shortcut here - could test for intersection with the
		// MBR of the root node. If no intersection, return immediately.

		float furthestDistanceSq = furthestDistance * furthestDistance;

		LOOP:
		while ( parents.size() > 0 ) {
			Node n = getNode(parents.peek());
			int startIndex = parentsEntry.peek() + 1;

			if ( !n.isLeaf() ) {
				// go through every entry in the index node to check
				// if it could contain an entry closer than the farthest entry
				// currently stored.
				for ( int i = startIndex; i < n.entryCount; i++ ) {
					if ( Area.distanceSq(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i], p.x, p.y) <= furthestDistanceSq ) {
						parents.push(n.ids[i]);
						parentsEntry.pop();
						parentsEntry.push(i); // this becomes the start index when the child has been searched
						parentsEntry.push(-1);
						continue LOOP;
				}
				}
			} else {
				// go through every entry in the leaf to check if
				// it is currently one of the nearest N entries.
				for ( int i = 0; i < n.entryCount; i++ ) {
					float entryDistanceSq = Area.distanceSq(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i], p.x, p.y);
					int entryId = n.ids[i];

					if ( entryDistanceSq <= furthestDistanceSq ) {
						distanceQueue.insert(entryId, entryDistanceSq);

						while ( distanceQueue.size() > count ) {
							// normal case - we can simply remove the lowest priority (highest distance) entry
							int value = distanceQueue.getValue();
							float distanceSq = distanceQueue.getPriority();
							distanceQueue.pop();

							// rare case - multiple items of the same priority (distance)
							if ( distanceSq == distanceQueue.getPriority() ) {
								savedValues.add(value);
								savedPriority = distanceSq;
							} else {
								savedValues.reset();
							}
						}

						// if the saved values have the same distance as the
						// next one in the tree, add them back in.
						if ( savedValues.size() > 0 && savedPriority == distanceQueue.getPriority() ) {
							for ( int svi = 0; svi < savedValues.size(); svi++ ) {
								distanceQueue.insert(savedValues.get(svi), savedPriority);
							}
							savedValues.reset();
						}

						// narrow the search, if we have already found N items
						if ( distanceQueue.getPriority() < furthestDistanceSq && distanceQueue.size() >= count ) {
							furthestDistanceSq = distanceQueue.getPriority();
						}
					}
				}
			}
			parents.pop();
			parentsEntry.pop();
		}
	}


	/**
	 * Recursively searches the tree for the nearest entry. Other queries
	 * call execute() on an IntProcedure when a matching entry is found;
	 * however nearest() must store the entry Ids as it searches the tree,
	 * in case a nearer entry is found.
	 * Uses the member variable nearestIds to store the nearest
	 * entry IDs (it is an array, rather than a single value, in case
	 * multiple entries are equally near)
	 */
	private float nearest(Spot p, Node n, float furthestDistanceSq, IntArray nearestIds)
	{
		for ( int i = 0; i < n.entryCount; i++ ) {
			float tempDistanceSq = Area.distanceSq(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i], p.x, p.y);
			if ( n.isLeaf() ) { // for leaves, the distance is an actual nearest distance
				if ( tempDistanceSq < furthestDistanceSq ) {
					furthestDistanceSq = tempDistanceSq;
					nearestIds.reset();
				}
				if ( tempDistanceSq <= furthestDistanceSq ) {
					nearestIds.add(n.ids[i]);
				}
			} else { // for index nodeMap, only go into them if they potentially could have
				// a rectangle nearer than actualNearest
				if ( tempDistanceSq <= furthestDistanceSq ) {
					// search the child node
					furthestDistanceSq = nearest(p, getNode(n.ids[i]), furthestDistanceSq, nearestIds);
				}
			}
		}
		return furthestDistanceSq;
	}


	/**
	 * Recursively searches the tree for all intersecting entries.
	 * Immediately calls execute() on the passed IntProcedure when
	 * a matching entry is found.
	 *
	 * TODO rewrite this to be non-recursive? Make sure it doesn't slow it down.
	 */
	private boolean intersects(Area r, AreaCallback v, Node n)
	{
		for ( int i = 0; i < n.entryCount; i++ ) {
			if ( Area.intersects(r.minX, r.minY, r.maxX, r.maxY, n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i]) ) {
				if ( n.isLeaf() ) {
					if ( !v.processArea(n.ids[i]) ) return false;
				} else {
					Node childNode = getNode(n.ids[i]);
					if ( !intersects(r, v, childNode) ) return false;
				}
			}
		}
		return true;
	}
	
}
