OBJ= WmfConverter.class WmfDecoder.class WmfDecObj.class WmfDecDC.class

JAVA= java
JAVAC= javac
JAVAC_OPT=  -Xlint:deprecation -Xlint:unchecked
JAR= jar

all: wmf2png

wmf2png: wmf2png.sh WmfConverter.jar
	cat wmf2png.sh WmfConverter.jar >> $@
	chmod +x $@

WmfConverter.jar: $(OBJ) manifest.mf
	$(JAR) cfm $@ manifest.mf $(OBJ) 

.SUFFIXES: .class .java

.java.class:
	$(JAVAC) $(JAVAC_OPT) $<

test: WmfConverter.jar
	#time $(JAVA) WmfConverter world.wmf
	#time $(JAVA) -jar WmfConverter.jar 
	time ./wmf2png world.wmf
	ls -l world.*

clean: wmf2png
	$(RM) -rf *.class *.jar
