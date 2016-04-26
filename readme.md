uTurn (Android)
=============== 

Experimental tesylation game for Android



#### uTurn Application Icon: 
![alt text][2][1]
  [2]: doc-files/iconLarge.png
  [1]: https://github.com/deanclark/uTurnAnd/doc-files/iconLarge.png "Image of Main Screen"

#### Main Board: 
![alt text][4][3]
  [4]: doc-files/iconLargeFlat.png
  [3]: https://github.com/deanclark/uTurnAnd/doc-files/iconLargeFlat.png "Image of Main Screen"

#### Main Screen: 
![alt text][6][5]
  [6]: doc-files/ScreenShot-Main-2016-04-26.png
  [5]: https://github.com/deanclark/uTurnAnd/doc-files/ScreenShot-Main-2016-04-26.png "Image of Main Screen"

#### Options Screen: 
![alt text][8][7]
  [8]: doc-files/ScreenShot-Options-2016-04-26.png
  [7]: https://github.com/deanclark/uTurnAnd/doc-files/ScreenShot-Options-2016-04-26.png "Image of Main Screen"


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
