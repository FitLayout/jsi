//   RTree.java
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <p>
 * This is a lightweight RTree implementation, specifically designed
 * for the following features (in order of importance):
 * <ul>
 * <li>Fast intersection query performance. To achieve this, the RTree
 * uses only main memory to store entries. Obviously this will only improve
 * performance if there is enough physical memory to avoid paging.</li>
 * <li>Low memory requirements.</li>
 * <li>Fast add performance.</li>
 * </ul>
 * <p>
 * The main reason for the high speed of this implementation is the
 * avoidance of the creation of unnecessary objects, mainly achieved by using
 * primitive collections.
 */
public class RTree extends RTreeBase implements Serializable
{
	private static final long serialVersionUID = 8440068248349540350L;

	private static final Logger log = Logger.getLogger(RTree.class.getName());
	private static final Logger logDel = Logger.getLogger(RTree.class.getName() + "-delete");

	// internal consistency checking - set to true if debugging tree corruption
	private final static boolean INTERNAL_CONSISTENCY_CHECKING = false;

	private final static int DEFAULT_MAX_NODE_ENTRIES = 50;
	private final static int DEFAULT_MIN_NODE_ENTRIES = 20;
	private final static int ENTRY_STATUS_ASSIGNED = 0;
	private final static int ENTRY_STATUS_UNASSIGNED = 1;

	private final boolean isDebug = log.isLoggable(Level.FINE);
	private final boolean isDebugDel = logDel.isLoggable(Level.FINE);

	/** Parameters of the tree */
	final int maxNodeEntries, minNodeEntries;

	/**
	 * Map of nodeId to node object.
	 * TODO: eliminate this map - it should not be needed. Nodes can be found by traversing the tree.
	 * Due to {@link #deletedNodeIds} this can be a list.
	 */
	private ArrayList<Node> nodeMap = new ArrayList<>();

	/**
	 * Stores the IDs of nodes which were deleted and can be reused.
	 * <p>
	 * Deleted node objects were planned to be retained in the nodeMap, so that they could be reused.
	 * Actually nodes are never reused, this array allows to use a list as node-map, though.
	 */
	private final IntArray deletedNodeIds = new IntArray();

	/** Cached instance used to mark the status of entries during a node split. */
	private final byte[] entryStatus;

	/**
	 * Stacks used to store nodeId and entry index of each node
	 * from the root down to the leaf. Enables fast lookup
	 * of nodes when a split is propagated up the tree.
	 */
	private final IntArray parents = new IntArray(), parentsEntry = new IntArray();

	/** leaves are always level 1 */
	private int treeHeight = 1;
	private int rootNodeId = 0;
	private int size = 0;


	public RTree()
	{
		this(null);
	}


	/**
	 * Initialize implementation dependent properties of the RTree.
	 * Currently implemented properties are:
	 * <dl>
	 * <dt>MaxNodeEntries</dt><dd>This specifies the maximum number of entries
	 * in a node. The default value is 10, which is used if the property is
	 * not specified, or is less than 2.</dd>
	 * <dt>MinNodeEntries</dt><dd>This specifies the minimum number of entries
	 * in a node. The default value is half of the MaxNodeEntries value (rounded
	 * down), which is used if the property is not specified or is less than 1.</dd>
	 * </dl>
	 */
	public RTree(Properties props)
	{
		int max = DEFAULT_MAX_NODE_ENTRIES;
		int min = DEFAULT_MIN_NODE_ENTRIES;
		if ( props != null ) {
			max = Integer.parseInt(props.getProperty("MaxNodeEntries", "0"));
			min = Integer.parseInt(props.getProperty("MinNodeEntries", "0"));

			// Obviously a node with less than 2 entries cannot be split.
			// The node splitting algorithm will work with only 2 entries
			// per node, but will be inefficient.
			if ( max < 2 ) {
				log.warning("Invalid MaxNodeEntries = " + max + " Resetting to default value of " + DEFAULT_MAX_NODE_ENTRIES);
				max = DEFAULT_MAX_NODE_ENTRIES;
			}

			// The MinNodeEntries must be less than or equal to (int) (MaxNodeEntries / 2)
			if ( min < 1 || min > max / 2 ) {
				log.warning("MinNodeEntries must be between 1 and MaxNodeEntries / 2");
				min = max / 2;
			}
		}

		maxNodeEntries = max;
		minNodeEntries = min;
		entryStatus = new byte[maxNodeEntries];
		putNode(rootNodeId, new Node(rootNodeId, 1, maxNodeEntries));

		if ( isDebug ) log.fine("init() " + " MaxNodeEntries = " + maxNodeEntries + ", MinNodeEntries = " + minNodeEntries);
	}


	public void clear()
	{
		nodeMap.clear();
		deletedNodeIds.clear();
		parents.clear();
		parentsEntry.clear();
		treeHeight = 1;
		rootNodeId = 0;
		size = 0;
		putNode(rootNodeId, new Node(rootNodeId, 1, maxNodeEntries));
	}


	/**
	 * <b>Transfers</b> all nodes of this instance into a {@link SpatialIndex}.
	 * This tree will be empty afterwards.
	 * <p>
	 * An index is a condensed read-only version that can be faster and uses
	 * less memory, especially if this tree has many deleted nodes.
	 */
	public SpatialIndex toIndex()
	{
		if ( size == 0 ) return new SpatialIndex(new ArrayList<>(), 0, 0);

		int deleted = deletedNodeIds.size();
		while ( !deletedNodeIds.isEmpty() ) {
			int idx = deletedNodeIds.pop();
			if ( idx < nodeMap.size() ) nodeMap.set(idx, null);
		}

		SpatialIndex result;
		if ( size < 128 || deleted == 0 || deleted < size / 10 ) {
			result = new SpatialIndex(nodeMap, rootNodeId, size);
		}
		else {
			// TODO: compact nodemap and rewrite indexes in nodes.
			result = new SpatialIndex(nodeMap, rootNodeId, size);
		}
		nodeMap = new ArrayList<>();
		clear();
		return result;
	}


	/**
	 * Returns the number of entries in the spatial index
	 */
	public int size()
	{
		return size;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void nearest(Spot p, AreaCallback v, float furthestDistance)
	{
		super.nearest(p, v, furthestDistance);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void nearestNUnsorted(Spot p, AreaCallback v, int count, float furthestDistance)
	{
		super.nearestNUnsorted(p, v, count, furthestDistance);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void nearestN(Spot p, AreaCallback v, int count, float furthestDistance)
	{
		super.nearestN(p, v, count, furthestDistance);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void intersects(Area r, AreaCallback v)
	{
		super.intersects(r, v);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void contains(Area r, AreaCallback v)
	{
		super.contains(r, v);
	}


	public void add(SpatialIndex tree)
	{
		for ( Node n : tree.nodes ) {
			if ( n == null || !n.isLeaf() ) continue;
			for ( int i = 0; i < n.entryCount; i++ ) {
				add(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i], n.ids[i], 1);
				size++;
				if ( INTERNAL_CONSISTENCY_CHECKING ) checkConsistency();
			}
		}
	}


	/**
	 * Adds a new rectangle to the spatial index
	 *
	 * @param r The rectangle to add to the spatial index.
	 * @param id The ID of the rectangle to add to the spatial index.
	 *        The result of adding more than one rectangle with
	 *        the same ID is undefined.
	 */
	public void add(Area r, int id)
	{
		if ( isDebug ) log.fine("Adding rectangle " + r + ", id " + id);

		add(r.minX, r.minY, r.maxX, r.maxY, id, 1);
		size++;
		if ( INTERNAL_CONSISTENCY_CHECKING ) checkConsistency();
	}


	/**
	 * Deletes a rectangle from the spatial index
	 *
	 * @param r The rectangle to delete from the spatial index
	 * @param id The ID of the rectangle to delete from the spatial
	 *        index
	 * @return true if the rectangle was deleted
	 *         false if the rectangle was not found, or the
	 *         rectangle was found but with a different ID
	 */
	public boolean delete(Area r, int id)
	{
		// FindLeaf algorithm inlined here. Note the "official" algorithm
		// searches all overlapping entries. This seems inefficient to me,
		// as an entry is only worth searching if it contains (NOT overlaps)
		// the rectangle we are searching for.
		//
		// Also the algorithm has been changed so that it is not recursive.

		// FL1 [Search subtrees] If root is not a leaf, check each entry
		// to determine if it contains r. For each entry found, invoke
		// findLeaf on the node pointed to by the entry, until r is found or
		// all entries have been checked.
		parents.reset();
		parents.push(rootNodeId);

		parentsEntry.reset();
		parentsEntry.push(-1);
		Node n = null;
		int foundIndex = -1; // index of entry to be deleted in leaf

		while ( foundIndex == -1 && parents.size() > 0 ) {
			n = getNode(parents.peek());
			int startIndex = parentsEntry.peek() + 1;

			if ( !n.isLeaf() ) {
				if ( isDebugDel ) logDel.fine("searching node " + n.nodeId + ", from index " + startIndex);
				boolean contains = false;
				for ( int i = startIndex; i < n.entryCount; i++ ) {
					if ( Area.contains(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i],
						r.minX, r.minY, r.maxX, r.maxY) ) {
						parents.push(n.ids[i]);
						parentsEntry.pop();
						parentsEntry.push(i); // this becomes the start index when the child has been searched
						parentsEntry.push(-1);
						contains = true;
						break; // ie go to next iteration of while()
					}
				}
				if ( contains ) {
					continue;
				}
			}
			else {
				foundIndex = n.findEntry(r.minX, r.minY, r.maxX, r.maxY, id);
			}

			parents.pop();
			parentsEntry.pop();
		} // while not found

		if ( foundIndex != -1 && n != null ) {
			n.deleteEntry(foundIndex);
			condenseTree(n);
			size--;
		}

		// shrink the tree if possible (i.e. if root node has exactly one entry,and that
		// entry is not a leaf node, delete the root (it's entry becomes the new root)
		Node root = getNode(rootNodeId);
		while ( root.entryCount == 1 && treeHeight > 1 ) {
			removeNode(rootNodeId);
			root.entryCount = 0;
			rootNodeId = root.ids[0];
			treeHeight--;
			root = getNode(rootNodeId);
		}

		// if the tree is now empty, then set the MBR of the root node back to it's original state
		// (this is only needed when the tree is empty, as this is the only state where an empty node
		// is not eliminated)
		if ( size == 0 ) {
			root.mbrMinX = Float.MAX_VALUE;
			root.mbrMinY = Float.MAX_VALUE;
			root.mbrMaxX = -Float.MAX_VALUE;
			root.mbrMaxY = -Float.MAX_VALUE;
		}

		if ( INTERNAL_CONSISTENCY_CHECKING ) {
			checkConsistency();
		}

		return (foundIndex != -1);
	}


	/**
	 * Adds a new entry at a specified level in the tree
	 */
	private void add(float minX, float minY, float maxX, float maxY, int id, int level)
	{
		// I1 [Find position for new record] Invoke ChooseLeaf to select a
		// leaf node L in which to place r
		Node n = chooseNode(minX, minY, maxX, maxY, level);
		Node newLeaf = null;

		// I2 [Add record to leaf node] If L has room for another entry,
		// install E. Otherwise invoke SplitNode to obtain L and LL containing
		// E and all the old entries of L
		if ( n.entryCount < maxNodeEntries ) {
			n.addEntry(minX, minY, maxX, maxY, id);
		}
		else {
			newLeaf = splitNode(n, minX, minY, maxX, maxY, id);
		}

		// I3 [Propagate changes upwards] Invoke AdjustTree on L, also passing LL
		// if a split was performed
		Node newNode = adjustTree(n, newLeaf);

		// I4 [Grow tree taller] If node split propagation caused the root to
		// split, create a new root whose children are the two resulting nodes.
		if ( newNode != null ) {
			Node oldRoot = getNode(rootNodeId);
			rootNodeId = getNextNodeId();
			treeHeight++;
			Node root = new Node(rootNodeId, treeHeight, maxNodeEntries);
			root.addEntry(newNode.mbrMinX, newNode.mbrMinY, newNode.mbrMaxX, newNode.mbrMaxY, newNode.nodeId);
			root.addEntry(oldRoot.mbrMinX, oldRoot.mbrMinY, oldRoot.mbrMaxX, oldRoot.mbrMaxY, oldRoot.nodeId);
			putNode(rootNodeId, root);
		}
	}


	/**
	 * Split a node. Algorithm is taken pretty much verbatim from
	 * Guttman's original paper.
	 * <p>
	 * Initializes {@link #entryStatus}, which is used by "child methods".
	 *
	 * @return new node object.
	 */
	private Node splitNode(Node n, float newRectMinX, float newRectMinY, float newRectMaxX, float newRectMaxY, int newId)
	{
		// [Pick first entry for each group] Apply algorithm pickSeeds to
		// choose two entries to be the first elements of the groups. Assign
		// each to a group.

		// debug code
		float initialArea = 0;
		if ( isDebug ) {
			float unionMinX = Math.min(n.mbrMinX, newRectMinX);
			float unionMinY = Math.min(n.mbrMinY, newRectMinY);
			float unionMaxX = Math.max(n.mbrMaxX, newRectMaxX);
			float unionMaxY = Math.max(n.mbrMaxY, newRectMaxY);

			initialArea = (unionMaxX - unionMinX) * (unionMaxY - unionMinY);
		}

		Arrays.fill(entryStatus, (byte)ENTRY_STATUS_UNASSIGNED);

		Node newNode = null;
		newNode = new Node(getNextNodeId(), n.level, maxNodeEntries);
		putNode(newNode.nodeId, newNode);

		pickSeeds(n, newRectMinX, newRectMinY, newRectMaxX, newRectMaxY, newId, newNode); // this also sets the entryCount to 1

		// [Check if done] If all entries have been assigned, stop. If one
		// group has so few entries that all the rest must be assigned to it in
		// order for it to have the minimum number m, assign them and stop.
		while ( n.entryCount + newNode.entryCount < maxNodeEntries + 1 ) {
			if ( maxNodeEntries + 1 - newNode.entryCount == minNodeEntries ) {
				// assign all remaining entries to original node
				for ( int i = 0; i < maxNodeEntries; i++ ) {
					if ( entryStatus[i] == ENTRY_STATUS_UNASSIGNED ) {
						entryStatus[i] = ENTRY_STATUS_ASSIGNED;

						if ( n.entriesMinX[i] < n.mbrMinX ) n.mbrMinX = n.entriesMinX[i];
						if ( n.entriesMinY[i] < n.mbrMinY ) n.mbrMinY = n.entriesMinY[i];
						if ( n.entriesMaxX[i] > n.mbrMaxX ) n.mbrMaxX = n.entriesMaxX[i];
						if ( n.entriesMaxY[i] > n.mbrMaxY ) n.mbrMaxY = n.entriesMaxY[i];

						n.entryCount++;
					}
				}
				break;
			}
			if ( maxNodeEntries + 1 - n.entryCount == minNodeEntries ) {
				// assign all remaining entries to new node
				for ( int i = 0; i < maxNodeEntries; i++ ) {
					if ( entryStatus[i] == ENTRY_STATUS_UNASSIGNED ) {
						entryStatus[i] = ENTRY_STATUS_ASSIGNED;
						newNode.addEntry(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i], n.ids[i]);
						n.ids[i] = -1; // an id of -1 indicates the entry is not in use
					}
				}
				break;
			}

			// [Select entry to assign] Invoke algorithm pickNext to choose the
			// next entry to assign. Add it to the group whose covering rectangle
			// will have to be enlarged least to accommodate it. Resolve ties
			// by adding the entry to the group with smaller area, then to the
			// the one with fewer entries, then to either. Repeat from S2
			pickNext(n, newNode);
		}

		n.reorganize(this);

		// check that the MBR stored for each node is correct.
		if ( INTERNAL_CONSISTENCY_CHECKING ) {
			Area nMBR = new Area(n.mbrMinX, n.mbrMinY, n.mbrMaxX, n.mbrMaxY);
			if ( !nMBR.equals(calculateMBR(n)) ) {
				log.severe("Error: splitNode old node MBR wrong");
			}
			Area newNodeMBR = new Area(newNode.mbrMinX, newNode.mbrMinY, newNode.mbrMaxX, newNode.mbrMaxY);
			if ( !newNodeMBR.equals(calculateMBR(newNode)) ) {
				log.severe("Error: splitNode new node MBR wrong");
			}
		}

		// debug code
		if ( isDebug ) {
			float newArea = Area.area(n.mbrMinX, n.mbrMinY, n.mbrMaxX, n.mbrMaxY)
				+ Area.area(newNode.mbrMinX, newNode.mbrMinY, newNode.mbrMaxX, newNode.mbrMaxY);
			float percentageIncrease = (100 * (newArea - initialArea)) / initialArea;
			log.fine("Node " + n.nodeId + " split. New area increased by " + percentageIncrease + "%");
		}

		return newNode;
	}


	/**
	 * Pick the seeds used to split a node.
	 * <p>
	 * Select two entries to be the first elements of the groups.
	 * <p>
	 * Used by {@link #splitNode(Node, float, float, float, float, int)} and
	 * uses {@link #entryStatus}.
	 */
	private void pickSeeds(Node n, float newRectMinX, float newRectMinY, float newRectMaxX, float newRectMaxY, int newId, Node newNode)
	{
		// Find extreme rectangles along all dimension. Along each dimension,
		// find the entry whose rectangle has the highest low side, and the one
		// with the lowest high side. Record the separation.
		float maxNormalizedSeparation = -1; // initialize to -1 so that even overlapping rectangles will be considered for the seeds
		int highestLowIndex = -1;
		int lowestHighIndex = -1;

		// for the purposes of picking seeds, take the MBR of the node to include
		// the new rectangle aswell.
		if ( newRectMinX < n.mbrMinX ) n.mbrMinX = newRectMinX;
		if ( newRectMinY < n.mbrMinY ) n.mbrMinY = newRectMinY;
		if ( newRectMaxX > n.mbrMaxX ) n.mbrMaxX = newRectMaxX;
		if ( newRectMaxY > n.mbrMaxY ) n.mbrMaxY = newRectMaxY;

		float mbrLenX = n.mbrMaxX - n.mbrMinX;
		float mbrLenY = n.mbrMaxY - n.mbrMinY;

		if ( isDebug ) {
			log.fine("pickSeeds(): NodeId = " + n.nodeId);
		}

		float tempHighestLow = newRectMinX;
		int tempHighestLowIndex = -1; // -1 indicates the new rectangle is the seed

		float tempLowestHigh = newRectMaxX;
		int tempLowestHighIndex = -1; // -1 indicates the new rectangle is the seed

		for ( int i = 0; i < n.entryCount; i++ ) {
			float tempLow = n.entriesMinX[i];
			if ( tempLow >= tempHighestLow ) {
				tempHighestLow = tempLow;
				tempHighestLowIndex = i;
			}
			else { // ensure that the same index cannot be both lowestHigh and highestLow
				float tempHigh = n.entriesMaxX[i];
				if ( tempHigh <= tempLowestHigh ) {
					tempLowestHigh = tempHigh;
					tempLowestHighIndex = i;
				}
			}

			// PS2 [Adjust for shape of the rectangle cluster] Normalize the separations
			// by dividing by the widths of the entire set along the corresponding
			// dimension
			float normalizedSeparation = mbrLenX == 0 ? 1 : (tempHighestLow - tempLowestHigh) / mbrLenX;
			if ( normalizedSeparation > 1 || normalizedSeparation < -1 ) {
				log.severe("Invalid normalized separation X");
			}

			if ( isDebug ) {
				log.fine("Entry " + i + ", dimension X: HighestLow = " + tempHighestLow
					+ " (index " + tempHighestLowIndex + ")"
					+ ", LowestHigh = " + tempLowestHigh
					+ " (index " + tempLowestHighIndex
					+ ", NormalizedSeparation = " + normalizedSeparation);
			}

			// PS3 [Select the most extreme pair] Choose the pair with the greatest
			// normalized separation along any dimension.
			// Note that if negative it means the rectangles overlapped. However still include
			// overlapping rectangles if that is the only choice available.
			if ( normalizedSeparation >= maxNormalizedSeparation ) {
				highestLowIndex = tempHighestLowIndex;
				lowestHighIndex = tempLowestHighIndex;
				maxNormalizedSeparation = normalizedSeparation;
			}
		}

		// Repeat for the Y dimension
		tempHighestLow = newRectMinY;
		tempHighestLowIndex = -1; // -1 indicates the new rectangle is the seed

		tempLowestHigh = newRectMaxY;
		tempLowestHighIndex = -1; // -1 indicates the new rectangle is the seed

		for ( int i = 0; i < n.entryCount; i++ ) {
			float tempLow = n.entriesMinY[i];
			if ( tempLow >= tempHighestLow ) {
				tempHighestLow = tempLow;
				tempHighestLowIndex = i;
			}
			else { // ensure that the same index cannot be both lowestHigh and highestLow
				float tempHigh = n.entriesMaxY[i];
				if ( tempHigh <= tempLowestHigh ) {
					tempLowestHigh = tempHigh;
					tempLowestHighIndex = i;
				}
			}

			// PS2 [Adjust for shape of the rectangle cluster] Normalize the separations
			// by dividing by the widths of the entire set along the corresponding
			// dimension
			float normalizedSeparation = mbrLenY == 0 ? 1 : (tempHighestLow - tempLowestHigh) / mbrLenY;
			if ( normalizedSeparation > 1 || normalizedSeparation < -1 ) {
				log.severe("Invalid normalized separation Y");
			}

			if ( isDebug ) {
				log.fine("Entry " + i + ", dimension Y: HighestLow = " + tempHighestLow + " (index " + tempHighestLowIndex + ")"
					+ ", LowestHigh = "
					+ tempLowestHigh + " (index " + tempLowestHighIndex + ", NormalizedSeparation = " + normalizedSeparation);
			}

			// PS3 [Select the most extreme pair] Choose the pair with the greatest
			// normalized separation along any dimension.
			// Note that if negative it means the rectangles overlapped. However still include
			// overlapping rectangles if that is the only choice available.
			if ( normalizedSeparation >= maxNormalizedSeparation ) {
				highestLowIndex = tempHighestLowIndex;
				lowestHighIndex = tempLowestHighIndex;
				maxNormalizedSeparation = normalizedSeparation;
			}
		}

		// At this point it is possible that the new rectangle is both highestLow and lowestHigh.
		// This can happen if all rectangles in the node overlap the new rectangle.
		// Resolve this by declaring that the highestLowIndex is the lowest Y and,
		// the lowestHighIndex is the largest X (but always a different rectangle)
		if ( highestLowIndex == lowestHighIndex ) {
			highestLowIndex = -1;
			lowestHighIndex = 0;
			float tempMinY = newRectMinY;
			float tempMaxX = n.entriesMaxX[0];

			for ( int i = 1; i < n.entryCount; i++ ) {
				if ( n.entriesMinY[i] < tempMinY ) {
					tempMinY = n.entriesMinY[i];
					highestLowIndex = i;
				}
				else if ( n.entriesMaxX[i] > tempMaxX ) {
					tempMaxX = n.entriesMaxX[i];
					lowestHighIndex = i;
				}
			}
		}

		// highestLowIndex is the seed for the new node.
		if ( highestLowIndex == -1 ) {
			newNode.addEntry(newRectMinX, newRectMinY, newRectMaxX, newRectMaxY, newId);
		}
		else {
			newNode.addEntry(n.entriesMinX[highestLowIndex], n.entriesMinY[highestLowIndex],
				n.entriesMaxX[highestLowIndex], n.entriesMaxY[highestLowIndex], n.ids[highestLowIndex]);
			n.ids[highestLowIndex] = -1;

			// move the new rectangle into the space vacated by the seed for the new node
			n.entriesMinX[highestLowIndex] = newRectMinX;
			n.entriesMinY[highestLowIndex] = newRectMinY;
			n.entriesMaxX[highestLowIndex] = newRectMaxX;
			n.entriesMaxY[highestLowIndex] = newRectMaxY;

			n.ids[highestLowIndex] = newId;
		}

		// lowestHighIndex is the seed for the original node.
		if ( lowestHighIndex == -1 ) {
			lowestHighIndex = highestLowIndex;
		}

		entryStatus[lowestHighIndex] = ENTRY_STATUS_ASSIGNED;
		n.entryCount = 1;
		n.mbrMinX = n.entriesMinX[lowestHighIndex];
		n.mbrMinY = n.entriesMinY[lowestHighIndex];
		n.mbrMaxX = n.entriesMaxX[lowestHighIndex];
		n.mbrMaxY = n.entriesMaxY[lowestHighIndex];
	}


	/**
	 * Pick the next entry to be assigned to a group during a node split.
	 * <p>
	 * [Determine cost of putting each entry in each group]
	 * For each entry not yet in a group, calculate the area increase required
	 * in the covering rectangles of each group.
	 * <p>
	 * Used by {@link #splitNode(Node, float, float, float, float, int)} and
	 * uses {@link #entryStatus}.
	 */
	private int pickNext(final Node n, final Node newNode)
	{
		float maxDifference = Float.NEGATIVE_INFINITY;
		int next = 0;
		int nextGroup = 0;

		maxDifference = Float.NEGATIVE_INFINITY;

		if ( isDebug ) log.fine("pickNext()");

		final float rn = Area.area(n.mbrMinX, n.mbrMinY, n.mbrMaxX, n.mbrMaxY);
		final float rnn = Area.area(newNode.mbrMinX, newNode.mbrMinY, newNode.mbrMaxX, newNode.mbrMaxY);

		for ( int i = 0; i < maxNodeEntries; i++ ) {
			if ( entryStatus[i] == ENTRY_STATUS_UNASSIGNED ) {

				if ( n.ids[i] == -1 ) {
					log.severe("Error: Node " + n.nodeId + ", entry " + i + " is null");
				}

				float nIncrease = Area.enlargement(n.mbrMinX, n.mbrMinY, n.mbrMaxX, n.mbrMaxY,
					rn, n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i]);
				float newNodeIncrease = Area.enlargement(newNode.mbrMinX, newNode.mbrMinY, newNode.mbrMaxX, newNode.mbrMaxY,
					rnn, n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i]);

				float difference = Math.abs(nIncrease - newNodeIncrease);

				if ( difference > maxDifference ) {
					next = i;

					if ( nIncrease < newNodeIncrease ) {
						nextGroup = 0;
					}
					else if ( newNodeIncrease < nIncrease ) {
						nextGroup = 1;
					}
					else if ( rn < rnn ) {
						nextGroup = 0;
					}
					else if ( rnn < rn ) {
						nextGroup = 1;
					}
					else if ( newNode.entryCount < maxNodeEntries / 2 ) {
						nextGroup = 0;
					}
					else {
						nextGroup = 1;
					}
					maxDifference = difference;
				}
				if ( isDebug ) {
					log.fine("Entry " + i + " group0 increase = " + nIncrease + ", group1 increase = " + newNodeIncrease + ", diff = "
						+ difference
						+ ", MaxDiff = " + maxDifference + " (entry " + next + ")");
				}
			}
		}

		entryStatus[next] = ENTRY_STATUS_ASSIGNED;

		if ( nextGroup == 0 ) {
			if ( n.entriesMinX[next] < n.mbrMinX ) n.mbrMinX = n.entriesMinX[next];
			if ( n.entriesMinY[next] < n.mbrMinY ) n.mbrMinY = n.entriesMinY[next];
			if ( n.entriesMaxX[next] > n.mbrMaxX ) n.mbrMaxX = n.entriesMaxX[next];
			if ( n.entriesMaxY[next] > n.mbrMaxY ) n.mbrMaxY = n.entriesMaxY[next];
			n.entryCount++;
		}
		else {
			// move to new node.
			newNode.addEntry(n.entriesMinX[next], n.entriesMinY[next], n.entriesMaxX[next], n.entriesMaxY[next], n.ids[next]);
			n.ids[next] = -1;
		}

		return next;
	}


	/**
	 * Ensures that all nodes from the passed node up to the root have the minimum number of entries.
	 * <p>
	 * Used by {@link #delete(Area, int)}, uses {@link #parents} and {@link #parentsEntry},
	 * both stacks are expected to contain the nodeIds of all parents up to the root.
	 */
	private void condenseTree(Node l)
	{
		// CT1 [Initialize] Set n=l. Set the list of eliminated
		// nodes to be empty.
		Node n = l;
		Node parent = null;
		int parentEntry = 0;

		IntArray eliminatedNodeIds = new IntArray();

		// CT2 [Find parent entry] If N is the root, go to CT6. Otherwise
		// let P be the parent of N, and let En be N's entry in P
		while ( n.level != treeHeight ) {
			parent = getNode(parents.pop());
			parentEntry = parentsEntry.pop();

			// CT3 [Eliminiate under-full node] If N has too few entries,
			// delete En from P and add N to the list of eliminated nodes
			if ( n.entryCount < minNodeEntries ) {
				parent.deleteEntry(parentEntry);
				eliminatedNodeIds.push(n.nodeId);
			}
			else {
				// CT4 [Adjust covering rectangle] If N has not been eliminated,
				// adjust EnI to tightly contain all entries in N
				if ( n.mbrMinX != parent.entriesMinX[parentEntry] || n.mbrMinY != parent.entriesMinY[parentEntry]
					|| n.mbrMaxX != parent.entriesMaxX[parentEntry] || n.mbrMaxY != parent.entriesMaxY[parentEntry] ) {
					float deletedMinX = parent.entriesMinX[parentEntry];
					parent.entriesMinX[parentEntry] = n.mbrMinX;
					float deletedMinY = parent.entriesMinY[parentEntry];
					parent.entriesMinY[parentEntry] = n.mbrMinY;
					float deletedMaxX = parent.entriesMaxX[parentEntry];
					parent.entriesMaxX[parentEntry] = n.mbrMaxX;
					float deletedMaxY = parent.entriesMaxY[parentEntry];
					parent.entriesMaxY[parentEntry] = n.mbrMaxY;
					parent.recalculateMBRIfInfluencedBy(deletedMinX, deletedMinY, deletedMaxX, deletedMaxY);
				}
			}
			// CT5 [Move up one level in tree] Set N=P and repeat from CT2
			n = parent;
		}

		// CT6 [Reinsert orphaned entries] Reinsert all entries of nodes in set Q.
		// Entries from eliminated leaf nodes are reinserted in tree leaves as in
		// Insert(), but entries from higher level nodes must be placed higher in
		// the tree, so that leaves of their dependent subtrees will be on the same
		// level as leaves of the main tree
		while ( eliminatedNodeIds.size() > 0 ) {
			Node e = getNode(eliminatedNodeIds.pop());
			for ( int j = 0; j < e.entryCount; j++ ) {
				add(e.entriesMinX[j], e.entriesMinY[j], e.entriesMaxX[j], e.entriesMaxY[j], e.ids[j], e.level);
				e.ids[j] = -1;
			}
			e.entryCount = 0;
			removeNode(e.nodeId);
		}
	}


	/**
	 * Chooses a leaf to add the rectangle to.
	 * <p>
	 * Used by {@link #add(float, float, float, float, int, int)},
	 * initializes {@link #parents} and {@link #parentsEntry}.
	 */
	private Node chooseNode(final float minX, final float minY, final float maxX, final float maxY, int level)
	{
		// CL1 [Initialize] Set N to be the root node
		Node n = getNode(rootNodeId);
		parents.reset();
		parentsEntry.reset();

		// CL2 [Leaf check] If N is a leaf, return N
		while ( true ) {
			if ( n == null ) {
				log.severe("Could not get root node (" + rootNodeId + ")");
			}

			if ( n.level == level ) {
				return n;
			}

			// CL3 [Choose subtree] If N is not at the desired level, let F be the entry in N
			// whose rectangle FI needs least enlargement to include EI. Resolve
			// ties by choosing the entry with the rectangle of smaller area.
			float areaNIndex = Area.area(n.entriesMinX[0], n.entriesMinY[0], n.entriesMaxX[0], n.entriesMaxY[0]);
			float leastEnlargement = Area.enlargement(n.entriesMinX[0], n.entriesMinY[0], n.entriesMaxX[0], n.entriesMaxY[0],
				areaNIndex, minX, minY, maxX, maxY);
			int index = 0; // index of rectangle in subtree
			for ( int i = 1; i < n.entryCount; i++ ) {
				float tempMinX = n.entriesMinX[i];
				float tempMinY = n.entriesMinY[i];
				float tempMaxX = n.entriesMaxX[i];
				float tempMaxY = n.entriesMaxY[i];
				float tempArea = Area.area(tempMinX, tempMinY, tempMaxX, tempMaxY);
				float tempEnlargement = Area.enlargement(tempMinX, tempMinY, tempMaxX, tempMaxY, tempArea, minX, minY, maxX, maxY);
				if ( (tempEnlargement < leastEnlargement) || ((tempEnlargement == leastEnlargement) && (tempArea < areaNIndex)) ) {
					index = i;
					areaNIndex = tempArea;
					leastEnlargement = tempEnlargement;
				}
			}

			parents.push(n.nodeId);
			parentsEntry.push(index);

			// CL4 [Descend until a leaf is reached] Set N to be the child node
			// pointed to by Fp and repeat from CL2
			n = getNode(n.ids[index]);
		}
	}


	/**
	 * Ascend from a leaf node L to the root, adjusting covering rectangles and
	 * propagating node splits as necessary.
	 * <p>
	 * Used by {@link #add(float, float, float, float, int, int)}, uses {@link #parents} and {@link #parentsEntry}.
	 */
	private Node adjustTree(Node n, Node nn)
	{
		// AT1 [Initialize] Set N=L. If L was split previously, set NN to be
		// the resulting second node.

		// AT2 [Check if done] If N is the root, stop
		while ( n.level != treeHeight ) {

			// AT3 [Adjust covering rectangle in parent entry] Let P be the parent
			// node of N, and let En be N's entry in P. Adjust EnI so that it tightly
			// encloses all entry rectangles in N.
			Node parent = getNode(parents.pop());
			int entry = parentsEntry.pop();

			if ( parent.ids[entry] != n.nodeId ) {
				log.severe("Error: entry " + entry + " in node " + parent.nodeId + " should point to node "
					+ n.nodeId + "; actually points to node " + parent.ids[entry]);
			}

			if ( parent.entriesMinX[entry] != n.mbrMinX || parent.entriesMinY[entry] != n.mbrMinY
				|| parent.entriesMaxX[entry] != n.mbrMaxX || parent.entriesMaxY[entry] != n.mbrMaxY ) {

				parent.entriesMinX[entry] = n.mbrMinX;
				parent.entriesMinY[entry] = n.mbrMinY;
				parent.entriesMaxX[entry] = n.mbrMaxX;
				parent.entriesMaxY[entry] = n.mbrMaxY;

				parent.recalculateMBR();
			}

			// AT4 [Propagate node split upward] If N has a partner NN resulting from
			// an earlier split, create a new entry Enn with Ennp pointing to NN and
			// Enni enclosing all rectangles in NN. Add Enn to P if there is room.
			// Otherwise, invoke splitNode to produce P and PP containing Enn and
			// all P's old entries.
			Node newNode = null;
			if ( nn != null ) {
				if ( parent.entryCount < maxNodeEntries ) {
					parent.addEntry(nn.mbrMinX, nn.mbrMinY, nn.mbrMaxX, nn.mbrMaxY, nn.nodeId);
				}
				else {
					newNode = splitNode(parent, nn.mbrMinX, nn.mbrMinY, nn.mbrMaxX, nn.mbrMaxY, nn.nodeId);
				}
			}

			// AT5 [Move up to next level] Set N = P and set NN = PP if a split
			// occurred. Repeat from AT2
			n = parent;
			nn = newNode;

			parent = null;
			newNode = null;
		}

		return nn;
	}


	/**
	 * Given a node object, calculate the node MBR from it's entries.
	 * Used in consistency checking
	 */
	private Area calculateMBR(Node n)
	{
		Area mbr = new Area();

		for ( int i = 0; i < n.entryCount; i++ ) {
			if ( n.entriesMinX[i] < mbr.minX ) mbr.minX = n.entriesMinX[i];
			if ( n.entriesMinY[i] < mbr.minY ) mbr.minY = n.entriesMinY[i];
			if ( n.entriesMaxX[i] > mbr.maxX ) mbr.maxX = n.entriesMaxX[i];
			if ( n.entriesMaxY[i] > mbr.maxY ) mbr.maxY = n.entriesMaxY[i];
		}
		return mbr;
	}


	/**
	 * Check the consistency of the tree.
	 *
	 * @return false if an inconsistency is detected, true otherwise.
	 */
	public boolean checkConsistency()
	{
		return checkConsistency(rootNodeId, treeHeight, null);
	}


	private boolean checkConsistency(int nodeId, int expectedLevel, Area expectedMBR)
	{
		// go through the tree, and check that the internal data structures of
		// the tree are not corrupted.
		Node n = getNode(nodeId);

		if ( n == null ) {
			log.severe("Error: Could not read node " + nodeId);
			return false;
		}

		// if tree is empty, then there should be exactly one node, at level 1
		// TODO: also check the MBR is as for a new node
		if ( nodeId == rootNodeId && size() == 0 ) {
			if ( n.level != 1 ) {
				log.severe("Error: tree is empty but root node is not at level 1");
				return false;
			}
		}

		if ( n.level != expectedLevel ) {
			log.severe("Error: Node " + nodeId + ", expected level " + expectedLevel + ", actual level " + n.level);
			return false;
		}

		Area calculatedMBR = calculateMBR(n);
		Area actualMBR = new Area();
		actualMBR.minX = n.mbrMinX;
		actualMBR.minY = n.mbrMinY;
		actualMBR.maxX = n.mbrMaxX;
		actualMBR.maxY = n.mbrMaxY;
		if ( !actualMBR.equals(calculatedMBR) ) {
			log.severe("Error: Node " + nodeId + ", calculated MBR does not equal stored MBR");
			if ( actualMBR.minX != n.mbrMinX ) log.severe("  actualMinX=" + actualMBR.minX + ", calc=" + calculatedMBR.minX);
			if ( actualMBR.minY != n.mbrMinY ) log.severe("  actualMinY=" + actualMBR.minY + ", calc=" + calculatedMBR.minY);
			if ( actualMBR.maxX != n.mbrMaxX ) log.severe("  actualMaxX=" + actualMBR.maxX + ", calc=" + calculatedMBR.maxX);
			if ( actualMBR.maxY != n.mbrMaxY ) log.severe("  actualMaxY=" + actualMBR.maxY + ", calc=" + calculatedMBR.maxY);
			return false;
		}

		if ( expectedMBR != null && !actualMBR.equals(expectedMBR) ) {
			log.severe("Error: Node " + nodeId + ", expected MBR (from parent) does not equal stored MBR");
			return false;
		}

		// Check for corruption where a parent entry is the same object as the child MBR
		if ( expectedMBR != null && actualMBR.sameObject(expectedMBR) ) {
			log.severe("Error: Node " + nodeId + " MBR using same rectangle object as parent's entry");
			return false;
		}

		for ( int i = 0; i < n.entryCount; i++ ) {
			if ( n.ids[i] == -1 ) {
				log.severe("Error: Node " + nodeId + ", Entry " + i + " is null");
				return false;
			}

			if ( n.level > 1 ) { // if not a leaf
				if ( !checkConsistency(n.ids[i], n.level - 1,
					new Area(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i])) ) {
					return false;
				}
			}
		}
		return true;
	}


	@Override
	protected int getRoodNodeId()
	{
		return rootNodeId;
	}


	@Override
	protected Node getNode(int id)
	{
		return id < 0 || id >= nodeMap.size() ? null : nodeMap.get(id);
	}


	/**
	 * Get the next available node ID. Reuse deleted node IDs if possible.
	 */
	private int getNextNodeId()
	{
		return deletedNodeIds.isEmpty() ? nodeMap.size() : deletedNodeIds.pop();
	}


	private void putNode(int id, Node node)
	{
		if ( id == nodeMap.size() ) nodeMap.add(node);
		else nodeMap.set(id, node);
	}


	private void removeNode(int id)
	{
		deletedNodeIds.push(id);
		// if ( id!=nodeMap.size() ) nodeMap.set(id, null);
	}

}
