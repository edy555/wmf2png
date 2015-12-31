OBJ= WmfConverter.class WmfDecoder.class WmfView.class

all: $(OBJ)

JAVA= java
JAVAC= javac
JAVAC_OPT=  -Xlint:deprecation -Xlint:unchecked

.SUFFIXES: .class .java

.java.class:
	$(JAVAC) $(JAVAC_OPT) $<

test:
	$(JAVA) WmfConverter world.wmf
