//   SpatialIndex.java
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
import java.util.List;


/**
 * This is a lightweight RTree read-only lookup implementation,
 * specifically designed for the following features (in order of importance):
 * <ul>
 * <li>Fast intersection query performance. To achieve this, it uses
 * only main memory to store entries. Obviously this will only improve
 * performance if there is enough physical memory to avoid paging.</li>
 * <li>Low memory requirements.</li>
 * </ul>
 * <p>
 * The main reason for the high speed of this implementation is the
 * avoidance of the creation of unnecessary objects, mainly achieved by using
 * primitive collections.
 */
public class SpatialIndex extends RTreeBase implements Serializable
{
	private static final long serialVersionUID = -2492166776316792002L;

	final List<Node> nodes;
	private final int rootNodeId;
	private final int size;


	SpatialIndex(List<Node> nodes, int rootNodeId, int size)
	{
		this.nodes = nodes;
		this.rootNodeId = rootNodeId;
		this.size = size;
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


	@Override
	protected int getRoodNodeId()
	{
		return rootNodeId;
	}


	@Override
	protected Node getNode(int id)
	{
		return id < 0 || id >= nodes.size() ? null : nodes.get(id);
	}

}
