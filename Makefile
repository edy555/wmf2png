OBJ= WmfConverter.class WmfDecoder.class WmfDecObj.class WmfDecDC.class

JAVA= java
JAVAC= javac
JAVAC_OPT=  -Xlint:deprecation -Xlint:unchecked
JAR= jar

all: WmfConverter.jar

WmfConverter.jar: $(OBJ) manifest.mf
	$(JAR) cfm $@ manifest.mf $(OBJ) 

.SUFFIXES: .class .java

.java.class:
	$(JAVAC) $(JAVAC_OPT) $<

test: WmfConverter.jar
	time $(JAVA) -jar WmfConverter.jar world.wmf
	ls -l world.*

clean:
	$(RM) -rf *.class *.jar
