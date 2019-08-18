This is the task description.

Create console Data scraper utility which:

1) accepts as command line parameters:

- (must) local file system path to a *.txt file(s)
- (must) data command(s)
- (must) word (or list of words with "," delimiter)
- (nice to have) output verbosity flag, if on then the output should contains information about time spend on data scraping and data processing (-v)
- (nice to have) extract sentences which contain given words (-e)

2) supports the following data processing commands:
- (must) count number of provided word(s) occurrence in file. (-w)
- (must) count number of characters in each file (-c)
- (nice to have) extract sentences which contain given words (-e)

Data processing results should be printed to output for each file separately and for all resources as total.

Unit testing is not required.

Command line parameters example for Java implementation:

`java –jar scraper.jar sample.txt word1,word2 –v –w –c –e`
