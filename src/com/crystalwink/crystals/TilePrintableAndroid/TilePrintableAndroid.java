package com.crystalwink.crystals.TilePrintableAndroid;

import com.crystalwink.crystalscommon.Tile.Tile;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Tile with Android Canvas drawing.
 *
 * A tile is always drawn from its absolute position (the world space centre, or the touch point while it is being
 * dragged), never by accumulating offsets onto a stored path. That keeps tiles exactly on the grid however often they
 * are moved, swapped or dragged.
 *
 * @author Dean Clark deancl
 */
public class TilePrintableAndroid extends Tile {

    // true while the tile is being dragged; it is then drawn at the touch point instead of its world space centre
    private boolean inTransit = false;
    private long xWorldSpaceInTransitTouchPoint = 0;
    private long yWorldSpaceInTransitTouchPoint = 0;

    private final Path segmentPath = new Path();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public TilePrintableAndroid(int tileId, int colours[], int scale) {
        super(tileId, colours, scale);  // Tile constructor
        paint.setStrokeWidth(2);
    }

    public long getWorldSpaceX() {
        return Math.round(xWorldSpace);
    }

    public void setWorldSpaceX(long update) {
        xWorldSpace = update;
    }

    public long getWorldSpaceY() {
        return Math.round(yWorldSpace);
    }

    public void setWorldSpaceY(long update) {
        yWorldSpace = update;
    }

    public long getWorldSpaceInTransitTouchPointX() {
        return xWorldSpaceInTransitTouchPoint;
    }

    public long getWorldSpaceInTransitTouchPointY() {
        return yWorldSpaceInTransitTouchPoint;
    }

    public boolean getInTransitTileState() {
        return inTransit;
    }

    /** Start (or stop) dragging; the tile is initially drawn where it already is. */
    public void setInTransitTileState(boolean inTransitState, long moveX, long moveY) {
        inTransit = inTransitState;
        xWorldSpaceInTransitTouchPoint = moveX;
        yWorldSpaceInTransitTouchPoint = moveY;
    }

    public void setInTransitTileState(boolean state) {
        inTransit = state;
        if (!state) {
            xWorldSpaceInTransitTouchPoint = 0;
            yWorldSpaceInTransitTouchPoint = 0;
        }
    }

    /** Move a dragged tile so that its centre is drawn at the given point. */
    public void setInTransitTile(int moveX, int moveY) {
        if (inTransit) {
            xWorldSpaceInTransitTouchPoint = moveX;
            yWorldSpaceInTransitTouchPoint = moveY;
        }
    }

    public void PrintTile(Canvas canvas, int palate[], boolean showTileIdCheckbox, boolean drawRotation, boolean asWireframeTile) {
        long centreX = inTransit ? xWorldSpaceInTransitTouchPoint : getWorldSpaceX();
        long centreY = inTransit ? yWorldSpaceInTransitTouchPoint : getWorldSpaceY();

        for (int segment = 0; segment < Tile.segmentCount; segment++) {
            Segment points = new Segment(tileSegment[segment].xPointArray, tileSegment[segment].yPointArray);
            segmentPath.reset();
            for (int index = 0; index < 3; index++) {   // corner, centre, corner (the 4th point is the wire link)
                float x = centreX + points.x[index];
                float y = centreY + points.y[index];
                if (index == 0) {
                    segmentPath.moveTo(x, y);
                } else {
                    segmentPath.lineTo(x, y);
                }
            }
            segmentPath.close();

            int edgeColorId = tileSegment[segment].colour;
            paint.setColor(edgeColorId >= 0 && edgeColorId < palate.length ? palate[edgeColorId] : Color.DKGRAY);

            if (asWireframeTile) {
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(segmentPath, paint);
            } else {
                paint.setStyle(Paint.Style.FILL);
                canvas.drawPath(segmentPath, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(locked() ? Color.RED : Color.BLACK);
                canvas.drawPath(segmentPath, paint);
            }
        }

        if (showTileIdCheckbox) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);
            paint.setColor(Color.MAGENTA);
            paint.setTextSize(15);
            canvas.drawText(String.valueOf(getId()), centreX - paint.getTextSize() / 2, centreY + paint.getTextSize() / 2, paint);
            paint.setStrokeWidth(2);
        }
        moved(false);
    }

    private static final class Segment {
        final int[] x;
        final int[] y;

        Segment(int[] x, int[] y) {
            this.x = x;
            this.y = y;
        }
    }
}
