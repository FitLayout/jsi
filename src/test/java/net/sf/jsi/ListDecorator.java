//   ListDecorator.java
//   Java Spatial Index Library
//   Copyright (C) 2002-2005 Infomatiq Limited.
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
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA

package net.sf.jsi;

import java.util.ArrayList;
import java.util.List;


/**
 * ListDecorator
 */
public class ListDecorator
{
	RTree m_si = null;


	public ListDecorator(RTree si)
	{
		m_si = si;
	}


	class AddToListProcedure implements AreaCallback
	{
		private List<Integer> m_list = new ArrayList<Integer>();


		@Override
		public boolean processArea(int id)
		{
			m_list.add(id);
			return true;
		}


		public List<Integer> getList()
		{
			return m_list;
		}
	}


	/**
	 * Finds all rectangles that are nearest to the passed
	 * rectangle.
	 * 
	 * @param p The p point which this method finds
	 *        the nearest neighbours.
	 * @return List of IDs of rectangles that are nearest
	 *         to the passed rectangle, ordered by distance (nearest first).
	 */
	public List<Integer> nearest(Spot p, float furthestDistance)
	{
		AddToListProcedure v = new AddToListProcedure();
		m_si.nearest(p, v, furthestDistance);
		return v.getList();
	}


	/**
	 * Finds all rectangles that are nearest to the passed
	 * rectangle.
	 * 
	 * @param p The p point which this method finds
	 *        the nearest neighbours.
	 * @return List of IDs of rectangles that are nearest
	 *         to the passed rectangle, ordered by distance (nearest first).
	 *         If multiple rectangles have the same distance, order by ID.
	 */
	public List<Integer> nearestN(Spot p, int maxCount, float furthestDistance)
	{
		AddToListProcedure v = new AddToListProcedure();
		m_si.nearestN(p, v, maxCount, furthestDistance);
		return v.getList();
	}


	/**
	 * Finds all rectangles that intersect the passed rectangle.
	 * 
	 * @param r The rectangle for which this method finds
	 *        intersecting rectangles.
	 * @return List of IDs of rectangles that intersect the passed
	 *         rectangle.
	 */
	public List<Integer> intersects(Area r)
	{
		AddToListProcedure v = new AddToListProcedure();
		m_si.intersects(r, v);
		return v.getList();
	}


	/**
	 * Finds all rectangles contained by the passed rectangle.
	 * 
	 * @param r The rectangle for which this method finds
	 *        contained rectangles.
	 * @return Collection of IDs of rectangles that are contained by the
	 *         passed rectangle.
	 */
	public List<Integer> contains(Area r)
	{
		AddToListProcedure v = new AddToListProcedure();
		m_si.contains(r, v);
		return v.getList();
	}

}
