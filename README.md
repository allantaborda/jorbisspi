# JOrbis SPI
JOrbis SPI is a minimalist version of Javazoom's Vorbis SPI, a JavaSound SPI for JOrbis (an implementation of OGG Vorbis decoder created by JCraft), with only 4 source files and no dependency of Tritonus library.

The original Javazoom's Vorbis SPI used a few Tritonus library classes, some of which are unnecessary in newer versions of Java, since the Java Sound API of the newer versions of Java has implemented some features used in the Tritonus library.

The Tritonus library classes that were still needed were incorporated into the service provider code, while unnecessary source code parts were removed and a small piece of the code was redesigned to stay in a simpler way.

As the original Javazoom's Vorbis SPI as the Tritonus library were licensed under the LGPL 2.1, JOrbis SPI is licensed under that same license.

This program has only a single library dependency, which is the JOrbis library, an implementation of OGG Vorbis decoder created by JCraft.

Replacing the original Javazoom's Vorbis SPI by this minimalist version in a program that used the old servic provider is very simple, just in most of the times replacing references to the JavaZOOM's version in the pom.xml file (if you are using Maven) or replacing the libraries manually, if the program does not use a build tool.

To include JOrbis SPI as a dependency in the pom.xml file (in the case of using Maven), simply include the following excerpt inside the "dependencies" tag of the file:

<dependency>
	<groupId>com.allantaborda</groupId>
	<artifactId>jorbisspi</artifactId>
	<version>1.0.5</version>
</dependency>
