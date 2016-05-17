package com.crystalwink.crystals.TilePrintableAndroid;

//import com.crystalwink.crystals.GraphPanel.GraphPanel;
import com.crystalwink.crystalscommon.Tile.Tile;
//import com.crystalwink.crystals.Segment.Segment;
//import com.crystalwink.crystals.TilePlacementInfo.*;
//import android.graphics.Color;
//import java.awt.Color;
//import java.awt.Graphics;

//import android.app.Activity;
//import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
//import android.graphics.Rect;
//import android.graphics.drawable.shapes.Shape;
//import android.os.Bundle;
//import android.view.View;
//import com.crystalwink.crystals.Segment.*;
//import com.crystalwink.crystals.Tile.*;
import android.util.Log;


/**
 *
 * Tile defines the basic tile representative of the eternatyII tile with AWT Graphics Print capability.
 *
 * @author Dean Clark deancl
 */
public class TilePrintableAndroid extends Tile {
	private static final String TAG = "TilePrintable";

    static final int debugMaskAll       = 0x01;
    static final int debugMaskStartEnd  = 0x02;
    static final int debugMaskInit      = 0x04;
    static final int debugMaskPrintTile = 0x08;
    static final int debugMaskSetGet    = 0x10;
    static final int debugMaskInTransit = 0x20;
//    static final int debugMask = 0;
//    static final int debugMask = debugMaskAll + debugMaskStartEnd + debugMaskInTransit;
    static final int debugMask = debugMaskInTransit;
	
    Path tileSegmentPath[] = new Path[4];

	// Related to WorldSpace
    private boolean inTransit = false;    // indication that the tile is in the process of being moved and InTransite coordinates should be used.
    private boolean firstPrint = true;
    public boolean moveComplete = false;

    // record vector points during a move operation
    public long xWorldSpaceInTransitFrom = 0;
    public long yWorldSpaceInTransitFrom = 0;
    private long xWorldSpaceInTransitTouchPoint = 0;
    private long yWorldSpaceInTransitTouchPoint = 0;
    public long xWorldSpaceInTransitOffset = 0;
    public long yWorldSpaceInTransitOffset = 0;

    public long getWorldSpaceX() {
    	return Math.round(xWorldSpace);
    }
    public void setWorldSpaceX(long update) {
    	xWorldSpace = update;
    	
        if((debugMask & (debugMaskAll | debugMaskSetGet)) > 0)
        	Log.v(TAG, "setWorldSpaceX() " + update);
    }
    public long getWorldSpaceY() {
    	return Math.round(yWorldSpace);
    }
    public void setWorldSpaceY(long update) {
    	yWorldSpace = update;
    	
        if((debugMask & (debugMaskAll | debugMaskSetGet)) > 0)
        	Log.v(TAG, "setWorldSpaceY() " + update);
    }

    public long getWorldSpaceInTransitFromX() {
    	return xWorldSpaceInTransitFrom;
    }
    public void setWorldSpaceInTransitFromX(long update) {
    	xWorldSpaceInTransitFrom = update;

    	if((debugMask & (debugMaskAll | debugMaskSetGet)) > 0)
        	Log.v(TAG, "setWorldSpaceInTransitFromX() " + update);
    }
    public long getWorldSpaceInTransitFromY() {
    	return yWorldSpaceInTransitFrom;
    }
    public void setWorldSpaceInTransitFromY(long update) {
    	yWorldSpaceInTransitFrom = update;

    	if((debugMask & (debugMaskAll | debugMaskSetGet)) > 0)
        	Log.v(TAG, "setWorldSpaceInTransitFromY() " + update);
    }
    
    
    public long getWorldSpaceInTransitTouchPointX() {
    	return Math.round(xWorldSpaceInTransitTouchPoint);
    }
    public void setWorldSpaceInTransitTouchPointX(long update) {
    	xWorldSpaceInTransitTouchPoint = update;

    	if((debugMask & (debugMaskAll | debugMaskSetGet)) > 0)
        	Log.v(TAG, "setWorldSpaceInTransitFromX() " + update);
    }
    public long getWorldSpaceInTransitTouchPointY() {
    	return Math.round(yWorldSpaceInTransitTouchPoint);
    }
    public void setWorldSpaceInTransitTouchPointY(long update) {
    	yWorldSpaceInTransitTouchPoint = update;

    	if((debugMask & (debugMaskAll | debugMaskSetGet)) > 0)
        	Log.v(TAG, "setWorldSpaceInTransitTouchPointY() " + update);
    }
    
    

    public void setInTransitTileState(boolean inTransitState, long moveX, long moveY) {
        if((debugMask & (debugMaskAll | debugMaskStartEnd)) > 0)
        	Log.v(TAG, "onCreate() - Start");

        if(!getInTransitTileState())
    	{
        	if(inTransitState)
        	{
	    		// reset initial point
        		setWorldSpaceInTransitTouchPointX(moveX);
        		setWorldSpaceInTransitTouchPointY(moveY);
	
	    		// first in transit move uses origin
	    		setWorldSpaceInTransitFromX( getWorldSpaceX() );
	    		setWorldSpaceInTransitFromY( getWorldSpaceY() );
	    		
//				xWorldSpaceInTransitOffset = xWorldSpaceInTransitTouchPoint - getWorldSpaceInTransitFromX();
//				yWorldSpaceInTransitOffset = yWorldSpaceInTransitTouchPoint - getWorldSpaceInTransitFromY();
				xWorldSpaceInTransitOffset = 0;
				yWorldSpaceInTransitOffset = 0;
	    		
	        	if((debugMask & (debugMaskAll | debugMaskInTransit)) > 0)
		    		Log.v(TAG, "setInTransitTileState() id:" + getId() + "  relative(" + xWorldSpaceInTransitOffset + "," + yWorldSpaceInTransitOffset + ") origin(" + getWorldSpaceX() + "," + getWorldSpaceY() + ") from(" + xWorldSpaceInTransitFrom + "," + yWorldSpaceInTransitFrom + ")" + " to(" + xWorldSpaceInTransitTouchPoint + "," + yWorldSpaceInTransitTouchPoint + ")");
        	}
    	}
//    	else
//          moved(true);

        setInTransitTileState(inTransitState);
        
        if((debugMask & (debugMaskAll | debugMaskStartEnd)) > 0)
        	Log.v(TAG, "onCreate() - End");
    }

	public void setInTransitTile(int moveX, int moveY) {
		Log.v(TAG, "setInTransitTile()       id:" + getId() + "  move(" + moveX + "," + moveY + ")");
        if(getInTransitTileState())
    	{
			// from point becomes the previous to point
			//setWorldSpaceInTransitFromX( getWorldSpaceInTransitFromX() + xWorldSpaceInTransitOffset);
			//setWorldSpaceInTransitFromY( getWorldSpaceInTransitFromY() + yWorldSpaceInTransitOffset);

			setWorldSpaceInTransitFromX(xWorldSpaceInTransitTouchPoint);
			setWorldSpaceInTransitFromY(yWorldSpaceInTransitTouchPoint);
			
			xWorldSpaceInTransitTouchPoint = moveX;
	    	yWorldSpaceInTransitTouchPoint = moveY;
	
			xWorldSpaceInTransitOffset = xWorldSpaceInTransitTouchPoint - getWorldSpaceInTransitFromX();
			yWorldSpaceInTransitOffset = yWorldSpaceInTransitTouchPoint - getWorldSpaceInTransitFromY();
	
	    	if((debugMask & (debugMaskAll | debugMaskInTransit)) > 0)
	    		Log.v(TAG, "setInTransitTile()       id:" + getId() + "  relative(" + xWorldSpaceInTransitOffset + "," + yWorldSpaceInTransitOffset + ") origin(" + getWorldSpaceX() + "," + getWorldSpaceY() + ") from(" + getWorldSpaceInTransitFromX() + "," + getWorldSpaceInTransitFromY() + ")" + " to(" + xWorldSpaceInTransitTouchPoint + "," + yWorldSpaceInTransitTouchPoint + ")");
	
	    	moved(true);
    	}
        else
	    	if((debugMask & (debugMaskAll | debugMaskInTransit)) > 0)
	    		Log.v(TAG, "setInTransitTile() NOT MOVED WARNING!");
    }

    public boolean getInTransitTileState() {
    	return(inTransit);
    }

    public void setInTransitTileState(boolean state) {
    	inTransit = state;
    	
    	if(!inTransit)
    	{
            xWorldSpaceInTransitOffset = 0;
    		yWorldSpaceInTransitOffset = 0;
    		xWorldSpaceInTransitTouchPoint = 0;
    		yWorldSpaceInTransitTouchPoint = 0;
    		setWorldSpaceInTransitFromX(0);
    		setWorldSpaceInTransitFromY(0);
    	}
    }

    public TilePrintableAndroid(int tileId, int colours[], int scale) {
        super(tileId, colours, scale);  // Tile constructor

        tileSegmentPath[0] = new Path();
        tileSegmentPath[1] = new Path();
        tileSegmentPath[2] = new Path();
        tileSegmentPath[3] = new Path();
        
        for (int segment = 0; segment < tileSegment[0].segmentPoints; segment++) {
            // prevent duplication translation
            if (moved()) {
                //System.out.printf("Tile %d moved x=%d y=%d\n", id, (int)x, (int)y);
                for (int pointsIndex = 0; pointsIndex < tileSegment[0].segmentPoints; pointsIndex++) {
                    // update print poly array
                    tileSegment[segment].xPointArrayPrint[pointsIndex] = tileSegment[getOrientation(segment)].xPointArray[pointsIndex] + (int) getWorldSpaceX();
                    tileSegment[segment].yPointArrayPrint[pointsIndex] = tileSegment[getOrientation(segment)].yPointArray[pointsIndex] + (int) getWorldSpaceY();
                }
            }
            
            // update path points
            for (int pointsIndex = 0; pointsIndex < tileSegment[0].segmentPoints; pointsIndex++) {
            	// update print poly array
                if(pointsIndex == 0)
                    tileSegmentPath[segment].moveTo(tileSegment[segment].xPointArray[pointsIndex], tileSegment[segment].yPointArray[pointsIndex]);    // centre point
                else
                    tileSegmentPath[segment].lineTo(tileSegment[segment].xPointArray[pointsIndex], tileSegment[segment].yPointArray[pointsIndex]);
                //System.out.printf("Tile Path segment:%d moved x=%d y=%d\n", segment, tileSegment[segment].xPointArrayPrint[pointsIndex], tileSegment[segment].yPointArrayPrint[pointsIndex]);
            }
            tileSegmentPath[segment].close();

        	
        }
    }
    
    public void translateTilePath(int xWorldSpaceNew, int yWorldSpaceNew) {

    	int xWorldSpaceOffset = (int) (xWorldSpaceNew - getWorldSpaceX());
    	int yWorldSpaceOffset = (int) (yWorldSpaceNew - getWorldSpaceY());

    	Log.v(TAG, "translateTilePath() offset(" + xWorldSpaceOffset + "," + yWorldSpaceOffset + ") origin(" + getWorldSpaceX() + "," + getWorldSpaceY() + ")  new(" + xWorldSpaceNew + "," + yWorldSpaceNew + ")");

    	// autoTestIncrement = 10
    	int maxOffsetStepIncrement = 10;

//    	if( xWorldSpaceOffset > maxOffsetStepIncrement || xWorldSpaceOffset < -1*(maxOffsetStepIncrement) || 
//  			yWorldSpaceOffset > maxOffsetStepIncrement || yWorldSpaceOffset < -1*(maxOffsetStepIncrement) )
//    		Log.v(TAG, "translateTilePath() offset(" + xWorldSpaceOffset + "," + yWorldSpaceOffset + ") origin(" + getWorldSpaceX() + "," + getWorldSpaceY() + ")  new(" + xWorldSpaceNew + "," + yWorldSpaceNew + ")  WARNING");

        for (int segment = 0; segment < tileSegment[0].segmentPoints; segment++) {
    		//tileSegmentPath[segment].offset((int)xWorldSpaceOffset, (int)yWorldSpaceOffset);        // TODO - remove the hard numbers once the WorldSpace are correct, transpose tile to world Location
    		myPathOffset(segment, (int)xWorldSpaceOffset, (int)yWorldSpaceOffset);        // TODO - remove the hard numbers once the WorldSpace are correct, transpose tile to world Location
        }
    }
    
    
    public void myPathOffset(int segment, int xOffset, int yOffset) {
		tileSegmentPath[segment].offset(xOffset, yOffset);        // TODO - remove the hard numbers once the WorldSpace are correct, transpose tile to world Location
    }

    public void translateTileToOrigin() {
    	int xWorldSpaceOffset = (int) (xWorldSpaceInTransitTouchPoint - getWorldSpaceX());
    	int yWorldSpaceOffset = (int) (xWorldSpaceInTransitTouchPoint - getWorldSpaceY());

    	Log.v(TAG, "translateTileToOrigin() - offset:" + xWorldSpaceOffset + "," + yWorldSpaceOffset + " origin(" + xWorldSpaceInTransitTouchPoint + "," + yWorldSpaceInTransitTouchPoint + ")  new(" + getWorldSpaceX() + "," + getWorldSpaceY() + ")");
    	
        for (int segment = 0; segment < tileSegment[0].segmentPoints; segment++) {
    		//tileSegmentPath[segment].offset((int)xWorldSpaceOffset, (int)yWorldSpaceOffset);        // TODO - remove the hard numbers once the WorldSpace are correct, transpose tile to world Location
    		myPathOffset(segment, (int)xWorldSpaceOffset, (int)yWorldSpaceOffset);        // TODO - remove the hard numbers once the WorldSpace are correct, transpose tile to world Location
        }

        // clear in transit parameters
        setInTransitTileState(false);

    }
    
    public void PrintTile(Canvas canvas, int palate[], boolean showTileIdCheckbox, boolean drawRotation, boolean asWireframeTile) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);

        // draw blue with antialiasing turned on
        // create and draw
        // use a Path object to store the 3 line segments
        // use .offset to draw in many locations
        // note: this triangle is not centered at 0,0
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);

        if((getWorldSpaceX() % (getSegmentScale() / 2) > 0) || (getWorldSpaceY() % (getSegmentScale() / 2) > 0))
        	Log.v(TAG, "PrintTile() id:" + getId() + " ****  WorldSpace Corruption detected *** (" + getWorldSpaceX() + "," + getWorldSpaceY() + ")");

        if(getInTransitTileState())
        	Log.v(TAG, "PrintTile() offset id:" + getId() + "       relative(" + xWorldSpaceInTransitOffset + "," + yWorldSpaceInTransitOffset + ") origin(" + getWorldSpaceX() + "," + getWorldSpaceY() + ")  new(" + (getWorldSpaceX() + xWorldSpaceInTransitOffset) + "," + (getWorldSpaceY() + yWorldSpaceInTransitOffset) + ")");
        else
            if (moved())
            	if(firstPrint)
            		Log.v(TAG, "PrintTile() offset id:" + getId() + " --------(" + getWorldSpaceX() + "," + getWorldSpaceY() + ")  - only on init - WARNING" );
        	
        //
        for (int segment = 0; segment < tileSegment[0].segmentPoints; segment++)
        {
            // if tile location has moved, update the print

	            if(getInTransitTileState())
	            {
	            	//tileSegmentPath[segment].offset((int) (xWorldSpaceInTransitOffset), (int) (yWorldSpaceInTransitOffset));        // TODO - remove the hard numbers once the WorldSpace are correct, transpose tile to world Location
	        		myPathOffset(segment, (int)xWorldSpaceInTransitOffset, (int)yWorldSpaceInTransitOffset);        // TODO - remove the hard numbers once the WorldSpace are correct, transpose tile to world Location

	            }
	            else
	            {
	                if (moved()) {
	                	if(firstPrint)
	                	{
	                		//tileSegmentPath[segment].offset((int)getWorldSpaceX(), (int)getWorldSpaceY());        // TODO - remove the hard numbers once the WorldSpace are correct, transpose tile to world Location
	    	        		myPathOffset(segment, (int)getWorldSpaceX(), (int)getWorldSpaceY());        // TODO - remove the hard numbers once the WorldSpace are correct, transpose tile to world Location
	                	}
	                }
	            }
            
            //if (drawRotation) {
            //    rotationZ(tileSegment[segment], this);
            //}

            // TODO replace the following fudge once the colour palate has been initialised correctly 
            paint.setColor(0xffff0000 + (tileSegment[segment].colour * 0x71c));

            // comment the following for wireframe
            if (asWireframeTile) {
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(tileSegmentPath[segment], paint);
            } else {
                // paint solid
                paint.setStyle(Paint.Style.FILL);
                canvas.drawPath(tileSegmentPath[segment], paint);

                
                // prepare for outline
                paint.setStyle(Paint.Style.STROKE);

                if (locked())
                    paint.setColor(Color.RED);
                else // show unlocked tiles
                    paint.setColor(Color.BLACK);

                canvas.drawPath(tileSegmentPath[segment], paint);       // print outline

                paint.setColor(Color.BLACK);
            }
        }
		if(moveComplete)
        {
        	setInTransitTileState(false);
        	moveComplete = false;
        }

		firstPrint = false;
		
		// prevent duplicate offset being applied.
		xWorldSpaceInTransitOffset = 0;
		yWorldSpaceInTransitOffset = 0;
        
/*
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);
        // all share the same centre point
        for (int segment = 0; segment < 4; segment++) {
            tileSegmentPath[segment].offset((int)xWorldSpace + 120, (int)yWorldSpace + 60);        // TODO - remove the hard numbers once the WorldSpace are correct, transpose tile to world Location
            //paint.setColor(Color.rgb(0xFF, 0x0F * segment, 0x0F * segment));  // red??
            paint.setColor(0xffff0000 + (tileSegment[segment].colour * 0x71c));
            //paint.setColor( palate[tileSegment[segment].colour] );
            canvas.drawPath(tileSegmentPath[segment], paint);
        }
*/      
        
        if (showTileIdCheckbox) {
            String stTileId = String.valueOf(super.getId());

            // draw some text using STROKE style
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);

            //String stTileId = String.valueOf(rotationVector);   //
            if (locked()) {
                paint.setColor(Color.RED);
            } else {
                paint.setColor(Color.BLUE);
            }

            paint.setColor(Color.MAGENTA);
            paint.setTextSize(15);
            
            paint.setAntiAlias(true);
            if(getInTransitTileState())
            {
                canvas.drawText(stTileId, (int)xWorldSpaceInTransitTouchPoint-(int)(paint.getTextSize()/2), (int)yWorldSpaceInTransitTouchPoint+(int)(paint.getTextSize()/2), paint);  // TODO - remove the hard numbers once the WorldSpace are correct
            }
            else
            {
                canvas.drawText(stTileId, (int)getWorldSpaceX() - (int)(paint.getTextSize()/2), (int)getWorldSpaceY() + (int)(paint.getTextSize()/2), paint);  // TODO - remove the hard numbers once the WorldSpace are correct
            }
        }
        moved(false);
    }    
}