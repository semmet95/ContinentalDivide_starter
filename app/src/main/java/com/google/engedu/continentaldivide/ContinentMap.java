/* Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.engedu.continentaldivide;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;


public class ContinentMap extends View {
    public static final int MAX_HEIGHT = 100;
    private Cell[] map;
    private int boardSize;
    private Random random = new Random();
    private int maxHeight = 0, minHeight = 0;

    private Integer[] DEFAULT_MAP = {
            50, 50, 50, 50, 60,
            50, 22, 26, 70, 50,
            50, 24, 30, 30, 29,
            50, 28, 28, 29, 22,
            60, 50, 50, 50, 50,
    };

    private static float[] grey={0, 0, 0}, blue={215, 100, 0}, red={0, 100, 0}, green={130, 100, 0};
    private Paint painter;
    private boolean rotated=false;

    private Cell previousCell=null;

    public ContinentMap(Context context) {
        super(context);
        painter=new Paint();
        //isSolving=false;
        boardSize = (int) (Math.sqrt(DEFAULT_MAP.length));
        map = new Cell[boardSize * boardSize];
        for (int i = 0; i < boardSize * boardSize; i++) {
            map[i] = new Cell();
            map[i].height = DEFAULT_MAP[i];
        }
        maxHeight = Collections.max(Arrays.asList(DEFAULT_MAP));
        minHeight=Collections.min(Arrays.asList(DEFAULT_MAP));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int size = width > height ? height : width;
        setMeasuredDimension(size, size);
    }

    private class Cell {
        protected int height = 0;
        protected boolean flowsNW = false;
        protected boolean flowsSE = false;
        protected boolean basin = false;
        protected boolean processing = false;

        boolean dependency=false;
        int dependencyX=0, DependencyY=0;
    }

    private Cell getMap(int x, int y) {
        if (x >=0 && x < boardSize && y >= 0 && y < boardSize)
            return map[x + boardSize * y];
        else
            return null;
    }

    public void clearContinentalDivide() {
        for (int i = 0; i < boardSize * boardSize; i++) {
            map[i].flowsNW = false;
            map[i].flowsSE = false;
            map[i].basin = false;
            map[i].processing = false;
        }
        rotated=false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int sideLength=(int)((canvas.getWidth()/boardSize)/Math.sqrt(2));
        if(!rotated) {

            rotated=true;
        }
        super.onDraw(canvas);
        for (int i = 0; i < boardSize; i++) {
            for(int j=0;j<boardSize;j++) {
                Cell tempCell=getMap(j, i);
                int height=tempCell.height;
                if(tempCell.flowsNW&&tempCell.flowsSE) {
                    tempCell.basin=false;
                    red[2]=50+(50*(height-maxHeight)/(minHeight-maxHeight));
                    red[2]/=100;
                    painter.setColor(Color.HSVToColor(red));
                } else if(tempCell.flowsNW) {
                    tempCell.basin=false;
                    green[2]=50+(50*(height-maxHeight)/(minHeight-maxHeight));
                    green[2]/=100;
                    painter.setColor(Color.HSVToColor(green));
                } else if(tempCell.flowsSE) {
                    tempCell.basin=false;
                    blue[2]=50+(50*(height-maxHeight)/(minHeight-maxHeight));
                    blue[2]/=100;
                    painter.setColor(Color.HSVToColor(blue));
                } else {
                    //tempCell.basin=true;
                    grey[2]=50+(50*(height-maxHeight)/(minHeight-maxHeight));
                    grey[2]/=100;
                    painter.setColor(Color.HSVToColor(grey));
                }
                canvas.save();
                canvas.rotate(45, 0, sideLength*boardSize);
                //Log.e("drawing :", "hsv values = "+grey[0]+" "+grey[1]+" "+grey[2]+" and Color = "+Color.HSVToColor(grey));
                canvas.drawRect(j*sideLength, (int)(i*sideLength-(sideLength/Math.sqrt(2))), (j+1)*sideLength, (int)((i+1)*sideLength-(sideLength/Math.sqrt(2))), painter);
                canvas.restore();
            }
        }
    }

    public void buildUpContinentalDivide(boolean oneStep) {
        if (!oneStep)
            clearContinentalDivide();
        boolean iterated = false;
        for (int x = 0; x < boardSize; x++) {
            for (int y = 0; y < boardSize; y++) {
                Cell cell = getMap(x, y);
                if ((x == 0 || y == 0 || x == boardSize - 1 || y == boardSize - 1)) {
                    //Log.e("building up :", "x = "+x+" y = "+y);
                    buildUpContinentalDivideRecursively(
                            x, y, x == 0 || y == 0, x == boardSize - 1 || y == boardSize - 1, -1);
                    if (oneStep) {
                        iterated = true;
                        break;
                    }
                }
            }
            if (iterated && oneStep)
                break;
        }
        invalidate();
    }

    private void buildUpContinentalDivideRecursively(
            int x, int y, boolean flowsNW, boolean flowsSE, int previousHeight) {
        Cell currentCell=getMap(x, y);
        if(currentCell.height>=previousHeight&&!currentCell.processing) {
            currentCell.processing=true;
            if(flowsNW)
                currentCell.flowsNW=true;
            if(flowsSE)
                currentCell.flowsSE=true;
            if(x-1>=0)
                buildUpContinentalDivideRecursively(x-1, y, flowsNW, flowsSE, currentCell.height);
            if(y-1>=0)
                buildUpContinentalDivideRecursively(x, y-1, flowsNW, flowsSE, currentCell.height);
            if(y+1<boardSize)
                buildUpContinentalDivideRecursively(x, y+1, flowsNW, flowsSE, currentCell.height);
            if(x+1<boardSize)
                buildUpContinentalDivideRecursively(x+1, y, flowsNW, flowsSE, currentCell.height);
            currentCell.processing=false;
        }
    }

    public void buildDownContinentalDivide(boolean oneStep) {
        int ctr=0;
        if (!oneStep)
            clearContinentalDivide();
        while (true) {

            int maxUnprocessedX = -1, maxUnprocessedY = -1, foundMaxHeight = -1;
            for (int y = 0; y < boardSize; y++) {
                for (int x = 0; x < boardSize; x++) {
                    Cell cell = getMap(x, y);

                    if (!(cell.flowsNW || cell.flowsSE || (cell.basin&&!cell.dependency)) && cell.height > foundMaxHeight) {
                        //Log.e("found :", "max height = "+cell.height);
                        maxUnprocessedX = x;
                        maxUnprocessedY = y;
                        foundMaxHeight = cell.height;
                    }
                }
            }
            if (maxUnprocessedX != -1) {
                ++ctr;
                Log.e("visiting :", "height = "+getMap(maxUnprocessedX, maxUnprocessedY).height);
                Cell tempCell=getMap(maxUnprocessedX, maxUnprocessedY);
                if(tempCell.dependency&&tempCell.basin) {
                    ArrayList<Cell> neighs=new ArrayList<>();
                    if(maxUnprocessedX-1>=0) {
                        Cell tempC=getMap(maxUnprocessedX-1, maxUnprocessedY);
                        neighs.add(tempC);
                    }
                    if(maxUnprocessedY-1>=0) {
                        Cell tempC=getMap(maxUnprocessedX, maxUnprocessedY-1);
                        neighs.add(tempC);
                    }
                    if(maxUnprocessedY+1<boardSize) {
                        Cell tempC=getMap(maxUnprocessedX, maxUnprocessedY+1);
                        neighs.add(tempC);
                    }
                    if(maxUnprocessedX+1<boardSize) {
                        Cell tempC=getMap(maxUnprocessedX+1, maxUnprocessedY);
                        neighs.add(tempC);
                    }
                    for(Cell x: neighs) {
                        if(x.height==tempCell.height) {
                            tempCell.flowsSE=x.flowsSE;
                            tempCell.flowsNW=x.flowsNW;
                            tempCell.dependency=false;
                            if(!tempCell.flowsNW&&!tempCell.flowsSE)
                                tempCell.basin=true;
                            else
                                tempCell.basin=false;
                        }
                    }
                    continue;
                }
                buildDownContinentalDivideRecursively(maxUnprocessedX, maxUnprocessedY, foundMaxHeight + 1);
                if (oneStep) {
                    break;
                }
            } else {
                break;
            }
        }
        invalidate();
    }

    private Cell buildDownContinentalDivideRecursively(int x, int y, int previousHeight) {
        if((x==-1||y==-1)||(x==boardSize||y==boardSize)) {
            Cell tempCell=new Cell();
            tempCell.height=-1;
            if(x==-1||y==-1)
                tempCell.flowsNW=true;
            if(x==boardSize||y==boardSize)
                tempCell.flowsSE=true;
            return tempCell;
        }
        Cell workingCell = getMap(x, y);
        if(workingCell.height<=previousHeight&&!workingCell.processing) {
            workingCell.processing=true;
            workingCell.dependency=false;
            ArrayList<Cell> neighbors=new ArrayList<>();
            //Log.e("previousCell :", "previousCell's x = "+x+" y = "+y);
            //previousCell=workingCell;
            //Log.e("buildingDown :", "x = "+x+" y = "+y+" value = "+workingCell.height+" NW = "+workingCell.flowsNW+" SE = "+workingCell.flowsSE);
            if(x-1>=-1) {
                if(x-1>=0) {
                    Cell temp = getMap(x - 1, y);
                    if (temp.height == workingCell.height && temp.processing)
                        workingCell.dependency = true;
                }
                neighbors.add(buildDownContinentalDivideRecursively(x - 1, y, workingCell.height));
            }
            if(y-1>=-1) {
                if(y-1>=0) {
                    Cell temp = getMap(x, y - 1);
                    if (temp.height == workingCell.height && temp.processing)
                        workingCell.dependency = true;
                }
                neighbors.add(buildDownContinentalDivideRecursively(x, y - 1, workingCell.height));
            }
            if(y+1<=boardSize) {
                if(y+1<boardSize) {
                    Cell temp = getMap(x, y + 1);
                    if (temp.height == workingCell.height && temp.processing)
                        workingCell.dependency = true;
                }
                neighbors.add(buildDownContinentalDivideRecursively(x, y + 1, workingCell.height));
            }
            if(x+1<=boardSize) {
                if(x+1<boardSize) {
                    Cell temp = getMap(x + 1, y);
                    if (temp.height == workingCell.height && temp.processing)
                        workingCell.dependency = true;
                }
                neighbors.add(buildDownContinentalDivideRecursively(x + 1, y, workingCell.height));
            }
            boolean basinCondition=true;
            for(Cell temp: neighbors) {
                if (temp==null||temp.basin)
                    continue;
                basinCondition=false;
                //Log.e("neighbors :", " value = "+temp.height+" NW = "+temp.flowsNW+" SE = "+temp.flowsSE);
                if(temp.flowsNW)
                    workingCell.flowsNW=true;
                if(temp.flowsSE)
                    workingCell.flowsSE=true;
            }
            workingCell.processing=false;
            //Log.e("basin :", "setting basin = "+basinCondition+" for x = "+x+" y = "+y+" value = "+workingCell.height);
            workingCell.basin=basinCondition;
            return workingCell;
        }
        return null;
    }

    public void generateTerrain(int detail) {
        int newBoardSize = (int) (Math.pow(2, detail) + 1);
        if (newBoardSize != boardSize * boardSize) {
            boardSize = newBoardSize;
            map = new Cell[boardSize * boardSize];
            for (int i = 0; i < boardSize * boardSize; i++) {
                map[i] = new Cell();
                map[i].height=(2*random.nextInt(MAX_HEIGHT))%MAX_HEIGHT;
            }
        }

        invalidate();
    }


}
