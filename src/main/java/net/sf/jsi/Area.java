//   Rectangle.java
//   Java Spatial Index Library
//   Copyright (C) 2002-2005 Infomatiq Limited
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

/**
 * Currently hardcoded to 2 dimensions, but could be extended.
 */
public class Area
{

	/**
	 * use primitives instead of arrays for the coordinates of the area,
	 * to reduce memory requirements.
	 */
	public float minX, minY, maxX, maxY;


	public Area()
	{
		minX = Float.MAX_VALUE;
		minY = Float.MAX_VALUE;
		maxX = -Float.MAX_VALUE;
		maxY = -Float.MAX_VALUE;
	}


	/**
	 * Constructor.
	 *
	 * @param x1 coordinate of any corner of the area
	 * @param y1 (see x1)
	 * @param x2 coordinate of the opposite corner
	 * @param y2 (see x2)
	 */
	public Area(float x1, float y1, float x2, float y2)
	{
		set(x1, y1, x2, y2);
	}


	/**
	 * Sets the size of the area.
	 *
	 * @param x1 coordinate of any corner of the area
	 * @param y1 (see x1)
	 * @param x2 coordinate of the opposite corner
	 * @param y2 (see x2)
	 */
	public void set(float x1, float y1, float x2, float y2)
	{
		minX = Math.min(x1, x2);
		maxX = Math.max(x1, x2);
		minY = Math.min(y1, y2);
		maxY = Math.max(y1, y2);
	}


	/**
	 * Sets the size of this area to equal the passed area.
	 */
	public void set(Area r)
	{
		minX = r.minX;
		minY = r.minY;
		maxX = r.maxX;
		maxY = r.maxY;
	}


	/**
	 * Make a copy of this area
	 *
	 * @return copy of this area
	 */
	public Area copy()
	{
		return new Area(minX, minY, maxX, maxY);
	}


	/**
	 * Determine whether an edge of this area overlies the equivalent
	 * edge of the passed area
	 */
	public boolean edgeOverlaps(Area r)
	{
		return minX == r.minX || maxX == r.maxX || minY == r.minY || maxY == r.maxY;
	}


	/**
	 * Determine whether this area intersects the passed area
	 *
	 * @param r The area that might intersect this area
	 *
	 * @return true if the areas intersect, false if they do not intersect
	 */
	public boolean intersects(Area r)
	{
		return maxX >= r.minX && minX <= r.maxX && maxY >= r.minY && minY <= r.maxY;
	}


	/**
	 * Determine whether or not two areas intersect
	 *
	 * @param r1MinX minimum X coordinate of area 1
	 * @param r1MinY minimum Y coordinate of area 1
	 * @param r1MaxX maximum X coordinate of area 1
	 * @param r1MaxY maximum Y coordinate of area 1
	 * @param r2MinX minimum X coordinate of area 2
	 * @param r2MinY minimum Y coordinate of area 2
	 * @param r2MaxX maximum X coordinate of area 2
	 * @param r2MaxY maximum Y coordinate of area 2
	 *
	 * @return true if r1 intersects r2, false otherwise.
	 */
	static public boolean intersects(float r1MinX, float r1MinY, float r1MaxX, float r1MaxY, float r2MinX, float r2MinY, float r2MaxX, float r2MaxY)
	{
		return r1MaxX >= r2MinX && r1MinX <= r2MaxX && r1MaxY >= r2MinY && r1MinY <= r2MaxY;
	}


	/**
	 * Determine whether this area contains the passed area
	 *
	 * @param r The area that might be contained by this area
	 *
	 * @return true if this area contains the passed area, false if
	 *         it does not
	 */
	public boolean contains(Area r)
	{
		return maxX >= r.maxX && minX <= r.minX && maxY >= r.maxY && minY <= r.minY;
	}


	/**
	 * Determine whether or not one area contains another.
	 *
	 * @param r1MinX minimum X coordinate of area 1
	 * @param r1MinY minimum Y coordinate of area 1
	 * @param r1MaxX maximum X coordinate of area 1
	 * @param r1MaxY maximum Y coordinate of area 1
	 * @param r2MinX minimum X coordinate of area 2
	 * @param r2MinY minimum Y coordinate of area 2
	 * @param r2MaxX maximum X coordinate of area 2
	 * @param r2MaxY maximum Y coordinate of area 2
	 *
	 * @return true if r1 contains r2, false otherwise.
	 */
	static public boolean contains(float r1MinX, float r1MinY, float r1MaxX, float r1MaxY, float r2MinX, float r2MinY, float r2MaxX, float r2MaxY)
	{
		return r1MaxX >= r2MaxX && r1MinX <= r2MinX && r1MaxY >= r2MaxY && r1MinY <= r2MinY;
	}


	/**
	 * Determine whether this area is contained by the passed area
	 *
	 * @param r The area that might contain this area
	 *
	 * @return true if the passed area contains this area, false if
	 *         it does not
	 */
	public boolean containedBy(Area r)
	{
		return r.maxX >= maxX && r.minX <= minX && r.maxY >= maxY && r.minY <= minY;
	}


	/**
	 * Return the distance between this area and the passed point.
	 * If the area contains the point, the distance is zero.
	 *
	 * @param p Point to find the distance to
	 *
	 * @return distance beween this area and the passed point.
	 */
	public float distance(Spot p)
	{
		float distanceSquared = 0;

		float temp = minX - p.x;
		if ( temp < 0 ) {
			temp = p.x - maxX;
		}

		if ( temp > 0 ) {
			distanceSquared += (temp * temp);
		}

		temp = minY - p.y;
		if ( temp < 0 ) {
			temp = p.y - maxY;
		}

		if ( temp > 0 ) {
			distanceSquared += (temp * temp);
		}

		return (float)Math.sqrt(distanceSquared);
	}


	/**
	 * Return the distance between a area and a point.
	 * If the area contains the point, the distance is zero.
	 *
	 * @param minX minimum X coordinate of area
	 * @param minY minimum Y coordinate of area
	 * @param maxX maximum X coordinate of area
	 * @param maxY maximum Y coordinate of area
	 * @param pX X coordinate of point
	 * @param pY Y coordinate of point
	 *
	 * @return distance beween this area and the passed point.
	 */
	static public float distance(float minX, float minY, float maxX, float maxY, float pX, float pY)
	{
		return (float)Math.sqrt(distanceSq(minX, minY, maxX, maxY, pX, pY));
	}


	static public float distanceSq(float minX, float minY, float maxX, float maxY, float pX, float pY)
	{
		float distanceSqX = 0;
		float distanceSqY = 0;

		if ( minX > pX ) {
			distanceSqX = minX - pX;
			distanceSqX *= distanceSqX;
		} else if ( pX > maxX ) {
			distanceSqX = pX - maxX;
			distanceSqX *= distanceSqX;
		}

		if ( minY > pY ) {
			distanceSqY = minY - pY;
			distanceSqY *= distanceSqY;
		} else if ( pY > maxY ) {
			distanceSqY = pY - maxY;
			distanceSqY *= distanceSqY;
		}

		return distanceSqX + distanceSqY;
	}


	/**
	 * Return the distance between this area and the passed area.
	 * If the areas overlap, the distance is zero.
	 *
	 * @param r Rectangle to find the distance to
	 *
	 * @return distance between this area and the passed area
	 */

	public float distance(Area r)
	{
		float distanceSquared = 0;
		float greatestMin = Math.max(minX, r.minX);
		float leastMax = Math.min(maxX, r.maxX);
		if ( greatestMin > leastMax ) {
			distanceSquared += ((greatestMin - leastMax) * (greatestMin - leastMax));
		}
		greatestMin = Math.max(minY, r.minY);
		leastMax = Math.min(maxY, r.maxY);
		if ( greatestMin > leastMax ) {
			distanceSquared += ((greatestMin - leastMax) * (greatestMin - leastMax));
		}
		return (float)Math.sqrt(distanceSquared);
	}


	/**
	 * Calculate the area by which this area would be enlarged if
	 * added to the passed area. Neither area is altered.
	 *
	 * @param r Rectangle to union with this area, in order to
	 *        compute the difference in area of the union and the
	 *        original area
	 *
	 * @return enlargement
	 */
	public float enlargement(Area r)
	{
		float enlargedArea = (Math.max(maxX, r.maxX) - Math.min(minX, r.minX)) * (Math.max(maxY, r.maxY) - Math.min(minY, r.minY));

		return enlargedArea - area();
	}


	/**
	 * Calculate the area by which a area would be enlarged if
	 * added to the passed area..
	 *
	 * @param r1MinX minimum X coordinate of area 1
	 * @param r1MinY minimum Y coordinate of area 1
	 * @param r1MaxX maximum X coordinate of area 1
	 * @param r1MaxY maximum Y coordinate of area 1
	 * @param r2MinX minimum X coordinate of area 2
	 * @param r2MinY minimum Y coordinate of area 2
	 * @param r2MaxX maximum X coordinate of area 2
	 * @param r2MaxY maximum Y coordinate of area 2
	 *
	 * @return enlargement
	 */
	static public float enlargement(float r1MinX, float r1MinY, float r1MaxX, float r1MaxY, float r2MinX, float r2MinY, float r2MaxX, float r2MaxY)
	{
		float r1Area = (r1MaxX - r1MinX) * (r1MaxY - r1MinY);

		if ( r1Area == Float.POSITIVE_INFINITY ) {
			return 0; // cannot enlarge an infinite area...
		}

		if ( r2MinX < r1MinX ) r1MinX = r2MinX;
		if ( r2MinY < r1MinY ) r1MinY = r2MinY;
		if ( r2MaxX > r1MaxX ) r1MaxX = r2MaxX;
		if ( r2MaxY > r1MaxY ) r1MaxY = r2MaxY;

		float r1r2UnionArea = (r1MaxX - r1MinX) * (r1MaxY - r1MinY);

		if ( r1r2UnionArea == Float.POSITIVE_INFINITY ) {
			// if a finite area is enlarged and becomes infinite,
			// then the enlargement must be infinite.
			return Float.POSITIVE_INFINITY;
		}
		return r1r2UnionArea - r1Area;
	}


	/**
	 * Compute the area of this area.
	 *
	 * @return The area of this area
	 */
	public float area()
	{
		return (maxX - minX) * (maxY - minY);
	}


	/**
	 * Compute the area of a area.
	 *
	 * @param minX the minimum X coordinate of the area
	 * @param minY the minimum Y coordinate of the area
	 * @param maxX the maximum X coordinate of the area
	 * @param maxY the maximum Y coordinate of the area
	 *
	 * @return The area of the area
	 */
	static public float area(float minX, float minY, float maxX, float maxY)
	{
		return (maxX - minX) * (maxY - minY);
	}


	/**
	 * Computes the union of this area and the passed area, storing
	 * the result in this area.
	 *
	 * @param r Rectangle to add to this area
	 */
	public void add(Area r)
	{
		if ( r.minX < minX ) minX = r.minX;
		if ( r.maxX > maxX ) maxX = r.maxX;
		if ( r.minY < minY ) minY = r.minY;
		if ( r.maxY > maxY ) maxY = r.maxY;
	}


	/**
	 * Computes the union of this area and the passed point, storing
	 * the result in this area.
	 *
	 * @param p Point to add to this area
	 */
	public void add(Spot p)
	{
		if ( p.x < minX ) minX = p.x;
		if ( p.x > maxX ) maxX = p.x;
		if ( p.y < minY ) minY = p.y;
		if ( p.y > maxY ) maxY = p.y;
	}


	/**
	 * Find the the union of this area and the passed area.
	 * Neither area is altered
	 *
	 * @param r The area to union with this area
	 */
	public Area union(Area r)
	{
		Area union = this.copy();
		union.add(r);
		return union;
	}


	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(this.maxX);
		result = prime * result + Float.floatToIntBits(this.maxY);
		result = prime * result + Float.floatToIntBits(this.minX);
		result = prime * result + Float.floatToIntBits(this.minY);
		return result;
	}


	/**
	 * Determine whether this area is equal to a given object.
	 * Equality is determined by the bounds of the area.
	 *
	 * @param o The object to compare with this area
	 */
	@Override
	public boolean equals(Object o)
	{
		boolean equals = false;
		if ( o instanceof Area ) {
			Area r = (Area)o;
			if ( minX == r.minX && minY == r.minY && maxX == r.maxX && maxY == r.maxY ) {
				equals = true;
			}
		}
		return equals;
	}


	/**
	 * Determine whether this area is the same as another object
	 *
	 * Note that two areas can be equal but not the same object,
	 * if they both have the same bounds.
	 *
	 * @param o The object to compare with this area.
	 */
	public boolean sameObject(Object o)
	{
		return super.equals(o);
	}


	/**
	 * Return a string representation of this area, in the form:
	 * (1.2, 3.4), (5.6, 7.8)
	 *
	 * @return String String representation of this area.
	 */
	@Override
	public String toString()
	{
		return "(" + minX + ", " + minY + "), (" + maxX + ", " + maxY + ")";
	}


	/**
	 * Utility methods (not used by JSI); added to
	 * enable this to be used as a generic area class
	 */
	public float width()
	{
		return maxX - minX;
	}


	public float height()
	{
		return maxY - minY;
	}


	public float aspectRatio()
	{
		return width() / height();
	}


	public Spot centre()
	{
		return new Spot((minX + maxX) / 2, (minY + maxY) / 2);
	}

}
