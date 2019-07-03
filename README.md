# truffler
Derivative of TruffleHog

### Use Maven to build and install this kit into a maven repository.

*  i.e.: ```mvn clean install```
*  In some project, reference the kit via a pom dependency in test scope.


### In some project, create a test class that inherits from TruffleHunt (a JUnit test):

    package example;

    import com.brentcroft.trufflehog.TruffleHunt;

    public class TrufflerTest extends TruffleHunt
    {
    }

*  Look at TruffleHunt and maybe override, or enhance, it's @Before method


### Run the test and open the test report file in a browser:

    target/truffler/truffler.html

*  Select known high-entropy strings to ignore
*  Select sensitive strings to be replaced
*  Select sensitive files to be removed


### Create a file to store known strings (explicitly publishing them):

    src/test/resources/truffle/entropy-known-strings.txt

*  Populate the file with strings, one per line
*  Re-run the test and inspect the report
*  Eventually, any remaining issues must be resolved by rewriting history


## Rewriting History
This uses [BFG](https://rtyley.github.io/bfg-repo-cleaner/)

Create a new directory and:
*  copy the windows script file **rewrite-history.cmd** into it.
*  download the bfg jar file into the new directory.

Create a text file in the new directory named:

    strings-to-replace.txt

*  copy strings to be replaced from the report
*  NB: this file is deleted immediately after use

Modify the script to set the repository directory name (aka project)
and the modify the url, from the new directory, to the current directory ".git" directory.

Run the script.

Two directories are created:

*  ```<project>.git```
The bare mirror repository that was edited.

*  ```<project>```
A new clone from the edited mirror repository.

Review and test the ```<project>``` directory and if satisfactory, then push to the origin.

*  For example, run the TruffleHunt test in ```<project>``` and review the report to see that the sensitive strings no longer exist
*  Both these directories must be deleted, and the file ```strings-to-replace.txt``` re-created, before the process can be repeated
