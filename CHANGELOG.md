# Change Log

## Version 1.9 (July 19, 2016)
* Classic: Use original fonts, original gun, original 64x64 enemies, no title or help screen.
* Resizable window with pixel scaling.
* Better 3D audio (panning, real-time volume changes).
* Added stats (Hit accuracy, time, etc).
* A few texture cleanups, improved exit texture.
* Restore feature: Show fps.
* Settings (from the console): Depth cueing, auto pixel scaling, audio volume.
* New console font.
* Many rendering/raycast fixes.
* Console history (like bash).
* Improved gun display when dead.
* Create mipmaps at runtime.
* Game over tweaks: Show stats on win, delay before “click to continue”, allow spacebar to continue.
* Lower CPU usage, especially on Java 8.
* Convert to gradle / Android Studio.
* Code cleanup (Java 7, warnings, analyzer issues).
* Code style cleanup (braces, etc).
* Requires Java 7.

## Version 1.8.1 (December 15, 2015)
* Fixed audio crash on some Linux systems

## Version 1.8 (June 6, 2013)
* Made the jar executable
* Hide default cursor

## Version 1.7 (February 11, 2012)
* Open source
* Removed “show fps”
* Removed mute option
* Removed PulpCore. Lost features:
    - No pulpcore.js
    - No audio panning
    - No pack file
    - No Ogg Vorbis
    - Probably other things

## Version 1.6.3 (April 15, 2009)
* "Official" PulpCore 0.11.5 (displaymode fix for Java 1.4.2)

## Version 1.6.2 (April 14, 2009)
* Fixed cursor bug introduced in 1.6.1
* Added depth cueing
* Ported to "almost official" 0.11.5
* Update JOrbis

## Version 1.6.1 (Dec 15, 2008)
* Made a pack file
* Ported to PulpCore 0.11.5

## Version 1.5.3 (Jan 19, 2008)
* Fixed another mute/unmute bug
* Fixed possible division by zero
* Fixed broken Pulp logo

## Version 1.5.2 (Jan 13, 2008)
* Ported to PulpCore 0.10.10

## Version 1.5.1 (Oct 21, 2007)
* Ported to PulpCore 0.10.4 (pulpcore.js fix)

## Version 1.5.0 (Oct 11, 2007)
* Ported to PulpCore 0.10
* Invisible cursor, larger crosshair
* Red health font color if health <= 20
* 3D sound: panning for doors, robot sounds, and generator
* OGG support

## Version 1.4.2 (May 24, 2007)
* Ported to PulpCore 0.9
* New pulpcore.js

## Version 1.4.1 (May 22, 2007)
* Hijack detection fix
* Codebase fix for frames sites

## Version 1.4 (May 20, 2007)
* Ported to PulpCore 0.8

## Version 1.3.2 (April 18, 2007)
* Crosshair is always shown
* Max of 100 ms elapsed per frame

## Version 1.3.1 (March 27, 2007)
* Update to PulpCore 0.7.0
* New Help/About scene
* "New Game" confirm
* Change behavior of "show status", and changed the key to ESCAPE
* First release on pulpgames.net.

## Version 1.3
* Changed the palette gamma (modern monitors are a lot more brighter)
* Anti-aliased gun and message text
* Fixed robot "float" problem
* Higher resolution robots (128x128 instead of 64x64)
* Faster door open time
* Click to fire / New mouse aiming. 
    - Weapons hurt robots less
    - Robots have a 33% chance to fire immediately after getting hurt
    - If an enemy is about to fire, and it gets hurt, there is only a 50% chance of the firing to be interrupted (previously it was 100%) 
* Mute button only shown if the sound system is available
* Sound preference is saved between sessions
* Start screen has a "continue" option if there is a saved level.
* Converted to PulpCore architecture
* Handlers used instead of new Threads

## Version 1.2 (October 6, 2006)
* Changed view size to 550x400
* Game now appears smoother at higher frame rates.
* New crosshair appearance.
* New gun appearance.
* New "Window"/fence appearance.
* Blast is shown if the player's laser hits a wall. This helps players aim.
* Gun bobbing while running.
* Door-side texture.
* Actions are performed automatically - doors open when you are near them, wall switches activate when you bump against them.
* Fire weapon action is now binded to the spacebar.
* Doors open faster and allow the player to move through them and fire though them when they are 3/4 open.
* Controls are smoother (slower turn velocity), which helps players aim.
* Updated laser-fire sound.
* A sound is now heard when the player has no ammo.
* Fixed aiming issues.
* Rearranged the levels to push harder levels to later in the game.
* New in-game console (no longer a popup window).
* In-game Mute button.
* In-game help.
* New startup screen.
* New splash screen.
* Better sound engine for Java 1.3+.
* Robust downloading, with retry on failure.

## Version 1.1.7 (October 19, 2004)
* Java 1.4 and Java 5: Allowed Tab key
* Java 5: Fixed sound problem. 
* Java 5: Uses High-resolution timer for smoother graphics and gameplay.

## Version 1.1.6 (May 1, 2002)
* Fixed a bug introduced in the last version: gameplay is jerky in on some machines.

## Version 1.1.5 (April 19, 2002)
* Allowed "translate.google.com" as a doc host
* Fixed sounds and performance problems on java 1.4.0
* Fixed hang-bug that appeared most noticeably on level 3.
* Capped the frame rate to 60fps.
 
## Version 1.1.4 (August 9, 1999)
* Added security features. (The code must be on a particular host and the html document must be on a particular host)

## Version 1.1.3 (June 6, 1999)
* Changed the game controls based on user feedback. 
* Fixed yet another DoorHandler bug for people with fast Internet access (like a cable modem). The DoorHandler code kinda assumed it took a second or two to load a level, but that wasn't always the case, and it ended up hogging the processor. 
* Added a "Load Sounds" checkbox at the beginning, so you can choose whether you want to load the sounds on startup. 
* When you get hit, the border of the game will flash. This is very useful if you play the game without sound. 
* If the game loses input focus (like if you click on another part of the   page), the a message will pop up telling you to click on the game. 
* Made the game controls list a little more comprehensive. 

## Version 1.1.2 (October 26, 1998)
* Hold tab to see stats
* Added strafing
* Fixed problem with DoorHandler/MovableWallHandler if starting Java threads on your system is slow.
* All the class files are now stored in a compressed JAR, so it's faster to download.

## Version 1.1.1 (July 31, 1998)
* Smoother framerate
* Fixed bug when pressing reload/refresh in the browser
* Warning for users with Java 1.0
* Fixed framerate calculation

## Version 1.1 (March 22, 1998)
* Fixed MovableWallHandler bug which was the same as the DoorHandler bug. 
* Fixed several aiming bugs. You can now kill an enemy with 2-4 shots, depending on how good your aim is. 
* Keys now appear on the screen when you get them. 
* Press 'x' to show the aiming crosshair. 
* Created the Scared console. Cheat codes, anyone? 
* Redid and/or touched up a few of the images. 
* Added new item: Nuclear Health (appears on levels 3 and 5) 
* Made a better death sequence. Before, the game froze when you died. Now, you fall to the ground and cannot move, but you can turn your head to see who killed you. 
* Fixed bug with the secret walls (movable walls) that put the texture of the secret wall on the floor when it moved. 
* Added three more levels and touched up the existing three levels. Also, I rearranged the order of the levels. Level 3 is now Level 5. 

## Version 1.0.1 (March 10, 1998)
* Fixed DoorHandler bug appearing on some Windows NT systems. 

## Version 1.0 (March 1, 1998)
* Initial Release 
