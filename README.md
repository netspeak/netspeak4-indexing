# Netspeak 4 indexing

This project contains all necessities to create a new Netspeak 4 index.

This project is mainly intended for developers that want to build a new Netspeak 4 index from a given data set.


## Build Netspeak from n-gram collection

To build Netspeak from a collection of n-grams you have to provide a dedicated
directory with one or more text files as input. Each of these files have to
list a number of n-grams together with their frequencies, one by line. The
format of a single line is defined as follows:

```
word_1 [SPACE] word_2 [SPACE] ... word_n [TAB] frequency
```

In words: Each line defines an n-gram with its frequency. The delimiter between
the n-gram and the frequency is a single tabulator ('\t'). The delimiter to
separate the n-gram's words is a single whitespace (' '). Lines have to separated
by a new line ('\n'). Empty lines are not allowed except for the very last line.

Note: Follow this specification strictly to prevent parsing errors. In
particular, ensure the single `\t` delimiter between n-gram and frequency.


---

## Contributors

Michael Schmidt (2018 - 2020)

Martin Trenkmann (2008 - 2013)

Martin Potthast (2008 - 2020)

Benno Stein (2008 - 2020)
