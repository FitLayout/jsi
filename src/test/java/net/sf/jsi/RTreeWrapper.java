//   RTreeWrapper.java
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

import gnu.trove.procedure.TIntProcedure;

import java.util.Properties;

/**
 * A completely useless wrapper class for the RTree class.
 * 
 * Actually the point to introduce the same overhead as 
 * the SILWrapper class, so that performance comparisons
 * can be made.
 */
public class RTreeWrapper implements SpatialIndex {
  private RTree tree;
  
  class IntProcedure2 implements TIntProcedure {
    private TIntProcedure m_intProcedure = null;
    
    public IntProcedure2(TIntProcedure ip) {
      m_intProcedure = ip;
    }
    
    public boolean execute(int i) {
      return m_intProcedure.execute(i);
    }
  }
  
  /**
   * @see net.sf.jsi.SpatialIndex#init(Properties)
   */
  public void init(Properties props) {
    // create a memory-based storage manager
    
    tree = new RTree();
    tree.init(props);
  }

  /**
   * @see net.sf.jsi.SpatialIndex#nearest(Spot, gnu.trove.TIntProcedure, float)
   */
  public void nearest(Spot p, TIntProcedure v, float furthestDistance) {
    tree.nearest(new Spot(p.x, p.y),
                 new IntProcedure2(v),
                 Float.POSITIVE_INFINITY);
  }
  
  /**
   * @see net.sf.jsi.SpatialIndex#nearestN(Spot, gnu.trove.TIntProcedure, int, float)
   */
  public void nearestN(Spot p, TIntProcedure v, int n, float furthestDistance) {
    tree.nearestN(new Spot(p.x, p.y),
                 new IntProcedure2(v),
                 n,
                 furthestDistance);
  }

  /**
   * @see net.sf.jsi.SpatialIndex#nearestNUnsorted(Spot, gnu.trove.TIntProcedure, int, float)
   */
  public void nearestNUnsorted(Spot p, TIntProcedure v, int n, float furthestDistance) {
    tree.nearestNUnsorted(new Spot(p.x, p.y),
                 new IntProcedure2(v),
                 n,
                 furthestDistance);
  }
  
  /**
   * @see net.sf.jsi.SpatialIndex#intersects(Area, gnu.trove.TIntProcedure)
   */
  public void intersects(Area r, TIntProcedure ip) {
    Area r2 = new Area(r.minX, r.minY, r.maxX, r.maxY);  
    tree.intersects(r2, new IntProcedure2(ip));
  }

  /**
   * @see net.sf.jsi.SpatialIndex#contains(Area, gnu.trove.TIntProcedure)
   */
  public void contains(Area r, TIntProcedure ip) {
    Area r2 = new Area(r.minX, r.minY, r.maxX, r.maxY);
    tree.contains(r2, new IntProcedure2(ip));
  }

  /**
   * @see net.sf.jsi.SpatialIndex#add(Area, int)
   */
  public void add(Area r, int id) {
    Area r2 = new Area(r.minX, r.minY, r.maxX, r.maxY);
    tree.add(r2, id);
  }

  /**
   * @see net.sf.jsi.SpatialIndex#delete(Area, int)
   */
  public boolean delete(Area r, int id) {
    Area r2 = new Area(r.minX, r.minY, r.maxX, r.maxY);
    return tree.delete(r2, id);
  }

  /**
   * @see net.sf.jsi.SpatialIndex#size()
   */
  public int size() {
    return tree.size();  
  }
  
  /**
   * @see net.sf.jsi.SpatialIndex#getBounds()
   */
  public Area getBounds() {
    return tree.getBounds(); 
  }
  
  /**
   * @see net.sf.jsi.SpatialIndex#getVersion()
   */
  public String getVersion() {
    return "RTreeWrapper-" + BuildProperties.getVersion();
  }
  
}
