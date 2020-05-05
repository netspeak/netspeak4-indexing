# Netspeak 4 indexing

This project contains all necessities to create a new Netspeak 4 index.

This project is mainly intended for developers that want to build a new Netspeak 4 index from a given data set.


---

## Contributors

Michael Schmidt (2018 - 2020)

Martin Trenkmann (2008 - 2013)

Martin Potthast (2008 - 2020)

Benno Stein (2008 - 2020)



---

# Old Notes

% NETSPEAK 4 JAVA NOTES
% martin.trenkmann@uni-weimar.de
% November 22, 2013



Notation
--------

    # <command>   Does need admin permissions (sudo).
    $ <command>   Does not need admin permissions.


Project description
-------------------

<http://www.uni-weimar.de/en/media/chairs/webis/research/projects/netspeak/>


Library dependencies
--------------------

This Java project is a language binding for the C++ project netspeak4-application-cpp whose
implementation comes in form of a shared library (.so file). The present Java
application loads the library at runtime and invokes their native routines via
the Java Native Interface (JNI) method. Precompiled libraries for Ubuntu 10.04
and 12.04 can be found in the lib sub-directory of this project. The native
library itself has some dependencies you need to install as well. To do so run
the following script:

    # <project>/build/install-dependencies.sh


Build and install the native library
------------------------------------

In the case that there is no precompiled native library available for your
platform, you need to compile the corresponding C++ project by yourself.

- Checkout netspeak4-application-cpp from webis CVS.
- Build target "Library" with Qt Creator IDE.

# cp <project>/lib/<arch>/<lib>.so /usr/lib


Load native library
-------------------

Set "-Djava.library.path=/usr/lib" as VM argument.


Build Netspeak from n-gram collection
-------------------------------------

To build Netspeak from a collection of n-grams you have to provide a dedicated
directory with one or more text files as input. Each of these files have to
list a number of n-grams together with their frequencies, one by line. The
format of a single line is defined as follows:

    word_1 SPACE word_2 SPACE ... word_n TAB frequency

In words: Each line defines an n-gram with its frequency. The delimiter between
the n-gram and the frequency is a single tabulator ('\t'). The delimiter to
separate the n-gram's words is a single whitespace (' ').

Note: Follow this specification strictly to prevent parsing errors. In
particular, ensure the single `\t` delimiter between n-gram and frequency.


Getting Started
---------------

- `usage.NetspeakBuilderUsage.java` shows how to build Netspeak from a
   collection of n-grams.
- `usage.NetspeakTerminal.java` runs a simple command line to search a Netspeak
   instance interactively for testing purposes.
- `usage.NetspeakUsage.java` demonstrates how to search Netspeak in more detail
   using the Request and Response objects.

In some cases, if your local hardware, storage space or operating system
(Netspeak runs only on Linux) does not fit, it might be necessary to setup
Netspeak running on a Linux server and to request that instance remotely.

For that reason build your Netspeak application as usual and run it as a Java
servlet, e.g. with Tomcat, using the project `netspeak4-server`. A running
Netspeak server can then be requested with `netspeak3-client-java` project from
any Java application.


Netspeak query language
-----------------------

The Netspeak query syntax as described here should be used as reference. There
might be other syntax information out there, e.g. at netspeak.org, which
provides some syntactical simplifications in form of easier to use wildcards or
operators. However, these modified syntaxes are just front-ends and do not work
with the original Netspeak interface. Here is the truth:

    ?   is a placeholder for exactly one word and can be sequenced to search for
        exaclty two, three, four ... words.

        Example:    how to ? this
                 -> how to use this
                 -> how to do this
                 -> how to cite this

    *   is a placeholder for zero or many words.

        Example:    see * works
                 -> see how it works
                 -> see if it works
                 -> see what works

    []  compares options, i.e. it checks each word or phrase between these
        brackets plus the so called empty word at that position in the query.

        Example:    it's [ great well "so good" ]
                 -> it's
                 -> it's great
                 -> it's well
                 -> it's so good

    {}  checks the order, i.e. it tries to find each permutation of the given
        sequence of words or phrases at that position in the query.

        Example:    for { "very important people" only }
                 -> for very important people only
                 -> for only very important people

    #   searches for alternatives of the word following. This operator requests
        the optional Netspeak hash-dictionary component and uses [] to compare
        each retrieved alternative (except that the empty word is not checked).
        The mapping from word to alternatives is completely up to the user when
        building Netspeak, for netspeak.org we use this operator for a synonym
        search providing the Wordnet dictionary.

        Example:    waiting for #response
                 -> waiting for response
                 -> waiting for answer
                 -> waiting for reply

You can combine the introduced wildcards and operators as you want, but with the
exception that you may not place any wildcard within bracket operators. Also
nested brackets are not allowed. As you can see in the examples above you can
quote phrases to be handled as one entity is `[]` and `{}`.



% Compile via: pandoc from.txt > to.html
