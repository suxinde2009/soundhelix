SoundHelix
----------

Author: Thomas Schürger (thomas@schuerger.com)


Introduction
------------

SoundHelix is a framework for algorithmic music creation and playback.

In 1995, I started working on a similar software on the Amiga platform,
called AlgoMusic, which was published on Aminet and was improved over
time. In 1998, I stopped development. Due to the limitations of the
standard Amiga audio hardware, AlgoMusic itself was very limited and
inflexible.

SoundHelix has a similar goal, but strives to remove most (if not all)
of the limitations of AlgoMusic. Due to its nature as a framework, it
is possible to plug-in own implementations of most components.

SoundHelix is a Java rewrite from scratch which offers a whole lot of
flexibility and customization capabilities. It is also a framework
which allows other developers to easily write their own music creation
components and use them within SoundHelix.

Currently SoundHelix features some basic music creation abilities and a MIDI
player, which can access any MIDI resource accessible to Java. Note that
currently some external MIDI software is required for playback. This can be
any MIDI software or hardware which can be accessed using a MIDI driver on
your platform. You can even use the standard Java Software Synthesizer. Please
check the included configuration files.

SoundHelix currently does not provide any internal sound playback capabilities.

But the best of all: SoundHelix is free and fun!


Requirements
------------

End users:

- Java Runtime Environment 5 or later
- a MIDI playback device or MIDI-capable software
- some MIDI hub software (optional)


Developers:

- Java Software Development Kit 5 or later
- ant (http://ant.apache.org)
- Eclipse (optional)


Installing a MIDI hub
---------------------

Some MIDI software (like Propellerhead Reason) does not offer a
MIDI device itself, but can connect to a present MIDI device to receive
MIDI messages. SoundHelix also does not make itself visible as a MIDI device,
so neither can SoundHelix connect to the MIDI software
nor can the MIDI software connect to SoundHelix. In this case, you
must install some MIDI hub software, for example MIDI Yoke (available
for Windows, other platforms offer similar MIDI hub software).

This software simply sets up a MIDI device that is visible from Java
and from any other MIDI software and relays MIDI messages between
SoundHelix and the MIDI software. SoundHelix must be configured
to use MIDI Yoke as the MIDI output device and the MIDI software
must use MIDI Yoke as the MIDI input device. MIDI Yoke provides
up to 8 MIDI ports. It is sufficient to use one of the ports
for SoundHelix. The name of the MIDI Yoke MIDI output port is
"Out To MIDI Yoke:  n" (with n from 1 to 8). Note the extra
space after the colon. This must be used in SoundHelix for
output. The MIDI software can then use "In From MIDI Yoke:  n" for
input.

MIDI Yoke can be found here: http://www.midiox.com/myoke.htm


Configuration
-------------

Configuration is done using an XML file. If no filename is
provided at startup, the file "SoundHelix.xml" is read. A couple
of example configuration files are included in the "examples"
directory.

Details about configuration are still to be written.

Within each XML tag that requires an integer you can use
the following instead of a plain integer:

- <random min="minimum" max="maximum"/>

  to generate a random integer between minimum and maximum
  (both inclusive) with uniform distribution
  
- <random min="minimum" max="maximum" type="normal" mean="mean" variance="variance"/>

  to generate a random integer between minimum and maximum
  (both inclusive) with a Gaussian (normal) distribution having a mean of mean and
  a variance of variance; mean and variance are doubles
  
- <random list="value1,value2,..."/>

  to randomly select one of the specified values with uniform distribution

Within each XML tag that requires a string, you can use
the following instead of a plain string:

- <random list="string1|string2|..."/>

  to randomly select one of the specified strings with uniform distribution


Sourcecode
----------

SoundHelix is Open Source Software with GPL v3 license and is hosted on SourceForge:

http://sourceforge.net/projects/soundhelix/

The most recent sourcecode can be checked out using SVN like this:

svn co https://soundhelix.svn.sourceforge.net/svnroot/soundhelix soundhelix 


More developers welcome
-----------------------

I'm looking for more developers to join this project. If you
have Java and some music skills or documentation skills and think
you can contribute to the project, then please don't hesitate to contact
me.


Developer Resources
-------------------

Java Sound API:
http://java.sun.com/products/java-media/sound/reference/api/index.html

Java MIDI Programming FAQ:
http://www.jsresources.org/faq_midi.html
