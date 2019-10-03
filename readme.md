uTurn (Android)
=============== 

Experimental tesylation game for Android



#### uTurn Application Icon: 
 !["Image of Main Screen"](https://github.com/deanclark/uTurnAnd/blob/master/doc-files/iconLarge.png)

#### Main Board: 
 !["Image of Main Screen"](https://github.com/deanclark/uTurnAnd/blob/master/doc-files/iconLargeFlat.png)

#### Main Screen: 
 !["Image of Main Screen"](https://github.com/deanclark/uTurnAnd/blob/master/doc-files/ScreenShot-Main-2016-04-26.png)

#### Options Screen: 
 !["Image of Main Screen"](https://github.com/deanclark/uTurnAnd/blob/master/doc-files/ScreenShot-Options-2016-04-26.png)


Controls
-----------

* Click a tile to rotate it clockwise by one segment.
* Move a tile by swapping it with the tile directly benith the tile to be dropped



Running the application
-----------

 
#### To compile:

*     ant

#### To run:
   
   * from within Eclipse right click the AndroidManifest.xml and Run As->Android Application
    

#### To rebuild Eclipse Project:
*     mvn eclipse:clean eclipse:eclipse -DdownloadSources -Declipse:useProjectReferences=false -Dwtpversion=2.0

#### JUnit
* 	from within Eclipse right click the AndroidManifest.xml and Run As->Android Test
	
	
#### Ant build of jar file
*     ant createjar
