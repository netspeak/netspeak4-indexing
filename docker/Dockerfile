FROM openjdk:8-jdk-buster AS builder

ENV DEBIAN_FRONTEND noninteractive

RUN mkdir /netspeak4-indexing
WORKDIR /netspeak4-indexing
RUN apt-get install git -y

# Checkout a specific commit instead of a specific branch.
# This makes builds deterministic.
RUN git clone https://github.com/netspeak/netspeak4-indexing.git . \
	&& git checkout c0f52d2e73300574dc6170470bc936d75b8a3795

# Build
RUN chmod +x gradlew \
	&& ./gradlew build


# Now for the real image.
#
# This image is based on a JRE image instead of a JDK image to make the final image smaller.
# We only need to be able to run Java applications, not compile them.

FROM openjdk:8-jre-buster

ENV DEBIAN_FRONTEND noninteractive

ARG FILE_DIR=/netspeak4-indexing-files

RUN mkdir $FILE_DIR
WORKDIR $FILE_DIR

COPY --from=builder /netspeak4-indexing/build/distributions/netspeak4-indexing-1.0.tar $FILE_DIR/

# Unpack and setup executable
#
# The "executable" in /usr/local/bin will simply be a script calling the script that starts netspeak4-indexing.
# That's the easiest way because netspeak4-indexing-1.0/bin/netspeak4-indexing script uses relative paths, so hard links
# and symbolic links won't work.
RUN tar -xvf netspeak4-indexing-1.0.tar \
	# we don't need the tar anymore
	&& rm netspeak4-indexing-1.0.tar \
	&& echo "#!/usr/bin/env sh\nsh $FILE_DIR/netspeak4-indexing-1.0/bin/netspeak4-indexing \"\$@\"" > /usr/local/bin/netspeak4-indexing \
	&& chmod +x /usr/local/bin/netspeak4-indexing

CMD netspeak4-indexing --help
